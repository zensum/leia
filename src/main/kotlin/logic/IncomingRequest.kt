package leia.logic

import com.github.rholder.fauxflake.IdGenerators
import com.github.rholder.fauxflake.api.IdGenerator
import io.ktor.http.HttpMethod

private val idGen: IdGenerator = IdGenerators.newSnowflakeIdGenerator()
private fun generateId(): Long = idGen.generateId(10).asLong()

// Data-class representing an incoming http-request. It is created by the server
// front-end so as to provide a unitary representation between different servers
data class IncomingRequest(
    val method: HttpMethod,
    private val origin: String?,
    val path: String,
    val headers: Map<String, List<String>>,
    val queryString: String,
    private val host: String?,
    private val readBodyFn: suspend () -> ByteArray
) {
    private var savedBody: ByteArray? = null
    val requestId = generateId().also {
        if (it == 0L) throw IllegalStateException("Generated flake id was 0")
    }

    fun matchPath(otherPath: String?) = otherPath == path
    fun matchCors(origins: List<String>) =
        origin == null || origins.isEmpty() || origins.contains(origin) || origins.contains("*")

    suspend fun readBody(save: Boolean = false): ByteArray {
        if (save) {
            savedBody = readBodyFn()
        }
        return savedBody ?: readBodyFn()
    }
}
