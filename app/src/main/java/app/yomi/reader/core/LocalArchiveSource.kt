package app.yomi.reader.core

import app.yomi.reader.data.YomiStore

/** Imported CBZ volumes participate in the same discovery contract as remote sources. */
class LocalArchiveSource(private val store: YomiStore) : ContentSource {
    override val id = "local"
    override val name = "Archivos locales"
    override suspend fun search(query: String): List<Work> =
        store.savedWorks().filter { work ->
            work.sourceId == id && (query.isBlank() ||
                normalizedTitle(work.title).contains(normalizedTitle(query)))
        }
    override suspend fun chapters(work: Work): List<Chapter> =
        store.downloads().filter { it.work.key == work.key }.map { it.chapter }
    override suspend fun pages(chapter: Chapter): List<String> =
        store.downloadedPages(chapter.id).orEmpty()
}
