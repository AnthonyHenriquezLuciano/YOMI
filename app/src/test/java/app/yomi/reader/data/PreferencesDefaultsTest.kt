package app.yomi.reader.data

import app.yomi.reader.core.BundledMadaraSources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferencesDefaultsTest {
    @Test fun everyNativeSourceIsIncludedByDefault() {
        val enabled = YomiUiPreferences.defaultSources
        assertTrue("mangadex" in enabled)
        assertTrue("local" in enabled)
        assertEquals(BundledMadaraSources.configs.size + 2, enabled.size)
    }

    @Test fun readingModesFollowTheFormat() {
        val prefs = YomiUiPreferences()
        assertEquals("RTL", prefs.defaultMangaMode.name)
        assertEquals("VERTICAL", prefs.defaultWebtoonMode.name)
    }

    @Test fun readingShelfProvidesAllWorkflowStages() {
        assertEquals(5, ReadingShelf.entries.size)
        assertTrue(ReadingShelf.entries.any { it.label == "Pausado" })
        assertTrue(ReadingShelf.entries.any { it.label == "Releyendo" })
    }
}
