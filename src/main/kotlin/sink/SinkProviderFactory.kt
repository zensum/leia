package leia.sink

import se.zensum.leia.config.SinkProviderSpec

interface SinkProviderFactory {
    // Returns a SinkProvider or null
    // if the factory was inappropriate for creating the Sink
    fun create(spec: SinkProviderSpec): SinkProvider?
}
