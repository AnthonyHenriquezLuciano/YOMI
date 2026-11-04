package app.yomi.reader.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.yomi.reader.core.DownloadedChapter
import java.io.File
import java.util.Locale

/** Real chapter-level queue state; page-level progress requires the provider downloader. */
@Composable
fun YomiDownloadsScreen(
    downloads: List<DownloadedChapter>,
    downloading: Boolean,
    paused: Boolean,
    processed: Int,
    total: Int,
    message: String,
    failedCount: Int,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onOpen: (DownloadedChapter) -> Unit,
    onRemove: (DownloadedChapter) -> Unit
) {
    val grouped = remember(downloads) {
        downloads.groupBy { it.work.key }.values
            .sortedBy { it.firstOrNull()?.work?.title?.lowercase() }
    }
    val size = remember(downloads) {
        downloads.sumOf { chapter ->
            chapter.paths.sumOf { File(it).takeIf(File::exists)?.length() ?: 0L }
        }
    }
    var removeTarget by remember { mutableStateOf<DownloadedChapter?>(null) }
    if (removeTarget != null) AlertDialog(
        onDismissRequest = { removeTarget = null },
        title = { Text("Eliminar capítulo descargado") },
        text = { Text("Se borrarán las páginas guardadas, pero la obra continuará en tu biblioteca.") },
        confirmButton = {
            TextButton(onClick = { removeTarget?.let(onRemove); removeTarget = null }) {
                Text("Eliminar")
            }
        },
        dismissButton = { TextButton(onClick = { removeTarget = null }) { Text("Cancelar") } }
    )
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item("header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Descargas", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
                Text("Tus historias, disponibles sin conexión",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item("stats") {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .70f)
            ) {
                Row(Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Stat(grouped.size.toString(), "Obras")
                    Stat(downloads.size.toString(), "Capítulos")
                    Stat(String.format(Locale.US, "%.1f MB", size / 1048576.0), "Espacio")
                }
            }
        }
        if (downloading || failedCount > 0) item("queue") {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Cola de descargas", fontWeight = FontWeight.SemiBold)
                    if (downloading) {
                        Text(
                            if (paused) "En pausa · se detendrá al finalizar la página en curso"
                            else message.ifBlank { "Descargando capítulos…" },
                            style = MaterialTheme.typography.bodySmall
                        )
                        LinearProgressIndicator(
                            progress = { if (total > 0) processed.toFloat() / total else 0f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("$processed de $total capítulos procesados",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = if (paused) onResume else onPause,
                                modifier = Modifier.weight(1f)) {
                                Icon(if (paused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (paused) "Reanudar" else "Pausar")
                            }
                            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                                Text("Cancelar")
                            }
                        }
                    }
                    if (!downloading && failedCount > 0) {
                        Text("$failedCount capítulos fallaron en la última descarga.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Reintentar fallidos")
                        }
                    }
                }
            }
        }
        if (downloads.isEmpty() && !downloading) item("empty") {
            Column(
                Modifier.fillMaxWidth().padding(36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.OfflinePin, null, Modifier.size(46.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Sin lecturas descargadas", fontWeight = FontWeight.Bold)
                Text("Abre una obra y elige los capítulos que quieres conservar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        grouped.forEach { chapters ->
            val work = chapters.first().work
            item("group_" + work.key) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WorkCover(work, Modifier.width(42.dp).height(60.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(work.title, fontWeight = FontWeight.Bold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(chapters.size.toString() + " capítulos guardados",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(chapters, key = { it.chapter.id }) { chapter ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(chapter) }
                        .padding(vertical = 9.dp, horizontal = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(chapter.chapter.title, maxLines = 2,
                            style = MaterialTheme.typography.bodyMedium)
                        Text(chapter.paths.size.toString() + " páginas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { removeTarget = chapter }) {
                        Icon(Icons.Default.DeleteOutline, "Eliminar capítulo")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        item("notice") {
            Text("La pausa afecta a la cola tras completar la página en curso. " +
                "Las descargas por lotes requieren mantener YOMI abierta.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Stat(value: String, caption: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(caption, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
