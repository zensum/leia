package leia.logic

// A resolver created from a list of resolvers, combining the results from each
// to get a match if any.
class DisjunctiveResolver(private val resolvers: List<Resolver>) : Resolver {
    override fun resolve(req: IncomingRequest): Result =
        if (resolvers.isEmpty()) {
            NoMatch
        } else resolvers.asSequence().map {
            it.resolve(req)
        }.reduce { acc, result ->
            acc.combine(result)
        }
}