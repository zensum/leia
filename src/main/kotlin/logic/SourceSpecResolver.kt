package leia.logic

import io.ktor.http.HttpMethod
import se.zensum.leia.auth.AuthProvider
import se.zensum.leia.auth.AuthResult
import se.zensum.leia.config.SourceSpec

// A resolver that resolves an incoming request against a single source-spec
// object.
class SourceSpecResolver(private val cfg: SourceSpec, private val auth: AuthProvider) : Resolver {
    override fun resolve(req: IncomingRequest) : Result {
        return when {
            // TODO: Add hostname restriction here
            !req.matchPath(cfg.path) -> NoMatch
            !req.matchCors(cfg.corsHosts) -> CorsNotAllowed
            // TODO: We need to be able to tell this apart from explicitly allowed
            cfg.corsHosts.isNotEmpty() && req.method == HttpMethod.Options -> CorsPreflightAllowed
            !cfg.allowedMethodsSet.contains(req.method) -> NoMatch
            else -> authAndAppendToLog(req)
        }
    }

    private fun authAndAppendToLog(req: IncomingRequest): Result {
        val userId: String? = cfg.authenticateUsing
            .takeIf { it.isNotEmpty() }
            ?.let { authenticateUsing ->
                val authResult = auth.verify(authenticateUsing, req)
                when (authResult) {
                    is AuthResult.Denied.NoCredentials -> return NotAuthorized(authenticateUsing)
                    is AuthResult.Denied.InvalidCredentials -> return Forbidden
                    is AuthResult.Authorized -> authResult.identifier
                }
            }

        return LogAppend(
            SinkDescription(cfg.topic, req.requestId.toString(), cfg.format, cfg.sink, userId),
            req,
            Receipt(cfg.response.value, "")
        )
    }
}