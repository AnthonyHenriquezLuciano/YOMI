package app.yomi.reader.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {
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
