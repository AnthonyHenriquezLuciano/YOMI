package app.yomi.reader.data

import app.yomi.reader.core.Work
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySelectionTest {
    private fun item(
        id: String,
        title: String,
        cats: Set<String> = emptySet(),
        favorite: Boolean = false,
        shelf: ReadingShelf = ReadingShelf.PENDING,
        unread: Int = 0,
        chapters: Int = 0,
        downloads: Int = 0,
        lastRead: Long = 0L
    ) = LibraryItem(
        work = Work(id = id, sourceId = "mangadex", title = title),
        categories = cats,
        favorite = favorite,
        shelf = shelf,
        addedAt = 1000,
        lastReadAt = lastRead,
        chapters = chapters,
        unread = unread,
        downloaded = downloads,
        latestPublishedAt = "",
        fetchedAt = 0
    )
    @Test fun filtersUseRealFlags() {
        val all = listOf(
            item("1", "Alpha", favorite = true, unread = 3, downloads = 2, lastRead = 100),
            item("2", "Beta", shelf = ReadingShelf.COMPLETED),
            item("3", "Gamma", unread = 2)
        )
        val a = LibraryAppearance(onlyFavorite = true, onlyUnread = true, onlyDownloaded = true)
        assertEquals(listOf("1"), selectLibrary(all, "", a, null).flatMap { it.items }.map { it.work.id })
        assertEquals(1, selectLibrary(all, "", LibraryAppearance(onlyCompleted = true), null)
            .flatMap { it.items }.size)
        assertEquals(1, selectLibrary(all, "", LibraryAppearance(onlyStarted = true), null)
            .flatMap { it.items }.size)
    }

    @Test fun categoriesGroupWorksInMultipleSections() {
        val item = item("1", "One", cats = setOf("A", "B"))
        val grouped = selectLibrary(listOf(item), "", LibraryAppearance(group = LibraryGroup.CATEGORY), null)
        assertEquals(listOf("A", "B"), grouped.map { it.title })
        assertEquals(2, grouped.sumOf { it.items.size })
        assertEquals(1, selectLibrary(listOf(item), "", LibraryAppearance(), "A")
            .flatMap { it.items }.size)
    }

    @Test fun latestReadingSortsFirst() {
        val items = listOf(item("old", "A", lastRead = 100), item("new", "B", lastRead = 200))
        val sorted = selectLibrary(items, "",
            LibraryAppearance(sort = LibrarySort.LAST_READ, descending = true), null)
        assertEquals("new", sorted.first().items.first().work.id)
    }

    @Test fun appearanceDefaultsAreReasonable() {
        assertEquals(LibraryView.COMPACT, LibraryAppearance().view)
        assertTrue(LibraryAppearance().showCategoryTabs)
    }
}
