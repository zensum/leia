package leia

import io.ktor.http.HttpMethod.Companion.Get
import leia.logic.*
import org.junit.jupiter.api.assertThrows
import se.zensum.leia.config.Format
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DisjunctiveResolverTest {
    private val exampleIR = IncomingRequest(Get, null, "foo", emptyMap(), "", null) { ByteArray(0) }
    @Test
    fun testZeroToNoMatch() {
        val r = DisjunctiveResolver(emptyList())
        val res = r.resolve(exampleIR)
        assertTrue(res is NoMatch, "A disjunctive resolver of zero entries resolves to nothing")
    }

    private fun resolverReturning(r: Result) = object : Resolver {
        override fun resolve(req: IncomingRequest): Result = r
    }

    @Test
    fun testOneIdentity() {
        val la = LogAppend(
            SinkDescription("foo", "bar", Format.RAW_BODY, "baz", null),
            exampleIR,
            Receipt(200, "foo")
        )
        val r = DisjunctiveResolver(listOf(resolverReturning(la)))
        val res = r.resolve(exampleIR)
        assertEquals(la, res, "A unary disjunction is idempotent")
    }

    @Test
    fun testTwoTakesFirst() {
        val la = LogAppend(
            SinkDescription("foo", "bar", Format.RAW_BODY, "baz", null),
            exampleIR,
            Receipt(200, "foo")
        )
        val r = DisjunctiveResolver(listOf(
            resolverReturning(la),
            resolverReturning(la.copy(receipt = Receipt(500, "bar")))
        ))

        assertThrows<UnsupportedOperationException>("Should throw because matches are not supported") {
            r.resolve(exampleIR)
        }
    }

    @Test
    fun testTwoTakesFirstMatching() {
        val la = LogAppend(
            SinkDescription("foo", "bar", Format.RAW_BODY, "baz", null),
            exampleIR,
            Receipt(200, "foo")
        )
        val r = DisjunctiveResolver(listOf(
            resolverReturning(NoMatch),
            resolverReturning(la)
        ))
        val res = r.resolve(exampleIR)
        assertEquals(la, res, "A unary disjunction is idempotent")
    }
}