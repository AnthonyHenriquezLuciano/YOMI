package app.yomi.reader.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.yomi.reader.core.Work
import app.yomi.reader.core.safeText
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { value ->
        if (continuation.isActive) continuation.resume(value)
    }
    addOnFailureListener { error ->
        if (continuation.isActive) continuation.resumeWithException(error)
    }
    addOnCanceledListener { continuation.cancel() }
}

class SpanishSynopsisTranslator(private val context: Context) {
    private val cache = context.getSharedPreferences("yomi-spanish-synopsis", Context.MODE_PRIVATE)
    private fun key(work: Work) = work.key + "|" + work.description.hashCode()
    fun cached(work: Work): String = cache.getString(key(work), "").orEmpty()

    suspend fun translate(work: Work): String {
        val original = safeText(work.description)
        if (original.isBlank()) return ""
        val lang = work.descriptionLanguage.substringBefore('-').lowercase()
        if (lang == "es") return original
        cached(work).takeIf(String::isNotBlank)?.let { return it }
        val sourceLanguage = TranslateLanguage.fromLanguageTag(lang)
            ?: throw IllegalArgumentException("Idioma de origen no compatible")
        val translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(TranslateLanguage.SPANISH)
                .build()
        )
        return try {
            translator.downloadModelIfNeeded(
                DownloadConditions.Builder().requireWifi().build()
            ).awaitResult()
            val translation = translator.translate(original).awaitResult()
            cache.edit().putString(key(work), translation).apply()
            translation
        } finally {
            translator.close()
        }
    }
}

@Composable
fun SpanishSynopsis(work: Work) {
    val context = LocalContext.current
    val translator = remember { SpanishSynopsisTranslator(context.applicationContext) }
    val original = safeText(work.description)
    val spanish = work.descriptionLanguage.lowercase().startsWith("es")
    var translated by remember(work.key, original, work.descriptionLanguage) {
        mutableStateOf(if (spanish) original else translator.cached(work))
    }
    var loading by remember(work.key) { mutableStateOf(false) }
    var error by remember(work.key) { mutableStateOf("") }
    var attempt by remember(work.key) { mutableIntStateOf(0) }
    LaunchedEffect(work.key, original, attempt) {
        if (!spanish && original.isNotBlank() && translated.isBlank()) {
            loading = true
            error = ""
            try {
                translated = translator.translate(work)
            } catch (e: Exception) {
                error = "No se pudo traducir automáticamente. " +
                    "Conéctate a Wi-Fi para descargar el modelo de idioma."
            } finally {
                loading = false
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        when {
            original.isBlank() -> Text("Sinopsis en español no disponible.",
                style = MaterialTheme.typography.bodyMedium)
            translated.isNotBlank() -> Text(translated,
                style = MaterialTheme.typography.bodyMedium)
            loading -> Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Preparando sinopsis en español...",
                    style = MaterialTheme.typography.bodySmall)
            }
            else -> {
                Text(error.ifBlank { "Sinopsis en español no disponible." },
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { attempt++ }) {
                    Icon(Icons.Default.Translate, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reintentar traducción")
                }
            }
        }
        if (!spanish && translated.isNotBlank()) {
            Text(
                "Traducción automática al español · Puede contener errores.",
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
