import org.jetbrains.ktor.http.HttpMethod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.zensum.webhook.Format
import se.zensum.webhook.TopicRouting
import se.zensum.webhook.getRoutes
import se.zensum.webhook.httpMethods

class ParseRoutesTest {

    @Test
    fun testReadFile() {
        val routes: Map<String, TopicRouting> = getRoutes("src/test/routes")
        assertEquals(4, routes.size)

        routes["/status/mail"]!!.apply{
            assertEquals("mail-status", topic)
            assertEquals("/status/mail", path)
            assertFalse(verify)
            assertEquals(Format.PROTOBUF, format)
            val expectedMethods = setOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Head, HttpMethod.Get)
            assertEquals(expectedMethods, allowedMethods)
        }

        routes["/status/sms"]!!.apply{
            assertEquals("sms-status", topic)
            assertEquals("/status/sms", path)
            assertFalse(verify)
            assertEquals(Format.PROTOBUF, format)
            assertEquals(httpMethods.verbs, allowedMethods)
        }

        routes["/test"]!!.apply {
            assertEquals("test", topic)
            assertEquals("/test", path)
            assertFalse(verify)
            assertEquals(Format.PROTOBUF, format)
            assertEquals(httpMethods.verbs, allowedMethods)
        }

        routes["/auth"]!!.apply {
            assertEquals("test", topic)
            assertEquals("/auth", path)
            assertTrue(verify)
            val expectedMethods = setOf(HttpMethod.Post)
            assertEquals(expectedMethods, format)
        }
    }
}