package se.zensum.leia.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertNull

class SpecsAuthProviderTest {

    @Test
    fun `test combiner with one Successful auth`() {
        val authProviders: List<AuthProviderSpec> = listOf(
            basicAuthProviderSpecWithCredentials("spec1"),
            AuthProviderSpec("spec2", "jwk", mapOf(
                "jwk_config" to mapOf<String, String>(
                    "jwk_url" to "https://klira.io",
                    "jwk_issuer" to "my-issuer"
                )
            ))
        )
        val sap = SpecsAuthProvider(authProviders, DefaultAuthProviderFactory)
        val request = genericRequest(mapOf<String, List<String>>(
            "Authorization" to listOf(basicAuthHeaderValue("user-x", "x"))
        ))
        val result: AuthResult = sap.verify(listOf("spec1", "spec2"), request)
        assertTrue(result is AuthResult.Authorized, result::class.toString())
        result as AuthResult.Authorized
        assertEquals("user-x", result.identifier)
    }

    @Test
    fun `test combiner with one Denied auth`() {
        val authProviders: List<AuthProviderSpec> = listOf(
            basicAuthProviderSpecWithCredentials("spec1"),
            AuthProviderSpec("spec2", "jwk", mapOf(
                "jwk_config" to mapOf<String, String>(
                    "jwk_url" to "https://klira.io",
                    "jwk_issuer" to "my-issuer"
                )
            ))
        )
        val sap = SpecsAuthProvider(authProviders, DefaultAuthProviderFactory)
        val request = genericRequest(mapOf<String, List<String>>(
            "Authorization" to listOf(basicAuthHeaderValue("user-x", "!!wrong secret!!"))
        ))
        val result: AuthResult = sap.verify(listOf("spec1", "spec2"), request)
        assertTrue(result is AuthResult.Denied, result::class.toString())
    }

    @Test
    fun `test combiner throws with two Successful auth`() {
        val authProviders: List<AuthProviderSpec> = listOf(
            basicAuthProviderSpecWithCredentials("spec1"),
            basicAuthProviderSpecWithCredentials("spec2")
        )
        val sap = SpecsAuthProvider(authProviders, DefaultAuthProviderFactory)
        val request = genericRequest(mapOf<String, List<String>>(
            "Authorization" to listOf(basicAuthHeaderValue("user-x", "x"))
        ))
        assertThrows<UnsupportedOperationException> {
            sap.verify(listOf("spec1", "spec2"), request)
        }
    }

    @Test
    fun `test combiner with no auth`() {
        val authProviders: List<AuthProviderSpec> = listOf(
            AuthProviderSpec("spec1", "no_auth")
        )
        val sap = SpecsAuthProvider(authProviders, DefaultAuthProviderFactory)
        val request = genericRequest()
        val result = sap.verify(listOf("spec1"), request)
        assertTrue(result is AuthResult.Authorized)
        result as AuthResult.Authorized
        assertNull(result.identifier)
    }
}