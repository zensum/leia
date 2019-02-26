package leia

import io.ktor.http.HttpMethod
import leia.logic.*
import se.zensum.leia.config.Format
import kotlin.test.Test
import kotlin.test.assertEquals

class ResultTest {
    private val la = LogAppend(
        SinkDescription("foo", "bar", Format.RAW_BODY, "baz", null),
        IncomingRequest(HttpMethod.Get, null, "pleb", emptyMap(), "", null) { ByteArray(0) },
        Receipt(200, "foo")
    )
    private val em = NotAuthorized(emptyList())
    @Test
    fun noMatchOverriddenByAll() {
        assertEquals(la, NoMatch.combine(la), "Overridden by LogAppend")
        assertEquals(em, NoMatch.combine(em), "Overridden by ErrorMatch")

    }

    @Test
    fun noMatchIdempotent() {
        assertEquals(NoMatch, NoMatch.combine(NoMatch), "Idempotent when matched with itself")
    }

    @Test
    fun errorMatchOverrides() {
        assertEquals(em, em.combine(NoMatch), "Error match not overridden by no match")
    }

    @Test
    fun logAppendOverrides() {
        assertEquals(la, la.combine(NoMatch), "LogAppend overrides NoMatch")
        assertEquals(la, la.combine(em), "LogAppend overrides error match")
    }

}