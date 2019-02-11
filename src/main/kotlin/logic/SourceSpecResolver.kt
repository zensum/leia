package leia.logic

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.MalformedJsonException
import io.ktor.http.HttpMethod
import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import se.zensum.leia.auth.AuthProvider
import se.zensum.leia.auth.AuthResult
import se.zensum.leia.config.SourceSpec
import java.io.ByteArrayInputStream
import java.io.EOFException
import java.io.InputStreamReader
import org.everit.json.schema.ValidationException
import org.everit.json.schema.loader.SchemaLoader
import org.json.JSONException
import org.json.JSONObject

private val logger = KotlinLogging.logger("source-spec")

// A resolver that resolves an incoming request against a single source-spec
// object.
class SourceSpecResolver(private val cfg: SourceSpec, private val auth: AuthProvider) : Resolver {
    override fun resolve(req: IncomingRequest) : Result {
        return when {
            // TODO: Add hostname restriction here
            !req.matchPath(cfg.path) -> NoMatch
            !req.matchCors(cfg.corsHosts) -> CorsNotAllowed
            // TODO: We need to be able to tell this apart from explicitly allowed
            cfg.corsHosts.isNotEmpty() && req.method == HttpMethod.Options -> CorsPreflightAllowed
            cfg.allowedMethodsSet.let { it.isNotEmpty() && !it.contains(req.method) } -> NoMatch
            cfg.validateJson && !validateBodyAsJson(req, cfg.jsonSchema) -> JsonValidationFailed
            else -> authAndAppendToLog(req)
        }
    }

    // Validates if request body is in JSON format. If jsonSchema is not empty it is used for validation.
    private fun validateBodyAsJson(req: IncomingRequest, jsonSchema: String): Boolean {
        if (jsonSchema != "") {
            val schema = try {
                SchemaLoader.load(JSONObject(jsonSchema))
            } catch (e: JSONException) {
                logger.error { "Failed to parse JSON schema: ${e.message}" }
                return false
            }
            try {
                val body = req.readBody().toString(Charsets.UTF_8)
                schema.validate(JSONObject(body))
            } catch (e: ValidationException) {
                return false
            }
            return true
        }

        var valid = true
        var inputStream: ByteArrayInputStream
        runBlocking {
            inputStream = ByteArrayInputStream(req.readBody())
            val reader = JsonReader(InputStreamReader(inputStream, Charsets.UTF_8))
            try {
                while (reader.hasNext()) {
                    when (reader.peek()) {
                        JsonToken.BEGIN_ARRAY -> reader.beginArray()
                        JsonToken.END_ARRAY -> reader.endArray()
                        JsonToken.BEGIN_OBJECT -> reader.beginObject()
                        JsonToken.END_OBJECT -> reader.endObject()
                        JsonToken.NAME -> reader.nextName()
                        JsonToken.STRING -> reader.nextString()
                        JsonToken.NUMBER -> reader.nextDouble()
                        JsonToken.BOOLEAN -> reader.nextBoolean()
                        JsonToken.NULL -> reader.nextNull()
                        JsonToken.END_DOCUMENT -> {}
                    }
                }
            } catch (e: MalformedJsonException) {
                valid = false
            } catch (e: EOFException) {
                valid = false
            } finally {
                reader.close()
            }
        }
        return valid
    }

    private fun authAndAppendToLog(req: IncomingRequest): Result {
        val userId: String? = cfg.authenticateUsing
            .takeIf { it.isNotEmpty() }
            ?.let { authenticateUsing ->
                val authResult = auth.verify(authenticateUsing, req)
                when (authResult) {
                    is AuthResult.Denied.NoCredentials -> return NotAuthorized(authenticateUsing)
                    is AuthResult.Denied.InvalidCredentials -> return Forbidden
                    is AuthResult.Authorized -> authResult.identifier
                }
            }

        return LogAppend(
            SinkDescription(cfg.topic, req.requestId.toString(), cfg.format, cfg.sink, userId),
            req,
            Receipt(cfg.response.value, "")
        )
    }
}