package se.zensum.leia.auth

// https://github.com/zensum/leia/issues/13
interface AuthProvider<out T> {
    fun verify(credential: String): AuthResult<T>
}

sealed class AuthResult<out T> {
    data class Valid<out T>(val identifier: T): AuthResult<T>()
    object Invalid: AuthResult<Nothing>()
}