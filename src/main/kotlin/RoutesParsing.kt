package se.zensum.leia

import com.moandjiezana.toml.Toml
import org.jetbrains.ktor.http.HttpMethod
import org.jetbrains.ktor.http.HttpStatusCode
import java.io.File

private const val ROUTES_FILE ="/etc/config/routes"

enum class Format { RAW_BODY, PROTOBUF }

data class TopicRouting(val path: String,
                        val topic: String,
                        val format: Format = Format.PROTOBUF,
                        val verify: Boolean = false,
                        val allowedMethods: Collection<HttpMethod>,
                        val corsHosts: List<String>,
                        val response: HttpStatusCode)

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

private fun parseCors(routeConfig: Toml) =
    routeConfig.getList<String>("cors", emptyList()).toList()


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
    val cors = parseCors(routeConfig)
    val code: Int = routeConfig.getLong("response", 204L).toInt()
    val responseCode: HttpStatusCode = HttpStatusCode.fromValue(code)
    return TopicRouting(path, topic, format, verify, methods, cors, responseCode)
}

private fun parseMethods(toml: Toml): Set<HttpMethod> {
    val methodsInput: List<String> = toml.getList("methods", null) ?: return httpMethods.verbs
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