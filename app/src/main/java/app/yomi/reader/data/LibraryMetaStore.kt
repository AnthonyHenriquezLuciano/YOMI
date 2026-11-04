package app.yomi.reader.data

import android.content.Context
import app.yomi.reader.core.Chapter
import app.yomi.reader.core.Work
import org.json.JSONArray
import org.json.JSONObject

enum class ReadingShelf(val label: String) {
    PENDING("Pendiente"),
    READING("En lectura"),
    PAUSED("Pausado"),
    COMPLETED("Completado"),
    REREADING("Releyendo")
}
enum class LibrarySort(val label: String) {
    ALPHABETICAL("Alfabéticamente"),
    CHAPTER_COUNT("Por número de capítulos"),
    LAST_READ("Por última fecha de lectura"),
    REMAINING("Por capítulos restantes"),
    RECENT_CHAPTERS("Por capítulos más recientes"),
    FETCHED("Por fecha de obtención del capítulo"),
    OLDEST("Por antigüedad en la biblioteca"),
    RANDOM("Al azar")
}
enum class LibraryView(val label: String) {
    COMPACT("Cuadrícula compacta"),
    WIDE("Cuadrícula amplia"),
    COVERS("Cuadrícula solo de portadas"),
    LIST("Lista")
}
enum class LibraryGroup(val label: String) {
    NONE("Desagrupado"), CATEGORY("Categorías"),
    SOURCE("Fuentes"), STATUS("Estado")
}
data class LibraryAppearance(
    val view: LibraryView = LibraryView.COMPACT,
    val columns: Int = 3,
    val badgeDownloads: Boolean = true,
    val badgeUnread: Boolean = true,
    val badgeLocal: Boolean = true,
    val badgeLanguage: Boolean = false,
    val showContinue: Boolean = true,
    val showCategoryTabs: Boolean = true,
    val showCounts: Boolean = true,
    val group: LibraryGroup = LibraryGroup.NONE,
    val sort: LibrarySort = LibrarySort.ALPHABETICAL,
    val descending: Boolean = false,
    val onlyDownloaded: Boolean = false,
    val onlyUnread: Boolean = false,
    val onlyStarted: Boolean = false,
    val onlyFavorite: Boolean = false,
    val onlyCompleted: Boolean = false
)
data class CachedChapters(val chapters: List<Chapter>, val fetchedAt: Long)
data class LibraryItem(
    val work: Work,
    val categories: Set<String>,
    val favorite: Boolean,
    val shelf: ReadingShelf,
    val addedAt: Long,
    val lastReadAt: Long,
    val chapters: Int,
    val unread: Int,
    val downloaded: Int,
    val latestPublishedAt: String,
    val fetchedAt: Long
)

/** User-created categories and chapter metadata. All data stored locally. */
class LibraryMetaStore(context: Context) {
    private val prefs = context.getSharedPreferences("yomi-library-meta-v1", Context.MODE_PRIVATE)
    private val lock = Any()

    private fun read(key: String): JSONObject = try {
        JSONObject(prefs.getString(key, "{}") ?: "{}")
    } catch (_: Exception) { JSONObject() }

    private fun update(key: String, edit: (JSONObject) -> Unit) = synchronized(lock) {
        val root = read(key)
        edit(root)
        prefs.edit().putString(key, root.toString()).commit()
    }

    fun categories(): List<String> = synchronized(lock) {
        val array = read("categories").optJSONArray("names") ?: JSONArray()
        (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }
    }

    fun addCategory(value: String): Boolean {
        val name = value.trim().replace(Regex("\\s+"), " ")
        if (name.length !in 1..40) return false
        synchronized(lock) {
            val names = categories()
            if (names.any { it.equals(name, true) }) return false
            update("categories") { it.put("names", JSONArray(names + name)) }
        }
        return true
    }

