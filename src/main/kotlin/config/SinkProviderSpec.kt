package se.zensum.leia.config

data class SinkProviderSpec(
    val name: String,
    val isDefault: Boolean,
    val type: String,
    val options: Map<String, Any>
) {
    companion object {
        private val emptyOptions = mutableMapOf<String, Any>()

        private inline fun <reified T> unEraseMapType(map: Map<*, *>): Map<T, Any> =
            map.filter { it.key is T }.map {
                it.key as T to it.value as Any
            }.toMap()

        private fun parseOptions(option: Any?): Map<String, Any> = when (option) {
            null -> emptyMap()
            is Map<*, *> -> unEraseMapType(option)
            else -> throw RuntimeException("Invalid option: $option")
        }

        fun fromMap(m: Map<String, Any>) =
            SinkProviderSpec(
                name = m["name"] as String,
                isDefault = m.getOrDefault("default", false) as Boolean,
                type = m.getOrDefault("type", "kafka") as String,
                options = parseOptions((m.getOrDefault("options", emptyOptions)))
            )
    }
}

