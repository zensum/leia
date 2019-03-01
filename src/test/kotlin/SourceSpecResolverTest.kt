package se.zensum.leia

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import leia.logic.*
import se.zensum.leia.auth.AuthProvider
import se.zensum.leia.auth.AuthResult
import se.zensum.leia.auth.NoCheck
import se.zensum.leia.config.SourceSpec
import kotlin.test.*


private typealias SSR = SourceSpecResolver
private typealias Sp = SourceSpec
private typealias IR = IncomingRequest

fun Sp.ssr(auth: AuthProvider = NoCheck) = SSR(this, auth)

private fun pathIR(path: String) = IR(HttpMethod.Get, null, path, emptyMap(), "", null) { ByteArray(0) }

class SourceSpecResolverTest {
    private val goodPath = "this_is_the_path"
    private val defaultSp = Sp(goodPath, "rhee", allowedMethods = emptyList(), response = HttpStatusCode.OK, corsHosts = emptyList(),
        authenticateUsing = emptyList(), validateJson = false, jsonSchema = "", hosts = emptyList())

    @Test
    fun rejectsImproperPath() {
        val re = defaultSp.copy(path = "not_good_path").ssr()
        val ir = pathIR(goodPath)
        val res = re.resolve(ir)
        assertTrue(res is NoMatch, "Shouldn't match")
    }

    @Test
    fun rejectsIncorrectMethod() {
        val re = defaultSp.copy(allowedMethods = listOf(HttpMethod.Patch)).ssr()
        val ir = pathIR(goodPath)
        val res = re.resolve(ir)
        assertTrue(res is MethodNotAllowed, "Shouldn't match")
    }

    @Test
    fun rejectsMissingJWT() {
        val re = defaultSp.copy(
            authenticateUsing = listOf("jwk"),
            allowedMethods = HttpMethods.verbs
        ).ssr(object : AuthProvider {
            override fun verify(matching: List<String>, incomingRequest: IncomingRequest): AuthResult = AuthResult.Denied.NoCredentials
        })
        val ir = pathIR(goodPath)
        val res = re.resolve(ir)
        assertEquals(NotAuthorized(listOf("jwk")), res, "should give error match")
    }

    @Test
    fun rejectsCorsNotAllowed() {
        val re = defaultSp.copy(corsHosts = listOf("http://invalid")).ssr()
        val ir = pathIR(goodPath).copy(origin = "http://example.com")
        val res = re.resolve(ir)
        assertTrue(res is CorsNotAllowed, "should give error match")
    }

    @Test
    fun trickyRequestWorks() {
        val sp = defaultSp.copy(
            allowedMethods = listOf(HttpMethod.Post),
            corsHosts = listOf("http://example.com")
        )
        val re = sp.ssr()
        val ir = pathIR(goodPath)
            .copy(origin = "http://example.com", method = HttpMethod.Post)

        val res = re.resolve(ir) as? LogAppend ?: fail("Res must be log-append")

        val (sinkDes, req, rec) = res

        assertEquals("rhee", sinkDes.topic, "Topic set as appropriate")
        assertEquals(sp.sink, sinkDes.name, "Sink-name sent into sinkdescription")
        assertEquals(sp.format, sinkDes.dataFormat)

        assertEquals(ir, req, "Incoming request passed on")

        assertEquals(rec.status, sp.response.value, "Http status code set")
    }

    @Test
    fun validateValidJson() {
        val re = defaultSp.copy(validateJson = true).ssr()
        val validBytesFn = suspend { validJson.toByteArray() }
        val ir = pathIR(goodPath).copy(readBodyFn = validBytesFn)
        val res = re.resolve(ir)
        assertNotEquals(JsonValidationFailed, res, "should not give error match")
    }

    @Test
    fun validateInvalidJson() {
        val re = defaultSp.copy(validateJson = true).ssr()
        val invalidBytesFn = suspend { invalidJson.toByteArray() }
        val ir = pathIR(goodPath).copy(readBodyFn = invalidBytesFn)
        val res = re.resolve(ir)
        assertEquals(JsonValidationFailed, res, "should give error match")
    }

    @Test
    fun validateJsonSchemaValidJson() {
        val re = defaultSp.copy(validateJson = true, jsonSchema = jsonSchema).ssr()
        val validBytesFn = suspend { validJsonForSchema.toByteArray() }
        val ir = pathIR(goodPath).copy(readBodyFn = validBytesFn)
        val res = re.resolve(ir)
        assertNotEquals(JsonValidationFailed, res, "should not give error match")
    }

    @Test
    fun validateJsonSchemaInvalidJson() {
        val re = defaultSp.copy(validateJson = true, jsonSchema = jsonSchema).ssr()
        val validBytesFn = suspend { invalidJsonForSchema.toByteArray() }
        val ir = pathIR(goodPath).copy(readBodyFn = validBytesFn)
        val res = re.resolve(ir)
        assertEquals(JsonValidationFailed, res, "should give error match")
    }

    @Test
    fun validateInvalidJsonSchemaValidJson() {
        val re = defaultSp.copy(validateJson = true, jsonSchema = invalidJsonSchema).ssr()
        val validBytesFn = suspend { validJsonForSchema.toByteArray() }
        val ir = pathIR(goodPath).copy(readBodyFn = validBytesFn)
        val res = re.resolve(ir)
        assertEquals(JsonSchemaInvalid, res, "should give error match")
    }
}

// example from https://www.json.org/example.html
val validJson = """
{
    "glossary": {
        "title": "example glossary",
		"GlossDiv": {
            "title": "S",
			"GlossList": {
                "GlossEntry": {
                    "ID": "SGML",
					"SortAs": "SGML",
					"GlossTerm": "Standard Generalized Markup Language",
					"Acronym": "SGML",
					"Abbrev": "ISO 8879:1986",
					"GlossDef": {
                        "para": "A meta-markup language, used to create markup languages such as DocBook.",
						"GlossSeeAlso": ["GML", "XML"]
                    },
					"GlossSee": "markup"
                }
            }
        }
    }
}
""".trimIndent()
const val invalidJson = """ { "title": "invalid JSON """

// example from https://json-schema.org/learn/miscellaneous-examples.html
val jsonSchema = """
{
  "${"$"}id": "https://example.com/person.schema.json",
  "${"$"}schema": "http://json-schema.org/draft-07/schema#",
  "title": "Person",
  "type": "object",
  "properties": {
    "firstName": {
      "type": "string",
      "description": "The person's first name."
    },
    "lastName": {
      "type": "string",
      "description": "The person's last name."
    },
    "age": {
      "description": "Age in years which must be equal to or greater than zero.",
      "type": "integer",
      "minimum": 0
    }
  }
}
""".trimIndent()

val invalidJsonSchema = """
{
  "properties": {
""".trimIndent()

val validJsonForSchema = """
{
  "firstName": "John",
  "lastName": "Doe",
  "age": 21
}
""".trimIndent()

val invalidJsonForSchema = """
{
  "firstName": "John",
  "lastName": "Doe",
  "age": "21"
}
""".trimIndent()