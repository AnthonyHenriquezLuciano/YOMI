package app.yomi.reader.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MangaDexSource : ContentSource {
    override val id = "mangadex"
    override val name = "MangaDex"
    private val api = "https://api.mangadex.org"

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
    private fun parameters(vararg parts: Pair<String, String>) =
        parts.joinToString("&") { (key, value) -> enc(key) + "=" + enc(value) }

    private suspend fun getJson(url: String): JSONObject = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 18_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "YOMI-Android/0.1 (MangaDex credited in app)")
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IllegalStateException("MangaDex respondió con HTTP " + status)
            }
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun pickTitle(names: JSONObject?): String {
        if (names == null) return ""
        for (lang in listOf("es", "en", "ja-ro", "ko-ro", "zh-ro", "ja", "ko", "zh")) {
            val name = names.optString(lang)
            if (name.isNotBlank()) return name
        }
        val keys = names.keys()
        return if (keys.hasNext()) names.optString(keys.next()) else ""
    }

    override suspend fun search(query: String, options: SearchFilters): List<Work> {
        val filters = mutableListOf(
            "limit" to options.pageSize.toString(),
            "offset" to (options.page * options.pageSize).toString(),
            "includes[]" to "cover_art",
            "contentRating[]" to "safe", "contentRating[]" to "suggestive"
        )
        if (query.isNotBlank()) filters.add("title" to query.trim())
        when (options.format) {
            WorkFormat.MANGA -> filters.add("originalLanguage[]" to "ja")
            WorkFormat.MANHWA -> filters.add("originalLanguage[]" to "ko")
            WorkFormat.MANHUA -> {
                filters.add("originalLanguage[]" to "zh")
                filters.add("originalLanguage[]" to "zh-hk")
            }
            else -> Unit
        }
        options.status?.let { filters.add("status[]" to it) }
        filters.add("availableTranslatedLanguage[]" to "es")
        val order = if (query.isBlank() && options.sort == CatalogSort.RELEVANCE) {
            CatalogSort.UPDATED
        } else options.sort
        filters.add("order[" + order.apiKey + "]" to order.direction)
        val response = getJson(api + "/manga?" + parameters(*filters.toTypedArray()))
        val array = response.optJSONArray("data") ?: JSONArray()
        return buildList {
            for (i in 0 until array.length()) {
                val manga = array.optJSONObject(i) ?: continue
                val attr = manga.optJSONObject("attributes") ?: continue
                val id = manga.optString("id")
                val title = pickTitle(attr.optJSONObject("title"))
                if (id.isBlank() || title.isBlank()) continue

                val language = attr.optString("originalLanguage")
                val format = when {
                    language.startsWith("ja") -> WorkFormat.MANGA
                    language.startsWith("ko") -> WorkFormat.MANHWA
                    language.startsWith("zh") -> WorkFormat.MANHUA
                    else -> WorkFormat.OTHER
                }
                val alt = mutableListOf<String>()
                val altArray = attr.optJSONArray("altTitles") ?: JSONArray()
                for (j in 0 until altArray.length()) {
                    val t = pickTitle(altArray.optJSONObject(j))
                    if (t.isNotBlank()) alt.add(t)
                }
                var longStrip = false
                val tags = attr.optJSONArray("tags") ?: JSONArray()
                for (j in 0 until tags.length()) {
                    val tag = tags.optJSONObject(j)?.optJSONObject("attributes") ?: continue
                    if (tag.optJSONObject("name")?.optString("en")?.equals("Long Strip", true) == true) {
                        longStrip = true
                    }
                }
                var cover = ""
                val rels = manga.optJSONArray("relationships") ?: JSONArray()
                for (j in 0 until rels.length()) {
                    val rel = rels.optJSONObject(j) ?: continue
                    if (rel.optString("type") != "cover_art") continue
                    val file = rel.optJSONObject("attributes")?.optString("fileName").orEmpty()
                    if (file.isNotBlank()) {
                        cover = "https://uploads.mangadex.org/covers/" + id + "/" + file + ".256.jpg"
                    }
                }
                add(
                    Work(
                        id = id, sourceId = this@MangaDexSource.id,
                        title = title, description = pickTitle(attr.optJSONObject("description")),
                        coverUrl = cover, format = format, language = language,
                        alternativeTitles = alt, longStrip = longStrip,
                        status = attr.optString("status")
                    )
                )
            }
        }
    }

    override suspend fun chapters(work: Work): List<Chapter> {
        val result = mutableListOf<Chapter>()
        // MangaDex paginates the feed; cap the initial listing for predictable cost.
        for (offset in listOf(0, 100, 200)) {
            val url = api + "/manga/" + enc(work.id) + "/feed?" + parameters(
                "limit" to "100", "offset" to offset.toString(),
                "translatedLanguage[]" to "es",
                "order[chapter]" to "desc", "includes[]" to "scanlation_group"
            )
            val data = getJson(url).optJSONArray("data") ?: JSONArray()
            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val attr = item.optJSONObject("attributes") ?: continue
                val chapterId = item.optString("id")
                if (chapterId.isBlank() || attr.optString("externalUrl").startsWith("http")) continue
                if (attr.optString("translatedLanguage") != "es") continue
                var group = ""
                val rels = item.optJSONArray("relationships") ?: JSONArray()
                for (r in 0 until rels.length()) {
                    val relation = rels.optJSONObject(r) ?: continue
                    if (relation.optString("type") == "scanlation_group") {
                        group = relation.optJSONObject("attributes")?.optString("name").orEmpty()
                        break
                    }
                }
                val number = attr.optString("chapter")
                val chapterTitle = attr.optString("title")
                result.add(
                    Chapter(
                        id = chapterId, workKey = work.key, sourceId = id,
                        title = "Capítulo " + (number.ifBlank { "?" }) +
                            (if (chapterTitle.isBlank()) "" else " · " + chapterTitle),
                        number = number,
                        volume = attr.optString("volume"),
                        language = attr.optString("translatedLanguage"),
                        scanlationGroup = group,
                        publishedAt = attr.optString("publishAt")
                    )
                )
            }
            if (data.length() < 100) break
        }
        return result
    }

    override suspend fun pages(chapter: Chapter): List<String> {
        val data = getJson(api + "/at-home/server/" + enc(chapter.id))
        val base = data.optString("baseUrl")
        val chapterInfo = data.optJSONObject("chapter")
            ?: error("Capítulo sin imágenes")
        val hash = chapterInfo.optString("hash")
        val files = chapterInfo.optJSONArray("dataSaver") ?: JSONArray()
        if (base.isBlank() || hash.isBlank() || files.length() == 0) {
            error("No se encontraron páginas para este capítulo")
        }
        return (0 until files.length()).map { index ->
            base + "/data-saver/" + hash + "/" + files.getString(index)
        }
    }
}
