package leia

import leia.http.KtorServer
import leia.http.ServerFactory
import leia.logic.DisjunctiveResolver
import leia.logic.Resolver
import leia.logic.ResolverAtom
import leia.logic.RuleResolver
import leia.logic.SinkDescription
import leia.sink.CachedSinkProviderFactory
import leia.sink.DefaultSinkProviderFactory
import leia.sink.KafkaSinkProvider
import leia.sink.NullSinkProvider
import leia.sink.Sink
import leia.sink.SinkProvider
import leia.sink.SinkProviderAtom
import leia.sink.SinkProviderFactory
import leia.sink.SpecSinkProvider
import se.zensum.leia.config.DefaultConfiguration
import se.zensum.leia.config.SinkProviderSpec
import se.zensum.leia.config.TomlConfigProvider
import se.zensum.leia.config.TopicRouting
import javax.management.RuntimeErrorException

fun run(sf: ServerFactory, resolver: Resolver, sinkProvider: SinkProvider) {
    val res = sf.create(resolver, sinkProvider)
}

private fun createResolver(trs: List<TopicRouting>) = trs
    .map { RuleResolver(it) }
    .toList()
    .let { DisjunctiveResolver(it) }

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

    val spf = CachedSinkProviderFactory(DefaultSinkProviderFactory())
    val sp = SinkProviderAtom(SpecSinkProvider(spf, cfg.getSinkProviders()))
    val resolver = ResolverAtom(createResolver(cfg.getRoutes()))

    run(
        KtorServer,
        resolver,
        sp
    )
}

