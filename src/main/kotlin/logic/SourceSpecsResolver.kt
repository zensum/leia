package leia.logic

import se.zensum.leia.auth.AuthProvider
import se.zensum.leia.config.SourceSpec

// A resolver taking routing against a list of source-specs
class SourceSpecsResolver private constructor(resolver: Resolver) : Resolver by resolver {
    constructor(auth: AuthProvider, specs: List<SourceSpec>) : this(specsToResolver(auth, specs))
    companion object {
        private fun specsToResolver(auth: AuthProvider, specs: List<SourceSpec>) =
            specs.map { SourceSpecResolver(it, auth) }
                .toList()
                .let { DisjunctiveResolver(it) }
    }
}
