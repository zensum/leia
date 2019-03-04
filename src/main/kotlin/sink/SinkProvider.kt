package leia.sink

import leia.logic.IncomingRequest
import leia.logic.SinkDescription
import se.zensum.leia.config.Format

class SinkNotFoundException(desciption: String) : Exception("Unable to find sink $desciption") {
    constructor(sinkDescription: SinkDescription) : this("for description $sinkDescription")
}


interface SinkProvider {
    fun sinkFor(description: SinkDescription): Sink?

    // Convenience method for getting a sink and handling a request
    suspend fun handle(description: SinkDescription, req: IncomingRequest) =
        (sinkFor(description) ?: throw SinkNotFoundException(description)).handle(req)

    // Convenience method for getting a sink and checking it's health status
    suspend fun check(sinkName: String) =
        (sinkFor(SinkDescription("any", "any", Format.RAW_BODY, sinkName, null))
            ?: throw SinkNotFoundException(sinkName)).healthCheck()
}




