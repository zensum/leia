package se.zensum.leia.integrationTest

import io.ktor.client.HttpClient
import io.ktor.client.call.call
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpMethod
import kotlinx.coroutines.experimental.runBlocking
import leia.IntegrationTestBaseRule

open class IntegrationTestBase: IntegrationTestBaseRule() {
    companion object {
        fun getReqBuilder(req: Request): HttpRequestBuilder {
            val builder = HttpRequestBuilder()
            builder.method = req.method
            for (header in req.headers) {
                builder.headers.appendAll(header.key, header.value)
            }
            builder.url.encodedPath = req.path
            builder.body = req.body
            return builder
        }
    }

    data class Request(val method: HttpMethod,
                       val path: String,
                       val headers: Map<String, List<String>>,
                       val body: String)
}

fun HttpClient.getResponseCode(builder: HttpRequestBuilder) = this.let { runBlocking { it.call(builder).response.status.value } }