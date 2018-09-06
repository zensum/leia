package auth

import org.junit.jupiter.api.Test
import se.zensum.leia.auth.AuthResult
import se.zensum.leia.auth.NoCheck
import se.zensum.leia.auth.genericRequest
import kotlin.test.assertEquals

class NoAuthTest {

    @Test
    fun `allow request when no credentials are needed`() {
        val req = genericRequest()
        val result: AuthResult = NoCheck.verify(listOf("no_auth"), req)
        assertEquals(AuthResult.NoAuthorizationCheck, result)
    }
}