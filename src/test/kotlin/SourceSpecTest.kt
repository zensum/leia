import auth.JwkAuth
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import leia.ssr
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.zensum.leia.auth.AuthProvider
import se.zensum.leia.auth.AuthProviderFactory
import se.zensum.leia.auth.AuthProviderSpec
import se.zensum.leia.auth.BasicAuth
import se.zensum.leia.auth.DefaultAuthProviderFactory
import se.zensum.leia.config.Format
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
                    "authenticateUsing" to listOf("no_auth")
                )
            )
        }
    }
}