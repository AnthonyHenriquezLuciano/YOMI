package app.yomi.reader.core

import android.icu.text.Transliterator
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * MangaDex Spanish source. Chapter availability is checked against each
 * Spanish chapter feed; metadata alone is insufficient to avoid empty works.
 */
class MangaDexSource : ContentSource {
    override val id = "mangadex"
    override val name = "MangaDex · Español"
    private val api = "https://api.mangadex.org"
    private val requestMutex = Mutex()
    private val concurrentChecks = Semaphore(3)
    private var lastRequest = 0L
    private val spanishCounts = ConcurrentHashMap<String, Pair<Long, Int>>()

    private fun enc(text: String) = URLEncoder.encode(text, "UTF-8")
    private fun parameters(parts: List<Pair<String, String>>) =
        parts.joinToString("&") { (key, value) -> enc(key) + "=" + enc(value) }

    private suspend fun getJson(url: String): JSONObject {
        // Polite request pacing shared by discovery, chapters and tag catalog.
        requestMutex.withLock {
            val elapsed = SystemClock.elapsedRealtime() - lastRequest
            if (elapsed < 300L) delay(300L - elapsed)
            lastRequest = SystemClock.elapsedRealtime()
        }
        return withContext(Dispatchers.IO) {
            var result: JSONObject? = null
            for (attempt in 0..1) {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12_000
                    readTimeout = 20_000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "YOMI-Android/0.5 (MangaDex credited in app)")
                    instanceFollowRedirects = false
                }
                try {
                    val status = connection.responseCode
                    if (status == 429 && attempt == 0) {
                        // Release connection before waiting for retry.
                    } else if (status !in 200..299) {
                        error("MangaDex respondió con HTTP " + status)
                    } else {
                        result = JSONObject(connection.inputStream.bufferedReader().use {
                            it.readText()
                        })
                        break
                    }
                } finally {
                    connection.disconnect()
                }
                if (attempt == 0) delay(1200L)
            }
            result ?: error("MangaDex ha limitado temporalmente las consultas")
        }
    }

    companion object {
        private val genreTranslations = mapOf(
            "action" to "Acción", "adventure" to "Aventura",
            "boys' love" to "Amor entre chicos", "girls' love" to "Amor entre chicas",
            "comedy" to "Comedia", "crime" to "Crimen",
            "drama" to "Drama", "fantasy" to "Fantasía",
            "historical" to "Histórico", "horror" to "Terror",
            "isekai" to "Otro mundo", "magic" to "Magia",
            "martial arts" to "Artes marciales", "mecha" to "Mechas",
            "medical" to "Medicina", "mystery" to "Misterio",
            "philosophical" to "Filosófico", "psychological" to "Psicológico",
            "romance" to "Romance", "sci-fi" to "Ciencia ficción",
            "slice of life" to "Vida cotidiana", "sports" to "Deportes",
            "superhero" to "Superhéroes", "supernatural" to "Sobrenatural",
            "thriller" to "Suspenso", "tragedy" to "Tragedia",
            "school life" to "Vida escolar", "long strip" to "Tira vertical",
            "full color" to "A todo color", "4-koma" to "Cuatro viñetas",
            "web comic" to "Webcómic", "adaptation" to "Adaptación",
            "demons" to "Demonios", "monsters" to "Monstruos",
            "reincarnation" to "Reencarnación", "time travel" to "Viaje en el tiempo",
            "survival" to "Supervivencia", "zombies" to "Zombis",
            "villainess" to "Villana", "video games" to "Videojuegos",
            "virtual reality" to "Realidad virtual", "office workers" to "Oficinistas",
            "police" to "Policía", "military" to "Militar",
            "post-apocalyptic" to "Posapocalíptico", "apocalyptic" to "Apocalíptico",
            "aliens" to "Extraterrestres", "animals" to "Animales",
            "cooking" to "Cocina", "music" to "Música",
            "vampires" to "Vampiros", "ghosts" to "Fantasmas",
            "ninja" to "Ninjas", "samurai" to "Samuráis",
            "royal family" to "Familia real", "wuxia" to "Artes marciales chinas"
        )
        fun readableTitle(candidates: List<Pair<String, String>>): String {
            val clean = candidates.mapNotNull { (lang, value) ->
                safeText(value).takeIf(String::isNotBlank)?.let { lang to it }
            }
            if (clean.isEmpty()) return ""
            fun usable(value: String): Boolean =
                value.any { it.isLetter() && it.code < 0x0300 }
            val priority = listOf("es", "es-419", "en", "ja-ro", "ko-ro", "zh-ro",
                "pt-br", "fr", "id", "ja", "ko", "zh")
            for (language in priority) {
                clean.firstOrNull { it.first.lowercase(Locale.ROOT) == language &&
                    usable(it.second) }?.let { return it.second }
            }
            clean.firstOrNull { usable(it.second) }?.let { return it.second }
            // Transliterate rather than displaying a title exclusively in CJK script.
            val raw = clean.first().second
            return runCatching {
                safeText(Transliterator.getInstance(
                    "Han-Latin; Hangul-Latin; Katakana-Latin; Hiragana-Latin; Latin-ASCII"
                ).transliterate(raw))
            }.getOrDefault(raw)
        }

        fun localizedTag(en: String, es: String): String =
            safeText(es).ifBlank {
                genreTranslations[safeText(en).lowercase(Locale.ROOT)] ?: safeText(en)
            }

        fun spanishSynopsis(
            variants: List<Pair<String, String>>
        ): Pair<String, String> {
            val valid = variants.mapNotNull { (lang, text) ->
                safeText(text).takeIf(String::isNotBlank)?.let { lang to it }
            }
            return valid.firstOrNull { it.first == "es" }
                ?: valid.firstOrNull { it.first.startsWith("es-") }
                ?: valid.firstOrNull { it.first == "en" }
                ?: valid.firstOrNull { it.first == "ja" || it.first == "ko" || it.first == "zh" }
                ?: ("" to "")
        }

        private fun strings(obj: JSONObject?): List<Pair<String, String>> {
            if (obj == null) return emptyList()
            return obj.keys().asSequence().map { key -> key to obj.optString(key) }.toList()
        }
    }

    suspend fun tags(): List<CatalogTag> {
        val result = getJson(api + "/manga/tag")
        val list = result.optJSONArray("data") ?: JSONArray()
        return buildList {
            for (i in 0 until list.length()) {
                val tag = list.optJSONObject(i) ?: continue
                val attr = tag.optJSONObject("attributes") ?: continue
                val title = attr.optJSONObject("name") ?: continue
                val label = localizedTag(title.optString("en"), title.optString("es"))
                if (label.isBlank() || tag.optString("id").isBlank()) continue
                add(CatalogTag(tag.optString("id"), label, attr.optString("group")))
            }
        }.sortedWith(compareBy({ it.group }, { it.label }))
    }

    override suspend fun search(query: String, filters: SearchFilters): List<Work> =
        searchPage(query, filters).works

    override suspend fun searchPage(query: String, filters: SearchFilters): SourceSearchPage {
        val params = mutableListOf(
            "limit" to filters.pageSize.toString(),
            "offset" to (filters.page * filters.pageSize).toString(),
            "includes[]" to "cover_art",
            "availableTranslatedLanguage[]" to "es",
            "hasAvailableChapters" to "true",
            "contentRating[]" to "safe"
        )
        if (filters.allowSuggestive) params.add("contentRating[]" to "suggestive")
        if (query.isNotBlank()) params.add("title" to query.trim())
        when (filters.format) {
            WorkFormat.MANGA -> params.add("originalLanguage[]" to "ja")
            WorkFormat.MANHWA -> params.add("originalLanguage[]" to "ko")
            WorkFormat.MANHUA -> {
                params.add("originalLanguage[]" to "zh")
                params.add("originalLanguage[]" to "zh-hk")
            }
            else -> Unit
        }
        filters.status?.let { params.add("status[]" to it) }
        filters.demographic?.let { params.add("publicationDemographic[]" to it) }
        filters.year?.let { params.add("year" to it.toString()) }
        filters.includedTags.forEach { params.add("includedTags[]" to it) }
        filters.excludedTags.forEach { params.add("excludedTags[]" to it) }
        params.add("includedTagsMode" to filters.includeTagsMode)
        params.add("excludedTagsMode" to filters.excludeTagsMode)
        val sort = if (query.isBlank() && filters.sort == CatalogSort.RELEVANCE)
            CatalogSort.UPDATED else filters.sort
        params.add("order[" + sort.apiKey + "]" to sort.direction)

        val response = getJson(api + "/manga?" + parameters(params))
        val arr = response.optJSONArray("data") ?: JSONArray()
        val parsed = buildList {
            for (i in 0 until arr.length()) {
                val manga = arr.optJSONObject(i) ?: continue
                val attributes = manga.optJSONObject("attributes") ?: continue
                val id = safeText(manga.optString("id"))
                if (id.isBlank()) continue
                val candidates = mutableListOf<Pair<String, String>>()
                candidates += strings(attributes.optJSONObject("title"))
                val alternativeNames = mutableListOf<String>()
                val alt = attributes.optJSONArray("altTitles") ?: JSONArray()
                for (j in 0 until alt.length()) {
                    val choices = strings(alt.optJSONObject(j))
                    candidates += choices
                    alternativeNames += choices.map { it.second }.filter { safeText(it).isNotBlank() }
                }
                val displayName = readableTitle(candidates)
                if (displayName.isBlank()) continue
                val descriptions = strings(attributes.optJSONObject("description"))
                val synopsis = spanishSynopsis(descriptions)
                val originLanguage = safeText(attributes.optString("originalLanguage"))
                val format = when {
                    originLanguage.startsWith("ja") -> WorkFormat.MANGA
                    originLanguage.startsWith("ko") -> WorkFormat.MANHWA
                    originLanguage.startsWith("zh") -> WorkFormat.MANHUA
                    else -> WorkFormat.OTHER
                }
                val tags = mutableListOf<CatalogTag>()
                var longStrip = false
                val rawTags = attributes.optJSONArray("tags") ?: JSONArray()
                for (j in 0 until rawTags.length()) {
                    val tag = rawTags.optJSONObject(j) ?: continue
                    val attr = tag.optJSONObject("attributes") ?: continue
                    val enName = safeText(attr.optJSONObject("name")?.optString("en"))
                    val esName = safeText(attr.optJSONObject("name")?.optString("es"))
                    if (enName.equals("Long Strip", true)) longStrip = true
                    val label = localizedTag(enName, esName)
                    if (label.isNotBlank()) tags.add(CatalogTag(
                        id = tag.optString("id"), label = label,
                        group = attr.optString("group")
                    ))
                }
                var cover = ""
                val rels = manga.optJSONArray("relationships") ?: JSONArray()
                for (j in 0 until rels.length()) {
                    val rel = rels.optJSONObject(j) ?: continue
                    if (rel.optString("type") != "cover_art") continue
                    val file = safeText(rel.optJSONObject("attributes")?.optString("fileName"))
                    if (file.isNotBlank()) {
                        cover = "https://uploads.mangadex.org/covers/" +
                            id + "/" + file + ".256.jpg"
                    }
                    break
                }
                add(Work(
                    id = id, sourceId = this@MangaDexSource.id, title = displayName,
                    description = synopsis.second,
                    descriptionLanguage = synopsis.first.ifBlank { "es" },
                    coverUrl = cover, format = format, language = originLanguage,
                    alternativeTitles = alternativeNames.distinct(),
                    longStrip = longStrip,
                    status = safeText(attributes.optString("status")),
                    tags = tags, demographic = safeText(attributes.optString("publicationDemographic")),
                    year = attributes.optInt("year", 0)
                ))
            }
        }
        // Metadata can be stale or include titles with external-only chapters:
        // verify the Spanish feed rather than claiming "0 chapters" is readable.
        val checked = if (!filters.verifyChapters) parsed else coroutineScope {
            parsed.map { work ->
                async {
                    concurrentChecks.withPermit {
                        val count = try {
                            spanishChapterCount(work.id)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (e: Exception) {
                            -1 // Unknown: never display an unverified empty title.
                        }
                        work.copy(chapterCount = count)
                    }
                }
            }.awaitAll()
        }
        if (parsed.isNotEmpty() && checked.all { it.chapterCount == -1 }) {
            error("No se pudo verificar la disponibilidad de capítulos. Inténtalo de nuevo.")
        }
        return SourceSearchPage(
            checked.filter { it.chapterCount > 0 || !filters.verifyChapters },
            hasMore = response.optInt("total", arr.length()) >
                (filters.page * filters.pageSize + arr.length())
        )
    }

    private suspend fun spanishChapterCount(mangaId: String): Int {
        spanishCounts[mangaId]?.let { (checkedAt, count) ->
            if (SystemClock.elapsedRealtime() - checkedAt < 15 * 60 * 1000L) return count
        }
        val url = api + "/manga/" + enc(mangaId) + "/feed?" + parameters(listOf(
            "limit" to "1", "translatedLanguage[]" to "es", "includeExternalUrl" to "0"
        ))
        val response = getJson(url)
        val chapters = response.optJSONArray("data") ?: JSONArray()
        // The server's filtered total is the most accurate value for availability.
        val count = if (chapters.length() == 0) 0 else
            response.optInt("total", chapters.length()).coerceAtLeast(1)
        spanishCounts[mangaId] = SystemClock.elapsedRealtime() to count
        return count
    }

    override suspend fun chapters(work: Work): List<Chapter> {
        val result = mutableListOf<Chapter>()
        for (offset in listOf(0, 100, 200)) {
            val response = getJson(api + "/manga/" + enc(work.id) + "/feed?" +
                parameters(listOf(
                    "limit" to "100", "offset" to offset.toString(),
                    "translatedLanguage[]" to "es",
                    "includeExternalUrl" to "0",
                    "order[chapter]" to "desc", "includes[]" to "scanlation_group"
                )))
            val data = response.optJSONArray("data") ?: JSONArray()
            for (i in 0 until data.length()) {
                val item = data.optJSONObject(i) ?: continue
                val attr = item.optJSONObject("attributes") ?: continue
                val chapterId = safeText(item.optString("id"))
                if (chapterId.isBlank()) continue
                if (safeText(attr.optString("externalUrl")).startsWith("http")) continue
                if (safeText(attr.optString("translatedLanguage")) != "es") continue
                var group = ""
                val rels = item.optJSONArray("relationships") ?: JSONArray()
                for (r in 0 until rels.length()) {
                    val rel = rels.optJSONObject(r) ?: continue
                    if (rel.optString("type") == "scanlation_group") {
                        group = safeText(rel.optJSONObject("attributes")?.optString("name"))
                        break
                    }
                }
                val number = safeText(attr.optString("chapter"))
                val title = safeText(attr.optString("title"))
                result.add(Chapter(
                    id = chapterId, workKey = work.key, sourceId = id,
                    title = displayChapterTitle(number, title),
                    number = number, volume = safeText(attr.optString("volume")),
                    language = "es", scanlationGroup = group,
                    publishedAt = safeText(attr.optString("publishAt"))
                ))
            }
            if (data.length() < 100) break
        }
        return result
    }

    override suspend fun pages(chapter: Chapter): List<String> {
        val data = getJson(api + "/at-home/server/" + enc(chapter.id))
        val base = safeText(data.optString("baseUrl"))
        val chapterInfo = data.optJSONObject("chapter") ?: error("Capítulo sin imágenes")
        val hash = safeText(chapterInfo.optString("hash"))
        val names = chapterInfo.optJSONArray("dataSaver") ?: JSONArray()
        if (base.isBlank() || hash.isBlank() || names.length() == 0) {
            error("No se encontraron páginas para este capítulo")
        }
        return (0 until names.length()).map { idx ->
            base + "/data-saver/" + hash + "/" + names.getString(idx)
        }
    }
}
