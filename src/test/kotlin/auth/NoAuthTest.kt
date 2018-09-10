package se.zensum.leia.auth

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertNull

class NoAuthTest {

    @Test
    fun `allow request when no credentials are needed`() {
        val req = genericRequest()
        val result: AuthResult = NoCheck.verify(listOf("no_auth"), req)
        assertTrue(result is AuthResult.Authorized)
        result as AuthResult.Authorized
        assertNull(result.identifier)
    }
}