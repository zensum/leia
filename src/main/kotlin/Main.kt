package se.zensum.webhook

import franz.ProducerBuilder
import kotlinx.coroutines.experimental.future.await
import ktor_health_check.Health
import mu.KotlinLogging
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.errors.TimeoutException
import org.jetbrains.ktor.application.ApplicationCall
import org.jetbrains.ktor.application.install
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.pipeline.PipelineContext
import org.jetbrains.ktor.request.ApplicationRequest
import org.jetbrains.ktor.request.host
import org.jetbrains.ktor.request.httpMethod
import org.jetbrains.ktor.request.path
import org.jetbrains.ktor.response.respond
import org.jetbrains.ktor.routing.route
import org.jetbrains.ktor.routing.routing
import se.zensum.ktorPrometheusFeature.PrometheusFeature
import se.zensum.ktorSentry.SentryFeature
import se.zensum.webhook.PayloadOuterClass.Payload
import java.net.InetAddress

fun main(args: Array<String>) {
    val port: Int = Integer.parseInt(getEnv("PORT", "80"))
    server(port).start(true)
}

fun getEnv(e : String, default: String? = null) : String = System.getenv()[e] ?: default ?: throw RuntimeException("Missing environment variable $e and no default value is given.")

private val producer = ProducerBuilder.ofByteArray.create()
private val logger = KotlinLogging.logger("process-request")


fun server(port: Int) = embeddedServer(Netty, port) {
    val routes: Map<String, TopicRouting> = getRoutes()
    install(SentryFeature)
    install(PrometheusFeature)
    install(Health)
    routing {
        for((path, topicRouting) in routes) {
            route(path) {
                handle {
                    logRequest(call.request)
                    val response: HttpStatusCode = createResponse(this, topicRouting.topic)
                    call.respond(response)
                }
            }
        }
    }
}

private fun logRequest(request: ApplicationRequest) {
    request.apply {
        logger.info("${httpMethod.value} ${path()} from ${host()}")
    }
}

suspend fun createResponse(context: PipelineContext<Unit>, topic: String): HttpStatusCode
{
    context.run {
        return when(methodIsSupported(call.request.httpMethod)) {
            true -> {
                val request: Payload = createPayload(context)
                writeToKafka(this.call, topic, request)
            }
            false -> HttpStatusCode.MethodNotAllowed
        }
    }
}

private suspend fun writeToKafka(call: ApplicationCall, topic: String, request: Payload): HttpStatusCode
{
    val summary = "${call.request.httpMethod.value} ${call.request.path()}"
    return try {
        val metaData: RecordMetadata = producer.sendRaw(ProducerRecord(topic, request.toByteArray())).await()
        logger.info("$summary written to ${metaData.topic()}")
        HttpStatusCode.OK
    }
    catch (e: TimeoutException) {
        val kafkaIp: String = InetAddress.getByName("kafka").hostAddress
        logger.error("Time out when trying to write $summary to $topic at $kafkaIp")
        HttpStatusCode.InternalServerError
    }
}