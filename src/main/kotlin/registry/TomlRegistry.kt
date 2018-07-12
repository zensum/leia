package leia.registry

import ch.vorburger.fswatch.DirectoryWatcherBuilder
import com.moandjiezana.toml.Toml
import io.ktor.util.extension
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.util.concurrent.atomic.AtomicReference

private fun pathToBaseName(p: Path) =
    if (p.toFile().isFile)
        p.parent
    else p

class ResourceHolder<K, out V>(private val parser: (K) -> V) {
    private val entriesA = AtomicReference(mapOf<K, V>())
    fun onChange(ks: Collection<K>) {
        // actually more complex than this :( figure out what to do here
        entriesA.updateAndGet {
            it + ks.map { k -> k to parser(k) }.toMap()
        }
    }
    fun getData(): Map<K, V> = entriesA.get()
}

class TomlRegistry(configPath: String): Registry {
    private val watchers = mutableListOf<Triple<String, (Map<String, Any>) -> Any,(List<*>) -> Unit>>()
    private val holder = ResourceHolder<Path, Toml>({ path ->
        Toml().also {
            if (path.toFile().exists()) {
                it.read(path.toFile())
            }
        }
    })

    private val configP = FileSystems.getDefault().getPath(configPath)

    val w = DirectoryWatcherBuilder().fileFilter {
        !it.isHidden
    }
        .path(configP)
        .quietPeriodInMS(100)
        .listener { path, chng ->
            val p = path.toFile()
            println("$path ${chng.name}")
            if (!p.isHidden &&p.isFile && p.extension.toLowerCase() == "toml") {
                onUpdate(listOf(path))
            }
        }
        .build()

    private fun onUpdate(paths: List<Path>) {
        holder.onChange(paths)
        val s = computeCurrentState()
        watchers.forEach { (table, fn, handler) ->
            val m = if (s.containsTableArray(table)) {
                s.getTables(table).map { fn(it.toMap()) }
            } else emptyList()
            handler(m)
        }
    }

    fun forceUpdate() {
        configP
            .toFile()
            .listFiles()
            .filter { it.isFile && !it.isHidden && it.extension.toLowerCase() == "toml"}
            .map { it.toPath() }
            .let(this::onUpdate)
    }

    private fun computeCurrentState(): Toml {
        val state = Toml()
        holder.getData().values.forEach {
            state.read(it)
        }
        return state
    }

    override fun <T> watch(name: String, fn: (Map<String, Any>) -> T, handler: (List<T>) -> Unit) {
        // Generics are insufficient for this, just go with
        val t = Triple<String, (Map<String, Any>) -> Any,(List<*>) -> Unit>(
            name,
            fn as ((Map<String, Any>)) -> Any,
            handler as ((List<*>) -> Unit)
        )
        watchers.add(t)
    }
}

