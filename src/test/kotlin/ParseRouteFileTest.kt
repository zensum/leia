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

    private val routes: Map<String, TopicRouting> = getRoutes("src/test/routes")

    @Test
    fun testSize() {
        assertEquals(4, routes.size)
    }

    @Test
    fun testTopic() {
        routes["/status/mail"]!!.apply{
            assertEquals("mail-status", topic)
        }

        routes["/status/sms"]!!.apply{
            assertEquals("sms-status", topic)
        }

        routes["/test"]!!.apply {
            assertEquals("test", topic)
        }

        routes["/auth"]!!.apply {
            assertEquals("test", topic)
        }
    }

    @Test
    fun testPath() {
        routes["/status/mail"]!!.apply{
            assertEquals("/status/mail", path)
        }

        routes["/status/sms"]!!.apply{
            assertEquals("/status/sms", path)
        }

        routes["/test"]!!.apply {
            assertEquals("/test", path)
        }

        routes["/auth"]!!.apply {
            assertEquals("/auth", path)
        }
    }

    @Test
    fun testVerify() {
        routes["/status/mail"]!!.apply{
            assertFalse(verify)
        }

        routes["/status/sms"]!!.apply{
            assertFalse(verify)
        }

        routes["/test"]!!.apply {
            assertFalse(verify)
        }

        routes["/auth"]!!.apply {
            assertTrue(verify)
        }
    }

    @Test
    fun testFormat() {
        assertEquals(4, routes.size)

        routes["/status/mail"]!!.apply{
            assertEquals(Format.PROTOBUF, format)
        }

        routes["/status/sms"]!!.apply{
            assertEquals(Format.PROTOBUF, format)
        }

        routes["/test"]!!.apply {
            assertEquals(Format.PROTOBUF, format)
        }

        routes["/auth"]!!.apply {
            assertEquals(Format.RAW_BODY, format)
        }
    }

    @Test
    fun testExpectedMethods() {
        assertEquals(4, routes.size)

        routes["/status/mail"]!!.apply{
            val expectedMethods = setOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Head, HttpMethod.Get)
            assertEquals(expectedMethods, allowedMethods)
        }

        routes["/status/sms"]!!.apply{
            assertEquals(httpMethods.verbs, allowedMethods)
        }

        routes["/test"]!!.apply {
            assertEquals(httpMethods.verbs, allowedMethods)
        }

        routes["/auth"]!!.apply {
            val expectedMethods = setOf(HttpMethod.Post)
            assertEquals(expectedMethods, allowedMethods)
        }
    }
}