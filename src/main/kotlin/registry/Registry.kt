package leia.registry

// A registry works similar to kubernetes resources, there are named collections
// which consist of documents of that type. The registry provides the
// opportunity to watch such a collection for changes while providing a transform
// from JSON-style data to domain types.
interface Registry {
    fun <T> watch(name: String,
                  fn: (Map<String, Any>) -> T,
                  handler: (List<T>) -> Unit)
}