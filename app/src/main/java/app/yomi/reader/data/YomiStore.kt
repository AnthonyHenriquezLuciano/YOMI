package app.yomi.reader.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import app.yomi.reader.core.Chapter
import app.yomi.reader.core.DownloadedChapter
import app.yomi.reader.core.ReadingMode
import app.yomi.reader.core.ReadingProgress
import app.yomi.reader.core.Work
import app.yomi.reader.core.WorkFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.zip.ZipInputStream

class YomiStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("yomi-data-v1", Context.MODE_PRIVATE)
    private val lock = Any()

    private fun readObject(key: String): JSONObject = try {
        JSONObject(prefs.getString(key, "{}") ?: "{}")
    } catch (_: Exception) { JSONObject() }

    private fun update(key: String, edit: (JSONObject) -> Unit) = synchronized(lock) {
        val root = readObject(key)
        edit(root)
        prefs.edit().putString(key, root.toString()).commit()
    }

    private fun workJson(work: Work) = JSONObject().apply {
        put("id", work.id); put("sourceId", work.sourceId)
        put("title", work.title); put("description", work.description)
        put("cover", work.coverUrl); put("format", work.format.name)
        put("language", work.language); put("alternatives", JSONArray(work.alternativeTitles))
        put("longStrip", work.longStrip); put("status", work.status)
        put("catalogUrl", work.catalogUrl); put("catalogOnly", work.catalogOnly)
    }
    private fun decodeWork(json: JSONObject): Work = Work(
        id = json.optString("id"), sourceId = json.optString("sourceId"),
        title = json.optString("title"), description = json.optString("description"),
        coverUrl = json.optString("cover"),
        format = runCatching { WorkFormat.valueOf(json.optString("format")) }.getOrDefault(WorkFormat.OTHER),
        language = json.optString("language"),
        alternativeTitles = json.optJSONArray("alternatives")?.let { a ->
            (0 until a.length()).map { a.optString(it) }
        } ?: emptyList(),
        longStrip = json.optBoolean("longStrip"),
        status = json.optString("status"),
        catalogUrl = json.optString("catalogUrl"),
        catalogOnly = json.optBoolean("catalogOnly")
    )
    private fun chapterJson(chapter: Chapter) = JSONObject().apply {
        put("id", chapter.id); put("sourceId", chapter.sourceId)
        put("workKey", chapter.workKey); put("title", chapter.title)
        put("number", chapter.number); put("volume", chapter.volume)
        put("language", chapter.language)
        put("scanlationGroup", chapter.scanlationGroup); put("publishedAt", chapter.publishedAt)
    }
    private fun decodeChapter(json: JSONObject) = Chapter(
        id = json.optString("id"), workKey = json.optString("workKey"),
        sourceId = json.optString("sourceId"), title = json.optString("title"),
        number = json.optString("number"), volume = json.optString("volume"),
        language = json.optString("language"),
        scanlationGroup = json.optString("scanlationGroup"),
        publishedAt = json.optString("publishedAt")
    )

    fun savedWorks(): List<Work> = synchronized(lock) {
        val root = readObject("library")
        root.keys().asSequence().mapNotNull { key ->
            root.optJSONObject(key)?.let(::decodeWork)
        }.sortedBy { it.title }.toList()
    }
    fun contains(work: Work): Boolean = synchronized(lock) {
        readObject("library").has(work.key)
    }
    fun save(work: Work) = update("library") { it.put(work.key, workJson(work)) }
    fun remove(work: Work) = update("library") { it.remove(work.key) }

    fun progress(work: Work): ReadingProgress? = synchronized(lock) {
        val value = readObject("progress").optJSONObject(work.key) ?: return@synchronized null
        ReadingProgress(value.optString("chapterId"), value.optInt("pageIndex"),
            value.optLong("updatedAt"))
    }
    fun setProgress(work: Work, chapter: Chapter, index: Int) {
        update("progress") {
            it.put(work.key, JSONObject().apply {
                put("chapterId", chapter.id)
                put("pageIndex", index.coerceAtLeast(0))
                put("updatedAt", System.currentTimeMillis())
            })
        }
        // Persist work metadata independently of the user's saved library.
        update("historyWorks") { it.put(work.key, workJson(work)) }
    }

    fun historyWorks(): List<Work> = synchronized(lock) {
        val root = readObject("historyWorks")
        root.keys().asSequence().mapNotNull { root.optJSONObject(it)?.let(::decodeWork) }
            .toList()
    }
    fun isRead(chapterId: String): Boolean = synchronized(lock) {
        readObject("read").optBoolean(chapterId, false)
    }
    fun markRead(chapterId: String) = update("read") { it.put(chapterId, true) }
    fun overrideMode(work: Work): ReadingMode? = synchronized(lock) {
        readObject("modes").optString(work.key).takeIf(String::isNotEmpty)?.let {
            runCatching { ReadingMode.valueOf(it) }.getOrNull()
        }
    }
    fun setMode(work: Work, mode: ReadingMode) =
        update("modes") { it.put(work.key, mode.name) }

    fun downloads(): List<DownloadedChapter> = synchronized(lock) {
        val root = readObject("downloads")
        root.keys().asSequence().mapNotNull { key ->
            val json = root.optJSONObject(key) ?: return@mapNotNull null
            val work = json.optJSONObject("work")?.let(::decodeWork) ?: return@mapNotNull null
            val chapter = json.optJSONObject("chapter")?.let(::decodeChapter) ?: return@mapNotNull null
            val arr = json.optJSONArray("paths") ?: return@mapNotNull null
            DownloadedChapter(chapter, work, (0 until arr.length()).map { arr.optString(it) })
        }.toList()
    }
    fun downloadedPages(chapterId: String): List<String>? =
        downloads().firstOrNull { it.chapter.id == chapterId }
            ?.paths?.takeIf { it.isNotEmpty() && it.all { file -> File(file).exists() } }
            ?.map { Uri.fromFile(File(it)).toString() }

    private fun recordDownload(work: Work, chapter: Chapter, paths: List<String>) {
        update("downloads") {
            it.put(chapter.id, JSONObject().apply {
                put("work", workJson(work)); put("chapter", chapterJson(chapter))
                put("paths", JSONArray(paths))
            })
        }
    }
    fun removeDownload(chapterId: String) {
        val download = downloads().find { it.chapter.id == chapterId }
        download?.paths?.forEach { File(it).delete() }
        update("downloads") { it.remove(chapterId) }
    }

    suspend fun download(
        work: Work, chapter: Chapter, urls: List<String>, progress: (Int) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        require(urls.isNotEmpty()) { "No hay páginas disponibles" }
        val folder = File(context.filesDir, "chapters/" + chapter.id)
        folder.mkdirs()
        val written = mutableListOf<String>()
        try {
            for ((index, url) in urls.withIndex()) {
                val target = File(folder, index.toString().padStart(4, '0') + ".jpg")
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000; readTimeout = 30_000
                    setRequestProperty("User-Agent", "YOMI-Android/0.1")
                }
                try {
                    if (connection.responseCode !in 200..299) error("Error de descarga")
                    connection.inputStream.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                } finally { connection.disconnect() }
                written.add(target.absolutePath)
                progress(((index + 1) * 100) / urls.size)
            }
            recordDownload(work, chapter, written)
        } catch (e: Exception) {
            written.forEach { File(it).delete() }
            throw e
        }
    }

    suspend fun importCbz(uri: Uri): Work = withContext(Dispatchers.IO) {
        val displayName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME),
            null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: "Manga importado.cbz"
        val title = displayName.substringBeforeLast('.')
        val id = UUID.randomUUID().toString()
        val work = Work(
            id = id, sourceId = "local", title = title,
            description = "Archivo CBZ importado al dispositivo.",
            format = WorkFormat.MANGA, language = "local"
        )
        val chapter = Chapter(id = "local-" + id, workKey = work.key,
            sourceId = "local", title = "Archivo completo", number = "1", language = "local")
        val folder = File(context.filesDir, "archives/" + id).apply { mkdirs() }
        val paths = mutableListOf<Pair<String, String>>()
        var totalBytes = 0L
        try {
            val stream = context.contentResolver.openInputStream(uri) ?: error("No se pudo abrir CBZ")
            ZipInputStream(stream.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val original = entry.name
                    val extension = original.substringAfterLast('.', "").lowercase()
                    if (!entry.isDirectory && extension in setOf("jpg", "jpeg", "png", "webp")) {
                        require(paths.size < 600) { "El CBZ supera el límite de 600 imágenes" }
                        val file = File(folder, paths.size.toString().padStart(4, '0') + "." + extension)
                        file.outputStream().use { out ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                val read = zip.read(buffer)
                                if (read < 0) break
                                totalBytes += read
                                require(totalBytes <= 250L * 1024L * 1024L) {
                                    "CBZ demasiado grande (máximo 250 MB descomprimidos)"
                                }
                                out.write(buffer, 0, read)
                            }
                        }
                        paths.add(original to file.absolutePath)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            require(paths.isNotEmpty()) { "El CBZ no contiene imágenes compatibles" }
            recordDownload(work, chapter, paths.sortedBy { it.first }.map { it.second })
            save(work)
            work
        } catch (e: Exception) {
            folder.deleteRecursively()
            throw e
        }
    }
}
