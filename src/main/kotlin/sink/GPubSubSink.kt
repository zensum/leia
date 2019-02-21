package leia.sink

import com.google.cloud.pubsub.v1.Publisher
import com.google.protobuf.ByteString
import com.google.pubsub.v1.ProjectTopicName
import com.google.pubsub.v1.PubsubMessage
import leia.logic.IncomingRequest
import leia.logic.SinkDescription

class PublisherProvider(private val projectId: String?) {
    private val publishers = hashMapOf<String, Publisher>()
    fun getPublisher(topic: String): Publisher {
        var publisher = publishers[topic]
        if (publisher == null) {
            publisher = if (projectId != null)
                Publisher.newBuilder(ProjectTopicName.of(projectId, topic)).build()
            else
                Publisher.newBuilder(topic).build()
            publishers[topic] = publisher
        }
        return publisher!!
    }
}

// Sink for Cloud Pub/Sub from Google
private class GPubSubSink(
    private val publisherProvider: PublisherProvider,
    private val description: SinkDescription
) : Sink {
    override suspend fun handle(incomingRequest: IncomingRequest): SinkResult {
        val body = description.dataFormat.generateBody(incomingRequest)
        val data = ByteString.copyFrom(body)
        val message = PubsubMessage.newBuilder()
            .setData(data)
            .putAttributes("key", incomingRequest.requestId.toString())
            .putAttributes("leia/user", description.authorizedAs ?: "")
            .build()

        val publisher = publisherProvider.getPublisher(description.topic)
        val future = publisher.publish(message)

        return try {
            future.get()
            SinkResult.SuccessfullyWritten
        } catch (e: Throwable) {
            SinkResult.WritingFailed(RuntimeException(e.message))
        }
    }
}

class GPubSubSinkProvider(projectId: String?) : SinkProvider {
    private val publisher = PublisherProvider(projectId)
    override fun sinkFor(description: SinkDescription): Sink? =
        GPubSubSink(publisher, description)
}