package se.zensum.leia.auth.jwk

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT

class JwtValidator(
    config: JwkConfig
): JWTDecoder {
    private val verifier: JWTVerifier = MemoizedJWTVerifierFactory.createVerifier(config)
    override fun verifyToken(token: String): DecodedJWT? = verifier.verify(token)
}

/**
 * "Memoized" factory object for instances of [JWTVerifier], so a
 * new instance will not be created upon each request. Instead each created
 * verifies will be saves and reused for next use when a verifier is requested
 * for an identical [JwkConfig].
 */
private object MemoizedJWTVerifierFactory {
    private val verifiers: MutableMap<JwkConfig, JWTVerifier> = mutableMapOf()

    fun createVerifier(config: JwkConfig): JWTVerifier {
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
        algorithm: Algorithm = configToAlgorithm(config)
    ): JWTVerifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .build()

    private fun configToAlgorithm(config: JwkConfig): Algorithm = Algorithm.RSA256(
        JWKKeyProvider(
            JwkProviderBuilder(config.jwkUrl.toString())
                .cached(true)
                .build()
        )
    )
}