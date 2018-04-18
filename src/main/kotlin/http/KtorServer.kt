package leia.http

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.install
import io.ktor.http.HttpStatusCode
import io.ktor.pipeline.PipelineContext
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import io.ktor.request.host
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.request.receiveStream
import io.ktor.response.respondText
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import ktor_health_check.Health
import leia.logic.CorsNotAllowed
import leia.logic.ErrorMatch
import leia.logic.Forbidden
import leia.logic.IncomingRequest
import leia.logic.LogAppend
import leia.logic.NoMatch
import leia.logic.NotAuthorzied
import leia.logic.Receipt
import leia.logic.Resolver
import leia.sink.SinkProvider
import leia.sink.SinkResult
import se.zensum.jwt.JWTFeature
import se.zensum.jwt.JWTProvider
import se.zensum.jwt.isVerified
import se.zensum.jwt.token
import se.zensum.ktorPrometheusFeature.PrometheusFeature
import se.zensum.ktorSentry.SentryFeature

private val EMPTY_BUF = ByteArray(0)
private val RETURN_EMPTY_BUF: suspend () -> ByteArray = { EMPTY_BUF }

private fun ApplicationRequest.headerInt(name: String): Int =
    header(name)?.toIntOrNull() ?: 0


fun createIncomingRequest(req: ApplicationRequest) =
    IncomingRequest(
        req.httpMethod,
        req.header("Origin"),
        if (req.call.isVerified()) req.call.token() else null,
        req.path(),
        req.host(),
        req.headerInt("Content-Length").let { len ->
            if (len == 0) {
                RETURN_EMPTY_BUF
            } else {
                { req.call.receiveStream().readBytes(len) }
            }
        }
    )


private suspend fun sendErrorResponse(error: ErrorMatch, call: ApplicationCall) {
    val (text, status) = when(error) {
        NotAuthorzied ->
            "unauthorized" to HttpStatusCode.Unauthorized
        Forbidden ->
            "forbidden" to HttpStatusCode.Forbidden
        CorsNotAllowed ->
            "cors not allowed" to HttpStatusCode.Forbidden
    }
    call.respondText(text, status = status)
}

private suspend fun sendNotFoundResponse(call: ApplicationCall) {
    call.respondText(
        "404 - Not found!",
        status = HttpStatusCode.NotFound
    )
}

private suspend fun sendSuccessResponse(call: ApplicationCall,
                                        sr: SinkResult,
                                        receipt: Receipt) {
    when (sr) {
        is SinkResult.WritingFailed ->
            // todo: log res.exc
            call.respondText(
                "Something went wrong",
                status = HttpStatusCode.InternalServerError
            )
        is SinkResult.SuccessfullyWritten ->
            call.respondText(
                receipt.body,
                status = HttpStatusCode.fromValue(receipt.status)
            )
    }
}

class KtorServer private constructor(
    private val resolver: Resolver,
    private val appender: suspend (LogAppend) -> SinkResult,
    private val installPrometheus: Boolean,
    private val jwtProvider: JWTProvider?
) : Server {

    private suspend fun performLogAppend(logAppend: LogAppend,
                                         sinkResult: SinkResult,
                                         call: ApplicationCall) {
        sendSuccessResponse(call, sinkResult, logAppend.receipt)
    }

    suspend fun handleRequest(ctx: PipelineContext<Unit, ApplicationCall>) {
        val req = createIncomingRequest(ctx.context.request)
        val resolveResult = resolver.resolve(req)
        when (resolveResult) {
            is LogAppend -> performLogAppend(
                resolveResult,
                appender(resolveResult),
                ctx.context
            )
            is ErrorMatch -> sendErrorResponse(resolveResult, ctx.context)
            is NoMatch -> sendNotFoundResponse(ctx.context)
        }
    }

    private fun getKtorApplication(): Application.() -> Unit = {
        install(SentryFeature)
        if (installPrometheus) install(PrometheusFeature.Feature)
        install(JWTFeature) {
            jwtProvider?.let {
                jwtProvider(it)
            }
        }
        install(Health)
    }

    companion object : ServerFactory {
        override fun create(resolver: Resolver, sinkProvider: SinkProvider) : Server =
            KtorServer(
                resolver,
                { sinkProvider.handle(it.sinkDescription, it.request) },
                true,
                null
            ).also {
                // do we need a server spec here
                val port = 80
                val module = it.getKtorApplication()
                embeddedServer(Netty, port, module = module).start()
            }
    }
}

