package leia

import io.ktor.http.HttpMethod
import leia.logic.IncomingRequest
import leia.logic.LogAppend
import leia.logic.NoMatch
import leia.logic.NotAuthorized
import leia.logic.Receipt
import leia.logic.SinkDescription
import se.zensum.leia.config.Format
import kotlin.test.*;

class ResultTest {
    val la = LogAppend(
        SinkDescription("foo", "bar", Format.RAW_BODY, "baz", null),
        IncomingRequest(HttpMethod.Get, null, "pleb", emptyMap(), "", null, { ByteArray(0) }),
        Receipt(200, "foo")
    )
    val em = NotAuthorized
    @Test fun noMatchOverriddenByAll() {
        NoMatch.combine(la).let {
            assertEquals(la, it, "Overridden by LogAppend")
        }
        NoMatch.combine(em).let {
            assertEquals(em, it, "Overridden by ErrorMatch")
        }

    }

    @Test fun noMatchIdempotent() {
        NoMatch.combine(NoMatch).let {
            assertEquals(NoMatch, it, "Idempotent when matched with itself")
        }
    }

    @Test fun errorMatchOverrides() {
        em.combine(NoMatch).let {
            assertEquals(em, it, "Error match not overridden by no match")
        }
    }

    @Test fun logAppendOverrides() {
        la.combine(NoMatch).let {
            assertEquals(la, it, "LogAppend overrides NoMatch")
        }
        la.combine(em).let {
            assertEquals(la, it, "LogAppend overrides error match")
        }
    }

}