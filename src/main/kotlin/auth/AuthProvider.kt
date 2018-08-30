package se.zensum.leia.auth

// https://github.com/zensum/leia/issues/13
interface AuthProvider {
    fun verify(credential: String): AuthResult
}

sealed class AuthResult {
    data class Valid(val identifier: String): AuthResult()
    object Invalid: AuthResult()
}