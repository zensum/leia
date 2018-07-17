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
                it.option("boostrap.servers", servers)
            } else it
        }.create()

private class KafkaSink(
    private val producer: Producer<String, ByteArray>,
    private val description: SinkDescription
) : Sink {
    override suspend fun handle(incomingRequest: IncomingRequest): SinkResult {
        val body = description.dataFormat.generateBody(incomingRequest)
        val key = incomingRequest.requestId
        val headers = emptyMap<String, ByteArray>()
        return try {
            producer.send(description.topic, key.toString(), body, headers)
            SinkResult.SuccessfullyWritten
        } catch (exc: Exception) {
            SinkResult.WritingFailed(exc)
        }
    }
}

class KafkaSinkProvider(private val host: String? = null) : SinkProvider {
    private val producer = mkProducer(host)
    override fun sinkFor(description: SinkDescription): Sink? =
        KafkaSink(producer, description)
}