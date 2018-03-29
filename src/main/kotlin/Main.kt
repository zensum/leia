package se.zensum.leia

import franz.ProducerBuilder
import franz.producer.ProduceResult
import ktor_health_check.Health
import mu.KotlinLogging
import org.apache.kafka.common.errors.TimeoutException
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.origin
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import io.ktor.request.host
import io.ktor.request.httpMethod
import io.ktor.request.path
import io.ktor.request.receiveStream
import io.ktor.response.ApplicationResponse
import io.ktor.response.ResponseHeaders
import io.ktor.response.header
import io.ktor.response.respondText
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import se.zensum.jwt.JWTFeature
import se.zensum.jwt.isVerified
import se.zensum.ktorPrometheusFeature.PrometheusFeature
import se.zensum.ktorSentry.SentryFeature

fun main(args: Array<String>) {
    val port: Int = Integer.parseInt(getEnv("PORT", "80"))
    server(port).start(true)
}

fun getEnv(e : String, default: String? = null) : String = System.getenv()[e] ?: default ?: throw RuntimeException("Missing environment variable $e and no default value is given.")

private val producer = ProducerBuilder.ofByteArray
    .option("client.id", "leia")
    .create()

private val logger = KotlinLogging.logger("process-request")

private val genericHeaders: Map<String, String> = mapOf(
    "Server" to "Zensum/Leia"
)

fun server(port: Int) = embeddedServer(Netty, port) {
    val routes: Map<String, TopicRouting> = getRoutes()
    install(SentryFeature)
    install(PrometheusFeature.Feature)
    install(Health)
    install(JWTFeature)
    routing {
        for((path, topicRouting) in routes) {
            route(path) {
                // Being explicit here, we're install CORS to the route
                if (topicRouting.corsHosts.isNotEmpty()) {
                    this@route.install(CORS) {
                        hosts.addAll(topicRouting.corsHosts)
                    }
                }
                handle {
                    setGenericHeaders(call.response)
                    val method = call.request.httpMethod
                    val host: String = call.request.host() ?: "Unknown host"

                    logRequest(method, path, host)

                    val response = when(isVerified() || !topicRouting.verify) {
                        true -> createResponse(call, topicRouting)
                        false -> {
                            logAccessDenied(path, host)
                            HttpStatusCode.Unauthorized
                        }
                    }

                    call.respondText("", status = response).also { logResponse(call) }
                }
            }
        }
    }
}

private fun setGenericHeaders(response: ApplicationResponse) {
    genericHeaders.forEach { key, value -> response.header(key, value) }
}

private inline fun logRequest(method: HttpMethod, path: String, host: String) {
    logger.info { "${method.value} $path from $host" }
}

private inline fun logAccessDenied(path: String, host: String) {
    logger.info { "Unauthorized request was denied to $path from $host" }
}

private inline fun logResponse(call: ApplicationCall) {
    logger.debug {
        "Sent response to ${call.request.origin.host} with headers\n" +
        "${printHeaders(call.response.headers)}\n" +
        "\tand response code ${call.response.status()}"
    }
}

private fun printHeaders(headers: ResponseHeaders): String {
    return headers.allValues()
        .entries()
        .asSequence()
        .map { it.key to it.value }
        .map {
            (key, values) -> "$key: " + values.asSequence().joinToString(separator = ", ")
        }
        .joinToString(separator = "\n"){ "\t\t$it" }
}

suspend fun createResponse(call: ApplicationCall, routing: TopicRouting): HttpStatusCode {
    if(call.request.httpMethod !in routing.allowedMethods) {
        call.response.header("Allow", asHeaderValue(routing.allowedMethods))
        return HttpStatusCode.MethodNotAllowed
    }

    val method = call.request.httpMethod
    val path: String = call.request.path()
    val body: ByteArray = when(routing.format) {
        Format.RAW_BODY -> receiveBody(call.request)
        Format.PROTOBUF -> createPayload(call).toByteArray()
    }

    return writeToKafka(method, path, routing.topic, body, routing.response)
}

private fun asHeaderValue(values: Collection<HttpMethod>): String = values.joinToString(separator = ", ", transform = { it.value })

fun hasBody(req: ApplicationRequest): Boolean = Integer.parseInt(req.headers["Content-Length"] ?: "0") > 0

suspend fun receiveBody(req: ApplicationRequest): ByteArray = when(hasBody(req)) {
    true -> req.call.receiveStream().readBytes(64)
    false -> ByteArray(0)
}

private suspend fun writeToKafka(method: HttpMethod, path: String, topic: String, data: ByteArray, successResponse: HttpStatusCode): HttpStatusCode {
    val summary = "${method.value} $path"
    return try {
        val metaData: ProduceResult = producer.send(topic, data)
        logger.info("$summary written to ${metaData.topic()}")
        successResponse
    }
    catch (e: TimeoutException) {
        logger.error("Time out when trying to write $summary to $topic")
        HttpStatusCode.InternalServerError
    }
}