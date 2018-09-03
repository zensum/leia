package se.zensum.leia.config

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import se.zensum.leia.httpMethods

enum class Format { RAW_BODY, PROTOBUF }

data class SourceSpec(val path: String,
                      val topic: String,
                      val format: Format = Format.PROTOBUF,
                      val verify: Boolean = false,
                      private val allowedMethods: Collection<HttpMethod>,
                      val corsHosts: List<String>,
                      val response: HttpStatusCode,
                      val sink: String? = null,
                      val authenticateUsing: List<String>) {
    val allowedMethodsSet = allowedMethods.toSet()
    companion object {
        private inline fun <reified T> uneraseType(xs: Iterable<*>): List<T> =
            xs.map {
              if (it is T) {
                  it as T
              } else throw RuntimeException("rhee")
            }.toList()

        private fun parseFormat(x: Any?): Format = when(x) {
            "raw_body" -> Format.RAW_BODY
            "proto" -> Format.PROTOBUF
            null -> Format.PROTOBUF
            else -> throw IllegalArgumentException("Denied value for config parameter 'format'")
        }

        private fun parseCors(cors: Any?): List<String> = when(cors) {
            null -> emptyList()
            is Iterable<*> -> uneraseType(cors)
            else -> throw RuntimeException("rhee")
        }

        private fun parseMethods(methods: Any?): Set<HttpMethod> = when(methods) {
            null -> httpMethods.verbs
            is Iterable<*> ->
                uneraseType<String>(methods)
                    .map { HttpMethod.parse(it) }
                    .toSet()
            else -> throw RuntimeException("rhee")
        }

        private fun parseResponse(response: Any?): Int = when(response) {
            null -> 204
            is Number -> response.toInt()
            else -> throw RuntimeException("rhee")
        }

        fun fromMap(m: Map<String, Any>): SourceSpec = SourceSpec(
                // name = m["name"] as String,
                path = m["path"] as String,
                topic = m["topic"] as String,
                format = parseFormat(m["format"]),
                corsHosts = parseCors(m["cors"]),
                verify = (m["verify"] ?: false) as Boolean,
                allowedMethods = parseMethods(m["methods"]),
                response = HttpStatusCode.fromValue(parseResponse(m["response"])),
                sink = m["sink"]?.toString()?.takeIf { it.isNotBlank() },
                authenticateUsing = m["auth-providers"]
                    ?.let { it as List<Map<String, Any>> }
                    ?.map { it["name"].toString() }
                    ?.toList()
                    ?: emptyList()
        )

    }
}
