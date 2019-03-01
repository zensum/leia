package se.zensum.leia.integrationTest

import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
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
        val b = getReqBuilder(postJson().copy(body = invalidJsonSchema))
        assertEquals(HttpStatus.BAD_REQUEST_400, HttpClient().getResponseCode(b))
    }

    /* Test Redis */
    private fun mkJedis() = Jedis(
        environment.getServiceHost("redis", 6379),
        environment.getServicePort("redis", 6379)
    )

    private fun Jedis.onMessage(channel: String, callback: (channel: String?, message: String?) -> Unit) {
        thread {
            subscribe(object : JedisPubSub() {
                override fun onMessage(_channel: String?, _message: String?) {
                    super.onMessage(_channel, _message)
                    callback.invoke(_channel, _message)
                }
            }, channel)
        }
    }

    private fun checkMessageReceived(valid: Boolean, messages: AtomicInt, body: () -> Unit) {
        Thread.sleep(500)
        assertEquals(0, messages.value, "Should be no messages")
        body.invoke()
        Thread.sleep(500)
        assertEquals(if (valid) 1 else 0, messages.value, "Unexpected number of messages received")
    }

    @Test
    fun sendMessageToRedis() {
        assertEquals(HttpStatus.NO_CONTENT_204, getPath("/redis").getResponse())
    }

    private fun verifyMessageRedis(path: String, channel: String, valid: Boolean = true, body: String = json) {
        val req = postJson().copy(path = path, body = body)
        val messagesReceived = atomic(0)
        val receivedMessage = atomic<String?>(null)
        mkJedis().use {
            it.onMessage(channel) { _channel, message ->
                assertEquals(channel, _channel)
                messagesReceived.getAndIncrement()
                receivedMessage.value = message
            }
            checkMessageReceived(valid, messagesReceived) {
                assertEquals(if (valid) HttpStatus.NO_CONTENT_204 else HttpStatus.BAD_REQUEST_400, req.getResponse())
            }
            assertEquals(if (valid) body else null, receivedMessage.value, "Unexpected message")
        }
    }

    @Test(timeout = 10000)
    fun verifyMessageRedisTest() {
        verifyMessageRedis("/redis", "test")
    }

    @Test(timeout = 10000)
    fun verifyMessageJsonRedisTest() {
        verifyMessageRedis("/redis/json", "test")
    }

    @Test(timeout = 10000)
    fun verifyMessageJsonSchemaRedisTest() {
        verifyMessageRedis("/redis/json_schema", "test")
    }

    @Test(timeout = 10000)
    fun verifyMessageInvalidJsonRedisTest() {
        verifyMessageRedis("/redis/json", "test", valid = false, body = invalidJson)
    }

    @Test(timeout = 10000)
    fun verifyMessageInvalidJsonSchemaRedisTest() {
        verifyMessageRedis("/redis/json_schema", "test", valid = false, body = invalidJsonSchema)
    }

    private val json = """
    {
      "firstName": "John",
      "lastName": "Doe",
      "age": 21
    }
    """.trimIndent()

    private val invalidJsonSchema = """
    {
      "firstName": "John",
      "lastName": "Doe",
      "age": "21"
    }
    """.trimIndent()

    private val invalidJson = """
    {
      "firstName": "John",
      "lastName": "Doe",
      "age":
    """.trimIndent()
}