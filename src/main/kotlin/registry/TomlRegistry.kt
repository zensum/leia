package leia.registry

import ch.vorburger.fswatch.DirectoryWatcherBuilder
import com.moandjiezana.toml.Toml
import mu.KLogging
import registry.Tables
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference


// Combination of a Map of K,V and a function from K to V. Instead of
// supplying full pairs of K, V. We supply only Ks and the function finds
// a proper value of K. Adding the same K multiple time replaces the current
// value with a new value.
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

// Auto-watching registry for a directory of Toml-files.
class TomlRegistry(configPath: String) : Registry {
    private val watchers = mutableListOf<Triple<Tables, (Map<String, Any>) -> Any, (List<*>) -> Unit>>()
    private val holder = ResourceHolder<Path, Toml> { path ->
        Toml().also {
            if (path.toFile().exists()) {
                it.read(path.toFile())
            }
        }
    }

    private val scheduler = Executors.newScheduledThreadPool(1)

    init {
        scheduler.scheduleAtFixedRate({ this.forceUpdate() }, 1, 1, TimeUnit.MINUTES)
    }

    private val configP = FileSystems.getDefault().getPath(configPath)

    val w = DirectoryWatcherBuilder().fileFilter {
        !it.isHidden
    }
        .path(configP)
        .quietPeriodInMS(100)
        .listener { path, chng ->
            val p = path.toFile()
            logger.info { "Detected file change $path (${chng.name})" }
            if (!p.isHidden && p.isFile && p.extension.toLowerCase() == "toml") {
                onUpdate(listOf(path))
            }
        }
        .build()

    override fun getMaps(table: Tables): List<Map<String, Any>> =
        with(computeCurrentState()) {
            if (this.containsTableArray(table.key)) {
                this.getTables(table.key).map { it.toMap() }
            } else emptyList()
        }

    private fun onUpdate(paths: List<Path>) {
        holder.onChange(paths)
        watchers.forEach { (table, fn, handler) -> handler(getMaps(table).map { fn(it) }) }
        logger.info { "Updated ${paths.count()} files" }
    }

    override fun forceUpdate() {
        logger.info("Polling all config files")
        configP
            .toFile()
            .listFiles()
            .filter { it.isFile && !it.isHidden && it.extension.toLowerCase() == "toml" }
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

    override fun <T> watch(table: Tables, fn: (Map<String, Any>) -> T, handler: (List<T>) -> Unit) {
        // Generics are insufficient for this, just go with
        val t = Triple<Tables, (Map<String, Any>) -> Any, (List<*>) -> Unit>(
            table,
            fn as ((Map<String, Any>)) -> Any,
            handler as ((List<*>) -> Unit)
        )
        watchers.add(t)
    }

    companion object : KLogging()
}

