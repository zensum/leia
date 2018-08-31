package se.zensum.leia.auth

import leia.logic.IncomingRequest
import mu.KotlinLogging

private val log = KotlinLogging.logger("specs-auth-provider")

class SpecsAuthProvider(val specs: List<AuthProviderSpec>, val apf: AuthProviderFactory) : AuthProvider {
    private val namesToSpecs = specs.map { it.name to it }.toMap()
    private val mapping = namesToSpecs.mapValues { (_, v) -> apf.create(v) }


    override fun verify(matching: List<String>, incomingRequest: IncomingRequest): AuthResult = matching
        .asSequence()
        .mapNotNull { match ->
            mapping[match].also { if(it == null) log.warn { "Found no mapping for $match" } }
        }
        .map { it.verify(matching, incomingRequest) }
        .reduce { a: AuthResult, b: AuthResult -> a.combine(b) }
}

sealed class AuthResult {
    abstract fun combine(other: AuthResult): AuthResult
    /**
     * Authorized was required and successful
     */
    data class Authorized(val identifier: String): AuthResult() {
        override fun combine(other: AuthResult): AuthResult = when(other) {
            is Authorized ->
                throw UnsupportedOperationException("Only one means of authorization is allowed")
            else -> this
        }
    }

    /**
     * Authorized was required but not successful
     */
    object Denied: AuthResult() {
        override fun combine(other: AuthResult): AuthResult = when(other) {
            NoAuthorizationCheck -> this
            else -> other
        }
    }

    /**
     * No authorization was done
     */
    object NoAuthorizationCheck: AuthResult() {
        override fun combine(other: AuthResult): AuthResult = other
    }
}