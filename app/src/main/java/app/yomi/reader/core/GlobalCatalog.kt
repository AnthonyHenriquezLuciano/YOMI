package app.yomi.reader.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

data class SearchResponse(
    val works: List<Work>,
    val errors: List<String> = emptyList(),
    val sourceCounts: Map<String, Int> = emptyMap(),
    val alternatives: Map<String, List<Work>> = emptyMap(),
    val hasMore: Boolean = false
)

class GlobalCatalog(private val sources: List<ContentSource>) {
    val enabledSources: List<ContentSource> get() = sources

    suspend fun search(
        query: String, filters: SearchFilters = SearchFilters()
    ): SearchResponse = supervisorScope {
        data class Result(
            val id: String, val name: String,
            val page: SourceSearchPage, val error: String = ""
        )
        val all = sources.filter { it.id in filters.sourceIds }.map { source ->
            async {
                try {
                    Result(source.id, source.name, source.searchPage(query, filters))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    Result(source.id, source.name, SourceSearchPage(emptyList()),
                        e.message.orEmpty())
                }
            }
        }.awaitAll()
        val works = all.flatMap { it.page.works }
            .filter { filters.format == null || it.format == filters.format }
            .filter { !filters.verifyChapters || it.sourceId == "local" || it.chapterCount > 0 }
        val grouped = works.groupBy {
            normalizedTitle(it.title) + "|" + it.format.name
        }
        val representatives = grouped.values.map { versions ->
            versions.firstOrNull { it.sourceId == "mangadex" } ?: versions.first()
        }
        val alternatives = representatives.associate { selected ->
            selected.key to grouped.getValue(
                normalizedTitle(selected.title) + "|" + selected.format.name
            )
        }
        SearchResponse(
            works = representatives,
            errors = all.filter { it.error.isNotBlank() }
                .map { it.name + ": " + it.error },
            sourceCounts = all.associate { it.name to it.page.works.size },
            alternatives = alternatives,
            hasMore = all.any { it.page.hasMore }
        )
    }

    fun source(id: String): ContentSource? = sources.firstOrNull { it.id == id }
}
