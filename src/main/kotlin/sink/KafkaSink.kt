package leia.sink

import franz.ProducerBuilder
import franz.producer.Producer
import leia.logic.IncomingRequest
import leia.logic.SinkDescription
import se.zensum.leia.config.Format
import se.zensum.leia.convertMethod
import se.zensum.webhook.PayloadOuterClass

private fun mkProducer() =
    ProducerBuilder.ofByteArray
        .option("client.id", "leia")
        .create()

suspend fun createPayloadProtobuf(req: IncomingRequest): PayloadOuterClass.Payload =
    req.run {

        PayloadOuterClass.Payload.newBuilder().also {
            it.path = path
            it.method = convertMethod(method)
            // TODO: headers must be saved
            // it.headers = parseMap(requestHeaders)
            // TODO: We need to deal with this someone but I have no idea how
            //it.parameters = parseMap(call.parameters) // Deprecate?
            // TODO: add to incoming req
            // it.queryString = queryString()
            // Shouldn't this be a byteArray
            it.body = readBody().toString(Charsets.UTF_8)

            // Could we move id stamping to incoming requests
            // This kind of defensive coding would be insane if it weren't for the idGeneration bug
            it.flakeId = requestId
        }.build()
    }
private suspend fun Format.generateBody(req: IncomingRequest) : ByteArray = when (this) {
    Format.RAW_BODY -> req.readBody()
    Format.PROTOBUF -> createPayloadProtobuf(req).toByteArray()
}

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

class KafkaSinkProvider : SinkProvider {
    private val producer = mkProducer()
    override fun sinkFor(description: SinkDescription): Sink? =
        KafkaSink(producer, description)
}