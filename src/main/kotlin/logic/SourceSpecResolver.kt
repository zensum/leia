package leia.logic

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.MalformedJsonException
import io.ktor.http.HttpMethod
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.everit.json.schema.Schema
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
private val pass: Unit = Unit

// A resolver that resolves an incoming request against a single source-spec
// object.
class SourceSpecResolver(private val cfg: SourceSpec, private val auth: AuthProvider) : Resolver {
    var jsonSchema: Schema? = null

    init {
        initJsonSchema()
    }

    private fun initJsonSchema() {
        if (cfg.jsonSchema != "") {
            jsonSchema = try {
                SchemaLoader.load(JSONObject(cfg.jsonSchema))
            } catch (e: JSONException) {
                logger.error { "Failed to parse JSON schema: ${e.message}" }
                null
            }
        }
    }

    override fun resolve(req: IncomingRequest): Result {
        return when {
            // TODO: Add hostname restriction here
            !req.matchPath(cfg.path) -> NoMatch
            !req.matchCors(cfg.corsHosts) -> CorsNotAllowed
            // TODO: We need to be able to tell this apart from explicitly allowed
            cfg.corsHosts.isNotEmpty() && req.method == HttpMethod.Options -> CorsPreflightAllowed
            cfg.allowedMethodsSet.let { it.isNotEmpty() && !it.contains(req.method) } -> NoMatch
            else -> validateAndProcess(req)
        }
    }

    private fun validateAndProcess(req: IncomingRequest): Result {
        if (cfg.validateJson) {
            val errorMatch = if (cfg.jsonSchema != "") {
                if (jsonSchema != null) {
                    validateBodyWithJsonSchema(req)
                } else {
                    JsonSchemaInvalid
                }
            } else {
                validateBodyAsJson(req)
            }
            if (errorMatch != null) {
                return errorMatch
            }
        }
        return authAndAppendToLog(req)
    }

    private fun validateBodyWithJsonSchema(req: IncomingRequest) =
        try {
            val body = req.readBody().toString(Charsets.UTF_8)
            jsonSchema?.validate(JSONObject(body))
            null
        } catch (e: ValidationException) {
            JsonValidationFailed
        }

    // Validates if request body is in JSON format.
    private fun validateBodyAsJson(req: IncomingRequest): JsonValidationFailed? {
        var result: JsonValidationFailed? = null
        val inputStream = runBlocking { ByteArrayInputStream(req.readBody()) }
        val reader = JsonReader(InputStreamReader(inputStream, Charsets.UTF_8))
        try {
            while (reader.hasNext()) when (reader.peek()) {
                JsonToken.BEGIN_ARRAY -> reader.beginArray()
                JsonToken.END_ARRAY -> reader.endArray()
                JsonToken.BEGIN_OBJECT -> reader.beginObject()
                JsonToken.END_OBJECT -> reader.endObject()
                JsonToken.NAME -> reader.nextName()
                JsonToken.STRING -> reader.nextString()
                JsonToken.NUMBER -> reader.nextDouble()
                JsonToken.BOOLEAN -> reader.nextBoolean()
                JsonToken.NULL -> reader.nextNull()
                JsonToken.END_DOCUMENT -> pass
                else -> pass
            }
        } catch (e: MalformedJsonException) {
            result = JsonValidationFailed
        } catch (e: EOFException) {
            result = JsonValidationFailed
        } finally {
            reader.close()
        }
        return result
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