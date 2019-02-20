package leia.sink

import leia.logic.IncomingRequest
import leia.logic.SinkDescription
import redis.clients.jedis.Jedis


private fun mkJedis(host: String? = null, port: Int? = null) = when {
    host != null && port != null -> Jedis(host, port)
    host != null -> Jedis(host)
    else -> Jedis()
}

private class RedisSink(
    private val jedis: Jedis,
    private val description: SinkDescription
) : Sink {
    override suspend fun handle(incomingRequest: IncomingRequest): SinkResult {
        val body = description.dataFormat.generateBody(incomingRequest)

        return try {
            jedis.publish(description.topic.toByteArray(), body)
            SinkResult.SuccessfullyWritten
        } catch (exc: Exception) {
            SinkResult.WritingFailed(exc)
        }
    }
}

class RedisSinkProvider(host: String? = null, port: Int? = null) : SinkProvider {
    private val jedis = mkJedis(host, port)
    override fun sinkFor(description: SinkDescription): Sink? =
        RedisSink(jedis, description)
}