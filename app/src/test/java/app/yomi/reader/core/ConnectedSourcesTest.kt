package app.yomi.reader.core

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectedSourcesTest {
    @Test fun tenNativeSpanishSourcesAreBundled() {
        val sources = BundledMadaraSources.configs
        assertEquals(10, sources.size)
        assertEquals(10, sources.map { it.id }.toSet().size)
        assertTrue(sources.all { it.root.startsWith("https://") })
        assertTrue(sources.all { MadaraNativeSource(it).id.startsWith("mdr_") })
        assertTrue(DEFAULT_NATIVE_SOURCE_IDS.containsAll(
            sources.map { "mdr_" + it.id }
        ))
    }

    @Test fun parseArchiveWithoutWebviewOrExtensionApk() {
        val adapter = MadaraNativeSource(BundledMadaraSources.configs[0])
        val html = """
            <div class='page-item-detail'>
                <div class='post-title'>
                    <h3><a href='https://www.kazokuden.com/manga/historia/'>Historia</a></h3>
                </div>
                <img data-src='https://www.kazokuden.com/uploads/cover.jpg'>
            </div>
        """.trimIndent()
        val doc = Jsoup.parse(html, "https://www.kazokuden.com/manga/")
        val works = adapter.parseWorks(doc)
        assertEquals(1, works.size)
        assertEquals("/manga/historia/", works.first().id)
        assertEquals("Historia", works.first().title)
        assertEquals("es", works.first().language)
        assertEquals("mdr_kazokuden", works.first().sourceId)
    }

    @Test fun externalMangaLinksAreRejected() {
        val adapter = MadaraNativeSource(BundledMadaraSources.configs[0])
        val doc = Jsoup.parse(
            "<div class='page-item-detail'><div class='post-title'>" +
                "<a href='https://malicious.example/manga/other'>Malicioso</a>" +
                "</div></div>", "https://www.kazokuden.com"
        )
        assertTrue(adapter.parseWorks(doc).isEmpty())
    }

    @Test fun parseChapterAndPageImagesInPlace() {
        val adapter = MadaraNativeSource(BundledMadaraSources.configs[0])
        val work = Work(id = "/manga/historia/", sourceId = adapter.id, title = "Historia")
        val chapters = adapter.parseChapters(
            Jsoup.parse("<li class='wp-manga-chapter'>" +
                "<a href='https://www.kazokuden.com/manga/historia/capitulo-1/'>" +
                "Capítulo 1</a></li>", "https://www.kazokuden.com"),
            work
        )
        assertEquals(1, chapters.size)
        assertEquals(adapter.id, chapters.first().sourceId)
        val pages = adapter.parsePages(
            Jsoup.parse("<div class='page-break'>" +
                "<img data-src='https://cdn.example.org/chapters/1.webp'></div>",
                "https://www.kazokuden.com/manga/historia/capitulo-1/")
        )
        assertEquals(listOf("https://cdn.example.org/chapters/1.webp"), pages)
    }

    @Test fun dragonTranslationHasNativeChapterParser() {
        val adapter = MadaraNativeSource(
            BundledMadaraSources.configs.first { it.id == "dragontranslationorg" }
        )
        val work = Work(
            id = "/manga/historia/",
            sourceId = adapter.id,
            title = "Historia"
        )
        val document = Jsoup.parse(
            """<script type="application/json">
                {"mangaId":"abc","items":[
                    {"name":"Capítulo 1","url":"/manga/historia/chap1/","ago":"hoy"}
                ]}
            </script>""".trimIndent(),
            "https://dragontranslation.org/manga/historia/"
        )
        val chapters = adapter.parseDragonChapters(document, work)
        assertEquals(1, chapters.size)
        assertEquals("Capítulo 1", chapters.single().title)
        assertEquals("es", chapters.single().language)
    }

    @Test fun filtersIncludeAllInstalledAdaptersByDefault() {
        assertEquals(0, SearchFilters().activeCount())
        assertFalse(DEFAULT_NATIVE_SOURCE_IDS.contains("comick"))
        assertFalse(DEFAULT_NATIVE_SOURCE_IDS.contains("mangapdf"))
    }
}
