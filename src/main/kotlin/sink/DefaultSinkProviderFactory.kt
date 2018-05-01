package leia.sink

import se.zensum.leia.config.SinkProviderSpec

class DefaultSinkProviderFactory : SinkProviderFactory {
    override fun create(spec: SinkProviderSpec): SinkProvider? =
        when(spec.type.toLowerCase()) {
            "null" -> NullSinkProvider
            "kafka" -> KafkaSinkProvider(spec.options["host"])
            else ->
                throw RuntimeException("No sinkProvider matching type ${spec.type}")
        }
}
