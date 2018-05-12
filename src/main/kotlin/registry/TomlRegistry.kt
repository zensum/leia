package leia.registry

import com.moandjiezana.toml.Toml
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.util.concurrent.atomic.AtomicReference

private fun pathToBaseName(p: Path) =
    if (p.toFile().isFile)
        p.parent
    else p

private class Watcher(private val toWatch: Path, onChange: (Collection<Path>) -> Unit) {
    private val ws = FileSystems.getDefault().newWatchService()
    // If the path is a file, find its parent for watching
    private val watchingFileOnly = toWatch.toFile().let {
        !it.exists() || it.isFile
    }
    private val watchPath = pathToBaseName(toWatch.toAbsolutePath())

    // Watch for all file changes
    private val wk = watchPath.register(ws,
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_DELETE,
        StandardWatchEventKinds.ENTRY_MODIFY
    )

    private val th = Thread {
        while(true) {
            // Catch interrupted?
            val eventKey = try {
                ws.take()
            } catch (ex: InterruptedException) {
                break
            }

            val evts = eventKey
                .pollEvents()
                .asSequence()
                .filter {
                    it.kind() != StandardWatchEventKinds.OVERFLOW
                }.map { it.context() as Path }
                .filterNot { it.toFile().isHidden }
                .let {
                    if (watchingFileOnly) {
                        it.filter { it == toWatch }
                    } else it
                }.toSet()
            onChange(evts)
        }
    }

    fun start() = th.start()
    fun stop() = th.interrupt()
}

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
    private val watcher = Watcher(FileSystems.getDefault().getPath(configPath), {
        holder.onChange(it)
        val s = computeCurrentState()
        watchers.forEach { (table, fn, handler) ->
            val m = if (s.containsTableArray(table)) {
                s.getTables(table).map { fn(it.toMap()) }
            } else emptyList()
            handler(m)
        }
    })

    init {
        watcher.start()
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

