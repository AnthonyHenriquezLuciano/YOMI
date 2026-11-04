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
data class CatalogTag(
    val id: String,
    val label: String,
    val group: String = "genre"
)

data class Work(
    val id: String,
    val sourceId: String,
    val title: String,
    val description: String = "",
    val descriptionLanguage: String = "es",
    val coverUrl: String = "",
    val format: WorkFormat = WorkFormat.OTHER,
    val language: String = "",
    val alternativeTitles: List<String> = emptyList(),
    val longStrip: Boolean = false,
    val status: String = "",
    val catalogUrl: String = "",
    val catalogOnly: Boolean = false,
    val tags: List<CatalogTag> = emptyList(),
    val chapterCount: Int = -1,
    val demographic: String = "",
    val year: Int = 0
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
    val volume: String = "",
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
    val translatedLanguage: String? = "es",
    val sort: CatalogSort = CatalogSort.UPDATED,
    val page: Int = 0,
    val pageSize: Int = 20,
    val includedTags: Set<String> = emptySet(),
    val excludedTags: Set<String> = emptySet(),
    val includeTagsMode: String = "AND",
    val excludeTagsMode: String = "OR",
    val demographic: String? = null,
    val year: Int? = null,
    val allowSuggestive: Boolean = true,
    val sourceIds: Set<String> = setOf("mangadex", "local"),
    val verifyChapters: Boolean = true
) {
    init {
        require(page >= 0)
        require(pageSize in 1..100)
        require(year == null || year in 1900..2100)
        require(includeTagsMode in listOf("AND", "OR"))
        require(excludeTagsMode in listOf("AND", "OR"))
    }
    fun activeCount(): Int =
        listOfNotNull(format, status, demographic, year).size +
            includedTags.size + excludedTags.size +
            (if (sort != CatalogSort.UPDATED) 1 else 0) +
            (if (!allowSuggestive) 1 else 0) +
            (if (sourceIds != setOf("mangadex", "local")) 1 else 0)
}

data class SourceSearchPage(
    val works: List<Work>,
    val hasMore: Boolean = false
)

interface ContentSource {
    val id: String
    val name: String
    suspend fun search(query: String, filters: SearchFilters = SearchFilters()): List<Work>
    suspend fun searchPage(
        query: String, filters: SearchFilters = SearchFilters()
    ): SourceSearchPage {
        val result = search(query, filters)
        return SourceSearchPage(result, result.size >= filters.pageSize)
    }
    suspend fun chapters(work: Work): List<Chapter>
    suspend fun pages(chapter: Chapter): List<String>
}

fun chaptersForBulkDownload(chapters: List<Chapter>): List<Chapter> =
    chapters.asReversed().distinctBy {
        if (it.number.isBlank()) it.id else it.volume + "|" + it.number
    }

fun safeText(value: String?): String {
    val text = value?.trim().orEmpty()
    return if (text.equals("null", true) ||
        text.equals("undefined", true) || text == "-" || text == "N/A") "" else text
}

fun displayChapterTitle(number: String?, title: String?): String {
    val n = safeText(number)
    val t = safeText(title)
    return when {
        n.isNotBlank() && t.isNotBlank() -> "Capítulo " + n + " · " + t
        n.isNotBlank() -> "Capítulo " + n
        t.isNotBlank() -> t
        else -> "Capítulo especial"
    }
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
