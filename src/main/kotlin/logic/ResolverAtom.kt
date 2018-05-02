package leia.logic

import leia.Atom

class ResolverAtom(private var inner: Resolver) : Resolver by inner, Atom<Resolver> {
    override fun set(new: Resolver) {
        inner = new
    }
}

