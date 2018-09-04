package leia.logic

import io.ktor.http.HttpMethod
import se.zensum.leia.auth.AuthProvider
import se.zensum.leia.auth.AuthResult
import se.zensum.leia.config.SourceSpec

// A resolver that resolves an incoming request against a single source-spec
// object.
class SourceSpecResolver(private val cfg: SourceSpec, private val auth: AuthProvider) : Resolver {
    override fun resolve(req: IncomingRequest) : Result {
        // TODO: Add hostname restriction here
        if (!req.matchPath(cfg.path)) {
            return NoMatch
        }
        if (!req.matchCors(cfg.corsHosts)) {
            return CorsNotAllowed
        }
        // TODO: We need to be able to tell this apart from explicitly allowed
        // OPTIONS.
        if (cfg.corsHosts.isNotEmpty() && req.method == HttpMethod.Options) {
            return CorsPreflightAllowed
        }

        if (!cfg.allowedMethodsSet.contains(req.method)) {
            return NoMatch
        }

        val userId = cfg.authenticateUsing
            .takeIf { it.isNotEmpty() }
            ?.let { authenticateUsing ->
                val authResult = auth.verify(authenticateUsing, req)
                when (authResult) {
                    is AuthResult.Denied -> return NotAuthorized
                    is AuthResult.NoAuthorizationCheck -> throw IllegalStateException("Willy says this should not happen")
                    is AuthResult.Authorized -> authResult.identifier
                }
            }

        /* TODO: Handling of legacy property "verify" should be moved to the source spec parser and removed
        if (cfg.verify && !req.hasValidJWT()) {
            return NotAuthorized
        }*/

        return LogAppend(
            SinkDescription(cfg.topic, req.requestId.toString(), cfg.format, cfg.sink, userId),
            req,
            Receipt(cfg.response.value, "")
        )
    }
}