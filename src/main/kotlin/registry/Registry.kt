package leia.registry

interface Registry {
    fun <T> watch(name: String,
                  fn: (Map<String, Any>) -> T,
                  handler: (List<T>) -> Unit)
}