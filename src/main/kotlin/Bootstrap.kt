package leia

import leia.http.KtorServer
import leia.http.ServerFactory
import leia.logic.Resolver
import leia.logic.ResolverAtom
import leia.logic.SourceSpecsResolver
import leia.registry.Registry
import leia.registry.TomlRegistry
import leia.sink.CachedSinkProviderFactory
import leia.sink.DefaultSinkProviderFactory
import leia.sink.Sink
import leia.sink.SinkProvider
import leia.sink.SinkProviderAtom
import leia.sink.SpecSinkProvider
import se.zensum.leia.config.SinkProviderSpec
import se.zensum.leia.config.SourceSpec
import se.zensum.leia.config.TomlConfigProvider

fun run(sf: ServerFactory, resolver: Resolver, sinkProvider: SinkProvider) {
    val res = sf.create(resolver, sinkProvider)
}

fun setupSinkProvider(reg: Registry): SinkProvider {
    val spf = CachedSinkProviderFactory(DefaultSinkProviderFactory())
    return registryUpdated(
        { SinkProviderAtom(SpecSinkProvider(spf, emptyList())) },
        { SinkProviderSpec.fromMap(it) },
        { SpecSinkProvider(spf, it) },
        "sink-providers",
        reg
    ) as SinkProvider
}
fun setupResolver(reg: Registry): Resolver {
    return registryUpdated(
        { ResolverAtom(SourceSpecsResolver(listOf())) },
        { SourceSpec.fromMap(it) },
        { SourceSpecsResolver(it) },
        "routes",
        reg
    ) as Resolver
}

fun <T, U> registryUpdated(
    zero: () -> Atom<T>,
    mapper: (Map<String, Any>) -> U,
    combiner: (List<U>) -> T,
    key: String,
    reg: Registry) : Atom<T> {
    val atom = zero()
    reg.watch(key, mapper) {
        atom.set(combiner(it))
    }
    return atom
}
fun bootstrap() {
    val reg = TomlRegistry(".")

    run(
        KtorServer,
        setupResolver(reg),
        setupSinkProvider(reg)
            .also {
                reg.forceUpdate()
            }
    )
}

