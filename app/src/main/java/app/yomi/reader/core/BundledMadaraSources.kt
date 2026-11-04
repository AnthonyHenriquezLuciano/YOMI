package app.yomi.reader.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Native Kotlin implementation of the shared Madara source protocol.
 * Source configurations and selectors follow the Apache-2.0 project
 * keiyoushi/extensions-source. There are NO extension APKs, WebViews,
 * remote executable classes or dynamically downloaded code.
 *
 * Each connection works only when its site remains compatible with Madara.
 * An error or unavailable source must not be represented as a working result.
 */
enum class MadaraBrowse { AJAX, HTML }
enum class MadaraChapterMode { AJAX, PAGE }

data class MadaraConfig(
    val id: String,
    val title: String,
    val root: String,
    val section: String = "manga",
    val browse: MadaraBrowse = MadaraBrowse.AJAX,
    val chapterMode: MadaraChapterMode = MadaraChapterMode.AJAX,
    val archiveSelector: String = "div.page-item-detail, .manga__item, .c-tabs-item__content",
    val archiveUrlSelector: String = ".post-title a",
    val detailsTitle: String = "div.post-title h3, div.post-title h1, #manga-title > h1",
    val detailsDescription: String = "div.description-summary div.summary__content, div.summary_content div.post-content_item > h5 + div, div.summary_content div.manga-excerpt",
    val chapterSelector: String = "li.wp-manga-chapter",
    val chapterLink: String = "a",
    val pageSelector: String = "div.page-break img, li.blocks-gallery-item img, .reading-content img",
    val mirrors: Set<String> = emptySet(),
    val mixedContent: Boolean = false
) {
    val host: String = URI(root).host.lowercase()
    init {
        require(URI(root).scheme == "https")
        require(host.isNotBlank())
        require(id.matches(Regex("[a-z0-9_-]+")))
    }
}

/** Ten preconfigured Spanish extensions, bundled at install time. */
object BundledMadaraSources {
    val configs = listOf(
        MadaraConfig("kazokuden", "KazokuDen", "https://www.kazokuden.com",
            browse = MadaraBrowse.HTML),
        MadaraConfig("inventariooculto", "Inventario Oculto", "https://inventariooculto.com"),
        MadaraConfig("hadesnofansub", "Hades no Fansub", "https://lectorhades.latamtoon.com",
            section = "tmo", mixedContent = true),
        MadaraConfig("manhwalatino", "Manhwa Latino", "https://manhwa-latino.com",
            browse = MadaraBrowse.HTML,
            chapterMode = MadaraChapterMode.PAGE,
            chapterSelector = "div.mini-letters > a, li.wp-manga-chapter",
            chapterLink = "a",
            pageSelector = "div.page-break img.wp-manga-chapter-img, .reading-content img",
            mirrors = setOf("manhwa-es.com"), mixedContent = true),
        MadaraConfig("monopolyscan", "Monopoly Scan", "https://monopolymanhua.com"),
        MadaraConfig("taurusfansub", "Taurus Fansub", "https://lectortaurus.com",
            detailsTitle = "h1.post-title",
            detailsDescription = "div.summary__content p"),
        MadaraConfig("houseofotakus", "House of Otakus", "https://houseofotakusv2.xyz",
            mixedContent = true),
        MadaraConfig("marmota", "Marmota", "https://marmota.me", section = "comic"),
        MadaraConfig("ragnarokscanlation", "Ragnarok Scanlation",
            "https://ragnarokscanlation.org", section = "series"),
        MadaraConfig("dragontranslationorg", "Dragon Translation",
            "https://dragontranslation.org",
            browse = MadaraBrowse.HTML, chapterMode = MadaraChapterMode.PAGE,
            archiveSelector = "a:has(img.wp-post-image)",
            archiveUrlSelector = "a",
            detailsTitle = ".type-wp-manga h1",
            detailsDescription = "div:has(> p):has(+ button)",
            pageSelector = "img.wp-manga-chapter-img", mixedContent = true)
    )
    init {
        require(configs.size == 10)
        require(configs.map { it.id }.distinct().size == 10)
    }
}

class MadaraNativeSource(val config: MadaraConfig) : ContentSource {
    override val id = "mdr_" + config.id
    override val name = config.title + " · Español"
    private val countCache = ConcurrentHashMap<String, Pair<Long, Int>>()
    private val checkSemaphore = Semaphore(3)
    private val allowedHosts = setOf(config.host) + config.mirrors

