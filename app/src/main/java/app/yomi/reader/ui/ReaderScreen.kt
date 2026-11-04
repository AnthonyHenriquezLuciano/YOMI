package app.yomi.reader.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.consume
import kotlin.math.abs
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.yomi.reader.core.Chapter
import app.yomi.reader.core.ReadingMode
import app.yomi.reader.core.Work
import app.yomi.reader.data.YomiStore
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect

@Composable
fun ReaderScreen(
    work: Work, chapter: Chapter, urls: List<String>,
    loading: Boolean, error: String,
    stored: YomiStore, onBack: () -> Unit,
    onModeSelected: (ReadingMode) -> Unit,
    onProgress: (Int) -> Unit, onComplete: () -> Unit,
    onNext: () -> Unit, hasNext: Boolean, onDownload: () -> Unit
) {
    var mode by remember(work.key) {
        mutableStateOf(stored.overrideMode(work) ?: work.defaultMode())
    }
    var openMenu by remember { mutableStateOf(false) }
    val lastPosition = stored.progress(work)
    var pageIndex by remember(chapter.id) {
        mutableIntStateOf(if (lastPosition?.chapterId == chapter.id) lastPosition.pageIndex else 0)
    }
    val state = androidx.compose.runtime.key(chapter.id) {
        rememberLazyListState(initialFirstVisibleItemIndex = pageIndex)
    }
    LaunchedEffect(chapter.id, urls.size, mode) {
        if (mode == ReadingMode.VERTICAL && urls.isNotEmpty()) {
            snapshotFlow { state.firstVisibleItemIndex }
                .distinctUntilChanged()
                .collect { index ->
                    pageIndex = index
                    onProgress(index)
                    if (index >= urls.lastIndex) onComplete()
                }
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
            Column(Modifier.weight(1f)) {
                Text(work.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(chapter.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onDownload, enabled = urls.isNotEmpty() && chapter.sourceId != "local") {
                Icon(Icons.Default.Download, "Descargar capítulo")
            }
            Box {
                IconButton(onClick = { openMenu = true }) { Icon(Icons.Default.Tune, "Modo de lectura") }
                DropdownMenu(expanded = openMenu, onDismissRequest = { openMenu = false }) {
                    ReadingMode.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            leadingIcon = {
                                if (mode == option) Icon(Icons.Default.Check, null)
                            },
                            onClick = {
                                mode = option
                                onModeSelected(option)
                                openMenu = false
                            }
                        )
                    }
                }
            }
        }
        if (urls.isNotEmpty()) {
            val fraction = (pageIndex + 1).toFloat() / urls.size
            LinearProgressIndicator(progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth())
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                loading -> CircularProgressIndicator()
                error.isNotBlank() && urls.isEmpty() ->
                    Text(error, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(24.dp))
                urls.isEmpty() -> Text("No hay imágenes disponibles para este capítulo.")
                mode == ReadingMode.VERTICAL -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(), state = state,
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        itemsIndexed(urls) { _, page ->
                            AsyncImage(
                                model = page, contentDescription = "Página",
                                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 220.dp),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                        item {
                            Column(Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Fin del capítulo", fontWeight = FontWeight.Bold)
                                if (hasNext) Button(onClick = onNext) { Text("Siguiente capítulo") }
                            }
                        }
                    }
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = urls[pageIndex.coerceIn(0, urls.lastIndex)],
                            contentDescription = "Página " + (pageIndex + 1),
                            modifier = Modifier.fillMaxWidth().weight(1f)
                                .pointerInput(mode, urls.size, pageIndex) {
                                    var dragged = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = { dragged = 0f },
                                        onHorizontalDrag = { change, drag ->
                                            dragged += drag
                                            change.consume()
                                        },
                                        onDragEnd = {
                                            val offset = when {
                                                abs(dragged) < 70f -> 0
                                                mode == ReadingMode.RTL && dragged > 0f -> 1
                                                mode == ReadingMode.RTL -> -1
                                                dragged < 0f -> 1
                                                else -> -1
                                            }
                                            val target = pageIndex + offset
                                            if (offset != 0 && target in urls.indices) {
                                                pageIndex = target
                                                onProgress(target)
                                                if (target == urls.lastIndex) onComplete()
                                            }
                                        }
                                    )
                                },
                            contentScale = ContentScale.Fit
                        )
                        Row(Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            val previous = {
                                val target = if (mode == ReadingMode.RTL) pageIndex + 1 else pageIndex - 1
                                if (target in urls.indices) {
                                    pageIndex = target; onProgress(target)
                                    if (target == urls.lastIndex) onComplete()
                                }
                            }
                            val next = {
                                val target = if (mode == ReadingMode.RTL) pageIndex - 1 else pageIndex + 1
                                if (target in urls.indices) {
                                    pageIndex = target; onProgress(target)
                                    if (target == urls.lastIndex) onComplete()
                                }
                            }
                            IconButton(onClick = previous) { Icon(Icons.Default.ChevronLeft, "Página izquierda") }
                            Text((pageIndex + 1).toString() + " / " + urls.size,
                                style = MaterialTheme.typography.labelMedium)
                            IconButton(onClick = next) { Icon(Icons.Default.ChevronRight, "Página derecha") }
                        }
                        if (pageIndex == urls.lastIndex && hasNext) {
                            TextButton(onClick = onNext, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                Text("Siguiente capítulo")
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Fuente: " + if (chapter.sourceId == "mangadex") "MangaDex" else "Archivo local",
                    style = MaterialTheme.typography.labelSmall)
                if (chapter.scanlationGroup.isNotBlank()) Text("Traducción: " + chapter.scanlationGroup,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(mode.label.substringBefore(" ·"), style = MaterialTheme.typography.labelSmall)
        }
    }
}
