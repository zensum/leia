package leia.logic

import se.zensum.leia.config.TopicRouting

class RuleResolver(private val cfg: TopicRouting) : Resolver {
    override fun resolve(req: IncomingRequest) : Result {
        // TODO: Add hostname restriction here
        if (!req.matchPath(cfg.path)) {
            return NoMatch
        }
        if (!req.matchCors(cfg.corsHosts)) {
            return CorsNotAllowed
        }
        if (cfg.verify && !req.hasValidJWT()) {
            return NotAuthorzied
        }

        return LogAppend(
            SinkDescription(cfg.topic, cfg.topic, cfg.format, cfg.sink),
            req,
            Receipt(cfg.response.value, "")
        )
    }
}