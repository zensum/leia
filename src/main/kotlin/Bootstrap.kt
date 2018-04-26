package leia

import leia.http.KtorServer
import leia.http.ServerFactory
import leia.logic.DisjunctiveResolver
import leia.logic.Resolver
import leia.logic.RuleResolver
import leia.logic.SinkDescription
import leia.sink.KafkaSinkProvider
import leia.sink.NullSinkProvider
import leia.sink.Sink
import leia.sink.SinkProvider
import se.zensum.leia.config.DefaultConfiguration
import se.zensum.leia.config.SinkProviderSpec
import se.zensum.leia.config.TomlConfigProvider
import javax.management.RuntimeErrorException

fun run(sf: ServerFactory, resolver: Resolver, sinkProvider: SinkProvider) {
    val res = sf.create(resolver, sinkProvider)
}

fun bootstrap() {
    val cfg = TomlConfigProvider.fromString("""
        title = 'Config'
        [[sink-providers]]
          name = 'null'
          type = 'null'
          default = true

        [[routes]]
            path = '/test'
            topic = 'rhee'
        """.trimMargin())

    run(
        KtorServer,
            cfg.getRoutes()
            .map { RuleResolver(it) }
            .toList()
            .let { DisjunctiveResolver(it) },
        SpecSinkProvider(cfg.getSinkProviders())
    )
}

private fun sinkProviderForSpec(spec: SinkProviderSpec): SinkProvider =
    when(spec.type.toLowerCase()) {
        "null" -> NullSinkProvider
        "kafka" -> KafkaSinkProvider(spec.options["host"])
        else -> throw RuntimeException("No sinkProvider matching type ${spec.type}")
    }

class SpecSinkProvider(private val specs: List<SinkProviderSpec>) : SinkProvider {
    private val providers = specs.zip(specs.map(::sinkProviderForSpec)).toMap()
    private val defaultProvider = specs.first { it.isDefault }
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