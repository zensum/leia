package se.zensum.leia.auth

import leia.logic.IncomingRequest
import mu.KotlinLogging

private val log = KotlinLogging.logger("specs-auth-provider")

class SpecsAuthProvider(specs: List<AuthProviderSpec>, private val apf: AuthProviderFactory) : AuthProvider {
    private val namesToSpecs = specs.map { it.name to it }.toMap()
    private val mapping = namesToSpecs.mapValues { (_, v) -> apf.create(v) }


    override fun verify(matching: List<String>, incomingRequest: IncomingRequest): AuthResult = matching
        .asSequence()
        .mapNotNull { match ->
            mapping[match].also { if (it == null) log.warn { "Found no mapping for $match" } }
        }
        .map { it.verify(matching, incomingRequest) }
        .fold(AuthResult.Denied.InvalidCredentials) { a: AuthResult, b: AuthResult -> a.combine(b) }
}

sealed class AuthResult {
    abstract fun combine(other: AuthResult): AuthResult
    /**
     * Authorization was successful (if required)
     */
    data class Authorized(val identifier: String?) : AuthResult() {
        override fun combine(other: AuthResult): AuthResult = when (other) {
            is Authorized ->
                throw UnsupportedOperationException("Only one means of authorization is allowed")
            else -> this
        }
    }

    /**
     * Authorized was required but not successful
     */
    sealed class Denied : AuthResult() {

        /**
         * No credentials were found for authorization
         */
        object NoCredentials : AuthResult.Denied() {
            override fun combine(other: AuthResult): AuthResult = when (other) {
                is Authorized -> other
                else -> other
            }
        }

        /**
         * Credentials were found but they were not valid for this resource
         */
        object InvalidCredentials : AuthResult.Denied() {
            override fun combine(other: AuthResult): AuthResult = when (other) {
                is Authorized -> other
                else -> this
            }
        }
    }
}