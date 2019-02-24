package leia.registry

import registry.Tables

class Registries(private val registries: List<Registry>) : Registry {

    private val watchers = mutableListOf<Triple<Tables, (Map<String, Any>) -> Any, (List<*>) -> Unit>>()

    override fun forceUpdate() {
        registries.forEach { it.forceUpdate() }
    }

    override fun getMaps(table: Tables): List<Map<String, Any>> =
        registries.map { it.getMaps(table) }.reduce { acc, list -> acc + list }

    private fun onUpdate(table: Tables) {
        val maps = getMaps(table)
        watchers.filter { triple -> triple.first == table }.map { triple ->
            triple.third(maps.map { triple.second(it) })
        }
    }

    override fun <T> watch(table: Tables, fn: (Map<String, Any>) -> T, handler: (List<T>) -> Unit) {
        // Generics are insufficient for this, just go with
        @Suppress("UNCHECKED_CAST") val t = Triple(
            table,
            fn as ((Map<String, Any>)) -> Any,
            handler as ((List<*>) -> Unit)
        )
        watchers.add(t)
        registries.forEach {
            it.watch(table, {}, { onUpdate(table) })
        }
    }
}

