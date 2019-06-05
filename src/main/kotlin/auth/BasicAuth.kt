package se.zensum.leia.auth

import com.mantono.pyttipanna.hashing.HashAlgorithm
import com.mantono.pyttipanna.hashing.hash
import com.mantono.pyttipanna.transformation.Base64
import config.Utils.parseOptions
import config.Utils.unEraseMapTypes
import leia.logic.IncomingRequest
import mu.KotlinLogging
import org.apache.commons.codec.binary.Hex
import java.security.MessageDigest

private val log = KotlinLogging.logger("basic-auth")

private const val HEADER = "Authorization"

class BasicAuth(credentials: Map<String, String>) : AuthProvider {

    private val credentials: Map<String, ByteArray> = credentials.mapValues {
        Hex.decodeHex(it.value)
    }

    init {
        this.credentials.values.forEach { hash ->
            require(hash.size == 32) {
                "Input for hashed passwords is expected to be SHA256 of 32 bytes length (was ${hash.size})"
            }
        }
    }


    override fun verify(matching: List<String>, incomingRequest: IncomingRequest): AuthResult {
        if (!incomingRequest.hasHeader(HEADER))
            return AuthResult.Denied.NoCredentials

        val credential: String = incomingRequest.getHeaderValue(HEADER)
            .first()
            .removePrefix("Basic")
            .trim()

        return verify(credential)
    }

    /**
     * @param credential the base64 encoded version of the username and password, as
     * it is provided in the header without the "Basic" prefix
     */
    fun verify(credential: String): AuthResult {
        return try {
            val credentialDecoded = String(Base64.asBytes(credential))
            if (credentialDecoded.count { it == ':' } != 1)
                return AuthResult.Denied.InvalidCredentials
            val (user: String, password: String) = credentialDecoded.split(":")
            val hashedPass: ByteArray = hash(password, HashAlgorithm.SHA256)
            if (MessageDigest.isEqual(credentials[user], hashedPass)) {
                AuthResult.Authorized(user)
            } else {
                AuthResult.Denied.InvalidCredentials
            }
        } catch (e: IllegalArgumentException) {
            log.warn("Credentials for basic authentication with invalid base64 was used: ${e.message}")
            AuthResult.Denied.InvalidCredentials
        }
    }

    companion object {
        fun fromOptions(options: Map<String, Any>): BasicAuth {
            val map = parseOptions(options["basic_auth_users"], ::unEraseMapTypes)
            return BasicAuth(map)
        }
    }
}

/**
 * Always set as [AuthResult.Authorized]
 */
object NoCheck : AuthProvider {
    override fun verify(matching: List<String>, incomingRequest: IncomingRequest): AuthResult = AuthResult.Authorized(null)
}
