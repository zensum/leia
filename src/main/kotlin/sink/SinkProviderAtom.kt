package leia.sink

import leia.Atom

class SinkProviderAtom(private var cur: SinkProvider) : SinkProvider by cur, Atom<SinkProvider> {
    override fun set(new: SinkProvider) {
        cur = new
    }
}