    fun renameCategory(from: String, to: String): Boolean {
        val name = to.trim().replace(Regex("\\s+"), " ")
        if (name.length !in 1..40) return false
        synchronized(lock) {
            val names = categories()
            if (!names.contains(from) || names.any { it.equals(name, true) && it != from }) return false
            update("categories") {
                it.put("names", JSONArray(names.map { category ->
                    if (category == from) name else category
                }))
            }
            val keys = read("works").keys().asSequence().toList()
            update("works") { root ->
                keys.forEach { key ->
                    val entry = root.optJSONObject(key) ?: return@forEach
                    val labels = fromJsonArray(entry.optJSONArray("categories")).map {
                        if (it == from) name else it
                    }
                    entry.put("categories", JSONArray(labels.distinct()))
                }
            }
        }
        return true
    }

    fun deleteCategory(name: String) = synchronized(lock) {
        update("categories") {
            it.put("names", JSONArray(categories().filter { label -> label != name }))
        }
        val keys = read("works").keys().asSequence().toList()
        update("works") { root ->
            keys.forEach { key ->
                val entry = root.optJSONObject(key) ?: return@forEach
                entry.put("categories", JSONArray(fromJsonArray(entry.optJSONArray("categories"))
                    .filter { it != name }))
            }
        }
    }

    private fun fromJsonArray(array: JSONArray?): List<String> =
        if (array == null) emptyList() else (0 until array.length()).map {
            array.optString(it)
        }.filter(String::isNotBlank)

    private fun metadata(work: Work): JSONObject = read("works").optJSONObject(work.key)
        ?: JSONObject()

    fun assignments(work: Work): Set<String> = synchronized(lock) {
        fromJsonArray(metadata(work).optJSONArray("categories")).toSet()
    }

    fun saveAssignments(work: Work, names: Set<String>) {
        val valid = categories().toSet()
        update("works") { root ->
            val entry = root.optJSONObject(work.key) ?: JSONObject()
            entry.put("categories", JSONArray(names.filter { it in valid }.sorted()))
            if (!entry.has("addedAt")) entry.put("addedAt", System.currentTimeMillis())
            root.put(work.key, entry)
        }
    }

    fun ensureAdded(work: Work) = update("works") { root ->
        val entry = root.optJSONObject(work.key) ?: JSONObject()
        if (!entry.has("addedAt")) entry.put("addedAt", System.currentTimeMillis())
        root.put(work.key, entry)
    }

    fun favorite(work: Work) = synchronized(lock) {
        metadata(work).optBoolean("favorite", false)
    }
    fun toggleFavorite(work: Work) = update("works") { root ->
        val entry = root.optJSONObject(work.key) ?: JSONObject()
        entry.put("favorite", !entry.optBoolean("favorite", false))
        root.put(work.key, entry)
    }

    fun status(work: Work): ReadingShelf = synchronized(lock) {
        val raw = metadata(work).optString("shelf")
        runCatching { ReadingShelf.valueOf(raw) }.getOrDefault(ReadingShelf.PENDING)
    }
    fun setStatus(work: Work, shelf: ReadingShelf) = update("works") { root ->
        val entry = root.optJSONObject(work.key) ?: JSONObject()
        entry.put("shelf", shelf.name)
        root.put(work.key, entry)
    }

    /** Keep minimal chapter statistics; cap cache to avoid huge Preferences data. */
    fun cacheChapters(work: Work, chapters: List<Chapter>) = update("chapters") { root ->
        val items = JSONArray()
        chapters.take(500).forEach { chapter ->
            items.put(JSONObject().apply {
                put("id", chapter.id)
                put("title", chapter.title)
                put("number", chapter.number)
                put("volume", chapter.volume)
                put("publishedAt", chapter.publishedAt)
                put("language", chapter.language)
                put("sourceId", chapter.sourceId)
            })
        }
        root.put(work.key, JSONObject().apply {
            put("fetchedAt", System.currentTimeMillis())
            put("items", items)
        })
    }

