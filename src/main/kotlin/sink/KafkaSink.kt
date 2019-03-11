package leia.sink

import leia.logic.IncomingRequest
import leia.logic.SinkDescription
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import java.util.*

private val log = KotlinLogging.logger("kafka-sink")

val commonOptions = mapOf("client.id" to "leia")

val consumerOptions = mapOf(
    "key.deserializer" to "org.apache.kafka.common.serialization.ByteArrayDeserializer",
    "value.deserializer" to "org.apache.kafka.common.serialization.ByteArrayDeserializer",
    "enable.auto.commit" to "false")

val producerOptions = mapOf(
    "key.serializer" to "org.apache.kafka.common.serialization.StringSerializer",
    "value.serializer" to "org.apache.kafka.common.serialization.ByteArraySerializer",
    "request.timeout.ms" to "10000",
    "max.block.ms" to "5000",
    "acks" to "all",
    "compression.type" to "gzip",
    "retries" to "0")

private fun kafkaOptions(servers: String?, producer: Boolean) = Properties()
    .apply { putAll(commonOptions) }
    .apply { putAll(if (producer) producerOptions else consumerOptions) }
    .apply { servers?.let { put("bootstrap.servers", it) } }

private fun mkProducer(servers: String?) = KafkaProducer<String, ByteArray>(kafkaOptions(servers, producer = true))

private fun mkChecker(servers: String?) =
    KafkaConsumer<String, ByteArray>(kafkaOptions(servers, producer = false))

private fun <K, V> Map<K, V>.with(addend: Pair<K, V?>?): Map<K, V> =
    if (addend?.second != null)
        plus(addend.first to addend.second!!).toMap()
    else this

private class KafkaSink(
    private val producer: KafkaProducer<String, ByteArray>,
    private val description: SinkDescription,
    private val host: String?
) : Sink {
    override suspend fun handle(incomingRequest: IncomingRequest): SinkResult {
        val body = description.dataFormat.generateBody(incomingRequest)
        val key = incomingRequest.requestId.toString()
        val headers = emptyMap<String, ByteArray>()
            .with("leia/user" to description.authorizedAs?.toByteArray())
            .map { RecordHeader(it.key, it.value) }

        return try {
            producer.send(ProducerRecord<String, ByteArray>(description.topic, null, key, body, headers))
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