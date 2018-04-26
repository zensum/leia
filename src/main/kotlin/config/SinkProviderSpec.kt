package se.zensum.leia.config

data class SinkProviderSpec(
    val name: String,
    val isDefault: Boolean,
    val type: String,
    val options: Map<String, String>
)

