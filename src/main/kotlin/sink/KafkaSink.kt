package leia.sink

import franz.ProducerBuilder
import franz.producer.Producer
import leia.logic.IncomingRequest
import leia.logic.SinkDescription
import se.zensum.leia.config.Format
import io.ktor.http.HttpMethod as KtorHttpMethod
import se.zensum.webhook.PayloadOuterClass

typealias HttpMethod = PayloadOuterClass.HttpMethod
fun convertMethod(method: KtorHttpMethod): PayloadOuterClass.HttpMethod = when(method) {
    KtorHttpMethod.Put -> HttpMethod.PUT
    KtorHttpMethod.Patch -> HttpMethod.PATCH
    KtorHttpMethod.Delete -> HttpMethod.DELETE
    KtorHttpMethod.Get -> HttpMethod.GET
    KtorHttpMethod.Head -> HttpMethod.HEAD
    KtorHttpMethod.Post -> HttpMethod.POST
    KtorHttpMethod.Options -> HttpMethod.OPTIONS
    else -> throw IllegalArgumentException("Method ${method.value} is not supported")
}

private fun mkProducer() =
    ProducerBuilder.ofByteArray
        .option("client.id", "leia")
        .create()

fun toProtoPair(pair: Pair<String, String>): PayloadOuterClass.MultiMap.Pair {
    val (k, v) = pair
    return PayloadOuterClass.MultiMap.Pair.newBuilder().also {
        it.key = k
        it.value = v
    }.build()
}

fun parseMap(valuesMap: Map<String, List<String>>): PayloadOuterClass.MultiMap {
    return PayloadOuterClass.MultiMap.newBuilder().apply {
        valuesMap.entries.asSequence()
            .flatMap { (k, vs) -> vs.map { v -> k to v }.asSequence() }
            .map { toProtoPair(it) }
            .forEach { addPair(it) }
    }.build()
}

suspend fun createPayloadProtobuf(req: IncomingRequest): PayloadOuterClass.Payload =
    req.run {
        PayloadOuterClass.Payload.newBuilder().also {
            it.path = path
            it.method = convertMethod(method)
            it.headers = parseMap(headers)
            it.queryString = queryString
            it.body = readBody().toString(Charsets.UTF_8)
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