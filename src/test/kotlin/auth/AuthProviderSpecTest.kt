package se.zensum.leia.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthProviderSpecTest {

    @Test
    fun testDefaultAuthProviderFactory() {
        val spec = AuthProviderSpec(
            name = "internal",
            type = "basic_auth",
            options = mapOf(
                "basic_auth_users" to mapOf(
                    "alice" to "3df8232404be55d2d6b85be79b99316e028676b69efa2ce91d9094dd18dd3502",
                    "bob" to "c45441cb346b69e247711e9c86405ce9de964bb89c68ad02b8eb2f55912c58ba",
                    "eve" to "abb9eca0af80e9f321af97b2952ca035edfdc63d5118299e32047156339955b3"
                )
            )
        )

        val provider: AuthProvider? = DefaultAuthProviderFactory.create(spec)
        assertNotNull(provider)
    }

    @Test
    fun testAuthProviderSpecFromMap() {
        val spec = AuthProviderSpec.fromMap(
            mapOf(
                "name" to "foo",
                "type" to "basic_auth"
            )
        )
        assertEquals("foo", spec.name)
        assertEquals("basic_auth", spec.type)
        assertTrue(spec.options.isEmpty())
    }
}