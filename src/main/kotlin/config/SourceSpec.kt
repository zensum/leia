package se.zensum.leia.config

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode

enum class Format { RAW_BODY, PROTOBUF }

data class SourceSpec(val path: String,
                      val topic: String,
                      val format: Format = Format.PROTOBUF,
                      private val allowedMethods: Collection<HttpMethod> = HttpMethod.DefaultMethods,
                      val corsHosts: List<String>,
                      val response: HttpStatusCode,
                      val sink: String? = null,
                      val authenticateUsing: List<String>,
                      val validateJson: Boolean,
                      val jsonSchema: String) {
    val allowedMethodsSet = allowedMethods.toSet()

    init {

    }

    companion object {
        private inline fun <reified T> uneraseType(xs: Iterable<*>): List<T> =
            xs.map {
                it as? T ?: throw RuntimeException("Could not cast as ${T::class}")
            }.toList()

        private fun parseFormat(x: Any?): Format = when (x) {
            "raw_body" -> Format.RAW_BODY
            "proto" -> Format.PROTOBUF
            null -> Format.PROTOBUF
            else -> throw IllegalArgumentException("Denied value for config parameter 'format'")
        }

        private fun parseCors(cors: Any?): List<String> = when (cors) {
            null -> emptyList()
            is Iterable<*> -> uneraseType(cors)
            else -> throw RuntimeException("rhee")
        }

        private fun parseMethods(methods: Any?): Set<HttpMethod> = when (methods) {
            null -> HttpMethod.DefaultMethods.toSet()
            is Iterable<*> ->
                uneraseType<String>(methods)
                    .map { HttpMethod.parse(it) }
                    .toSet()
            else -> throw RuntimeException("Invalid method(s): $methods")
        }

        private fun parseAuthProviders(providers: Any?): List<String> = when (providers) {
            null -> emptyList()
            is Iterable<*> -> uneraseType(providers)
            else -> throw RuntimeException("Invalid auth provider(s): $providers")
        }

        private fun parseResponse(response: Any?): Int = when (response) {
            null -> 204
            is Number -> response.toInt()
            else -> throw RuntimeException("rhee")
        }

        fun fromMap(m: Map<String, Any>): SourceSpec {
            val verify: Boolean = m["verify"] as? Boolean ?: false
            val rawAuthProviders: List<String> = parseAuthProviders(m["auth_providers"])
            val authProviders: List<String> = if (verify) listOf("\$default_jwk_provider") else rawAuthProviders

            require(!(verify && rawAuthProviders.isNotEmpty())) {
                "Config parameter 'verify' cannot be true when an 'auth_provider' is " +
                    "also configured, these two options are mutually exclusive"
            }

            return specFromValidatedMap(m, authProviders)
        }

        private fun specFromValidatedMap(
            m: Map<String, Any>,
            authProviders: List<String>
        ): SourceSpec = SourceSpec(
            // name = m["name"] as String,
            path = m["path"] as String,
            topic = m["topic"] as String,
            format = parseFormat(m["format"]),
            corsHosts = parseCors(m["cors"]),
            allowedMethods = parseMethods(m["methods"]),
            response = HttpStatusCode.fromValue(parseResponse(m["response"])),
            sink = m["sink"]?.toString()?.takeIf { it.isNotBlank() },
            // IF verify is true and auth_providers is empty authenticateUsing is assigned the value ["$default_jwk_provider"]
            // This in conjunction with the rule that if JWK_URL is set a JWK auth-provider with the name $default_jwk_provider is
            // created, means that backward compat breaking changes were introduced for authenticateUsing...This fallback functionality
            // will be removed in a future version of the software.
            authenticateUsing = authProviders,
            validateJson = m["validateJson"] as? Boolean ?: false,
            jsonSchema = m["jsonSchema"] as? String ?: ""
        )
    }
}
