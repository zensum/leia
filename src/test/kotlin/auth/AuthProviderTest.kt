package auth

import io.ktor.http.HttpMethod
import leia.logic.IncomingRequest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.zensum.leia.auth.AuthProvider
import se.zensum.leia.auth.AuthProviderSpec
import se.zensum.leia.auth.AuthResult
import se.zensum.leia.auth.DefaultAuthProviderFactory
import java.util.*

class AuthProviderTest {
    private fun jwkAuthFromSpec(): AuthProvider
    {
        val options = mapOf<String, Any>(
            "jwk_issuer" to "x",
            "jwk_url" to "https://xx.yy"
        )
        val spec = AuthProviderSpec("some_auth", "jwk", true, options)

        return DefaultAuthProviderFactory.create(spec)
    }

    private fun basicAuthFromSpec(): AuthProvider
    {
        val options = mapOf<String, Any>(
            "basic_auth_users" to mapOf<String, String>(
                "bank-x" to "2d711642b726b04401627ca9fbac32f5c8530fb1903cc4db02258717921a4881", // "x"
                "bank-y" to "a1fce4363854ff888cff4b8e7875d600c2682390412a8cf79b37d0b11148b0fa", // "y"
                "bank-z" to "594e519ae499312b29433b7dd8a97ff068defcba9755b6d5d00e84c524d67b06" // "z
            )
        )
        val spec = AuthProviderSpec("some_auth", "basic_auth", true, options)

        return DefaultAuthProviderFactory.create(spec)
    }

    @Test
    fun `allow request without proper credentials`() {
        val basicAuth: AuthProvider = basicAuthFromSpec()
        val req = IncomingRequest(
            method = HttpMethod.Post,
            origin = null,
            jwt = null,
            path = "/",
            headers = mapOf<String, List<String>>(
                "Authorization" to listOf(basicAuthHeaderValue("bank-x", "x"))
            ),
            queryString = "",
            host = null,
            readBodyFn = { ByteArray(0) }
        )
        val result: AuthResult = basicAuth.verify(listOf("some_auth"), req)
        assertTrue(result is AuthResult.Authorized)
    }


    @Test
    fun `deny request without invalid credentials`() {
        val basicAuth: AuthProvider = basicAuthFromSpec()
        val req = IncomingRequest(
            method = HttpMethod.Post,
            origin = null,
            jwt = null,
            path = "/",
            headers = mapOf<String, List<String>>(
                "Authorization" to listOf(basicAuthHeaderValue("bank-x", "wrong secret!"))
            ),
            queryString = "",
            host = null,
            readBodyFn = { ByteArray(0) }
        )
        val result: AuthResult = basicAuth.verify(listOf("some_auth"), req)
        assertTrue(result is AuthResult.Denied)
    }

    @Test
    fun `deny request without no credentials`() {
        val basicAuth: AuthProvider = basicAuthFromSpec()
        val req = IncomingRequest(
            method = HttpMethod.Post,
            origin = null,
            jwt = null,
            path = "/",
            headers = emptyMap(),
            queryString = "",
            host = null,
            readBodyFn = { ByteArray(0) }
        )
        val result: AuthResult = basicAuth.verify(listOf("some_auth"), req)
        assertTrue(result is AuthResult.Denied)
    }

    private fun basicAuthHeaderValue(user: String, password: String): String = Base64
        .getEncoder()
        .encodeToString("$user:$password".toByteArray())!!
        .let { "Basic $it" }
}