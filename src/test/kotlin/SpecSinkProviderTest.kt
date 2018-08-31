package leia.sink

import leia.logic.IncomingRequest
import leia.logic.SinkDescription
import se.zensum.leia.config.Format
import se.zensum.leia.config.SinkProviderSpec
import kotlin.test.*

class SpecSinkProviderTest {
    private val baseSpec = SinkProviderSpec("name", false, "foo", emptyMap())
    private val sd = SinkDescription("foo", "foo", Format.RAW_BODY, "name", null)
    @Test
    fun testRouteToNamed() {
        val sink = object : Sink {
            override suspend fun handle(incomingRequest: IncomingRequest): SinkResult =
                SinkResult.SuccessfullyWritten
        }

        val provider = object : SinkProvider {
            override fun sinkFor(description: SinkDescription): Sink? = sink
        }

        val spf = object : SinkProviderFactory {
            override fun create(spec: SinkProviderSpec): SinkProvider? {
                return provider
            }
        }
        val sp = SpecSinkProvider(spf, listOf(baseSpec))

        val res = sp.sinkFor(sd)
        assertEquals(sink, res, "Should route to named sink provider")
    }

    @Test
    fun testRoutesToDefaultWhenNameNotFound() {
        val sink = object : Sink {
            override suspend fun handle(incomingRequest: IncomingRequest): SinkResult =
                SinkResult.SuccessfullyWritten
        }

        val provider = object : SinkProvider {
            override fun sinkFor(description: SinkDescription): Sink? = sink
        }

        val spf = object : SinkProviderFactory {
            override fun create(spec: SinkProviderSpec): SinkProvider? {
                return provider
            }
        }
        val sp = SpecSinkProvider(spf, listOf(baseSpec.copy(isDefault = true)))

        val res = sp.sinkFor(sd.copy(name = null))
        assertEquals(sink, res, "Should route to named default sink provider")
    }

}