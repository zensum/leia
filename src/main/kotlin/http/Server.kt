package leia.http

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.pipeline.PipelineContext
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import io.ktor.request.host
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.request.receiveStream
import io.ktor.response.respondText
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
import se.zensum.jwt.isVerified
import se.zensum.jwt.token

private val EMPTY_BUF = ByteArray(0)
private val RETURN_EMPTY_BUF: suspend () -> ByteArray = { EMPTY_BUF }

private fun ApplicationRequest.headerInt(name: String): Int =
    header(name)?.toIntOrNull() ?: 0


fun createIncomingRequest(req: ApplicationRequest) =
    IncomingRequest(
        req.httpMethod,
        req.header("Origin"),
        // FIXME: This doesn't verify
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


class Server(
    private val resolver: Resolver,
    private val sinkProvider: SinkProvider
) {
    private suspend fun performLogAppend(logAppend: LogAppend,
                                         call: ApplicationCall) {
        val res = sinkProvider
            .sinkFor(logAppend.sinkDescription)
            .handle(logAppend.request)
        sendSuccessResponse(call, res, logAppend.receipt)
    }

    suspend fun handleRequest(ctx: PipelineContext<Unit, ApplicationCall>) {
        val req = createIncomingRequest(ctx.context.request)
        val res = resolver.resolve(req)
        when (res) {
            is LogAppend -> performLogAppend(res, ctx.context)
            is ErrorMatch -> sendErrorResponse(res, ctx.context)
            is NoMatch -> sendNotFoundResponse(ctx.context)
        }
    }
}