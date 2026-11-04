package app.yomi.reader.core

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

data class SearchResponse(
    val works: List<Work>,
    val errors: List<String> = emptyList(),
    val sourceCounts: Map<String, Int> = emptyMap()
)
class GlobalCatalog(private val sources: List<ContentSource>) {
    val enabledSources: List<ContentSource> get() = sources
    suspend fun search(query: String, format: WorkFormat? = null): SearchResponse = supervisorScope {
        val all = sources.map { source ->
            async {
                runCatching { source.search(query) }
                    .fold(onSuccess = { Triple(source.name, it, "") },
                        onFailure = { Triple(source.name, emptyList(), it.message.orEmpty()) })
            }
        }.awaitAll()
        val works = all.flatMap { it.second }
        // Global search groups titles for display, but the original source ID is retained.
        SearchResponse(
            works = groupResults(works).filter { format == null || it.format == format },
            errors = all.filter { it.third.isNotBlank() }.map { it.first + ": " + it.third },
            sourceCounts = all.associate { it.first to it.second.size }
        )
    }
    fun source(id: String): ContentSource? = sources.firstOrNull { it.id == id }
}
