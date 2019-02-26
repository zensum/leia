package leia.registry

import registry.Tables

// A registry works similar to kubernetes resources, there are named collections
// which consist of documents of that type. The registry provides the
// opportunity to watch such a collection for changes while providing a transform
// from JSON-style data to domain types.
abstract class Registry {
    protected val watchers = mutableListOf<Triple<Tables, (Map<String, Any>) -> Any, (List<*>) -> Unit>>()
    abstract fun forceUpdate()

    // get list of key value pairs for given type of config e.g. route
    abstract fun getMaps(table: Tables): List<Map<String, Any>>

    protected fun notifyWatchers() {
        watchers.forEach { (table, fn, handler) -> handler(getMaps(table).map { fn(it) }) }
    }

    open fun <T> watch(table: Tables,
                       fn: (Map<String, Any>) -> T,
                       handler: (List<T>) -> Unit) {
        // Generics are insufficient for this, just go with
        @Suppress("UNCHECKED_CAST") val t = Triple(
            table,
            fn as ((Map<String, Any>)) -> Any,
            handler as ((List<*>) -> Unit)
        )
        watchers.add(t)
    }
}