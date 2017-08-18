package se.zensum.webhook

import org.jetbrains.ktor.http.ContentType
import org.jetbrains.ktor.http.HttpMethod
import org.jetbrains.ktor.pipeline.PipelineContext
import org.jetbrains.ktor.request.*
import org.jetbrains.ktor.util.ValuesMap

data class PayLoad(val path: String,
                   val method: HttpMethod,
                   val host: String,
                   val headers: ValuesMap,
                   val authorized: String,
                   val contentType: ContentType,
                   val userAgent: String,
                   val parameters: ValuesMap,
                   val queryString: String,
                   val body: String)

suspend fun createPayload(context: PipelineContext<Unit>): PayLoad {
    return context.call.request.run {
        val path: String = path()
        val method: HttpMethod = httpMethod
        val host: String = host() ?: "Unknown"
        val authorization: String = authorization() ?: "Unauthorized"
        val contentType: ContentType = contentType()
        val userAgent: String = userAgent() ?: "Unknown"
        val headers: ValuesMap = headers
        val parameters: ValuesMap = context.call.parameters
        val queryString: String = queryString()
        val body: String = context.call.receiveText()
        PayLoad(path, method, host, headers, authorization, contentType, userAgent, parameters, queryString, body)
    }
}