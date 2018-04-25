package leia.logic

import com.auth0.jwt.interfaces.DecodedJWT
import com.github.rholder.fauxflake.IdGenerators
import com.github.rholder.fauxflake.api.IdGenerator
import io.ktor.http.HttpMethod

private val idGen: IdGenerator = IdGenerators.newSnowflakeIdGenerator()
private fun generateId(): Long = idGen.generateId(10).asLong()

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

