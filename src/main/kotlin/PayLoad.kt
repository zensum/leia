package se.zensum.leia

import com.github.rholder.fauxflake.IdGenerators
import com.github.rholder.fauxflake.api.IdGenerator
import io.ktor.application.ApplicationCall
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.request.queryString
import io.ktor.request.receiveText
import io.ktor.util.StringValues
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.request.queryString
import io.ktor.request.receiveText
import se.zensum.webhook.PayloadOuterClass
import se.zensum.webhook.PayloadOuterClass.Payload
import io.ktor.http.HttpMethod as KtorHttpMethod
import se.zensum.webhook.PayloadOuterClass.HttpMethod as HttpMethod

val idGen: IdGenerator = IdGenerators.newSnowflakeIdGenerator()
fun generateId(): Long = idGen.generateId(10).asLong()

suspend fun createPayload(call: ApplicationCall): Payload  {
    return call.request.run {
        val requestHeaders = headers
        Payload.newBuilder().also {
            it.path = path()
            it.method = convertMethod(httpMethod)
            it.headers = parseMap(requestHeaders)
            it.parameters = parseMap(call.parameters)
            it.queryString = queryString()
            it.body = call.receiveText()
            // This kind of defensive coding would be insane if it weren't for the idGeneration bug
            it.flakeId = generateId().also {
                if(it == 0L) throw IllegalStateException("Generated flake id was 0")
            }
        }.build()
    }
}

fun parseMap(valuesMap: StringValues): PayloadOuterClass.MultiMap {
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

fun convertMethod(method: KtorHttpMethod): HttpMethod = when(method) {
    KtorHttpMethod.Put -> HttpMethod.PUT
    KtorHttpMethod.Patch -> HttpMethod.PATCH
    KtorHttpMethod.Delete -> HttpMethod.DELETE
    KtorHttpMethod.Get -> HttpMethod.GET
    KtorHttpMethod.Head -> HttpMethod.HEAD
    KtorHttpMethod.Post -> HttpMethod.POST
    KtorHttpMethod.Options -> HttpMethod.OPTIONS
    else -> throw IllegalArgumentException("Method ${method.value} is not supported")
}

fun methodIsSupported(method: KtorHttpMethod): Boolean = method in httpMethods