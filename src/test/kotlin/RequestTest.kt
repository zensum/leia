package se.zensum.leia

import frans.producer.mock.MockProducer
import frans.producer.mock.MockProducerFactory
import franz.ProducerBuilder
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.Test
import kotlin.test.*

private val routes: Map<String, TopicRouting> = getRoutes("src/test/routes")

private fun withApp(mp: MockProducer<String, ByteArray>, test: TestApplicationEngine.() -> Unit) {
    val p = ProducerBuilder.ofByteArray
        .setProducer(MockProducerFactory(mp)).create()
    return withTestApplication(leia(p, routes, false), test)
}

class RequestTest {

    @Test fun test404() {
        val p = MockProducer<String, ByteArray>()
        withApp(p) {
            with(handleRequest(HttpMethod.Get, "/url-that-doesnt-exist")) {
                assertEquals(HttpStatusCode.NotFound, response.status())
            }
        }
    }
    @Test fun testNotAuth() {
        val p = MockProducer<String, ByteArray>()
        withApp(p) {
            with(handleRequest(HttpMethod.Post, "/auth")) {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }
}