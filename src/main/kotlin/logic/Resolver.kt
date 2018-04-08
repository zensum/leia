package leia.logic

import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.http.HttpMethod
import se.zensum.leia.config.Format

sealed class Result


// The incoming request doesn't match this particular rule
object NoMatch : Result()

// The incoming request did match this rule
sealed class Match : Result()

// The rule matches but authorization is missing but is required
object NotAuthorzied: Match()
// The rule matches but authorization presented is invalid
object Forbidden: Match()

data class SinkDescription(
    private val topic: String,
    private val key: String,
    private val dataFormat: Format
)

data class Receipt(
    private val status: Int,
    private val body: String
)

// The rule matches and the
data class LogAppend(
    private val sinkDescription: SinkDescription,
    private val acceptedRequest: IncomingRequest,
    private val receipt: Receipt
) : Match()

data class IncomingRequest(
    private val method: HttpMethod,
    private val origin: String?,
    private val jwt: DecodedJWT?,
    private val path: String,
    private val host: String
)

class Resolver(

)

