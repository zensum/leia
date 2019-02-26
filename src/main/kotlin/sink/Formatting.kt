package leia.sink

import leia.logic.IncomingRequest
import se.zensum.leia.config.Format
import se.zensum.webhook.PayloadOuterClass
import io.ktor.http.HttpMethod as KtorHttpMethod

typealias HttpMethod = PayloadOuterClass.HttpMethod

fun convertMethod(method: KtorHttpMethod): PayloadOuterClass.HttpMethod = when (method) {
    KtorHttpMethod.Put -> HttpMethod.PUT
    KtorHttpMethod.Patch -> HttpMethod.PATCH
    KtorHttpMethod.Delete -> HttpMethod.DELETE
    KtorHttpMethod.Get -> HttpMethod.GET
    KtorHttpMethod.Head -> HttpMethod.HEAD
    KtorHttpMethod.Post -> HttpMethod.POST
    KtorHttpMethod.Options -> HttpMethod.OPTIONS
    else -> throw IllegalArgumentException("Method ${method.value} is not supported")
}

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

internal suspend fun Format.generateBody(req: IncomingRequest): ByteArray = when (this) {
    Format.RAW_BODY -> req.readBody()
    Format.PROTOBUF -> createPayloadProtobuf(req).toByteArray()
}

