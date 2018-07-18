package se.zensum.leia.config

data class SinkProviderSpec(
    val name: String,
    val isDefault: Boolean,
    val type: String,
    val options: Map<String, Any>
) {
    companion object {
        private val emptyOptions = mutableMapOf<String, Any>()
        fun fromMap(m: Map<String, Any>)  =
            SinkProviderSpec(
                name = m["name"] as String,
                isDefault = m.getOrDefault("default", false) as Boolean,
                type = m.getOrDefault("type", "kafka") as String,
                options = (m.getOrDefault("options", emptyOptions) as MutableMap<String, Any>).toMap()
            )
    }
}

