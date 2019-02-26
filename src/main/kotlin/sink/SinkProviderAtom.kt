package leia.sink

import leia.Atom
import leia.logic.IncomingRequest
import leia.logic.SinkDescription
import java.util.concurrent.atomic.AtomicReference

class SinkProviderAtom(cur: SinkProvider) : SinkProvider, Atom<SinkProvider> {
    override val reference: AtomicReference<SinkProvider> = AtomicReference(cur)

    override fun sinkFor(description: SinkDescription): Sink? = reference.get().sinkFor(description)
    override suspend fun handle(description: SinkDescription, req: IncomingRequest): SinkResult =
        reference.get().handle(description, req)

}