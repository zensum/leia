package leia.sink

import com.google.cloud.ServiceOptions
import com.google.cloud.pubsub.v1.Publisher
import com.google.protobuf.ByteString
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import kotlinx.coroutines.experimental.future.await
import leia.logic.IncomingRequest
import leia.logic.SinkDescription
import se.zensum.leia.config.Format
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

import java.util.concurrent.Executors


private fun longToBytes(x: Long): ByteArray =
    ByteBuffer.allocate(java.lang.Long.BYTES)
        .apply {
            putLong(x)
        }
        .array()

private val TPE = Executors.newScheduledThreadPool(2)

fun Publisher.publishCF(message: PubsubMessage) : CompletableFuture<Unit> =
    CompletableFuture<Unit>().also { cf ->
        publish(message).addListener(Runnable {
            cf.complete(Unit)
        }, TPE)
    }

suspend fun Publisher.publishAwait(message: PubsubMessage): Unit =
    publishCF(message).await()



class GooglePubSubSink internal constructor(private val pub: Publisher, private val format: Format) : Sink {

    override suspend fun handle(incomingRequest: IncomingRequest): SinkResult {
        val msg = PubsubMessage.newBuilder()
            .setMessageIdBytes(ByteString.copyFrom(longToBytes(incomingRequest.requestId)))
            .setData(ByteString.copyFrom(format.generateBody(incomingRequest)))
            .build()

        pub.publishAwait(msg)
        return SinkResult.SuccessfullyWritten
    }
}

class GooglePubSubSinkProvider(projectId: String?) : SinkProvider {
    private val projectId: String = projectId ?: ServiceOptions.getDefaultProjectId()
    private fun topicName(name: String): ProjectTopicName =
        ProjectTopicName.of(projectId, name)
    private val publishers = mutableMapOf<String, Publisher>()

    private fun publisherFor(ptn: ProjectTopicName) =
        Publisher.newBuilder(ptn).build()

    private fun getPublisher(name: String) = publishers.computeIfAbsent(name) {
        publisherFor(topicName(name))
    }

    override fun sinkFor(description: SinkDescription): Sink? {
        val pub = getPublisher(description.topic)
        return GooglePubSubSink(pub, description.dataFormat)
    }


}