    fun cachedChapters(work: Work): CachedChapters? = synchronized(lock) {
        val entry = read("chapters").optJSONObject(work.key) ?: return@synchronized null
        val arr = entry.optJSONArray("items") ?: JSONArray()
        val chapters = (0 until arr.length()).mapNotNull { i ->
            val c = arr.optJSONObject(i) ?: return@mapNotNull null
            Chapter(
                id = c.optString("id"), workKey = work.key,
                sourceId = c.optString("sourceId").ifBlank { work.sourceId },
                title = c.optString("title"), number = c.optString("number"),
                volume = c.optString("volume"), publishedAt = c.optString("publishedAt"),
                language = c.optString("language")
            )
        }
        CachedChapters(chapters, entry.optLong("fetchedAt"))
    }

    fun item(work: Work, store: YomiStore, downloadIds: Set<String>): LibraryItem {
        val meta = synchronized(lock) { metadata(work) }
        val cache = cachedChapters(work)
        val unique = cache?.chapters?.distinctBy { it.id }.orEmpty()
        val read = unique.count { store.isRead(it.id) }
        val inProgress = store.progress(work)
        val count = unique.size
        return LibraryItem(
            work = work,
            categories = assignments(work),
            favorite = meta.optBoolean("favorite"),
            shelf = runCatching { ReadingShelf.valueOf(meta.optString("shelf")) }
                .getOrDefault(ReadingShelf.PENDING),
            addedAt = meta.optLong("addedAt"),
            lastReadAt = inProgress?.updatedAt ?: 0L,
            chapters = count,
            unread = if (count == 0) 0 else (count - read).coerceAtLeast(0),
            downloaded = downloadIds.count { id -> unique.any { it.id == id } } +
                if (work.sourceId == "local" && unique.isEmpty() &&
                    store.downloads().any { it.work.key == work.key }) 1 else 0,
            latestPublishedAt = unique.maxOfOrNull { it.publishedAt }.orEmpty(),
            fetchedAt = cache?.fetchedAt ?: 0L
        )
    }

    fun appearance(): LibraryAppearance = synchronized(lock) {
        val obj = read("appearance")
        fun <T : Enum<T>> enumValue(raw: String, values: Array<T>, default: T): T =
            values.firstOrNull { it.name == raw } ?: default
        LibraryAppearance(
            view = enumValue(obj.optString("view"), LibraryView.entries.toTypedArray(), LibraryView.COMPACT),
            columns = obj.optInt("columns", 3).coerceIn(2, 6),
            badgeDownloads = obj.optBoolean("badgeDownloads", true),
            badgeUnread = obj.optBoolean("badgeUnread", true),
            badgeLocal = obj.optBoolean("badgeLocal", true),
            badgeLanguage = obj.optBoolean("badgeLanguage", false),
            showContinue = obj.optBoolean("showContinue", true),
            showCategoryTabs = obj.optBoolean("showCategoryTabs", true),
            showCounts = obj.optBoolean("showCounts", true),
            group = enumValue(obj.optString("group"), LibraryGroup.entries.toTypedArray(), LibraryGroup.NONE),
            sort = enumValue(obj.optString("sort"), LibrarySort.entries.toTypedArray(), LibrarySort.ALPHABETICAL),
            descending = obj.optBoolean("descending", false),
            onlyDownloaded = obj.optBoolean("onlyDownloaded", false),
            onlyUnread = obj.optBoolean("onlyUnread", false),
            onlyStarted = obj.optBoolean("onlyStarted", false),
            onlyFavorite = obj.optBoolean("onlyFavorite", false),
            onlyCompleted = obj.optBoolean("onlyCompleted", false)
        )
    }

