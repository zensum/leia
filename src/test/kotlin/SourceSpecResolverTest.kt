package leia

import auth.JwkAuth
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import leia.logic.CorsNotAllowed
import leia.logic.IncomingRequest
import leia.logic.LogAppend

import leia.logic.NoMatch
import leia.logic.NotAuthorized
import leia.logic.SourceSpecResolver
import se.zensum.leia.auth.AuthProvider
import se.zensum.leia.auth.AuthResult
import se.zensum.leia.auth.NoCheck
import se.zensum.leia.config.SourceSpec
import kotlin.test.*


private typealias SSR = SourceSpecResolver
private typealias Sp = SourceSpec
private typealias IR = IncomingRequest
fun Sp.ssr(auth: AuthProvider = NoCheck) = SSR(this, auth)

private fun pathIR(path: String) = IR(HttpMethod.Get, null, null, path, emptyMap(), "", null, { ByteArray(0 )})

class SourceSpecResolverTest {
    val goodPath = "this_is_the_path"
    val defaultSp = Sp(goodPath, "rhee", allowedMethods = emptyList(), response = HttpStatusCode.OK, corsHosts = emptyList(),
        authenticateUsing = emptyList())
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
        assertTrue(res is NoMatch, "Shouldn't match")
    }

    @Test
    fun rejectsMissingJWT() {
        val re = defaultSp.copy(
            authenticateUsing = listOf("jwk"),
            allowedMethods = HttpMethod.DefaultMethods
        ).ssr(object : AuthProvider {
            override fun verify(matching: List<String>, incomingRequest: IncomingRequest): AuthResult
            = AuthResult.Denied
        })
        val ir = pathIR(goodPath)
        val res = re.resolve(ir)
        assertEquals(NotAuthorized, res, "should give error match")
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

        val (sinkDes, req, rec)  = res

        assertEquals("rhee", sinkDes.topic, "Topic set as appropriate")
        assertEquals(sp.sink, sinkDes.name, "Sink-name sent into sinkdescription")
        assertEquals(sp.format, sinkDes.dataFormat)

        assertEquals(ir, req, "Incoming request passed on")

        assertEquals(rec.status, sp.response.value, "Http status code set")
    }
}
