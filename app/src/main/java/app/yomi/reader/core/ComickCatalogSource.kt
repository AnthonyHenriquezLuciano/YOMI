package app.yomi.reader.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Public Comick catalog adapter.
 * Searching metadata is distinct from reading pages. The public catalog API
 * is documented at https://comick.dev/docs (no OAuth for basic catalog search).
 *
 * No private API, login cookies, page scraping or undocumented reader endpoints.
 */
class ComickCatalogSource : ContentSource {
    override val id = "comick"
    override val name = "Comick · Catálogo"
    private val origin = "https://api.comick.dev"

    override suspend fun search(query: String, filters: SearchFilters): List<Work> =
        withContext(Dispatchers.IO) {
            // Anonymous catalog search is supported, but do not send empty
            // requests for discovery: MangaDex supplies the home feed.
            if (query.trim().length < 2 || filters.translatedLanguage == "es") {
                // Este catálogo no garantiza capítulos en español: no inventar disponibilidad.
                return@withContext emptyList()
            }
            val params = linkedMapOf(
                "q" to query.trim(),
                "page" to (filters.page + 1).toString(),
                "limit" to filters.pageSize.coerceAtMost(30).toString()
            )
            val url = origin + "/v1.0/search?" + params.entries.joinToString("&") {
                URLEncoder.encode(it.key, "UTF-8") + "=" +
                    URLEncoder.encode(it.value, "UTF-8")
            }
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 9_000
                readTimeout = 12_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "YOMI-Android/0.2")
            }
            try {
                if (conn.responseCode !in 200..299) {
                    throw IllegalStateException("Comick: HTTP " + conn.responseCode)
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parseSearch(body, filters)
            } finally {
                conn.disconnect()
            }
        }

    override suspend fun chapters(work: Work): List<Chapter> = emptyList()

    override suspend fun pages(chapter: Chapter): List<String> =
        error("Comick está integrado únicamente para descubrir obras. No se han habilitado imágenes de lectura.")

    companion object {
        internal fun parseSearch(body: String, filters: SearchFilters): List<Work> {
            val root = body.trim()
            val arr = if (root.startsWith("[")) JSONArray(root) else {
                val obj = JSONObject(root)
                obj.optJSONArray("data") ?: obj.optJSONArray("results") ?: JSONArray()
            }
            return buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val hid = item.optString("hid")
                    val title = item.optString("title")
                    if (hid.isBlank() || title.isBlank()) continue
                    val country = item.optString("country").lowercase()
                    val format = when (country) {
                        "jp" -> WorkFormat.MANGA
                        "kr" -> WorkFormat.MANHWA
                        "cn", "tw" -> WorkFormat.MANHUA
                        else -> WorkFormat.OTHER
                    }
                    if (filters.format != null && filters.format != format) continue
                    val rawStatus = item.optInt("status", 0)
                    val status = when(rawStatus) {
                        1 -> "ongoing"
                        2 -> "completed"
                        3 -> "cancelled"
                        4 -> "hiatus"
                        else -> ""
                    }
                    if (filters.status != null && filters.status != status) continue
                    // The anonymous catalog does not guarantee chapter languages.
                    // Avoid fabricating that a translation exists.
                    if (filters.translatedLanguage != null) continue
                    val slug = item.optString("slug")
                    val coverUrl = item.optString("cover_url").takeIf { it.startsWith("https://") }
                        ?: run {
                            val covers = item.optJSONArray("md_covers") ?: JSONArray()
                            val key = covers.optJSONObject(0)?.optString("b2key").orEmpty()
                            if (key.isBlank()) "" else "https://meo.comick.pictures/" + key
                        }
                    val description = item.optString("desc").ifBlank { item.optString("description") }
                    val alts = item.optJSONArray("md_titles") ?: JSONArray()
                    add(Work(
                        id = hid, sourceId = "comick", title = title,
                        coverUrl = coverUrl, description = description,
                        format = format, language = country, status = status,
                        alternativeTitles = (0 until alts.length()).mapNotNull {
                            alts.optJSONObject(it)?.optString("title")?.takeIf(String::isNotBlank)
                        },
                        catalogUrl = if (slug.isBlank()) "https://comick.dev" else
                            "https://comick.dev/comic/" + slug,
                        catalogOnly = true
                    ))
                }
            }
        }
    }
}
