package leia.sink

import se.zensum.leia.config.SinkProviderSpec

// A simple ref-counted cache map, it should use weakmaps in the future
private class RefCountedCache<K, V> {
    private val data = mutableMapOf<K, Pair<V, Int>>()

    fun computeIfAbsent(k: K, fn: () -> V?) = data.compute(k) { _, oldPair ->
        if (oldPair == null) {
            fn()?.let { it to 1 }
        } else {
            val (oldV, oldC) = oldPair
            oldV to oldC + 1
        }
    }
}

class CachedSinkProviderFactory(private val backing: SinkProviderFactory): SinkProviderFactory {
    private val sinkProviderCache = RefCountedCache<SinkProviderSpec, SinkProvider>()
    override fun create(spec: SinkProviderSpec): SinkProvider? =
        sinkProviderCache.computeIfAbsent(spec) { backing.create(spec) }?.first
}

