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
import kotlin.test.*

private val routes: Map<String, TopicRouting> = getRoutes("src/test/routes")
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

    @Test fun testOptionsCorsHeaders() {
        val p = MockProducer<String, ByteArray>()
        withApp(p) {
            with(handleRequest(HttpMethod.Options, "/cors-test", {
                addHeader("Origin", "https://HackerMan.net")
                addHeader("Access-Control-Request-Method", "POST")
            })) {
                val allowOrigin = response.headers["Access-Control-Allow-Origin"]
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("*", allowOrigin)
            }
        }
    }

    
}