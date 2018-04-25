package leia

import leia.http.KtorServer
import leia.http.ServerFactory
import leia.logic.DisjunctiveResolver
import leia.logic.Resolver
import leia.logic.RuleResolver
import leia.sink.KafkaSinkProvider
import leia.sink.NullSinkProvider
import leia.sink.SinkProvider
import se.zensum.leia.config.DefaultConfiguration
import se.zensum.leia.config.TomlConfigProvider

fun run(sf: ServerFactory, resolver: Resolver, sinkProvider: SinkProvider) {
    val res = sf.create(resolver, sinkProvider)
}

fun bootstrap() {
    val cfg = TomlConfigProvider.fromString("""
        title = 'Config'
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
        NullSinkProvider
    )
}