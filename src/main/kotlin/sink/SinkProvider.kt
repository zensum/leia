package leia.sink

import leia.logic.SinkDescription



interface SinkProvider {
    fun sinkFor (sink: SinkDescription): Sink
}




