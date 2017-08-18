package se.zensum.webhook

import franz.ProducerBuilder
import org.apache.kafka.clients.producer.ProducerRecord
import org.jetbrains.ktor.content.HttpStatusCodeContent
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.request.path
import org.jetbrains.ktor.response.respond
import org.jetbrains.ktor.routing.route
import org.jetbrains.ktor.routing.routing

fun main(args: Array<String>) {
    server(1234).start(true)
}

private val producer = ProducerBuilder.ofString.create()

fun server(port: Int) = embeddedServer(Netty, port) {
    routing {
        route("*") {
            handle {
                val path = call.request.path()
                if (path in routes.keys) {
                    val request: PayLoad = createPayload(this)
                    println(request)
                    val topic: String = routes[path]!!
                    producer.sendRaw(ProducerRecord(topic, request.toString()))
                    call.respond(HttpStatusCodeContent(HttpStatusCode.OK))
                }
                else {
                    call.respond(HttpStatusCodeContent(HttpStatusCode.NotFound))
                }
            }
        }
    }
}

private val routes = mapOf("/status" to "status",
    "/" to "all",
    "/test" to "test")