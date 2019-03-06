package leia.sink

import franz.ProducerBuilder
import franz.producer.Producer
import leia.logic.IncomingRequest
import leia.logic.SinkDescription
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.*

private val log = KotlinLogging.logger("kafka-sink")


private fun kafkaOptions(servers: String?) = Properties()
    .apply { put("client.id", "leia") }
    .apply { put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer") }
    .apply { put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer") }
    .apply { put("enable.auto.commit", "false") }
    .apply { servers?.let { put("bootstrap.servers", it) } }

private fun mkProducer(servers: String?) =
    ProducerBuilder.ofByteArray
        .option("client.id", "leia")
        .let {
            if (servers != null) {
                it.option("bootstrap.servers", servers)
            } else it
        }.create()

private fun mkChecker(servers: String?) =
    KafkaConsumer<String, ByteArray>(kafkaOptions(servers))

private fun <K, V> Map<K, V>.with(addend: Pair<K, V?>?): Map<K, V> =
    if (addend?.second != null)
        plus(addend.first to addend.second!!).toMap()
    else this

private class KafkaSink(
    private val producer: Producer<String, ByteArray>,
    private val description: SinkDescription,
    private val host: String?
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

    override suspend fun healthCheck() =
        try {
            mkChecker(host)
            SinkResult.SuccessfullyWritten
        } catch (e: Exception) {
            log.error { e }
            SinkResult.WritingFailed(e)
        }
}

class KafkaSinkProvider(private val host: String? = null) : SinkProvider {
    private val producer = mkProducer(host)
    override fun sinkFor(description: SinkDescription): Sink? =
        KafkaSink(producer, description, host)
}