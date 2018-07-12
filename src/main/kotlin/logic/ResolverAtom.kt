package leia.logic

import leia.Atom

class ResolverAtom(private var inner: Resolver) : Resolver, Atom<Resolver> {
    override fun set(new: Resolver) {
        inner = new
    }

    override fun resolve(req: IncomingRequest): Result = inner.resolve(req)
}

