package auth

import com.auth0.jwt.interfaces.Claim
import com.auth0.jwt.interfaces.DecodedJWT
import se.zensum.leia.auth.AuthProvider
import se.zensum.leia.auth.AuthProviderFactory
import se.zensum.leia.auth.AuthProviderSpec
import se.zensum.leia.auth.BasicAuth
import se.zensum.leia.auth.NoCheck
import se.zensum.leia.auth.jwk.JWTDecoder
import se.zensum.leia.auth.jwk.JwkAuth
import se.zensum.leia.auth.jwk.JwkConfig
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList

object MockAuthProviderFactory: AuthProviderFactory {
    override fun create(spec: AuthProviderSpec): AuthProvider =
        when(spec.type.toLowerCase()) {
            "basic_auth" -> BasicAuth.fromOptions(spec.options)
            "no_auth" -> NoCheck
            "jwk" -> JwkAuth.fromOptions(spec.options) { config ->
                ValidJWTDecoder(config, "subject")
            }
            else -> throw RuntimeException("No AuthProvider matching type ${spec.type}")
        }
}

internal object NullJWTDecoder: JWTDecoder {
    override fun verifyToken(token: String): DecodedJWT? = null
}

internal data class ValidJWTDecoder(
    private val config: JwkConfig,
    private val subject: String
    ): JWTDecoder {
    override fun verifyToken(token: String): DecodedJWT = object: DecodedJWT {
        override fun getAlgorithm(): String = ""
        override fun getExpiresAt(): Date = Date.from(Instant.MAX)
        override fun getAudience(): MutableList<String> = ArrayList(0)
        override fun getId(): String = ""
        override fun getType(): String = ""
        override fun getSignature(): String = ""
        override fun getKeyId(): String = ""
        override fun getHeader(): String = ""
        override fun getToken(): String = ""
        override fun getContentType(): String = ""
        override fun getNotBefore(): Date = Date.from(Instant.now().minusSeconds(1))
        override fun getSubject(): String = subject
        override fun getPayload(): String = ""
        override fun getIssuer(): String = config.issuer
        override fun getClaims(): MutableMap<String, Claim> = mutableMapOf()
        override fun getIssuedAt(): Date = Date.from(Instant.now())
        override fun getClaim(name: String?): Claim = claims[name]!!
        override fun getHeaderClaim(name: String?): Claim = TODO()

    }
}