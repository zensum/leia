package auth

import se.zensum.leia.auth.AuthProvider
import se.zensum.leia.auth.BasicAuth

interface AuthProviderFactory {
    fun create(spec: AuthProviderSpec): AuthProvider?
}

object DefaultAuthProviderFactory: AuthProviderFactory {
    override fun create(spec: AuthProviderSpec): AuthProvider? =
        when(spec.type.toLowerCase()) {
            "basic_auth" -> BasicAuth.fromOptions(spec.options)
            "jwk" -> TODO("Make me later...")
            else -> throw RuntimeException("No AuthProvider matching type ${spec.type}")
    }

}