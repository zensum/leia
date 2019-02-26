package leia.sink

import franz.ProducerBuilder
import franz.producer.Producer
import leia.logic.IncomingRequest
import leia.logic.SinkDescription


private fun mkProducer(servers: String?) =
    ProducerBuilder.ofByteArray
        .option("client.id", "leia")
        .let {
            if (servers != null) {
                it.option("bootstrap.servers", servers)
            } else it
        }.create()

private fun <K, V> Map<K, V>.with(addend: Pair<K, V?>?): Map<K, V> =
    if(addend?.second != null)
        plus(addend.first to addend.second!!).toMap()
    else this

private class KafkaSink(
    private val producer: Producer<String, ByteArray>,
    private val description: SinkDescription
) : Sink {
    override suspend fun handle(incomingRequest: IncomingRequest): SinkResult {
        val body = description.dataFormat.generateBody(incomingRequest)
        val key = incomingRequest.requestId
        val headers: Map<String, ByteArray> = emptyMap<String, ByteArray>()
            .with("leia/user" to description.authorizedAs?.toByteArray())

        return try {
            producer.send(description.topic, key.toString(), body, headers)
            SinkResult.SuccessfullyWritten
        } catch (exc: Exception) {
            SinkResult.WritingFailed(exc)
        }
    }
}

class KafkaSinkProvider(host: String? = null) : SinkProvider {
    private val producer = mkProducer(host)
    override fun sinkFor(description: SinkDescription): Sink? =
        KafkaSink(producer, description)
}