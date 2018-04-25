package leia.logic

class DisjunctiveResolver(private val resolvers: List<Resolver>) : Resolver {
    override fun resolve(req: IncomingRequest): Result =
        resolvers.asSequence().map {
            it.resolve(req)
        }.reduce { acc, result ->
            acc.combine(result)
        }
}

