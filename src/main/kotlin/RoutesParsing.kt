package se.zensum.webhook

import com.moandjiezana.toml.Toml
import org.jetbrains.ktor.http.HttpMethod
import java.io.File
import java.nio.file.Path

private const val ROUTES_FILE ="/etc/config/routes"

enum class Format { RAW_BODY, PROTOBUF }

data class TopicRouting(val path: String,
                        val topic: String,
                        val format: Format = Format.PROTOBUF,
                        val verify: Boolean = false,
                        val allowedMethods: Collection<HttpMethod> = httpMethods.verbs)

fun getRoutes(routesFile: String = ROUTES_FILE): Map<String, TopicRouting> {
    val file = File(routesFile)
    verifyFile(file.toPath())
    val toml = Toml().read(file)
    val routes = toml.getTables("routes")
    return routes.asSequence()
        .filter { it.getString("path") != null }
        .filter { it.getString("topic") != null }
        .map { TopicRouting(it) }
        .map { Pair(it.path, it) }
        .toMap()
}

private fun TopicRouting(routeConfig: Toml): TopicRouting {
    val path: String = routeConfig.getString("path")
    val topic: String = routeConfig.getString("topic")
    val format: Format = when(routeConfig.getString("format", "proto")) {
        "raw_body" -> Format.RAW_BODY
        "proto" -> Format.PROTOBUF
        else -> throw IllegalArgumentException("Invalid value for config parameter 'format'")
    }
    val verify: Boolean = routeConfig.getBoolean("verify", false)
    val methodsInput: List<String> = routeConfig.getList("methods", emptyList())
    val methods: Set<HttpMethod> = methodsInput.asSequence()
        .map { HttpMethod.parse(it) }
        .toSet()

    return when(methods.isEmpty()) {
        true -> TopicRouting(path, topic, format, verify)
        false -> TopicRouting(path, topic, format, verify, methods)
    }
}

fun verifyFile(path: Path) {
    val file: File = path.toFile()
    if(!file.exists()) {
        throw IllegalArgumentException("File $file does not exist")
    }
    if(file.isDirectory) {
        throw IllegalArgumentException("File $file is a directory")
    }
}