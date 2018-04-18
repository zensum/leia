package leia.sink

import leia.logic.SinkDescription

class DisjunctiveSinkProvider(private val providers: List<SinkProvider>) : SinkProvider {
    override fun sinkFor(description: SinkDescription): Sink? =
        providers.asSequence().map {
            it.sinkFor(description)
        }.filterNotNull().first()
}

