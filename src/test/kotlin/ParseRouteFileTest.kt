import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.zensum.webhook.Format
import se.zensum.webhook.TopicRouting
import se.zensum.webhook.getRoutes

class ParseRoutesTest {

    @Test
    fun testReadFile() {
        val routes: Map<String, TopicRouting> = getRoutes("src/test/routes")
        assertEquals(4, routes.size)

        val test = routes["/test"]!!
        assertEquals("test", test.topic)
        assertEquals("/test", test.path)
        assertFalse(test.verify)
        assertEquals(Format.PROTOBUF, test.format)

        val mailStatus = routes["/status/mail"]!!
        assertEquals("mail-status", mailStatus.topic)
        assertEquals("/status/mail", mailStatus.path)
        assertFalse(mailStatus.verify)
        assertEquals(Format.PROTOBUF, mailStatus.format)

        val smsStatus = routes["/status/sms"]!!
        assertEquals("sms-status", smsStatus.topic)
        assertEquals("/status/sms", smsStatus.path)
        assertFalse(smsStatus.verify)
        assertEquals(Format.PROTOBUF, smsStatus.format)

        val auth = routes["/auth"]!!
        assertEquals("test", auth.topic)
        assertEquals("/auth", auth.path)
        assertTrue(auth.verify)
        assertEquals(Format.RAW_BODY, auth.format)
    }
}