    private fun urlFor(path: String): URL {
        val uri = URI(config.root.trimEnd('/') + "/").resolve(path)
        require(uri.scheme.equals("https", true) && uri.host?.lowercase() in allowedHosts &&
            uri.userInfo == null && uri.port == -1) {
            "La fuente intentó abrir un dominio no autorizado"
        }
        return uri.toURL()
    }

    private suspend fun load(path: String, body: Map<String, String>? = null): Document =
        withContext(Dispatchers.IO) {
            val url = urlFor(path)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 14_000
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml")
            conn.setRequestProperty("User-Agent", "YOMI-Android/0.7 (bundled source)")
            if (body != null) {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                conn.setRequestProperty("X-Requested-With", "XMLHttpRequest")
                conn.setRequestProperty("Referer", config.root + "/")
                val bytes = body.entries.joinToString("&") {
                    URLEncoder.encode(it.key, "UTF-8") + "=" +
                        URLEncoder.encode(it.value, "UTF-8")
                }.toByteArray(Charsets.UTF_8)
                conn.outputStream.use { it.write(bytes) }
            }
            try {
                if (conn.responseCode !in 200..299) {
                    error(name + ": HTTP " + conn.responseCode +
                        ". Puede requerir inicio de sesión o bloquear conexiones directas.")
                }
                val length = conn.contentLengthLong
                require(length < 3_000_000 || length < 0) { "Respuesta demasiado grande" }
                val html = conn.inputStream.bufferedReader(Charsets.UTF_8).use {
                    it.readText().take(3_000_000)
                }
                val parsed = Jsoup.parse(html, url.toString())
                require(parsed.selectFirst("title")?.text()
                    ?.contains("Just a moment", true) != true) {
                    "El proveedor requiere verificación. No se usa navegador ni se elude."
                }
                parsed
            } finally {
                conn.disconnect()
            }
        }

    private fun image(element: Element?): String {
        if (element == null) return ""
        val raw = listOf("data-src", "data-lazy-src", "data-original",
            "data-cfsrc", "src", "srcset")
            .map { element.attr(it) }
            .firstOrNull { value ->
                value.startsWith("http") || value.startsWith("/") && !value.startsWith("//")
            }.orEmpty().substringBefore(" ")
        val candidate = if (raw.isBlank()) "" else
            runCatching { element.baseUri().let { URI(it).resolve(raw).toString() } }.getOrDefault("")
        return candidate.takeIf { it.startsWith("https://") }.orEmpty()
    }

    private fun anchor(element: Element): Element? {
        if (element.tagName() == "a" && element.hasAttr("href")) return element
        return element.selectFirst(config.archiveUrlSelector)
            ?: element.selectFirst(".post-title a, a[href*='/manga/'], a[href*='/comic/']")
    }

    internal fun parseWorks(doc: Document): List<Work> {
        return doc.select(config.archiveSelector)
            .mapNotNull { entry ->
                val link = anchor(entry) ?: return@mapNotNull null
                val relative = runCatching {
                    val href = link.attr("abs:href")
                    val url = URI(href)
                    if (url.scheme != "https" || url.host?.lowercase() !in allowedHosts) return@runCatching ""
                    url.rawPath + (url.rawQuery?.let { "?" + it } ?: "")
                }.getOrDefault("")
                if (relative.isBlank()) return@mapNotNull null
                val title = safeText(link.attr("title")).ifBlank { safeText(link.text()) }
                if (title.isBlank()) return@mapNotNull null
                Work(
                    id = relative, sourceId = id, title = title,
                    coverUrl = image(entry.selectFirst("img")),
                    language = "es", format = WorkFormat.OTHER,
                    descriptionLanguage = "es", chapterCount = -1
                )
            }.distinctBy { it.id }
    }

    private fun madaraRequest(query: String, page: Int, sort: CatalogSort): Map<String,String> {
        val params = linkedMapOf(
            "action" to "madara_load_more",
            "page" to (page - 1).toString(),
            "template" to "madara-core/content/content-archive",
            "vars[paged]" to "1",
            "vars[template]" to "archive",
            "vars[posts_per_page]" to "25",
            "vars[post_type]" to "wp-manga",
            "vars[post_status]" to "publish",
            "vars[manga_archives_item_layout]" to "big_thumbnail"
        )
        if (query.isNotBlank()) params["vars[s]"] = query
        if (sort == CatalogSort.TITLE) {
            params["vars[orderby]"] = "post_title"
            params["vars[order]"] = "ASC"
        } else {
            params["vars[orderby]"] = "meta_value_num"
            params["vars[meta_key]"] = when (sort) {
                CatalogSort.POPULAR -> "_wp_manga_views"
                else -> "_latest_update"
            }
            params["vars[order]"] = "DESC"
        }
        return params
    }

