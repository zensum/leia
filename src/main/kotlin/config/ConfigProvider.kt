package se.zensum.leia.config

interface ConfigProvider {
    fun getRoutes(): List<SourceSpec>
    fun getSinkProviders(): List<SinkProviderSpec>
}

