package registry

enum class Tables(val key: String) {
    Routes("routes"),
    SinkProviders("sink-providers"),
    AuthProviders("auth_providers")
}