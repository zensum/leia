package leia.logic

import se.zensum.leia.config.Format

data class SinkDescription(
    val topic: String,
    val key: String,
    val dataFormat: Format,
    val name: String?
)

data class Receipt(
    val status: Int,
    val body: String
)

interface Resolver {
    fun resolve(req: IncomingRequest) : Result
}