package app.yomi.reader.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local and explicit SAF JSON backup. No online sync, cloud services, or file
 * permissions. Does not archive downloaded chapter images or CBZ binary files.
 */
class YomiBackup(private val context: Context) {
    private val sources = mapOf(
        "library" to ("yomi-data-v1" to setOf("library", "progress", "read", "modes", "historyWorks")),
        "metadata" to ("yomi-library-meta-v1" to setOf("categories", "works", "chapters", "appearance")),
        "preferences" to ("yomi-ui-settings-v1" to emptySet())
    )

    suspend fun exportTo(uri: Uri): Int = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("schema", 1)
            put("app", "YOMI")
            put("exportedAt", System.currentTimeMillis())
        }
        sources.forEach { (name, location) ->
            val (prefsName, keys) = location
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val section = JSONObject()
            val all = prefs.all
            (if (keys.isEmpty()) all.keys else keys).forEach { key ->
                when (val value = all[key]) {
                    is String -> {
                        val parsed = runCatching { JSONObject(value) }.getOrNull()
                        section.put(key, parsed ?: value)
                    }
                    is Boolean -> section.put(key, value)
                    is Int -> section.put(key, value)
                    is Long -> section.put(key, value)
                    is Float -> section.put(key, value.toDouble())
                    is Set<*> -> section.put(key, JSONArray(value.filterIsInstance<String>()))
                }
            }
            json.put(name, section)
        }
        val bytes = json.toString(2).toByteArray(Charsets.UTF_8)
        require(bytes.size <= 6_000_000) { "La copia excede 6 MB; no se exportan imágenes." }
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) }
            ?: error("No se pudo abrir el destino seleccionado")
        bytes.size
    }

    suspend fun importFrom(uri: Uri): Int = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)
            ?: error("No se pudo abrir el respaldo")
        val data = input.use {
            val bytes = it.readNBytes(6_000_001)
            require(bytes.size <= 6_000_000) { "Archivo mayor de 6 MB" }
            bytes.toString(Charsets.UTF_8)
        }
        val root = JSONObject(data)
        require(root.optString("app") == "YOMI" && root.optInt("schema") == 1) {
            "Este archivo no es un respaldo compatible de YOMI"
        }
        var count = 0
        sources.forEach { (sectionName, location) ->
            val (prefsName, keys) = location
            val section = root.optJSONObject(sectionName) ?: return@forEach
            val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            val accepted = if (keys.isEmpty()) section.keys().asSequence().toSet()
                else keys
            for (key in accepted) {
                if (!section.has(key) || section.isNull(key)) continue
                if (sectionName == "preferences") {
                    val value = section.get(key)
                    when (value) {
                        is Boolean -> editor.putBoolean(key, value)
                        is Int -> editor.putInt(key, value)
                        is Double -> editor.putFloat(key, value.toFloat())
                        is String -> editor.putString(key, value)
                        is JSONArray -> {
                            editor.putStringSet(key, (0 until value.length())
                                .map { value.optString(it) }.toSet())
                        }
                    }
                } else {
                    // Metadata and the library are stored as JSON objects.
                    val value = section.optJSONObject(key) ?: continue
                    val current = runCatching {
                        JSONObject(prefs.getString(key, "{}") ?: "{}")
                    }.getOrDefault(JSONObject())
                    value.keys().forEach { id ->
                        current.put(id, value.get(id))
                    }
                    editor.putString(key, current.toString())
                }
                count++
            }
            editor.commit()
        }
        count
    }
}
