package leia.sink

import leia.Atom
import leia.logic.IncomingRequest
import leia.logic.SinkDescription

class SinkProviderAtom(private var cur: SinkProvider) : SinkProvider, Atom<SinkProvider> {
    override fun set(new: SinkProvider) {
        cur = new
    }
    override fun sinkFor(description: SinkDescription): Sink? = cur.sinkFor(description)
    override suspend fun handle(description: SinkDescription, req: IncomingRequest): SinkResult =
        cur.handle(description, req)

}