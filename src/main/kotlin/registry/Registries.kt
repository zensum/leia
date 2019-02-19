package leia.registry

import java.util.concurrent.atomic.AtomicReference

class Registries(private val registries: List<Registry>) : Registry {

    private val watchers = mutableListOf<Triple<String, (Map<String, Any>) -> Any, (List<*>) -> Unit>>()

    override fun forceUpdate() {
        registries.forEach { it.forceUpdate() }
    }

    override fun getMaps(name: String): List<Map<String, Any>> =
        registries.map { it.getMaps(name) }.reduce { acc, list -> acc + list }

    private fun onUpdate(name: String) {
        val maps = getMaps(name)
        watchers.filter { triple -> triple.first == name }.map { triple ->
            triple.third(maps.map { triple.second(it) })
        }
    }

    override fun <T> watch(name: String, fn: (Map<String, Any>) -> T, handler: (List<T>) -> Unit) {
        // Generics are insufficient for this, just go with
        val t = Triple<String, (Map<String, Any>) -> Any, (List<*>) -> Unit>(
            name,
            fn as ((Map<String, Any>)) -> Any,
            handler as ((List<*>) -> Unit)
        )
        watchers.add(t)
        registries.forEach {
            it.watch(name, {}, { onUpdate(name) })
        }
    }
}

