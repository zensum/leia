package se.zensum.leia.auth

import se.zensum.leia.auth.AuthProvider

data class AuthProviderSpec(
    /**
     *
     */
    val name: String,
    /**
     * Key to identify the auth module type in the configuration, such
     * as "jwt", "basic_auth" or other supported auth module.
     */
    val type: String,

    /**
     * True if this is specified in the config as the default
     * spec for Auth-Provider
     */
    val isDefault: Boolean,

    /**
     * Module specific options or config that is needed for the [AuthProvider]
     */

    val options: Map<String, Any> = emptyMap()
) {
    companion object {
    	fun fromMap(config: Map<String, Any>): AuthProviderSpec {
            val options: Map<String, Any> = config
                .filterKeys { it.startsWith("auth-providers.") }
                .mapKeys { it.key.replace("auth-providers.", "") }

            return AuthProviderSpec(
                name = config["name"].toString(),
                type = config.getOrDefault("type", "no_auth").toString(),
                isDefault = config.getOrDefault("default", false).toString().toBoolean(),
                options = options
            )
        }
    }
}