package se.zensum.leia.auth

import com.auth0.jwk.JwkProvider
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.RSAKeyProvider
import leia.logic.IncomingRequest
import se.zensum.jwt.JWTProvider
import se.zensum.jwt.JWTProviderImpl
import java.net.URL
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

class JwkAuth(
    private val provider: JWTProvider
): AuthProvider {
    override fun verify(matching: List<String>, incomingRequest: IncomingRequest): AuthResult {
        val token: String = incomingRequest.headers["Authorization"]
            ?.first()
            ?.removePrefix("Bearer")
            ?.trim()
            ?: return AuthResult.Denied

        val jwt: DecodedJWT = provider.verifyJWT(token) ?: return  AuthResult.Denied
        val subject: String = jwt.claims["sub"]?.asString() ?: return AuthResult.Denied

        return AuthResult.Authorized(subject)
    }

    companion object {
        fun fromOptions(
            options: Map<String, Any>,
            jwtVerifierFactory: JWTVerifierFactory = MemoizedJWTVerifierFactory
        ): JwkAuth {
            val jwkConfig: JwkConfig = validateConfig(options)
            val verifier: JWTVerifier = jwtVerifierFactory.createVerifier(jwkConfig)
            val provider: JWTProvider = JWTProviderImpl(verifier)
            return JwkAuth(provider)
        }

        private fun validateConfig(options: Map<String, Any>): JwkConfig {
            require(options.containsKey("jwk_config")) { "Found no config for JWK" }
            val jwkConfig: Map<String, String> = options["jwk_config"] as Map<String, String>
            require(jwkConfig.containsKey("jwk_url")) { "Missing required config key 'jwk_url'" }
            require(jwkConfig.containsKey("jwk_issuer")) { "Missing required config key 'jwk_issuer'" }
            return jwkConfig.toJwkConfig()
        }
    }
}

private fun Map<String, String>.toJwkConfig(): JwkConfig {
    val url = URL(this["jwk_url"]!!)
    val issuer = this["jwk_issuer"]!!
    return JwkConfig(url, issuer)
}

internal class JWKKeyProvider(private val jwkProvider: JwkProvider) : RSAKeyProvider {
    override fun getPublicKeyById(kid: String) =
        jwkProvider.get(kid).publicKey as RSAPublicKey
    override fun getPrivateKey(): RSAPrivateKey {
        throw UnsupportedOperationException()
    }

    override fun getPrivateKeyId(): String {
        throw UnsupportedOperationException()
    }
}