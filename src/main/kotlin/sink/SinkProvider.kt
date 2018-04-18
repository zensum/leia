package leia.sink

import leia.logic.IncomingRequest
import leia.logic.SinkDescription

interface SinkProvider {
    fun sinkFor (description: SinkDescription): Sink?

    // Convenience method for getting a sink and handling a request
    suspend fun handle(description: SinkDescription, req: IncomingRequest) =
        (sinkFor(description) ?: throw NullPointerException("Unable to find sink for description $description")).handle(req)
}




