package leia.logic

import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.http.HttpMethod
import se.zensum.leia.config.Format
import se.zensum.leia.config.TopicRouting
import se.zensum.leia.generateId

sealed class Result {
    abstract fun combine(other: Result): Result
}


// The incoming request doesn't match this particular rule
object NoMatch : Result() {
    override fun combine(other: Result): Result = other
}

// The incoming request did match this rule
sealed class Match : Result() {
    // Match + Match = Undefined
    // Match + NoMatch = Match
    // Match + ErrorMatch = Match
    override fun combine(other: Result) =
        if (other is Match) {
            throw UnsupportedOperationException("we cant handle this yet")
        } else this
}

// There is a match but it doesn't satisify the full rules engine
sealed class ErrorMatch : Match() {
    override fun combine(other: Result): Match =
        when(other) {
            is Match -> other
            is ErrorMatch -> throw UnsupportedOperationException("we cant handle this yet")
            is NoMatch -> this
        }
}
// The rule matches but authorization is missing but is required
object NotAuthorzied: ErrorMatch()
// The rule matches but authorization presented is invalid
object Forbidden: ErrorMatch()

object CorsNotAllowed : ErrorMatch()

data class SinkDescription(
    val topic: String,
    val key: String,
    val dataFormat: Format
)

data class Receipt(
    val status: Int,
    val body: String
)

// The rule matches and the
data class LogAppend(
    val sinkDescription: SinkDescription,
    val request: IncomingRequest,
    val receipt: Receipt
) : Match()

data class IncomingRequest(
    val method: HttpMethod,
    private val origin: String?,
    private val jwt: DecodedJWT?,
    val path: String,
    val headers: Map<String, List<String>>,
    val queryString: String,
    private val host: String?,
    private val readBodyFn: suspend () -> ByteArray
) {
    val requestId = generateId().also {
        if(it == 0L) throw IllegalStateException("Generated flake id was 0")
    }
    fun matchPath(otherPath: String?) = otherPath == path
    fun matchCors(origins: List<String>) =
        origin == null || origins.isEmpty() || origins.contains(origin)
    fun hasValidJWT() = jwt != null

    suspend fun readBody() = readBodyFn()
}

interface Resolver {
    fun resolve(req: IncomingRequest) : Result
}

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
            SinkDescription(cfg.topic, cfg.topic, cfg.format),
            req,
            Receipt(cfg.response.value, "")
        )
    }
}
