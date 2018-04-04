package se.zensum.leia.config

interface ConfigProvider {
    fun getRoutes(): Map<String, TopicRouting>
}

