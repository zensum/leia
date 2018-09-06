package auth

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.zensum.leia.auth.AuthProvider
import se.zensum.leia.auth.AuthProviderSpec
import se.zensum.leia.auth.DefaultAuthProviderFactory

class JwkAuthTest {

    private fun jwkAuthFromSpec(): AuthProvider {
        val options = mapOf<String, Any>(
            "jwk_config" to mapOf<String, String>(
                "jwk_issuer" to "x",
                "jwk_url" to "https://xx.yy"
            )
        )
        val spec = AuthProviderSpec("some_auth", "jwk", true, options)

        return DefaultAuthProviderFactory.create(spec)
    }

    @Test
    fun `check that JWK auth is loaded`() {
        val spec: AuthProvider = jwkAuthFromSpec()
        assertTrue(spec is JwkAuth)
    }
}