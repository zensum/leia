package auth

import se.zensum.leia.auth.AuthProvider
import se.zensum.leia.auth.AuthProviderSpec
import se.zensum.leia.auth.DefaultAuthProviderFactory

class AuthProviderTest {
    private fun jwkAuthFromSpec(): AuthProvider
    {
        val options = mapOf<String, Any>(
            "jwk_issuer" to "x",
            "jwk_url" to "https://xx.yy"
        )
        val spec = AuthProviderSpec("some_auth", "jwk_auth", true, options)

        return DefaultAuthProviderFactory.create(spec)
    }

    private fun basicAuthFromSpec(): AuthProvider
    {
        val options = mapOf<String, Any>(
            "basic_auth_users" to mapOf<String, String>(

            )
        )
        val spec = AuthProviderSpec("some_auth", "jwk_auth", true, options)

        return DefaultAuthProviderFactory.create(spec)
    }

    fun `deny request without proper credentials`() {

    }
}