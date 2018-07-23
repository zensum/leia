package leia.sink

import leia.logic.IncomingRequest
import leia.logic.SinkDescription

class SinkNotFoundException(val sinkDescription: SinkDescription) :
    Exception("Unable to find sink for description $sinkDescription") {
}

interface SinkProvider {
    fun sinkFor (description: SinkDescription): Sink?

    // Convenience method for getting a sink and handling a request
    suspend fun handle(description: SinkDescription, req: IncomingRequest) =
        (sinkFor(description) ?: throw SinkNotFoundException(description)).handle(req)
}




