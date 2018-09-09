package se.zensum.leia.auth

import auth.MockJWTDecoder
import leia.logic.IncomingRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.zensum.leia.auth.jwk.JwkAuth

// Example where "sub" is "foo" and "alg" is "RS256"
private const val EXAMPLE_JWT = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJmb28iLCJpYXQiOjE1MTYyMzkwMjJ9.YPbmFuYu4MUGz8k-z7skOdElhYchqjaxRXxJwBAZTQYqy2DB0NnmRqrvFft4Mb29VR1V9DtsMK_gMLiHsPug1B4YxscdsNTXfbiuqbV0_9ZrZ2OjUTQsi_EaxMiiVSh97Ud2ZAWmKQGffx5lnOeD15INdJo9BSWPE1ca6T0o-dM"

class JwkAuthTest {

    private fun jwkAuthFromSpec(): AuthProvider {
        val options = mapOf<String, Any>(
            "jwk_config" to mapOf<String, String>(
                "jwk_issuer" to "x",
                "jwk_url" to "https://xx.yy"
            )
        )
        val spec = AuthProviderSpec("some_auth", "jwk", options)

        return DefaultAuthProviderFactory.create(spec)
    }

    @Test
    fun `check that JWK auth is loaded`() {
        val spec: AuthProvider = jwkAuthFromSpec()
        assertTrue(spec is JwkAuth)
    }

    @Test
    fun `missing Authorization headers results in Denied`() {
        val auth = JwkAuth(MockJWTDecoder.Valid)
        val requestWithNoHeaders: IncomingRequest = genericRequest()
        val result: AuthResult = auth.verify(emptyList(), requestWithNoHeaders)
        assertTrue(result is AuthResult.Denied)
    }

    @Test
    fun `present Authorization headers results in Authorized`() {
        val auth = JwkAuth(MockJWTDecoder.Valid)
        val requestWithHeaders: IncomingRequest
            = genericRequest(mapOf("Authorization" to listOf("Bearer $EXAMPLE_JWT")))
        val result: AuthResult = auth.verify(emptyList(), requestWithHeaders)
        assertTrue(result is AuthResult.Authorized)
        result as AuthResult.Authorized
        assertEquals("foo", result.identifier)
    }
}