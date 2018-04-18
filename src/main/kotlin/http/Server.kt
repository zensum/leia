package leia.http

import leia.logic.Resolver
import leia.sink.SinkProvider

// Marker-interface for servers
interface Server
interface ServerFactory {
    fun create(resolver: Resolver, sinkProvider: SinkProvider) : Server
}
