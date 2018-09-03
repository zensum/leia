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
import leia.sink.SinkProvider
import leia.sink.SinkProviderAtom
import leia.sink.SpecSinkProvider
import se.zensum.leia.auth.AuthProvider
import se.zensum.leia.auth.AuthProviderAtom
import se.zensum.leia.auth.AuthProviderSpec
import se.zensum.leia.auth.DefaultAuthProviderFactory
import se.zensum.leia.auth.NoCheck
import se.zensum.leia.auth.SpecsAuthProvider
import se.zensum.leia.config.SinkProviderSpec
import se.zensum.leia.config.SourceSpec
import se.zensum.leia.getEnv

fun run(sf: ServerFactory, resolver: Resolver, sinkProvider: SinkProvider) =
    sf.create(resolver, sinkProvider)

private const val DEFAULT_CONFIG_DIRECTORY ="/etc/config"

// Sets up a sink provider using the passed in registry
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

// Sets up a resolver configured using the passed in registry
fun setupResolver(reg: Registry, auth: AuthProvider): Resolver {
    return registryUpdated(
        { ResolverAtom(SourceSpecsResolver(auth, listOf())) },
        { SourceSpec.fromMap(it) },
        { SourceSpecsResolver(auth, it) },
        "routes",
        reg
    ) as Resolver
}

fun setupAuthProvider(reg: Registry): AuthProvider {
    val authFactory = DefaultAuthProviderFactory
    val atom: Atom<AuthProvider> = AuthProviderAtom(NoCheck)
    val mapper: (Map<String, Any>) -> AuthProviderSpec = { AuthProviderSpec.fromMap(it) }
    val combiner: (List<AuthProviderSpec>) -> AuthProvider = { specs -> SpecsAuthProvider(specs, authFactory) }
    return registryUpdated<AuthProvider, AuthProviderSpec>(
        zero = { atom },
        mapper = mapper,
        combiner = combiner,
        key = "auth_providers",
        reg = reg
    ) as AuthProvider
}

fun <T, U> registryUpdated(
    zero: () -> Atom<T>,
    mapper: (Map<String, Any>) -> U,
    combiner: (List<U>) -> T,
    key: String,
    reg: Registry) : Atom<T> = zero().also { atom ->
    reg.watch(key, mapper) {
        atom.set(combiner(it))
    }
}

fun bootstrap() {
    val reg = TomlRegistry(getEnv("CONFIG_DIRECTORY", DEFAULT_CONFIG_DIRECTORY))

    val auth = setupAuthProvider(reg)
    val server = run(
        KtorServer,
        setupResolver(reg, auth),
        setupSinkProvider(reg)
    )
    reg.forceUpdate()
    server.start()
}