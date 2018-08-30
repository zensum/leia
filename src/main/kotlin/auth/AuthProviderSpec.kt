package auth

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
     * Module specific options or config that is needed for the [AuthProvider]
     */
    val options: Map<String, Any> = emptyMap()
)

//typealias AuthProviderCreator<T, S> = (AuthProviderSpec<T, S>) -> AuthProvider<S>