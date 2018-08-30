package auth

import org.junit.jupiter.api.Test
import se.zensum.leia.auth.AuthProvider
import kotlin.test.assertNotNull

class AuthProviderSpecTest {

    @Test
    fun testDefaultAuthProviderFactory() {
        val spec = AuthProviderSpec(
            name = "internal",
            type = "basic_auth",
            options = emptyMap()
        )

        val provider: AuthProvider? = DefaultAuthProviderFactory.create(spec)
        assertNotNull(provider)
    }
}