    fun setAppearance(a: LibraryAppearance) = update("appearance") { root ->
        root.put("view", a.view.name)
        root.put("columns", a.columns.coerceIn(2, 6))
        root.put("badgeDownloads", a.badgeDownloads)
        root.put("badgeUnread", a.badgeUnread)
        root.put("badgeLocal", a.badgeLocal)
        root.put("badgeLanguage", a.badgeLanguage)
        root.put("showContinue", a.showContinue)
        root.put("showCategoryTabs", a.showCategoryTabs)
        root.put("showCounts", a.showCounts)
        root.put("group", a.group.name)
        root.put("sort", a.sort.name)
        root.put("descending", a.descending)
        root.put("onlyDownloaded", a.onlyDownloaded)
        root.put("onlyUnread", a.onlyUnread)
        root.put("onlyStarted", a.onlyStarted)
        root.put("onlyFavorite", a.onlyFavorite)
        root.put("onlyCompleted", a.onlyCompleted)
    }
}

data class LibrarySection(val title: String, val items: List<LibraryItem>)

fun selectLibrary(
    input: List<LibraryItem>,
    search: String,
    appearance: LibraryAppearance,
    activeCategory: String?,
    seed: Int = 0
): List<LibrarySection> {
    val query = search.trim().lowercase()
    val selected = input.filter { item ->
        (query.isBlank() || item.work.title.contains(query, ignoreCase = true) ||
            item.work.alternativeTitles.any { it.contains(query, ignoreCase = true) }) &&
        (activeCategory == null || activeCategory in item.categories) &&
        (!appearance.onlyDownloaded || item.downloaded > 0) &&
        (!appearance.onlyUnread || item.unread > 0) &&
        (!appearance.onlyStarted || item.lastReadAt > 0L) &&
        (!appearance.onlyFavorite || item.favorite) &&
        (!appearance.onlyCompleted || item.shelf == ReadingShelf.COMPLETED)
    }
    val comparator: Comparator<LibraryItem> = when (appearance.sort) {
        LibrarySort.ALPHABETICAL -> compareBy<LibraryItem> { it.work.title.lowercase() }
        LibrarySort.CHAPTER_COUNT -> compareBy { it.chapters }
        LibrarySort.LAST_READ -> compareBy { it.lastReadAt }
        LibrarySort.REMAINING -> compareBy { it.unread }
        LibrarySort.RECENT_CHAPTERS -> compareBy { it.latestPublishedAt }
        LibrarySort.FETCHED -> compareBy { it.fetchedAt }
        LibrarySort.OLDEST -> compareBy { it.addedAt }
        LibrarySort.RANDOM -> compareBy { (it.work.key + seed).hashCode() }
    }
    val sorted = if (appearance.descending) selected.sortedWith(comparator.reversed())
        else selected.sortedWith(comparator)
    if (appearance.group == LibraryGroup.NONE) return listOf(LibrarySection("", sorted))
    fun grouping(item: LibraryItem): List<String> = when (appearance.group) {
        LibraryGroup.CATEGORY ->
            item.categories.takeIf { it.isNotEmpty() }?.sorted() ?: listOf("Sin categoría")
        LibraryGroup.SOURCE -> listOf(when (item.work.sourceId) {
            "mangadex" -> "MangaDex"
            "comick" -> "Comick"
            "mangapdf" -> "MangaPDF"
            "local" -> "Archivos locales"
            else -> app.yomi.reader.core.BundledMadaraSources.configs
                .firstOrNull { item.work.sourceId == "mdr_" + it.id }
                ?.title ?: "Fuente integrada"
        })
        LibraryGroup.STATUS -> listOf(item.shelf.label)
        LibraryGroup.NONE -> listOf("")
    }
    val grouped = linkedMapOf<String, MutableList<LibraryItem>>()
    sorted.forEach { item ->
        grouping(item).forEach { label ->
            grouped.getOrPut(label) { mutableListOf() }.add(item)
        }
    }
    return grouped.toSortedMap().map { (title, rows) -> LibrarySection(title, rows) }
}
