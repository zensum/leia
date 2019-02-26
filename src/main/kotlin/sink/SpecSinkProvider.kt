package leia.sink

import leia.logic.SinkDescription
import se.zensum.leia.config.SinkProviderSpec

class SpecSinkProvider(
    sinkProviderFactory: SinkProviderFactory,
    specs: List<SinkProviderSpec>
) : SinkProvider {
    private val fallbackDefaultSpec =
        SinkProviderSpec("\$default", true, "always-error", mapOf("message" to "No default sink configured"))
    private val providers = specs.zip(specs.map(sinkProviderFactory::create)).toMap()
    private val defaultProviderSpec = specs.firstOrNull { it.isDefault }
    private val defaultProvider = providers[defaultProviderSpec] ?: sinkProviderFactory.create(fallbackDefaultSpec)
    private val nameToSpecs = specs.map { it.name }.zip(specs).toMap()

    private fun delegateTo(description: SinkDescription): SinkProvider? =
        if(description.name == null) {
            defaultProvider
        } else {
            providers[nameToSpecs[description.name]]
        }

    override fun sinkFor(description: SinkDescription): Sink? =
        delegateTo(description)?.sinkFor(description)
}


