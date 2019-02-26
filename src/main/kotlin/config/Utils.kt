package config

object Utils {
    inline fun <reified T> uneraseType(xs: Iterable<*>): List<T> =
        xs.map {
            it as? T ?: throw RuntimeException("Could not cast as ${T::class}")
        }.toList()

    inline fun <reified T> unEraseMapType(map: Map<*, *>): Map<T, Any> =
        map.filter { it.key is T }.map {
            it.key as T to it.value as Any
        }.toMap()

    inline fun <reified T> unEraseMapTypes(map: Map<*, *>): Map<T, T> =
        map.filter { it.key is T && it.value is T }.map {
            it.key as T to it.value as T
        }.toMap()

    fun <T> parseOptions(option: Any?, unereaseFn: (Map<*, *>) -> Map<String, T>): Map<String, T> = when (option) {
        null -> emptyMap()
        is Map<*, *> -> unereaseFn(option)
        else -> throw RuntimeException("Invalid option: $option")
    }
}