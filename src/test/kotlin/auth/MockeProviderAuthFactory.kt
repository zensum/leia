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
import java.util.*

object MockAuthProviderFactory: AuthProviderFactory {
    override fun create(spec: AuthProviderSpec): AuthProvider =
        when(spec.type.toLowerCase()) {
            "basic_auth" -> BasicAuth.fromOptions(spec.options)
            "no_auth" -> NoCheck
            "jwk" -> JwkAuth.fromOptions(spec.options) { _ ->
                MockJWTDecoder.Valid
            }
            else -> throw RuntimeException("No AuthProvider matching type ${spec.type}")
        }
}

internal sealed class MockJWTDecoder: JWTDecoder {
    object Invalid: MockJWTDecoder() {
        override fun verifyToken(token: String): DecodedJWT? = null
    }

    object Valid: MockJWTDecoder() {
        override fun verifyToken(token: String): DecodedJWT = JsonWebToken(token)

        private data class JsonWebToken(private val token: String): DecodedJWT {
            private val parts: List<String> = token.split(".")
            private val header: String = decode(parts[0])
            private val payload: String = decode(parts[1])
            private val signature: String = decode(parts[2])

            init {
                require(parts.size == 3) { "Not a valid Json Web Token: $token" }
            }

            private fun decode(b64: String): String = String(Base64.getUrlDecoder().decode(b64))

            private fun String.field(f: String): String? = this
                .split(Regex("[\n,}]"))
                .firstOrNull { it.contains("\"$f\":") }
                ?.split(":")
                ?.last()
                ?.replace("\"", "")
                ?.trim()

            operator fun get(claim: String): String? = payload.field(claim)

            override fun getAlgorithm(): String = header.field("alg")!!

            override fun getExpiresAt(): Date?
                = payload.field("exp")?.toLong()?.let { Date(it*1000L) }

            override fun getAudience(): List<String>
                = payload.field("aud")?.let { listOf(it) } ?: emptyList()

            override fun getId(): String? = this["jti"]

            override fun getType(): String? = header.field("typ")

            override fun getSignature(): String = signature
            override fun getKeyId(): String? = header.field("kid")
            override fun getHeader(): String = header
            override fun getToken(): String = token
            override fun getContentType(): String? = header.field("cty")

            override fun getNotBefore(): Date?
                = this["nbf"]?.toLong()?.let { Date(it*1000L) }

            override fun getSubject(): String? = this["sub"]
            override fun getPayload(): String = payload
            override fun getIssuer(): String = this["iss"]!!
            override fun getClaims(): MutableMap<String, Claim>
                = throw UnsupportedOperationException()

            override fun getIssuedAt(): Date =
                Date(this["iat"]!!.toLong() *1000L)

            override fun getClaim(name: String): Claim = Claim(this[name])

            override fun getHeaderClaim(name: String): Claim = Claim(header.field(name))
        }

        private data class Claim(private val value: String?): com.auth0.jwt.interfaces.Claim {
            override fun isNull(): Boolean = value == null
            override fun asDate(): Date?
                = value?.toLong()?.let { ts -> Date(ts*1000L) }
            override fun asMap(): MutableMap<String, Any>? = value as? MutableMap<String, Any>
            override fun <T : Any?> asList(tClazz: Class<T>?): MutableList<T>?
                = value as? MutableList<T>
            override fun asLong(): Long? = value?.toLong()
            override fun <T : Any?> `as`(tClazz: Class<T>?): T? = value as? T
            override fun asBoolean(): Boolean? = value?.toBoolean()
            override fun asDouble(): Double? = value?.toDouble()
            override fun asString(): String? = value
            override fun <T : Any?> asArray(tClazz: Class<T>?): Array<T>
                = throw UnsupportedOperationException()
            override fun asInt(): Int? = value?.toInt()

        }
    }
}