    override suspend fun search(query: String, filters: SearchFilters): List<Work> =
        searchPage(query, filters).works

    override suspend fun searchPage(query: String, filters: SearchFilters): SourceSearchPage {
        // The WordPress protocol cannot enforce MangaDex UUID tags or all demographic
        // filters. Return no matches instead of pretending those filters work.
        if (filters.includedTags.isNotEmpty() || filters.excludedTags.isNotEmpty() ||
            filters.demographic != null || filters.year != null || filters.format != null ||
            filters.status != null) return SourceSearchPage(emptyList())
        val page = filters.page + 1
        val doc = if (config.browse == MadaraBrowse.AJAX) {
            load("/wp-admin/admin-ajax.php", madaraRequest(query, page, filters.sort))
        } else {
            val path = if (query.isNotBlank()) {
                (if (page > 1) "/page/" + page + "/" else "/") +
                    "?post_type=wp-manga&s=" + URLEncoder.encode(query, "UTF-8")
            } else {
                "/" + config.section + "/" +
                    (if (page > 1) "page/" + page + "/" else "") +
                    "?m_orderby=" + if (filters.sort == CatalogSort.POPULAR) "views" else "latest"
            }
            load(path)
        }
        val found = parseWorks(doc).take(filters.pageSize.coerceAtMost(6))
        // Verify a small, bounded portion of the catalog. Never mark zero-chapter works readable.
        val verified = coroutineScope {
            found.map { work ->
                async {
                    checkSemaphore.withPermit {
                        val count = try {
                            verifiedCount(work)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) { 0 }
                        work.copy(chapterCount = count)
                    }
                }
            }.awaitAll()
        }.filter { it.chapterCount > 0 }
        return SourceSearchPage(verified,
            if (config.browse == MadaraBrowse.AJAX) found.isNotEmpty()
            else doc.selectFirst("a.nextpostslink, div.nav-previous a, a.next") != null)
    }

    private suspend fun verifiedCount(work: Work): Int {
        val now = System.currentTimeMillis()
        countCache[work.id]?.let { (saved, count) ->
            if (now - saved < 20L * 60_000) return count
        }
        val count = chapters(work).size
        countCache[work.id] = now to count
        return count
    }

    internal fun parseChapters(doc: Document, work: Work): List<Chapter> {
        val matches = doc.select(config.chapterSelector)
        return matches.mapNotNull { entry ->
            if (entry.hasClass("scheduled") || entry.hasClass("premium")) return@mapNotNull null
            val link = if (entry.tagName() == "a" && entry.hasAttr("href"))
                entry else entry.selectFirst(config.chapterLink) ?: return@mapNotNull null
            val path = runCatching {
                val url = URI(link.attr("abs:href"))
                if (url.scheme != "https" || url.host?.lowercase() !in allowedHosts) return@runCatching ""
                url.rawPath + (url.rawQuery?.let { "?" + it } ?: "")
            }.getOrDefault("")
            if (path.isBlank()) return@mapNotNull null
            val rawName = safeText(link.text())
            val name = rawName.replace(Regex("(?i)^chapter\\s*"), "Capítulo ")
                .ifBlank { "Capítulo especial" }
            Chapter(
                id = id + "|" + path,
                workKey = work.key,
                sourceId = id,
                title = name,
                language = "es"
            )
        }.distinctBy { it.id }
    }

    override suspend fun chapters(work: Work): List<Chapter> {
        require(work.sourceId == id)
        val page = load(work.id)
        val viaAjax = if (config.chapterMode == MadaraChapterMode.AJAX) {
            val path = work.id.substringBefore('?').trimEnd('/') + "/ajax/chapters/"
            try { load(path, emptyMap()) } catch (_: Exception) { page }
        } else page
        val chapters = parseChapters(viaAjax, work)
            .ifEmpty { parseChapters(page, work) }
        return chapters
    }

    internal fun parsePages(doc: Document): List<String> {
        require(doc.select("#chapter-protector-data").isEmpty()) {
            "Este capítulo usa protección no compatible. YOMI no la elude."
        }
        return doc.select(config.pageSelector).mapNotNull { element ->
            val img = if (element.tagName() == "img") element else element.selectFirst("img")
            image(img).takeIf(String::isNotBlank)
        }.distinct()
    }

    override suspend fun pages(chapter: Chapter): List<String> {
        require(chapter.sourceId == id && chapter.id.startsWith(id + "|"))
        val path = chapter.id.substringAfter('|')
        val document = load(path)
        val result = parsePages(document)
        if (result.isEmpty()) error("Sin páginas legibles: el proveedor cambió su estructura")
        return result
    }
}
