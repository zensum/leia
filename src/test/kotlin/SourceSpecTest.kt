import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
                    "format" to Format.PROTOBUF,
                    "verify" to true,
                    "allowedMethods" to listOf(HttpMethod.Post),
                    "corsHosts" to emptyList<String>(),
                    "response" to HttpStatusCode.Accepted,
                    "authenticateUsing" to listOf("no_auth")
                )
            )
        }
    }
}