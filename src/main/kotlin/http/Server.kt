package leia.http

import com.auth0.jwt.JWT
import io.ktor.application.ApplicationCall
import io.ktor.features.origin
import io.ktor.pipeline.PipelineContext
import io.ktor.request.ApplicationRequest
import io.ktor.request.authorization
import io.ktor.request.header
import io.ktor.request.host
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.request.receiveStream
import leia.logic.IncomingRequest
import leia.logic.Resolver
import se.zensum.jwt.isVerified
import se.zensum.jwt.token
import se.zensum.leia.config.TopicRouting

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

class Server(
    private val resolver: Resolver
) {

    fun handleRequest(ctx: PipelineContext<Unit, ApplicationCall>) {
        val req = createIncomingRequest(ctx.context.request)
        resolver.resolve(req)
        // TODO: handle the resolver result.
    }
}