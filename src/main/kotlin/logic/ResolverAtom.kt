package leia.logic

import leia.Atom

// An atom implementing Resolver, by passing calls on to the value contained in the atom
class ResolverAtom(private var inner: Resolver) : Resolver, Atom<Resolver> {
    override fun set(new: Resolver) {
        inner = new
    }

    override fun resolve(req: IncomingRequest): Result = inner.resolve(req)
}

