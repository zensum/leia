package se.zensum.leia.config


object DefaultConfiguration : ConfigProvider {
    private val cfg by lazy {
        TomlConfigProvider.fromConfiguredPath()
    }
    override fun getRoutes(): Map<String, TopicRouting> =
        cfg.getRoutes()
}

