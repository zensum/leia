package se.zensum.webhook

import franz.ProducerBuilder
import org.apache.kafka.clients.producer.ProducerRecord
import org.jetbrains.ktor.content.HttpStatusCodeContent
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.netty.Netty
import org.jetbrains.ktor.response.respond
import org.jetbrains.ktor.routing.route
import org.jetbrains.ktor.routing.routing
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main(args: Array<String>) {
    val port: Int = Integer.parseInt(getEnv("PORT", "80"))
    server(port).start(true)
}

fun getEnv(e : String, default: String? = null) : String = System.getenv()[e] ?: default ?: throw RuntimeException("Missing environment variable $e and no default value is given.")

private val producer = ProducerBuilder.ofString.create()
private val routesFile = getEnv("ROUTES_FILE", "/etc/config/routes")

fun server(port: Int) = embeddedServer(Netty, port) {
    val routes = parseRoutesFile(routesFile)

    if(routes.isEmpty()) {
        System.err.println("No routes found in $routesFile")
        System.exit(1)
    }

    routing {
        for((path, topic) in routes) {
            route(path) {
                handle {
                    val request: PayLoad = createPayload(this)
                    println(request)
                    producer.sendRaw(ProducerRecord(topic, request.toString()))
                    call.respond(HttpStatusCodeContent(HttpStatusCode.OK))
                }
            }
        }
    }
}

fun parseRoutesFile(file: String): Map<String, String> {
    val path: Path = Paths.get(file)
    return Files.readAllLines(path).asSequence()
        .map { lineToPair(it) }
        .toMap()
}

fun lineToPair(line: String): Pair<String, String> {
    val delimiter = Regex("\\s*->\\s*")
    val split: List<String> = line.split(delimiter)
    if(split.size != 2)
        throw IllegalArgumentException("Found invalid route entry: $line")

    return Pair(split[0], split[1])
}
