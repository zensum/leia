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
            SourceSpec(
                path = "/",
                topic = "topic",
                format = Format.PROTOBUF,
                verify = true,
                allowedMethods = listOf(HttpMethod.Post),
                corsHosts = emptyList(),
                response = HttpStatusCode.Accepted,
                authenticateUsing = listOf("no_auth")
            )
        }
    }
}