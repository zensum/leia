package se.zensum.leia.auth

import com.mantono.pyttipanna.HashAlgorithm
import com.mantono.pyttipanna.hash
import io.ktor.util.decodeBase64
import mu.KotlinLogging
import org.apache.commons.codec.binary.Hex
import java.util.*

private val log = KotlinLogging.logger("basic-auth")

class BasicAuth(credentials: Map<String, String>): AuthProvider {

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

    /**
     * @param credential the base64 encoded version of the username and password, as
     * it is provided in the header
     */
    override fun verify(credential: String): AuthResult {
        return try {
            val credentialDecoded = String(decodeBase64(credential))
            val (user: String, password: String) = credentialDecoded.split(":")
            val hashedPass: ByteArray = hash(password, HashAlgorithm.SHA256)
            if(Arrays.equals(credentials[user], hashedPass)) {
                AuthResult.Valid(user)
            }
            else {
                AuthResult.Invalid
            }
        }
        catch(e: IllegalArgumentException) {
            log.warn("Credentials for basic authentication with invalid base64 was used: ${e.message}")
            AuthResult.Invalid
        }
    }

    companion object {
    	fun fromOptions(options: Map<String, Any>): BasicAuth {
            println(options)
            TODO()
        }
    }
}