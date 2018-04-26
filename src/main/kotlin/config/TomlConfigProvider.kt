package se.zensum.leia.config

import java.io.File
import com.moandjiezana.toml.Toml
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import se.zensum.leia.config.TopicRouting
import se.zensum.leia.getEnv
import se.zensum.leia.httpMethods

private const val ROUTES_FILE ="/etc/config/routes"

private fun Toml.getStringOrError(k: String) =
    getString(k) ?: throw RuntimeException("Key $k, required but not in $this")

class TomlConfigProvider private constructor(toml: Toml) : ConfigProvider {
    private val routes = toml.getTables("routes")
        .asSequence()
        .map { tomlToTopicRouting(it) }
        .toList()
    override fun getRoutes(): List<TopicRouting> = routes

    private val sinkProviders = toml.getTables("sink-providers")
        .asSequence()
        .map {
            SinkProviderSpec(
                it.getStringOrError("name"),
                it.getBoolean("default", false),
                it.getString("type", "kafka"),
                it.getTable("options")
                    ?.toMap()
                    ?.mapValues { it.value.toString() }
                    ?: emptyMap()
            )
        }.toList()

    override fun getSinkProviders(): List<SinkProviderSpec> = sinkProviders

    companion object {
        fun fromPath(path: String) =
            TomlConfigProvider(readTomlFromFile(path))

        fun fromConfiguredPath() =
            fromPath(getEnv("ROUTES_FILE", ROUTES_FILE))

        fun fromString(tomlStr: String) =
            TomlConfigProvider(Toml().read(tomlStr))

    }
}

private fun readTomlFromFile(routesFile: String): Toml {
    val file: File = verifyFile(routesFile)
    return Toml().read(file)
}

private fun parseCors(routeConfig: Toml) =
    routeConfig.getList<String>("cors", emptyList()).toList()

private fun parseFormat(x: String): Format = when(x) {
    "raw_body" -> Format.RAW_BODY
    "proto" -> Format.PROTOBUF
    else -> throw IllegalArgumentException("Invalid value for config parameter 'format'")
}

private fun tomlToTopicRouting(routeConfig: Toml) = TopicRouting(
    path = routeConfig.getString("path")!!,
    topic = routeConfig.getString("topic")!!,
    format = parseFormat(routeConfig.getString("format", "proto")),
    corsHosts = parseCors(routeConfig),
    verify = routeConfig.getBoolean("verify", false),
    allowedMethods = parseMethods(routeConfig),
    response = HttpStatusCode.fromValue(routeConfig.getLong("response", 204L).toInt())
)

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
