package se.zensum.leia.auth

import io.ktor.http.HttpMethod
import leia.logic.IncomingRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import com.mantono.pyttipanna.transformation.Base64 as PBase64

class BasicAuthTest {
    /**
     * Credentials as they would be entered by the connecting user / application
     * that needs to be authenticated, before they are base64 encoded
     */
    private val credentials: Map<String, String> = mapOf(
        "Luke" to "Echo Three to Echo Seven",
        "R2-D2" to "beep beep boop",
        "Leia" to "It's a trap!"
    )

    /**
     * Credentials (where passwords are hashed) as they would be stored in the configuration
     * used by Leia
     */
    private val hashedCredentials: Map<String, String> = mapOf(
        "Luke" to "3df8232404be55d2d6b85be79b99316e028676b69efa2ce91d9094dd18dd3502", // "Echo Three to Echo Seven"
        "R2-D2" to "c45441cb346b69e247711e9c86405ce9de964bb89c68ad02b8eb2f55912c58ba", // "beep beep boop"
        "Leia" to "abb9eca0af80e9f321af97b2952ca035edfdc63d5118299e32047156339955b3"// "It's a trap!"
    )

    /**
     * Credentials as they would be received by Leia from the authorizing party, in base64 encoded form
     */
    private val base64Credentials: Map<String, String> = credentials
        .mapValues { "${it.key}:${it.value}" }
        .mapValues { it.value.toByteArray().toBase64() }

    @Test
    fun testDeniedOnBearerInsteadOfBasicInHeader() {
        val auth = BasicAuth(hashedCredentials)
        val req: IncomingRequest = genericRequest(mapOf(
            "Authorization" to listOf("Bearer Vader:Rebel scum")
        ))
        val result: AuthResult = auth.verify(emptyList(), req)
        assertTrue(result is AuthResult.Denied)
    }

    @Test
    fun testVerifyingCredentialsAsValid() {
        val auth = BasicAuth(hashedCredentials)
        val b64BasicAuth: String = base64Credentials.getValue("Luke")
        val result: AuthResult = auth.verify(b64BasicAuth)

        assertTrue(result is AuthResult.Authorized)
        result as AuthResult.Authorized
        assertEquals("Luke", result.identifier)
    }

    @Test
    fun testVerifyingCredentialsAsInvalid() {
        val auth = BasicAuth(hashedCredentials)
        val unauthorizedUser: String = "Vader:Rebel scum"
            .toByteArray()
            .toBase64()
        val result: AuthResult = auth.verify(unauthorizedUser)
        assertEquals(AuthResult.Denied.InvalidCredentials, result)
    }

    @Test
    fun testDenyCredentialsWithNonBase64() {
        val auth = BasicAuth(hashedCredentials)
        val unauthorizedUser = "Vader:Rebel scum"
        val result: AuthResult = auth.verify(unauthorizedUser)
        assertEquals(AuthResult.Denied.InvalidCredentials, result)
    }

    @Test
    fun testRequire256BitCheckSum() {
        assertThrows<IllegalArgumentException> {
            val badCredentials = mapOf("Vader" to "f472d02c7d9470429d5a2b49b9c5fbe3699f1722")
            BasicAuth(badCredentials)
        }
    }

    @Test
    fun `allow request with proper credentials`() {
        val basicAuth: AuthProvider = basicAuthFromSpec()
        val req = genericRequest(mapOf(
            "Authorization" to listOf(basicAuthHeaderValue("user-x", "x"))
        ))
        val result: AuthResult = basicAuth.verify(listOf("some_auth"), req)
        assertTrue(result is AuthResult.Authorized)
    }


    @Test
    fun `deny request with invalid credentials`() {
        val basicAuth: AuthProvider = basicAuthFromSpec()
        val req = genericRequest(mapOf(
            "Authorization" to listOf(basicAuthHeaderValue("user-x", "wrong secret!"))
        ))
        val result: AuthResult = basicAuth.verify(listOf("some_auth"), req)
        assertTrue(result is AuthResult.Denied)
    }

    @Test
    fun `deny request without no credentials`() {
        val basicAuth: AuthProvider = basicAuthFromSpec()
        val req = genericRequest()
        val result: AuthResult = basicAuth.verify(listOf("some_auth"), req)
        assertTrue(result is AuthResult.Denied)
    }
}

internal fun genericRequest(headers: Map<String, List<String>> = emptyMap()) = IncomingRequest(
    method = HttpMethod.Post,
    origin = null,
    path = "/",
    headers = headers,
    queryString = "",
    host = null,
    readBodyFn = { ByteArray(0) }
)

internal fun basicAuthHeaderValue(user: String, password: String): String = Base64
    .getEncoder()
    .encodeToString("$user:$password".toByteArray())!!
    .let { "Basic $it" }

internal fun basicAuthProviderSpecWithCredentials(name: String = "some_auth"): AuthProviderSpec {
    val options = mapOf<String, Any>(
        "basic_auth_users" to mapOf(
            "user-x" to "2d711642b726b04401627ca9fbac32f5c8530fb1903cc4db02258717921a4881", // "x"
            "user-y" to "a1fce4363854ff888cff4b8e7875d600c2682390412a8cf79b37d0b11148b0fa", // "y"
            "user-z" to "594e519ae499312b29433b7dd8a97ff068defcba9755b6d5d00e84c524d67b06" // "z
        )
    )
    return AuthProviderSpec(name, "basic_auth", options)
}

private fun basicAuthFromSpec(): AuthProvider {
    val spec = basicAuthProviderSpecWithCredentials()
    return DefaultAuthProviderFactory.create(spec)
}

private fun ByteArray.toBase64() = PBase64.asString(this)
