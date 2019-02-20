package se.zensum.leia.integrationTest

import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpRequestsTest : IntegrationTestBase() {

    private fun getPath(path: String) = Request(HttpMethod.Get, path, emptyMap(), "")
    private fun postJson() = getPath("/json").copy(method = HttpMethod.Post)

    @Test
    fun simpleTest() {
        val b = getReqBuilder(getPath("/"))
        assertEquals(204, HttpClient().getResponseCode(b))
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