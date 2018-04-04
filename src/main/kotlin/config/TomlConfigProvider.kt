package se.zensum.leia.config

import java.io.File
import com.moandjiezana.toml.Toml
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import se.zensum.leia.config.TopicRouting
import se.zensum.leia.getEnv
import se.zensum.leia.httpMethods

private const val ROUTES_FILE ="/etc/config/routes"

class TomlConfigProvider private constructor(toml: Toml) : ConfigProvider {
    private val parsed = parseTomlConfig(toml)
    override fun getRoutes(): Map<String, TopicRouting> = parsed

    companion object {
        fun fromPath(path: String) =
            TomlConfigProvider(readTomlFromFile(path))

        fun fromConfiguredPath() =
            fromPath(getEnv("ROUTES_FILE", ROUTES_FILE))

        fun fromString(tomlStr: String) =
            TomlConfigProvider(Toml().read(tomlStr))

    }
}

private fun getRoutes(routesFile: String): Map<String, TopicRouting> {
    val toml = readTomlFromFile(routesFile)
    return parseTomlConfig(toml)
}

private fun readTomlFromFile(routesFile: String): Toml {
    val file: File = verifyFile(routesFile)
    return Toml().read(file)
}

private fun parseTomlConfig(toml: Toml): Map<String, TopicRouting> {
    val routes = toml.getTables("routes")
    return routes.asSequence()
        .map { tomlToTopicRouting(it) }
        .map { Pair(it.path, it) }
        .toMap()
}

private fun parseCors(routeConfig: Toml) =
    routeConfig.getList<String>("cors", emptyList()).toList()


private fun tomlToTopicRouting(routeConfig: Toml): TopicRouting {
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

private fun verifyFile(filePath: String): File {
    return File(filePath).apply {
        if (!exists()) {
            throw IllegalArgumentException("File $this does not exist")
        }
        if (isDirectory) {
            throw IllegalArgumentException("File $this is a directory")
        }
    }

}