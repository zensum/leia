package leia.logic

import se.zensum.leia.auth.AuthResult
import se.zensum.leia.config.Format

// A description representing the destination to which the message is to be
// sinked to.
data class SinkDescription(
    val topic: String,
    val key: String,
    val dataFormat: Format,
    val name: String?,
    val authorizedAs: String?
)

// A receipt sent to the client when if the publication were to succeed.
data class Receipt(
    val status: Int,
    val body: String
)

// The resolver interface maps between incoming requests and resolver result,
// which contains information on the sink to which the message is to be
// routed, if any.
interface Resolver {
    fun resolve(req: IncomingRequest) : Result
}