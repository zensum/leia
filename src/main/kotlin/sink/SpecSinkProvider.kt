package leia.sink

import leia.logic.SinkDescription
import se.zensum.leia.config.SinkProviderSpec

class SpecSinkProvider(
    private val sinkProviderFactory: SinkProviderFactory,
    private val specs: List<SinkProviderSpec>
) : SinkProvider {
    private val fallbackDefaultSpec = SinkProviderSpec("\$default", true, "null", emptyMap())
    private val providers = specs.zip(specs.map(sinkProviderFactory::create)).toMap()
    private val defaultProvider = specs.firstOrNull { it.isDefault } ?: fallbackDefaultSpec
    private val nameToSpecs = specs.map { it.name }.zip(specs).toMap()

    private fun delegateTo(description: SinkDescription): SinkProvider? =
        if(description.name == null) {
            providers[defaultProvider]
        } else {
            providers[nameToSpecs[description.name]]
        }

    override fun sinkFor(description: SinkDescription): Sink? =
        delegateTo(description)?.sinkFor(description)
}


