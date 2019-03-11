package leia.sink

import leia.logic.IncomingRequest

interface Sink {
    suspend fun handle(incomingRequest: IncomingRequest): SinkResult
    suspend fun healthCheck(): SinkResult
}

