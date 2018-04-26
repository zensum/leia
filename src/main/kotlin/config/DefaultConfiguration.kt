package se.zensum.leia.config


object DefaultConfiguration : ConfigProvider {
    private val cfg by lazy {
        TomlConfigProvider.fromConfiguredPath()
    }
    override fun getRoutes(): List<TopicRouting> =
        cfg.getRoutes()

    override fun getSinkProviders(): List<SinkProviderSpec> =
        cfg.getSinkProviders()
}

