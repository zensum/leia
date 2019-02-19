package leia.sink

import se.zensum.leia.config.SinkProviderSpec

class DefaultSinkProviderFactory : SinkProviderFactory {
    override fun create(spec: SinkProviderSpec): SinkProvider? =
        when (spec.type.toLowerCase()) {
            "null" -> NullSinkProvider
            "kafka" -> KafkaSinkProvider(spec.options["host"] as String?)
            "redis" -> RedisSinkProvider(spec.options["host"] as String?, (spec.options["port"] as String?)?.toIntOrNull())
            "gpubsub" -> GPubSubSinkProvider(spec.options["projectId"] as String?)
            "always-error" -> AlwaysErrorSinkProvider(spec.options["message"] as String)
            else ->
                throw RuntimeException("No sinkProvider matching type ${spec.type}")
        }
}
