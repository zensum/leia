package leia.sink

import leia.logic.IncomingRequest
import leia.logic.SinkDescription

class PreSinkError(msg: String) : Exception(msg)

private class AlwaysErrorSink(private val exc: Exception) : Sink {
    override suspend fun handle(incomingRequest: IncomingRequest): SinkResult =
        SinkResult.WritingFailed(exc)
}

internal class AlwaysErrorSinkProvider(msg: String) : SinkProvider {
    private val sink = AlwaysErrorSink(PreSinkError(msg))
    override fun sinkFor(description: SinkDescription): Sink? = sink
}