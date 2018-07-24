package leia.logic

import io.ktor.http.HttpMethod
import se.zensum.leia.config.SourceSpec

// A resolver that resolves an incoming request against a single source-spec
// object.
class SourceSpecResolver(private val cfg: SourceSpec) : Resolver {
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
        if (cfg.verify && !req.hasValidJWT()) {
            return NotAuthorzied
        }

        if (!cfg.allowedMethodsSet.contains(req.method)) {
            return NoMatch
        }

        return LogAppend(
            SinkDescription(cfg.topic, req.requestId.toString(), cfg.format, cfg.sink),
            req,
            Receipt(cfg.response.value, "")
        )
    }
}