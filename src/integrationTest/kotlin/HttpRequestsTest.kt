package se.zensum.leia.integrationTest

import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import org.eclipse.jetty.http.HttpStatus
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPubSub
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpRequestsTest : IntegrationTestBase() {

    private fun getPath(path: String) = Request(HttpMethod.Get, path, emptyMap(), "")

    @Test
    fun simpleTest() {
        val b = getReqBuilder(getPath("/"))
        assertEquals(HttpStatus.NO_CONTENT_204, HttpClient().getResponseCode(b))
    }

    @Test
    fun notAllowedMethod() {
        val req = getPath("/").copy(method = HttpMethod.Delete)
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED_405, req.getResponse())
    }

    /* Test CORS */
    private fun validCors() = getPath("/with_cors").copy(headers = mapOf("Origin" to "http://example.com"))

    private fun invalidCors() = validCors().copy(headers = mapOf("Origin" to "http://invalid.example.com"))
    private fun corsPreflight() = validCors().copy(method = HttpMethod.Options)

    @Test
    fun corsPreflightOnCorsPath() {
        assertEquals(200, corsPreflight().getResponse())
    }

    @Test
    fun corsPreflightOnNonCorsPath() {
        assertEquals(HttpStatus.NO_CONTENT_204, corsPreflight().copy(path = "/").getResponse())
    }

    @Test
    fun invalidCorsPreflightOnCors() {
        val req = invalidCors().copy(method = HttpMethod.Options)
        assertEquals(HttpStatus.FORBIDDEN_403, req.getResponse())
    }

    @Test
    fun corsOnCorsPath() {
        assertEquals(HttpStatus.NO_CONTENT_204, validCors().getResponse())
    }

    @Test
    fun corsOnNonCorsPath() {
        assertEquals(HttpStatus.NO_CONTENT_204, validCors().copy(path = "/").getResponse())
    }

    @Test
    fun invalidCorsOnCorsPath() {
        assertEquals(HttpStatus.FORBIDDEN_403, invalidCors().getResponse())
    }

    /* Test Redis */

    @Test
    fun sendMessageToRedis() {
        assertEquals(HttpStatus.NO_CONTENT_204, getPath("/redis").getResponse())
    }

    @Test(timeout = 10000)
    fun receiveOneMessageFromRedisTest() {
        val host = environment.getServiceHost("redis", 6379)
        val port = environment.getServicePort("redis", 6379)
        var messagesReceived = 0
        val jedis = Jedis(host, port)
        thread {
            jedis.subscribe(object : JedisPubSub() {
                override fun onMessage(channel: String?, message: String?) {
                    super.onMessage(channel, message)
                    assertEquals("test", channel)
                    messagesReceived++
                }
            }, "test")
        }
        Thread.sleep(500)
        assertEquals(0, messagesReceived)
        assertEquals(HttpStatus.NO_CONTENT_204, getPath("/redis").getResponse())
        Thread.sleep(500)
        assertEquals(1, messagesReceived)
        jedis.close()
    }

    /* Test JSON */
    private fun postJson() = getPath("/json").copy(method = HttpMethod.Post)

    @Test
    fun invalidRequestTest() {
        val b = getReqBuilder(getPath("/invalid"))
        assertEquals(HttpStatus.NOT_FOUND_404, HttpClient().getResponseCode(b))
    }

    @Test
    fun validateJsonTest() {
        val b = getReqBuilder(postJson().copy(body = json))
        assertEquals(HttpStatus.NO_CONTENT_204, HttpClient().getResponseCode(b))
    }

    @Test
    fun validateInvalidJsonTest() {
        val b = getReqBuilder(postJson().copy(body = invalidJson))
        assertEquals(HttpStatus.BAD_REQUEST_400, HttpClient().getResponseCode(b))
    }

    private val json = """
    {
      "firstName": "John",
      "lastName": "Doe",
      "age": 21
    }
    """.trimIndent()

    private val invalidJson = """
    {
      "firstName": "John",
      "lastName": "Doe",
      "age": "21"
    }
    """.trimIndent()
}