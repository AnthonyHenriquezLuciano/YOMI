package app.yomi.reader.core

import app.yomi.reader.data.YomiStore

/** Imported CBZ volumes participate in the same discovery contract as remote sources. */
class LocalArchiveSource(private val store: YomiStore) : ContentSource {
    override val id = "local"
    override val name = "Archivos locales"
    override suspend fun search(query: String, filters: SearchFilters): List<Work> {
        if (filters.page > 0 || filters.translatedLanguage != null) return emptyList()
        val needle = normalizedTitle(query)
        return store.savedWorks().filter { work ->
            work.sourceId == id &&
                (filters.format == null || work.format == filters.format) &&
                (filters.status == null || work.status == filters.status) &&
                (query.isBlank() || normalizedTitle(work.title).contains(needle) ||
                    work.alternativeTitles.any { normalizedTitle(it).contains(needle) })
        }.sortedBy { it.title }.take(filters.pageSize)
    }
    override suspend fun chapters(work: Work): List<Chapter> =
        store.downloads().filter { it.work.key == work.key }.map { it.chapter }
    override suspend fun pages(chapter: Chapter): List<String> =
        store.downloadedPages(chapter.id).orEmpty()
}
