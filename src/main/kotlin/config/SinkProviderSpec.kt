package se.zensum.leia.config

import config.Utils.parseOptions
import config.Utils.unEraseMapType

data class SinkProviderSpec(
    val name: String,
    val isDefault: Boolean,
    val type: String,
    val options: Map<String, Any>
) {
    companion object {
        private val emptyOptions = mutableMapOf<String, Any>()

        fun fromMap(m: Map<String, Any>) =
            SinkProviderSpec(
                name = m["name"] as String,
                isDefault = m.getOrDefault("default", false) as Boolean,
                type = m.getOrDefault("type", "kafka") as String,
                options = parseOptions((m.getOrDefault("options", emptyOptions)), ::unEraseMapType)
            )
    }
}

