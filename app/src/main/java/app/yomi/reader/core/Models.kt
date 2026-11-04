package app.yomi.reader.core

import java.text.Normalizer
import java.util.Locale

enum class WorkFormat(val label: String) {
    MANGA("Manga"), MANHWA("Manhwa"), MANHUA("Manhua"), OTHER("Otro")
}
enum class ReadingMode(val label: String) {
    RTL("Páginas · derecha a izquierda"),
    LTR("Páginas · izquierda a derecha"),
    VERTICAL("Desplazamiento vertical")
}
data class Work(
    val id: String,
    val sourceId: String,
    val title: String,
    val description: String = "",
    val coverUrl: String = "",
    val format: WorkFormat = WorkFormat.OTHER,
    val language: String = "",
    val alternativeTitles: List<String> = emptyList(),
    val longStrip: Boolean = false,
    val status: String = ""
) {
    val key: String get() = sourceId + ":" + id
    fun defaultMode(): ReadingMode = when {
        longStrip || format == WorkFormat.MANHWA -> ReadingMode.VERTICAL
        format == WorkFormat.MANHUA -> ReadingMode.LTR
        format == WorkFormat.MANGA -> ReadingMode.RTL
        else -> ReadingMode.LTR
    }
}
data class Chapter(
    val id: String,
    val workKey: String,
    val sourceId: String,
    val title: String,
    val number: String = "",
    val language: String = "",
    val scanlationGroup: String = "",
    val publishedAt: String = ""
)
data class DownloadedChapter(
    val chapter: Chapter,
    val work: Work,
    val paths: List<String>
)
data class ReadingProgress(
    val chapterId: String,
    val pageIndex: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

enum class CatalogSort(val label: String, val apiKey: String, val direction: String) {
    UPDATED("Actualizados", "latestUploadedChapter", "desc"),
    POPULAR("Populares", "followedCount", "desc"),
    NEWEST("Nuevos", "createdAt", "desc"),
    TITLE("Título A-Z", "title", "asc"),
    RELEVANCE("Relevancia", "relevance", "desc")
}

data class SearchFilters(
    val format: WorkFormat? = null,
    val status: String? = null,
    val translatedLanguage: String? = null,
    val sort: CatalogSort = CatalogSort.UPDATED,
    val page: Int = 0,
    val pageSize: Int = 30
) {
    init {
        require(page >= 0)
        require(pageSize in 1..100)
    }
    fun activeCount(): Int =
        listOfNotNull(format, status, translatedLanguage).size +
            if (sort != CatalogSort.UPDATED) 1 else 0
}

interface ContentSource {
    val id: String
    val name: String
    suspend fun search(query: String, filters: SearchFilters = SearchFilters()): List<Work>
    suspend fun chapters(work: Work): List<Chapter>
    suspend fun pages(chapter: Chapter): List<String>
}

fun normalizedTitle(title: String): String {
    val clean = Normalizer.normalize(title.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    return clean.replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
}

fun groupResults(results: List<Work>): List<Work> {
    // Only match exact normalized titles. Never silently merge ambiguous chapter lists.
    return results.groupBy { normalizedTitle(it.title) + "|" + it.format.name }
        .values.map { group ->
            group.firstOrNull { it.language == "es" } ?: group.first()
        }.sortedBy { it.title.lowercase(Locale.ROOT) }
}
