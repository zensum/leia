package leia.registry

import registry.Tables

// A registry works similar to kubernetes resources, there are named collections
// which consist of documents of that type. The registry provides the
// opportunity to watch such a collection for changes while providing a transform
// from JSON-style data to domain types.
interface Registry {
    fun forceUpdate()
    // get list of key value pairs for given type of config e.g. route
    fun getMaps(table: Tables): List<Map<String, Any>>

    fun <T> watch(table: Tables,
                  fn: (Map<String, Any>) -> T,
                  handler: (List<T>) -> Unit)
}