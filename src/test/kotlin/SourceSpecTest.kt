
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.zensum.leia.config.SourceSpec

class SourceSpecTest {

    @Test
    fun `check that verify and auth-provider are not present at the same time`() {
        assertThrows<IllegalArgumentException> {
            SourceSpec.fromMap(
                mapOf<String, Any>(
                    "path" to "/",
                    "topic" to "topic",
                    "format" to "proto",
                    "verify" to true,
                    "allowedMethods" to listOf("POST"),
                    "corsHosts" to emptyList<String>(),
                    "response" to HttpStatusCode.Accepted.value,
                    "auth_providers" to listOf("no_auth")
                )
            )
        }
    }

    @Test
    fun `jwk_auth should be used when verify is true`() {
        // Essentially, check backwards compatibility with old config
        val spec = SourceSpec.fromMap(
            mapOf<String, Any>(
                "path" to "/",
                "topic" to "topic",
                "format" to "proto",
                "verify" to true,
                "allowedMethods" to listOf("POST"),
                "corsHosts" to emptyList<String>(),
                "response" to HttpStatusCode.Accepted.value,
                "auth_providers" to emptyList<String>()
            )
        )

        assertTrue("\$default_jwk_provider" in spec.authenticateUsing)
        assertEquals(1, spec.authenticateUsing.size)
    }

    @Test
    fun `no auth provider should be used when verify is false and list of providers is empty`() {
        val spec = SourceSpec.fromMap(
            mapOf<String, Any>(
                "path" to "/",
                "topic" to "topic",
                "format" to "proto",
                "verify" to false,
                "allowedMethods" to listOf("POST"),
                "corsHosts" to emptyList<String>(),
                "response" to HttpStatusCode.Accepted.value,
                "auth_providers" to emptyList<String>()
            )
        )

        assertTrue(spec.authenticateUsing.isEmpty())
    }
}