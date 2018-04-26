package se.zensum.leia.config

interface ConfigProvider {
    fun getRoutes(): List<TopicRouting>
    fun getSinkProviders(): List<SinkProviderSpec>
}

