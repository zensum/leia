package leia.logic

import se.zensum.leia.config.SourceSpec

class SourceSpecsResolver private constructor(resolver: Resolver) : Resolver by resolver {
    constructor(specs: List<SourceSpec>) : this(specsToResolver(specs))
    companion object {
        private fun specsToResolver(specs: List<SourceSpec>) =
            specs.map { SourceSpecResolver(it) }
                .toList()
                .let { DisjunctiveResolver(it) }
    }
}
