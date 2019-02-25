package se.zensum.leia.integrationTest

import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpRequestsTest : IntegrationTestBase() {

    private fun getPath(path: String) = Request(HttpMethod.Get, path, emptyMap(), "")
    private fun validCors() = getPath("/with_cors").copy(headers = mapOf("Origin" to "http://example.com"))
    private fun invalidCors() = validCors().copy(headers = mapOf("Origin" to "http://invalid.example.com"))
    private fun corsPreflight() = validCors().copy(method = HttpMethod.Options)
    private fun invalidPreflight() = invalidCors().copy(method = HttpMethod.Options)

    @Test
    fun corsPreflightOnCorsPath() {
        assertEquals(200, corsPreflight().getResponse())
    }

    @Test
    fun invalidCorsPreflightOnCors() {
        assertEquals(403, invalidPreflight().copy().getResponse())
    }

    @Test
    fun corsOnCorsPath() {
        assertEquals(204, validCors().getResponse())
    }

    @Test
    fun corsOnNonCorsPath() {
        assertEquals(204, validCors().copy(path = "/").getResponse())
    }

    @Test
    fun invalidCorsOnCorsPath() {
        assertEquals(403, invalidCors().getResponse())
    }

    private fun postJson() = getPath("/json").copy(method = HttpMethod.Post)

    @Test
    fun simpleTest() {
        val b = getReqBuilder(getPath("/"))
        assertEquals(204, HttpClient().getResponseCode(b))
    }

    @Test
    fun invalidRequestTest() {
        val b = getReqBuilder(getPath("/invalid"))
        assertEquals(404, HttpClient().getResponseCode(b))
    }

    @Test
    fun validateJsonTest() {
        val b = getReqBuilder(postJson().copy(body = json))
        assertEquals(204, HttpClient().getResponseCode(b))
    }

    @Test
    fun validateInvalidJsonTest() {
        val b = getReqBuilder(postJson().copy(body = invalidJson))
        assertEquals(400, HttpClient().getResponseCode(b))
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