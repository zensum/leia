package se.zensum.leia.auth

import io.ktor.http.HttpMethod
import leia.logic.IncomingRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import toBase64
import java.util.*

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
        "Leia" to  "abb9eca0af80e9f321af97b2952ca035edfdc63d5118299e32047156339955b3"// "It's a trap!"
    )

    /**
     * Credentials as they would be received by Leia from the authorizing party, in base64 encoded form
     */
    private val base64Credentials: Map<String, String> = credentials
        .mapValues { "${it.key}:${it.value}" }
        .mapValues { it.value.toByteArray().toBase64() }

    @Test
    fun testVerifyingCredentialsAsValid() {
        val auth: BasicAuth = BasicAuth(hashedCredentials)
        val b64BasicAuth: String = base64Credentials["Luke"]!!
        val result: AuthResult = auth.verify(b64BasicAuth)

        assertTrue(result is AuthResult.Authorized)
        result as AuthResult.Authorized
        assertEquals("Luke", result.identifier)
    }

    @Test
    fun testVerifyingCredentialsAsInvalid() {
        val auth: BasicAuth = BasicAuth(hashedCredentials)
        val unauthorizedUser: String = "Vader:Rebel scum"
            .toByteArray()
            .toBase64()
        val result: AuthResult = auth.verify(unauthorizedUser)
        assertEquals(AuthResult.Denied, result)
    }

    @Test
    fun testRequire256BitCheckSum() {
        assertThrows<IllegalArgumentException> {
            val badCredentials = mapOf("Vader" to "f472d02c7d9470429d5a2b49b9c5fbe3699f1722")
            BasicAuth(badCredentials)
        }
    }
}