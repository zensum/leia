package auth

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.RSAKeyProvider
import leia.logic.IncomingRequest
import se.zensum.jwt.JWTProvider
import se.zensum.jwt.JWTProviderImpl
import se.zensum.leia.auth.AuthProvider
import se.zensum.leia.auth.AuthResult
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
        fun fromOptions(options: Map<String, Any>): JwkAuth {
            require(options.containsKey("jwk_config")) { "Found no config for JWK" }
            val jwkConfig: Map<String, String> = options["jwk_config"] as Map<String, String>
            require(jwkConfig.containsKey("jwk_url")) { "Missing required config key 'jwk_url'" }
            require(jwkConfig.containsKey("jwk_issuer")) { "Missing required config key 'jwk_issuer'" }

            val url: String = jwkConfig["jwk_url"].toString()
            val issuer: String = jwkConfig["jwk_issuer"].toString()
            val verifier: JWTVerifier = createVerifier(url, issuer)

            val provider: JWTProvider = JWTProviderImpl(verifier)
            return JwkAuth(provider)
        }
    }
}

fun createVerifier(
    jwkUrl: String,
    issuer: String,
    algorithm: Algorithm = Algorithm.RSA256(JWKKeyProvider(JwkProviderBuilder(jwkUrl).cached(true).build()))
): JWTVerifier = JWT.require(algorithm)
    .withIssuer(issuer)
    .build()

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