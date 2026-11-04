package app.yomi.reader.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

data class ExtensionRepository(
    val name: String,
    val url: String,
    val builtIn: Boolean = false
)
data class IndexedSource(
    val name: String,
    val language: String,
    val originalUrl: String,
    val repository: String,
    val extensionName: String
)
data class RepositoryListing(
    val repository: ExtensionRepository,
    val sources: List<IndexedSource>,
    val error: String? = null
)

/**
 * Index-only Mihon/Tachiyomi extension directory.
 *
 * SECURITY: We never download, install, link, reflect or execute extension APKs.
 * A repo index is untrusted data; limit download sizes and validate input.
 * This is not a ContentSource implementation and cannot be searched for manga.
 */
class ExtensionRepositoryDirectory(private val context: Context) {
    private val prefs = context.getSharedPreferences("yomi-extension-indexes-v1", Context.MODE_PRIVATE)
    private val supportedHosts = setOf("raw.githubusercontent.com", "github.com")
    companion object {
        val defaults = listOf(
            ExtensionRepository(
                name = "Keiyoushi",
                url = "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.min.json",
                builtIn = true
            ),
            ExtensionRepository(
                name = "Nyora · fuentes SFW",
                url = "https://raw.githubusercontent.com/Nyora-Manga/nyora-mihon/main/index.min.json",
                builtIn = true
            )
        )
        const val MAX_INDEX_BYTES = 3_145_728

        fun validatedUrl(value: String): String {
            val uri = URI(value.trim())
            val host = uri.host?.lowercase() ?: error("Dominio inválido")
            require(uri.scheme.equals("https", true)) { "Solo enlaces HTTPS" }
            require(uri.userInfo == null && uri.port == -1 && uri.fragment == null) {
                "Enlace no permitido"
            }
            require(host in setOf("raw.githubusercontent.com", "github.com") ||
                host.endsWith(".github.io")) {
                "Solo índices publicados en GitHub o GitHub Pages"
            }
            require(uri.path.endsWith("/index.min.json") || uri.path.endsWith("/index.json")) {
                "El enlace debe terminar en index.min.json o index.json"
            }
            require(uri.rawQuery == null) { "No se aceptan parámetros en la URL" }
            return uri.toASCIIString()
        }

        internal fun parseIndex(
            repository: ExtensionRepository, json: String
        ): List<IndexedSource> {
            val array = JSONArray(json)
            val seen = HashSet<String>()
            val entries = mutableListOf<IndexedSource>()
            for (i in 0 until array.length()) {
                val extension = array.optJSONObject(i) ?: continue
                if (extension.optInt("nsfw", 0) != 0) continue
                val extensionName = extension.optString("name")
                val extensions = extension.optJSONArray("sources")
                if (extensions == null || extensions.length() == 0) {
                    val name = extensionName.takeIf(String::isNotBlank) ?: continue
                    val key = name.lowercase() + "|" + extension.optString("lang")
                    if (seen.add(key)) entries += IndexedSource(
                        name = name, language = extension.optString("lang"),
                        originalUrl = "", repository = repository.name,
                        extensionName = name
                    )
                    continue
                }
                for (j in 0 until extensions.length()) {
                    val source = extensions.optJSONObject(j) ?: continue
                    val name = source.optString("name").ifBlank { extensionName }
                    val lang = source.optString("lang").ifBlank { extension.optString("lang") }
                    val link = source.optString("baseUrl")
                    if (name.isBlank()) continue
                    val key = name.lowercase() + "|" + lang + "|" + link
                    if (seen.add(key)) entries += IndexedSource(
                        name = name, language = lang,
                        originalUrl = if (link.startsWith("https://")) link else "",
                        repository = repository.name,
                        extensionName = extensionName
                    )
                }
            }
            return entries.sortedWith(compareBy({ it.language }, { it.name }))
        }
    }

    fun repositories(): List<ExtensionRepository> {
        val extra = prefs.getStringSet("custom_indexes", emptySet())
            .orEmpty().mapNotNull { url ->
                runCatching { ExtensionRepository(
                    name = URI(url).host.orEmpty().removePrefix("raw."),
                    url = validatedUrl(url)
                ) }.getOrNull()
            }
        return (defaults + extra).distinctBy { it.url }
    }

    fun add(url: String) {
        val validated = validatedUrl(url)
        if (defaults.any { it.url == validated }) return
        val urls = prefs.getStringSet("custom_indexes", emptySet()).orEmpty().toMutableSet()
        urls += validated
        prefs.edit().putStringSet("custom_indexes", urls).apply()
    }

    fun remove(url: String) {
        if (defaults.any { it.url == url }) return
        val urls = prefs.getStringSet("custom_indexes", emptySet()).orEmpty().toMutableSet()
        urls -= url
        prefs.edit().putStringSet("custom_indexes", urls).apply()
    }

    suspend fun refresh(): List<RepositoryListing> = supervisorScope {
        repositories().map { repo ->
            async {
                try {
                    val text = downloadIndex(repo.url)
                    RepositoryListing(repo, parseIndex(repo, text))
                } catch (e: Exception) {
                    RepositoryListing(repo, emptyList(), e.message ?: "No disponible")
                }
            }
        }.awaitAll()
    }

    private suspend fun downloadIndex(rawUrl: String): String = withContext(Dispatchers.IO) {
        val conn = (URL(validatedUrl(rawUrl)).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 9_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "YOMI-Android/0.2")
        }
        try {
            if (conn.responseCode !in 200..299) {
                error("Repositorio HTTP " + conn.responseCode)
            }
            val output = ByteArrayOutputStream()
            conn.inputStream.use { input ->
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_INDEX_BYTES) { "Índice demasiado grande" }
                    output.write(buffer, 0, count)
                }
            }
            output.toString(Charsets.UTF_8.name())
        } finally {
            conn.disconnect()
        }
    }
}
