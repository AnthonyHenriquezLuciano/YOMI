package app.yomi.reader.core

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
    suspend fun search(query: String, filters: SearchFilters = SearchFilters()): SearchResponse = supervisorScope {
        val all = sources.map { source ->
            async {
                runCatching { source.search(query, filters) }
                    .fold(onSuccess = { Triple(source.name, it, "") },
                        onFailure = { Triple(source.name, emptyList(), it.message.orEmpty()) })
            }
        }.awaitAll()
        val works = all.flatMap { it.second }
        // Keep every source variant accessible: never discard it when unifying titles.
        val grouped = works.groupBy { normalizedTitle(it.title) + "|" + it.format.name }
        val representatives = grouped.values.map { group ->
            group.firstOrNull { it.language == "es" } ?: group.first()
        }.filter { filters.format == null || it.format == filters.format }
        val alternatives = representatives.associate { selected ->
            selected.key to grouped.getValue(normalizedTitle(selected.title) + "|" + selected.format.name)
        }
        SearchResponse(
            works = representatives,
            errors = all.filter { it.third.isNotBlank() }.map { it.first + ": " + it.third },
            sourceCounts = all.associate { it.first to it.second.size },
            alternatives = alternatives,
            hasMore = all.any { it.second.size >= filters.pageSize }
        )
    }
    fun source(id: String): ContentSource? = sources.firstOrNull { it.id == id }
}
