package leia.registry

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.response.readBytes
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mu.KLogging
import registry.Tables
import java.net.ConnectException
import java.nio.channels.UnresolvedAddressException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class K8sResourceHolder<T>(private val parser: (String) -> List<T>) {
    private val entriesA = AtomicReference(listOf<T>())
    fun onChange(yaml: String) {
        entriesA.updateAndGet {
            try {
                parser(yaml)
            } catch (e: MissingKotlinParameterException) {
                K8sRegistry.logger.error { K8sRegistry.logger.error { "Failed to read objects from kubernetes: ${e.message}" } }
                it
            }
        }
    }

    fun getData(): List<T> = entriesA.get()
}

// Auto-watching registry for a directory of Kubernetes Yaml files.
class K8sRegistry(private val host: String, private val port: String) : Registry() {
    private val mapper = ObjectMapper(JsonFactory())
        .also { it.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) }
        .also { it.registerModule(KotlinModule()) }
    private val routesHolder = K8sResourceHolder { yaml ->
        val routes = mapper.readValue(yaml, K8sRegistry.Routes::class.java)
        routes.items.filter { apiVersions.contains(it.apiVersion) }
    }
    private val sinksHolder = K8sResourceHolder { yaml ->
        val sinks = mapper.readValue(yaml, K8sRegistry.Sinks::class.java)
        sinks.items.filter { apiVersions.contains(it.apiVersion) }
    }

    private val scheduler = Executors.newScheduledThreadPool(1)

    init {
        scheduler.scheduleAtFixedRate({ this.forceUpdate() }, 1, 1, TimeUnit.MINUTES)
    }

    override fun getMaps(table: Tables) = when (table) {
        Tables.Routes -> routesHolder.getData().map { it.spec.toMap() }
        Tables.SinkProviders -> sinksHolder.getData().map { it.spec.toMap() }
        else -> listOf()
    }

    private fun getPort(): Int = try {
        port.toInt()
    } catch (e: NumberFormatException) {
        logger.error { "Invalid port number: $port, using default 8080" }
        DEFAULT_KUBERNETES_PORT.toInt()
    }

    private fun forceUpdateAll() {
        val futureRoutes = forceUpdateAsync("leiaroutes") { content -> routesHolder.onChange(content) }
        val futureSinks = forceUpdateAsync("leiasinks") { content -> sinksHolder.onChange(content) }
        runBlocking {
            futureRoutes.await()
            futureSinks.await()
        }
    }

    override fun forceUpdate() {
        logger.info("Polling all objects from kubernetes")
        forceUpdateAll()
        notifyWatchers()
        logger.info { "Loaded ${routesHolder.getData().size} routes ${sinksHolder.getData().size} sinks from kubernetes" }
    }

    private fun forceUpdateAsync(path: String, onUpdate: (content: String) -> Unit) = GlobalScope.async {
        val builder = HttpRequestBuilder()
            .also { it.method = HttpMethod.Get }
            .also {
                it.url.also { url ->
                    url.host = host
                    url.port = getPort()
                }.encodedPath = "/apis/leia.klira.io/v1/namespaces/default/$path"
            }
        fetchData(builder, onUpdate)?.let { logger.warn { "Failed to connect to kubernetes: $it" } }
    }

    private suspend fun fetchData(builder: HttpRequestBuilder, callback: (content: String) -> Unit): String? {
        try {
            val response = HttpClient().call(builder).response
            val content = response.readBytes().toString(Charsets.UTF_8)
            if (response.status.isSuccess()) {
                callback.invoke(content)
            } else {
                logger.warn { "Failed to get from kubernetes: ${builder.url.encodedPath}" }
            }
        } catch (e: UnresolvedAddressException) {
            return e.message
        } catch (e: ConnectException) {
            return e.message
        }
        return null
    }

    companion object : KLogging() {
        const val DEFAULT_KUBERNETES_HOST = "localhost"
        const val DEFAULT_KUBERNETES_PORT = "8080"
        const val DEFAULT_KUBERNETES_ENABLE = "true"
        val apiVersions = listOf("leia.klira.io/v1") // supported versions
    }

    // classes representing Custom Resource Definition in Kubernetes
    data class Routes(val apiVersion: String, val items: List<RouteItem>)

    data class RouteItem(val apiVersion: String, val kind: String, val spec: Route)
    data class Route(val path: String,
                     val topic: String,
                     val format: String? = null,
                     val verify: Boolean? = null,
                     val methods: Collection<String>? = null,
                     val cors: List<String>? = null,
                     val response: HttpStatusCode? = null,
                     val sink: String? = null,
                     val authenticateUsing: List<String>? = null,
                     val validateJson: Boolean?,
                     val jsonSchema: String? = null) {
        fun toMap(): Map<String, Any> {
            val map = hashMapOf("path" to path, "topic" to topic, "format" to format, "verify" to verify,
                "methods" to methods, "cors" to cors, "response" to response, "sink" to sink,
                "authenticateUsing" to authenticateUsing, "validateJson" to validateJson, "jsonSchema" to jsonSchema)
            return map.filter { it.value != null }.map { it.key to it.value as Any }.toMap()
        }
    }

    data class Sinks(val apiVersion: String, val items: List<SinkItem>)

    data class SinkItem(val apiVersion: String, val kind: String, val spec: Sink)
    data class Sink(val name: String,
                    val default: Boolean? = null,
                    val type: String? = null,
                    val options: Map<String, Any>? = null) {
        fun toMap(): Map<String, Any> {
            val map = HashMap<String, Any>()
            map["name"] = name
            default?.let { map["default"] = it }
            type?.let { map["type"] = it }
            options?.let { map["options"] = it }
            return map
        }
    }
}

