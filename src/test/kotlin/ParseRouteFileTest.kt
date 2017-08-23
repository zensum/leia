import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import se.zensum.webhook.lineToPair
import se.zensum.webhook.parseRoutesFile
import java.lang.IllegalArgumentException

class ParseRoutesTest {

    @Test
    fun testReadFile() {
        val routes: Map<String, String> = parseRoutesFile("src/test/routes")
        assertEquals(4, routes.size)

        assertEquals("test", routes["/test"])
        assertEquals("mail", routes["/status/mail"])
        assertEquals("sms", routes["/status/sms"])
        assertEquals("banks", routes["/bank/{bank}"])
    }

    @Test
    fun testLineToPair() {
        val line1 = "/test -> test"
        val line2 = "/bank/{bank} -> banks"

        val (key1, value1) = lineToPair(line1)
        assertEquals("/test", key1)
        assertEquals("test", value1)

        val (key2, value2) = lineToPair(line2)
        assertEquals("/bank/{bank}", key2)
        assertEquals("banks", value2)
    }

    @Test
    fun testLineToPairWithExtraWhiteSpace() {
        val line1 = "/test  ->  test"
        val line2 = "/bank/{bank}   ->   banks"

        val (key1, value1) = lineToPair(line1)
        assertEquals("/test", key1)
        assertEquals("test", value1)

        val (key2, value2) = lineToPair(line2)
        assertEquals("/bank/{bank}", key2)
        assertEquals("banks", value2)
    }

    @Test
    fun testLineToTrimExtraWhiteSpace() {
        val line1 = "/test -> test "

        val (key1, value1) = lineToPair(line1)
        assertEquals("/test", key1)
        assertEquals("test", value1)
    }

    @Test
    fun testLineToPairWithNoWhiteSpace() {
        val line1 = "/test->test"
        val line2 = "/bank/{bank}->banks"

        val (key1, value1) = lineToPair(line1)
        assertEquals("/test", key1)
        assertEquals("test", value1)

        val (key2, value2) = lineToPair(line2)
        assertEquals("/bank/{bank}", key2)
        assertEquals("banks", value2)
    }

    @Test
    fun testLineToPairWithMissingDelimiter() {
        assertThrows(IllegalArgumentException::class.java) {
            lineToPair("/test-test")
        }
    }

    @Test
    fun testLineToPairWithMultipleDelimiters() {
        assertThrows(IllegalArgumentException::class.java) {
            lineToPair("/test->test1->test2")
        }
    }
}