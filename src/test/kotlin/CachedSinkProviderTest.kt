package leia.sink

import leia.logic.SinkDescription
import se.zensum.leia.config.SinkProviderSpec
import kotlin.test.*

class CachedSinkProviderTest {
    private val dummySpec = SinkProviderSpec("foo", true, "kafka", emptyMap())
    private fun dummy() = object : SinkProviderFactory {
        var ctr = 0
        override fun create(spec: SinkProviderSpec): SinkProvider? {
            ctr++
            return object : SinkProvider {
                override fun sinkFor(description: SinkDescription): Sink? = null
            }
        }
    }
    @Test fun createsSpecOnce() {
        val bck = dummy()
        val spf = CachedSinkProviderFactory(bck)
        spf.create(dummySpec)
        assertEquals(1, bck.ctr, "Backing factory should only called once!")
    }

    @Test fun createSpecOnlyOnce() {
        val bck = dummy()
        val spf = CachedSinkProviderFactory(bck)
        spf.create(dummySpec)
        spf.create(dummySpec)
        assertEquals(1, bck.ctr, "Backing factory should only called once!")
    }

    @Test fun createsForDifferentSpecsStillCount() {
        val bck = dummy()
        val spf = CachedSinkProviderFactory(bck)
        spf.create(dummySpec)
        spf.create(dummySpec.copy(name = "lol"))
        spf.create(dummySpec)
        assertEquals(2, bck.ctr, "Backing factory should only called once!")
    }
}