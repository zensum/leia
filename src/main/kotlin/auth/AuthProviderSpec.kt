package se.zensum.leia.auth

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
     * Module specific options or config that is needed for the [AuthProvider]
     */

    val options: Map<String, Any> = emptyMap()
) {
    companion object {
    	fun fromMap(config: Map<String, Any>): AuthProviderSpec {
            val options: Map<String, Any> = config
                .filterKeys { it !in listOf("type", "name") }

            return AuthProviderSpec(
                name = config["name"].toString(),
                type = config.getOrDefault("type", "no_auth").toString(),
                options = options
            )
        }
    }
}