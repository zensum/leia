package se.zensum.leia

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import frans.producer.mock.MockProducer
import frans.producer.mock.MockProducerFactory
import franz.ProducerBuilder
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.Test
import se.zensum.jwt.JWTProvider
import se.zensum.leia.config.TomlConfigProvider
import se.zensum.leia.config.TopicRouting
import kotlin.test.*

private val routes = TomlConfigProvider.fromPath("src/test/routes").getRoutes()
private val DUMMY_JWT =
    JWT.decode("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ")

private val jwtProvider = object : JWTProvider {
    override fun verifyJWT(token: String): DecodedJWT? {
        return if (token == "Account1")
            DUMMY_JWT
        else null
    }
}

private fun withApp(mp: MockProducer<String, ByteArray>, test: TestApplicationEngine.() -> Unit) {
    val p = ProducerBuilder.ofByteArray
        .setProducer(MockProducerFactory(mp)).create()
    return withTestApplication(leia(p, routes, jwtProvider, false), test)
}

class RequestTest {

    @Test fun test404() {
        val p = MockProducer<String, ByteArray>()
        withApp(p) {
            with(handleRequest(HttpMethod.Get, "/url-that-doesnt-exist")) {
                //assertEquals(HttpStatusCode.NotFound, response.status())
                // Status is null in the test-env!
                assertNull(response.status())
            }
        }
    }
    @Test fun testNotAuth() {
        val p = MockProducer<String, ByteArray>()
        withApp(p) {
            with(handleRequest(HttpMethod.Post, "/auth")) {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }
    @Test fun testAuthenticated() {
        val p = MockProducer<String, ByteArray>()
        withApp(p) {
            with(handleRequest(HttpMethod.Post, "/auth", {
                this.addHeader("Authorization", "Bearer Account1")
            })) {
                assertEquals(HttpStatusCode.NoContent, response.status())
            }
        }
    }

    @Test fun testGetCorsHeaders() {
        val p = MockProducer<String, ByteArray>()
        withApp(p) {
            with(handleRequest(HttpMethod.Get, "/cors-test", {
                addHeader("Origin", "https://HackerMan.net")
            })) {
                val allowOrigin = response.headers["Access-Control-Allow-Origin"]
                assertEquals("*", allowOrigin)
            }
        }
    }

    @Test fun testPostCorsHeaders() {
        val p = MockProducer<String, ByteArray>()
        withApp(p) {
            with(handleRequest(HttpMethod.Post, "/cors-test", {
                addHeader("Origin", "https://HackerMan.net")
            })) {
                val allowOrigin = response.headers["Access-Control-Allow-Origin"]
                assertEquals("*", allowOrigin)
            }
        }
    }

    @Test fun testStatusCode() {
        val p = MockProducer<String, ByteArray>()
        withApp(p) {
            with(handleRequest(HttpMethod.Get, "/status-test")) {
                assertEquals(HttpStatusCode.fromValue(255), response.status())
            }
        }
    }

    @Test fun testUnsupportedMethod() {
        val p = MockProducer<String, ByteArray>()
        withApp(p) {
            with(handleRequest(HttpMethod.Put, "/cors-test", {
            })) {
                assertEquals(HttpStatusCode.MethodNotAllowed, response.status())
            }
        }
    }


    @Test fun testWritingToTopic() {
        var sentBytes = 0
        val p = MockProducer<String, ByteArray>(onSendAsync = {
            sentBytes = it.size
        })
        val example = "hoi m8"
        withApp(p) {
            with(handleRequest(HttpMethod.Put, "/test", {
                body = example
            })) {
                assert(sentBytes > example.length) {
                    "np/o sent bytes should be longer than the body"
                }
            }
        }
    }
}