package auth

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import se.zensum.leia.auth.JWKKeyProvider
import java.net.URL

interface JWTVerifierFactory {
    fun createVerifier(config: JwkConfig): JWTVerifier
}

object MemoizedJWTVerifierFactory: JWTVerifierFactory {
    private val verifiers: MutableMap<JwkConfig, JWTVerifier> = HashMap()

    override fun createVerifier(config: JwkConfig): JWTVerifier {
        return if(config in verifiers)
            verifiers[config]!!
        else {
            buildVerifier(config).also { createdVerifier ->
                verifiers[config] = createdVerifier
            }
        }
    }

    private fun buildVerifier(
        config: JwkConfig,
        algorithm: Algorithm = Algorithm.RSA256(JWKKeyProvider(JwkProviderBuilder(config.jwkUrl.toString()).cached(true).build()))
    ): JWTVerifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .build()
}

data class JwkConfig(val jwkUrl: URL, val issuer: String)