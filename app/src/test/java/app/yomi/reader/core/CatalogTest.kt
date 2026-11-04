package app.yomi.reader.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {
    @Test fun completeDownloadsKeepDifferentVolumes() {
        val first = Chapter("c1", "w", "md", "Capítulo 1", number = "1", volume = "1")
        val repeated = Chapter("c2", "w", "md", "Capítulo 1", number = "1", volume = "1")
        val nextVolume = Chapter("c3", "w", "md", "Capítulo 1", number = "1", volume = "2")
        val selected = chaptersForBulkDownload(listOf(first, repeated, nextVolume))
        assertEquals(2, selected.size)
        assertEquals(setOf("1", "2"), selected.map { it.volume }.toSet())
    }

    @Test fun spanishIsAlwaysTheDefaultCatalogLanguage() {
        assertEquals("es", SearchFilters().translatedLanguage)
    }

    @Test fun searchFiltersCountAndPaging() {
        assertEquals(0, SearchFilters().activeCount())
        val applied = SearchFilters(
            format = WorkFormat.MANHWA, translatedLanguage = "es",
            status = "completed", sort = CatalogSort.POPULAR
        )
        assertEquals(3, applied.activeCount())
        assertEquals("completed", applied.copy(page = 2).status)
        assertEquals(2, applied.copy(page = 2).page)
    }

    @Test fun accentsAndPunctuationNormalize() {
        assertEquals("solo leveling", normalizedTitle("SÓLO - Leveling!!"))
    }
    @Test fun exactDuplicateWorkIsShownOnce() {
        val a = Work("1", "a", "The Hero", format = WorkFormat.MANHWA, language = "en")
        val b = Work("2", "b", "The Hero", format = WorkFormat.MANHWA, language = "es")
        assertEquals("2", groupResults(listOf(a, b)).single().id)
    }
    @Test fun similarButDistinctTitlesRemainSeparate() {
        val a = Work("1", "a", "Hero", format = WorkFormat.MANGA)
        val b = Work("2", "b", "Hero II", format = WorkFormat.MANGA)
        assertEquals(2, groupResults(listOf(a, b)).size)
    }
    @Test fun formatDefaultsAreCorrect() {
        assertEquals(ReadingMode.RTL,
            Work("1", "x", "Manga", format = WorkFormat.MANGA).defaultMode())
        assertEquals(ReadingMode.VERTICAL,
            Work("2", "x", "Manhwa", format = WorkFormat.MANHWA).defaultMode())
        assertTrue(Work("3", "x", "Webtoon", longStrip = true).defaultMode() == ReadingMode.VERTICAL)
    }
}
