package leia.sink

import leia.logic.IncomingRequest
import leia.logic.SinkDescription

private object NullSink : Sink {
    override suspend fun handle(incomingRequest: IncomingRequest): SinkResult =
        SinkResult.SuccessfullyWritten
}

object NullSinkProvider : SinkProvider {
    override fun sinkFor(description: SinkDescription): Sink? {
        return NullSink
    }
}