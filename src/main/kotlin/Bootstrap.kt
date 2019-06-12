package leia

import leia.Env.prometheusEnable
import leia.http.KtorServer
import leia.http.ServerFactory
import leia.logic.IncomingRequest
import leia.logic.Resolver
import leia.logic.ResolverAtom
import leia.logic.SourceSpecsResolver
import leia.registry.K8sRegistry
import leia.registry.K8sRegistry.Companion.DEFAULT_KUBERNETES_ENABLE
import leia.registry.K8sRegistry.Companion.DEFAULT_KUBERNETES_HOST
import leia.registry.K8sRegistry.Companion.DEFAULT_KUBERNETES_PORT
import leia.registry.K8sRegistry.Companion.DEFAULT_PROMETHEUS_ENABLE
import leia.registry.Registries
import leia.registry.Registry
import leia.registry.TomlRegistry
import leia.sink.*
import mu.KotlinLogging
import registry.Tables
import se.zensum.leia.auth.*
import se.zensum.leia.auth.jwk.JwkAuth
import se.zensum.leia.config.SinkProviderSpec
import se.zensum.leia.config.SourceSpec
import se.zensum.leia.getEnv

fun run(sf: ServerFactory, resolver: Resolver, sinkProvider: SinkProvider, registry: Registry, prometheusEnable: Boolean) =
    sf.create(resolver, sinkProvider, registry, prometheusEnable)

private const val DEFAULT_CONFIG_DIRECTORY = "/etc/config"
private val logger = KotlinLogging.logger {}
// Sets up a sink provider using the passed in registry
fun setupSinkProvider(reg: Registry): SinkProvider {
    val spf = CachedSinkProviderFactory(DefaultSinkProviderFactory())
    return reg.registryUpdated(
        { SinkProviderAtom(SpecSinkProvider(spf, emptyList())) },
        { SinkProviderSpec.fromMap(it) },
        { SpecSinkProvider(spf, it) },
        Tables.SinkProviders
    ) as SinkProvider
}

// Sets up a resolver configured using the passed in registry
fun setupResolver(reg: Registry, auth: AuthProvider): Resolver {
    return reg.registryUpdated(
        { ResolverAtom(SourceSpecsResolver(auth, listOf())) },
        { SourceSpec.fromMap(it) },
        { SourceSpecsResolver(auth, it) },
        Tables.Routes
    ) as Resolver
}

fun setupAuthProvider(reg: Registry): AuthProvider {
    val authFactory = DefaultAuthProviderFactory
    val atom: Atom<AuthProvider> = AuthProviderAtom(NoCheck)
    val mapper: (Map<String, Any>) -> AuthProviderSpec = { AuthProviderSpec.fromMap(it) }
    val combiner: (List<AuthProviderSpec>) -> AuthProvider = { specs -> SpecsAuthProvider(specs, authFactory) }
    return reg.registryUpdated(
        zero = { atom },
        mapper = mapper,
        combiner = combiner,
        table = Tables.AuthProviders
    ) as AuthProvider
}

fun <T, U> Registry.registryUpdated(
    zero: () -> Atom<T>,
    mapper: (Map<String, Any>) -> U,
    combiner: (List<U>) -> T,
    table: Tables): Atom<T> = zero().also { atom ->
    watch(table, mapper) {
        atom.reference.set(combiner(it))
    }
}

private const val DEFAULT_JWK_PROVIDER_NAME = "\$default_jwk_provider"

object Env {
    val configDir = getEnv("CONFIG_DIRECTORY", DEFAULT_CONFIG_DIRECTORY)
    val k8sHost = getEnv("KUBERNETES_SERVICE_HOST", DEFAULT_KUBERNETES_HOST)
    val k8sPort = getEnv("KUBERNETES_SERVICE_PORT", DEFAULT_KUBERNETES_PORT)
    val k8sEnable = getEnv("KUBERNETES_ENABLE", DEFAULT_KUBERNETES_ENABLE)
    val prometheusEnable = getEnv("PROMETHEUS_ENABLE", DEFAULT_PROMETHEUS_ENABLE) == "true"
}

fun prepareRegistry(): Registries {
    val tomlReg = TomlRegistry(Env.configDir)
    val registries = mutableListOf<Registry>()
    logger.info { "Env.k8sHost: ${Env.k8sHost} Env.k8sPort: ${Env.k8sPort}" }
    if (Env.k8sEnable == "true") registries.add(K8sRegistry(Env.k8sHost, Env.k8sPort))
    registries.add(tomlReg)
    return Registries(registries)
}

fun bootstrap() {
    val reg = prepareRegistry()
    val auth = setupAuthProvider(reg).addJwkProvider()
    val server = run(
        KtorServer,
        setupResolver(reg, auth),
        setupSinkProvider(reg),
        reg,
        prometheusEnable
    )
    reg.forceUpdate()
    server.start()
}

private fun AuthProvider.addJwkProvider(): AuthProvider {
    val envVars: Map<String, String> = sequenceOf("JWK_URL", "JWK_ISSUER")
        .map { it to getEnv(it, "") }
        .toMap()
        .mapKeys { it.value.toLowerCase() }
        .filterValues { it.isBlank() }

    if (envVars.size != 2)
        return this

    return createJwkAuth(envVars)
}

private fun createJwkAuth(configs: Map<String, String>): AuthProvider {
    val jwkAuth = JwkAuth.fromOptions(mapOf("jwk_config" to configs))
    return object : AuthProvider {
        override fun verify(matching: List<String>, incomingRequest: IncomingRequest): AuthResult =
            if (matching.contains(DEFAULT_JWK_PROVIDER_NAME))
                jwkAuth.verify(matching, incomingRequest).combine(this.verify(matching, incomingRequest))
            else
                this.verify(matching, incomingRequest)
    }
}