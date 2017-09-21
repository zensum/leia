package se.zensum.webhook

import com.github.rholder.fauxflake.IdGenerators
import org.jetbrains.ktor.application.ApplicationCall
import org.jetbrains.ktor.request.httpMethod
import org.jetbrains.ktor.request.path
import org.jetbrains.ktor.request.queryString
import org.jetbrains.ktor.request.receiveText
import org.jetbrains.ktor.util.ValuesMap
import se.zensum.webhook.PayloadOuterClass.Payload
import org.jetbrains.ktor.http.HttpMethod as KtorHttpMethod
import se.zensum.webhook.PayloadOuterClass.HttpMethod as WebhookHttpMethod

val idGen = IdGenerators.newFlakeIdGenerator()!!
fun generateId(): Long = idGen.generateId(10).asLong()

suspend fun createPayload(call: ApplicationCall): Payload  {
    return call.request.run {
        val requestHeaders: ValuesMap = headers
        Payload.newBuilder().apply {
            this.path = path()
            this.method = convertMethod(httpMethod)
            this.headers = parseMap(requestHeaders)
            this.parameters = parseMap(call.parameters)
            this.queryString = queryString()
            this.body = call.receiveText()
            this.flakeId = generateId()
        }.build()
    }
}

fun parseMap(valuesMap: ValuesMap): PayloadOuterClass.MultiMap {
    return PayloadOuterClass.MultiMap.newBuilder().apply {
        valuesMap.entries().asSequence()
            .map { toListOfPair(it.key, it.value) }
            .flatMap { it.asSequence() }
            .map { toProtoPair(it) }
            .forEach { addPair(it) }
    }.build()
}

fun toProtoPair(pair: Pair<String, String>): PayloadOuterClass.MultiMap.Pair {
    return PayloadOuterClass.MultiMap.Pair.newBuilder().apply {
        this.key = pair.first
        this.value = pair.second
    }.build()
}

fun toListOfPair(key: String, values: List<String>): List<Pair<String, String>> {
    return values.asSequence()
        .map { key to it }
        .toList()
}

fun convertMethod(method: KtorHttpMethod): WebhookHttpMethod = when(method) {
    KtorHttpMethod.Put -> WebhookHttpMethod.PUT
    KtorHttpMethod.Patch -> WebhookHttpMethod.PATCH
    KtorHttpMethod.Delete -> WebhookHttpMethod.DELETE
    KtorHttpMethod.Get -> WebhookHttpMethod.GET
    KtorHttpMethod.Head -> WebhookHttpMethod.HEAD
    KtorHttpMethod.Post -> WebhookHttpMethod.POST
    KtorHttpMethod.Options -> WebhookHttpMethod.OPTIONS
    else -> throw IllegalArgumentException("Method ${method.value} is not supported")
}

fun methodIsSupported(method: KtorHttpMethod): Boolean = method in httpMethods