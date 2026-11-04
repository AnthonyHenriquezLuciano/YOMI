package app.yomi.reader.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Public third-party developer API, not an extension APK.
 * Documentation: https://docs.mangapdf.org/guide/api/
 *
 * LANGUAGE: This provider does NOT expose a verified Spanish translation flag.
 * YOMI therefore keeps it in its own opt-in browser, not in the Spanish-only
 * global feed. No authentication, paywall bypass, or private endpoints.
 */
class MangaPdfSource : ContentSource {
    override val id: String = "mangapdf"
    override val name: String = "MangaPDF · API"
    private val base = "https://api.coffeemanga.shop"
    private val details = ConcurrentHashMap<String, JSONObject>()

    private suspend fun apiGet(path: String): JSONObject = withContext(Dispatchers.IO) {
        require(path.startsWith("/api/v1/mihon/"))
        val connection = URL(base + path).openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 20_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("X-Client", "api-consumer")
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "YOMI-Android/0.6")
        try {
            if (connection.responseCode !in 200..299) {
                error("MangaPDF: HTTP " + connection.responseCode)
            }
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally { connection.disconnect() }
    }

    private fun segment(value: String): String {
        require(value.length in 1..255 && value.all { it.isLetterOrDigit() })
        return URLEncoder.encode(value, "UTF-8")
    }

    override suspend fun search(
        query: String, filters: SearchFilters
    ): List<Work> = searchPage(query, filters).works

    override suspend fun searchPage(
        query: String, filters: SearchFilters
    ): SourceSearchPage {
        // API v1 does not support language, tag, or genre filtering.
        // Avoid implying unsupported filters are enforced.
        val pageNumber = filters.page + 1
        val path = when {
            query.isNotBlank() -> "/api/v1/mihon/search?q=" +
                URLEncoder.encode(query.take(200).trim(), "UTF-8") +
                "&page=" + pageNumber
            filters.sort == CatalogSort.UPDATED ->
                "/api/v1/mihon/latest?page=" + pageNumber
            else -> "/api/v1/mihon/popular?page=" + pageNumber
        }
        val json = apiGet(path)
        val arr = json.optJSONArray("items") ?: JSONArray()
        val works = buildList {
            for (index in 0 until arr.length()) {
                val obj = arr.optJSONObject(index) ?: continue
                val id = safeText(obj.optString("id"))
                val title = safeText(obj.optString("title"))
                if (id.isBlank() || title.isBlank()) continue
                add(Work(
                    id = id, sourceId = this@MangaPdfSource.id,
                    title = title,
                    coverUrl = safeText(obj.optString("thumbnail_url")),
                    format = WorkFormat.OTHER,
                    language = "und",
                    descriptionLanguage = "en",
                    chapterCount = -1
                ))
            }
        }
        return SourceSearchPage(works, json.optBoolean("has_next"))
    }

    private suspend fun details(id: String): JSONObject {
        details[id]?.let { return it }
        val response = apiGet("/api/v1/mihon/manga/" + segment(id))
        details[id] = response
        if (details.size > 48) details.clear()
        return response
    }

    override suspend fun chapters(work: Work): List<Chapter> {
        val response = details(work.id)
        val chapters = response.optJSONArray("chapters") ?: JSONArray()
        return buildList {
            for (index in 0 until chapters.length()) {
                val obj = chapters.optJSONObject(index) ?: continue
                val id = safeText(obj.optString("id"))
                if (id.isBlank()) continue
                val rawNumber = if (obj.isNull("number")) ""
                    else safeText(obj.optString("number"))
                val rawTitle = safeText(obj.optString("name"))
                val translated = rawTitle.replace(
                    Regex("^Chapter\\s*", RegexOption.IGNORE_CASE), "Capítulo "
                )
                add(Chapter(
                    id = id,
                    workKey = work.key,
                    sourceId = this@MangaPdfSource.id,
                    title = if (translated.isBlank()) displayChapterTitle(rawNumber, "")
                        else translated,
                    number = rawNumber,
                    language = "und",
                    scanlationGroup = safeText(obj.optString("scanlator")),
                    publishedAt = safeText(obj.optString("uploaded_at"))
                ))
            }
        }
    }

    override suspend fun pages(chapter: Chapter): List<String> {
        val response = apiGet(
            "/api/v1/mihon/chapter/" + segment(chapter.id) + "/pages"
        )
        val pages = response.optJSONArray("pages") ?: JSONArray()
        return (0 until pages.length()).mapNotNull { index ->
            val url = safeText(pages.optJSONObject(index)?.optString("image_url"))
            url.takeIf {
                it.startsWith("https://api.coffeemanga.shop/images/") ||
                    it.startsWith("https://api.mangapdf.org/images/")
            }
        }
    }
}
