package leia.logic

import leia.Atom
import java.util.concurrent.atomic.AtomicReference

// An atom implementing Resolver, by passing calls on to the value contained in the atom
class ResolverAtom(inner: Resolver) : Resolver, Atom<Resolver> {
    override val reference: AtomicReference<Resolver> = AtomicReference(inner)

    override fun resolve(req: IncomingRequest): Result = reference.get().resolve(req)
}

