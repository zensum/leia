package se.zensum.leia

import com.moandjiezana.toml.Toml
import org.jetbrains.ktor.http.HttpMethod
import java.io.File

private const val ROUTES_FILE ="/etc/config/routes"

enum class Format { RAW_BODY, PROTOBUF }

data class TopicRouting(val path: String,
                        val topic: String,
                        val format: Format = Format.PROTOBUF,
                        val verify: Boolean = false,
                        val allowedMethods: Collection<HttpMethod> = httpMethods.verbs)

fun getRoutes(routesFile: String = getEnv("ROUTES_FILE", ROUTES_FILE)): Map<String, TopicRouting> {
    val toml = readTomlFromFile(routesFile)
    return parseTomlConfig(toml)
}

private fun readTomlFromFile(routesFile: String): Toml {
    val file: File = verifyFile(routesFile)
    return Toml().read(file)
}

fun parseTomlConfig(toml: Toml): Map<String, TopicRouting> {
    val routes = toml.getTables("routes")
    return routes.asSequence()
        .map { TopicRouting(it) }
        .map { Pair(it.path, it) }
        .toMap()
}

private fun TopicRouting(routeConfig: Toml): TopicRouting {
    val path: String = routeConfig.getString("path")!!
    val topic: String = routeConfig.getString("topic")!!
    val format: Format = when(routeConfig.getString("format", "proto")) {
        "raw_body" -> Format.RAW_BODY
        "proto" -> Format.PROTOBUF
        else -> throw IllegalArgumentException("Invalid value for config parameter 'format'")
    }
    val verify: Boolean = routeConfig.getBoolean("verify", false)
    val methods: Set<HttpMethod> = parseMethods(routeConfig)

    return when(methods.isEmpty()) {
        true -> TopicRouting(path, topic, format, verify)
        false -> TopicRouting(path, topic, format, verify, methods)
    }
}

private fun parseMethods(toml: Toml): Set<HttpMethod> {
    val methodsInput: List<String> = toml.getList("methods", emptyList())
    return methodsInput.asSequence()
        .map { HttpMethod.parse(it) }
        .toSet()
}

fun verifyFile(filePath: String): File {
    return File(filePath).apply {
        if(!exists()) {
            throw IllegalArgumentException("File $this does not exist")
        }
        if(isDirectory) {
            throw IllegalArgumentException("File $this is a directory")
        }
    }
}