package app.yomi.reader.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectedSourcesTest {
    @Test fun tenDifferentSourcesAreBundled() {
        val sources = ConnectedSources.builtIn
        assertEquals(10, sources.size)
        assertEquals(10, sources.map { it.id }.toSet().size)
        assertEquals(9, sources.count { it.kind == ConnectionKind.IN_APP_WEBSITE })
        assertEquals(1, sources.count { it.kind == ConnectionKind.NATIVE_API })
    }

    @Test fun allConnectionsUseSecureOfficialUrls() {
        assertTrue(ConnectedSources.builtIn.all { source ->
            source.homeUrl.startsWith("https://") && source.isAllowed(source.homeUrl)
        })
    }

    @Test fun browserRejectsUntrustedDomainsAndSchemes() {
        val webtoon = ConnectedSources.builtIn.first { it.id == "webtoon" }
        assertTrue(webtoon.isAllowed("https://www.webtoons.com/es/originals"))
        assertFalse(webtoon.isAllowed("http://www.webtoons.com/es/"))
        assertFalse(webtoon.isAllowed("javascript:alert(1)"))
        assertFalse(webtoon.isAllowed("https://webtoons.com.evil.example/es/"))
        assertFalse(webtoon.isAllowed("https://example.org/"))
    }

    @Test fun nativeApiHasNoFalseSpanishGuarantee() {
        val source = ConnectedSources.builtIn.first { it.id == "mangapdf" }
        assertEquals(ConnectionKind.NATIVE_API, source.kind)
        assertFalse(source.spanishCatalog)
    }
}
