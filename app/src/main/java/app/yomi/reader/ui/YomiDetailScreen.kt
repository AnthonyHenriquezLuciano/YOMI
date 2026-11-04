package app.yomi.reader.ui

import androidx.compose.foundation.*
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.yomi.reader.core.Chapter
import app.yomi.reader.core.CatalogTag
import app.yomi.reader.core.Work
import coil3.compose.AsyncImage
import coil3.toBitmap
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Portada inmersiva, sinopsis plegada y selección de capítulos para descarga.
 * Los tramos se expresan como posiciones desde el capítulo más antiguo.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun YomiDetailScreen(
    work: Work,
    chapters: List<Chapter>,
    loading: Boolean,
    error: String,
    sourceChoices: List<Work>,
    onSourceChange: (Work) -> Unit,
    onTagClick: (CatalogTag) -> Unit,
    onOpenExternal: (String) -> Unit,
    isSaved: Boolean,
    isRead: (String) -> Boolean,
    onBack: () -> Unit,
    onBookmark: () -> Unit,
    onRead: (Chapter) -> Unit,
    onRefresh: () -> Unit,
    onDownloadBatch: (List<Chapter>) -> Unit,
    downloading: Boolean,
    downloadStatus: String,
    onCancelDownloads: () -> Unit
) {
    var synopsisExpanded by rememberSaveable(work.key) { mutableStateOf(false) }
    var coverAccent by remember(work.key) { mutableStateOf(Color(0xFF1A1A1E)) }
    val paletteScope = rememberCoroutineScope()
    var downloadDialog by remember(work.key) { mutableStateOf(false) }
    var downloadMode by remember(work.key) { mutableStateOf("full") }
    var first by remember(work.key) { mutableStateOf("1") }
    var last by remember(work.key) { mutableStateOf("10") }
    val downloadable = remember(chapters) {
        app.yomi.reader.core.chaptersForBulkDownload(chapters)
    }
    val start = first.toIntOrNull()
    val finish = last.toIntOrNull()
    val validRange = start != null && finish != null &&
        start >= 1 && finish >= start && finish <= downloadable.size
    val statusLabel = when (work.status) {
        "ongoing" -> "En publicación"
        "completed" -> "Finalizado"
        "hiatus" -> "En pausa"
        "cancelled" -> "Cancelado"
        else -> "Disponible"
    }
    val sourceLabel = when (work.sourceId) {
        "mangadex" -> "MangaDex"
        "comick" -> "Comick · Catálogo"
        "mangapdf" -> "MangaPDF · API"
        "local" -> "Archivo local"
        else -> app.yomi.reader.core.BundledMadaraSources.configs
            .firstOrNull { work.sourceId == "mdr_" + it.id }?.title ?: "Fuente nativa"
    }
    if (downloadDialog) {
        AlertDialog(
            onDismissRequest = { downloadDialog = false },
            icon = { Icon(Icons.Default.Download, null) },
            title = { Text("Descargar capítulos") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Selecciona cómo guardar la obra para leer sin conexión.")
                    Row(
                        Modifier.fillMaxWidth().clickable { downloadMode = "full" },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(downloadMode == "full", onClick = { downloadMode = "full" })
                        Column {
                            Text("Completo", fontWeight = FontWeight.SemiBold)
                            Text(downloadable.size.toString() + " capítulos cargados",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable { downloadMode = "range" },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(downloadMode == "range", onClick = { downloadMode = "range" })
                        Text("Por tramo", fontWeight = FontWeight.SemiBold)
                    }
                    if (downloadMode == "range") {
                        Text("Posiciones de la lista, del más antiguo al más reciente.",
                            style = MaterialTheme.typography.labelSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = first,
                                onValueChange = { first = it.filter(Char::isDigit).take(5) },
                                label = { Text("Desde") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = last,
                                onValueChange = { last = it.filter(Char::isDigit).take(5) },
                                label = { Text("Hasta") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (!validRange) Text(
                            "Indica un tramo entre 1 y " + downloadable.size,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall)
                    }
                    Text("Se descargan los capítulos cargados hasta ahora. " +
                        "Mantén YOMI abierta durante el proceso.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val selected = if (downloadMode == "full") downloadable
                            else downloadable.subList(start!! - 1, finish!!)
                        downloadDialog = false
                        onDownloadBatch(selected)
                    },
                    enabled = downloadable.isNotEmpty() && !downloading &&
                        (downloadMode == "full" || validRange)
                ) { Text("Descargar") }
            },
            dismissButton = {
                TextButton(onClick = { downloadDialog = false }) { Text("Cancelar") }
            }
        )
    }
    PullToRefreshBox(
        isRefreshing = loading,
        onRefresh = { if (!loading) onRefresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item(key = "cover") {
                Box(Modifier.fillMaxWidth().height(335.dp).clipToBounds()
                    .background(MaterialTheme.colorScheme.surfaceVariant)) {
                    if (work.coverUrl.isNotBlank()) AsyncImage(
                        model = work.coverUrl, contentDescription = null,
                        modifier = Modifier.fillMaxSize().blur(26.dp),
                        contentScale = ContentScale.Crop,
                        onSuccess = { state ->
                            paletteScope.launch {
                                val tone = runCatching {
                                    withContext(Dispatchers.Default) {
                                        Palette.from(state.result.image.toBitmap(128, 128))
                                            .maximumColorCount(8).generate()
                                            .getDarkMutedColor(0xFF1A1A1E.toInt())
                                    }
                                }.getOrDefault(0xFF1A1A1E.toInt())
                                coverAccent = Color(tone)
                            }
                        }
                    )
                    Box(Modifier.matchParentSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.30f),
                                coverAccent.copy(alpha = 0.75f),
                                Color.Black.copy(alpha = 0.94f))
                        )
                    ))
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                            .background(Color.Black.copy(alpha = 0.40f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                    Text(
                        "YOMI", color = Color.White, fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.TopEnd).padding(22.dp)
                    )
                    Row(
                        modifier = Modifier.align(Alignment.BottomStart)
                            .fillMaxWidth().padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        WorkCover(
                            work, Modifier.width(106.dp).height(151.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.65f),
                                    RoundedCornerShape(14.dp))
                        )
                        Column(Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(
                                work.title, color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold, maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(work.format.label + " · " + statusLabel,
                                color = Color.White.copy(alpha = 0.90f),
                                style = MaterialTheme.typography.bodySmall)
                            Text(sourceLabel, color = Color.White.copy(alpha = 0.80f),
                                style = MaterialTheme.typography.labelMedium)
                            Text(when (work.sourceId) {
                                    "local" -> "Archivo importado"
                                    "mangapdf" -> "Idioma por verificar"
                                    else -> "Traducción en español"
                                },
                                color = Color.White.copy(alpha = 0.90f),
                                style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            item(key = "controls") {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Column(Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onBookmark,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(if (isSaved) Icons.Default.Bookmark
                                else Icons.Default.BookmarkAdd, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isSaved) "Añadido a la biblioteca" else "Añadir a la biblioteca",
                                maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { downloadDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !work.catalogOnly && work.sourceId != "local" &&
                                downloadable.isNotEmpty() && !downloading
                        ) {
                            Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Descargar")
                        }
                    }
                    if (work.catalogOnly && work.catalogUrl.startsWith("https://")) {
                        OutlinedButton(
                            onClick = { onOpenExternal(work.catalogUrl) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInNew, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Abrir ficha del proveedor")
                        }
                    }
                    if (downloading) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(downloadStatus.ifBlank { "Descargando..." },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f))
                            TextButton(onClick = onCancelDownloads) { Text("Cancelar") }
                        }
                    }
                    if (sourceChoices.size > 1) {
                        Text("Ediciones disponibles", fontWeight = FontWeight.SemiBold)
                        Row(Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            sourceChoices.forEach { option ->
                                FilterChip(
                                    selected = option.key == work.key,
                                    onClick = { onSourceChange(option) },
                                    label = {
                                        Text(when (option.sourceId) {
                                            "mangadex" -> "MangaDex"
                                            "comick" -> "Comick · Catálogo"
                                            else -> "Archivo local"
                                        })
                                    }
                                )
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ) {
                        Column(Modifier.animateContentSize()) {
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable { synopsisExpanded = !synopsisExpanded }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sinopsis", fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f))
                                Text(if (synopsisExpanded) "Ocultar" else "Mostrar",
                                    style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.width(6.dp))
                                Icon(if (synopsisExpanded) Icons.Default.ExpandLess
                                    else Icons.Default.ExpandMore,
                                    if (synopsisExpanded) "Ocultar" else "Mostrar")
                            }
                            if (synopsisExpanded) {
                                Box(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                                    SpanishSynopsis(work)
                                }
                            }
                        }
                    }
                    if (work.tags.isNotEmpty()) {
                        Text("Etiquetas y géneros",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text(
                            if (work.sourceId == "mangadex")
                                "Toca una etiqueta para encontrar obras similares."
                            else "Etiquetas proporcionadas por la fuente.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            work.tags.distinctBy { it.id }.forEach { tag ->
                                SuggestionChip(
                                    onClick = { onTagClick(tag) },
                                    label = { Text(tag.label) }
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Lista de lectura",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold)
                            Text(downloadable.size.toString() + " capítulos disponibles",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!work.catalogOnly && work.sourceId != "local") {
                            IconButton(onClick = onRefresh, enabled = !loading) {
                                Icon(Icons.Default.Refresh, "Actualizar capítulos")
                            }
                        }
                    }
                    Text("Desliza hacia abajo para actualizar.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (error.isNotBlank()) Text(
                        error, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!loading && chapters.isEmpty()) item(key = "empty") {
                Box(
                    Modifier.fillMaxWidth().padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (work.catalogOnly) "Solo catálogo: sin lectura integrada"
                        else "Todavía no hay capítulos disponibles en español.")
                }
            }
            items(chapters, key = { it.id }) { chapter ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable(enabled = !work.catalogOnly) { onRead(chapter) }
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(9.dp).background(
                        if (isRead(chapter.id)) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(50)
                    ))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(app.yomi.reader.core.safeText(chapter.title)
                            .replace("Capítulo null", "Capítulo especial"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isRead(chapter.id)) FontWeight.Normal
                            else FontWeight.SemiBold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(app.yomi.reader.core.safeText(chapter.scanlationGroup)
                            .ifBlank {
                                if (work.sourceId == "mangapdf") "Idioma sin verificar"
                                else "Traducción en español"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isRead(chapter.id)) Icon(
                        Icons.Default.Check, "Leído",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                HorizontalDivider(Modifier.padding(horizontal = 22.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}
