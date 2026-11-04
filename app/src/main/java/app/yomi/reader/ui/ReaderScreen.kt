package app.yomi.reader.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.abs

/**
 * No persistent toolbars. Tap the center to reveal/hide controls.
 * The host activity hides Android system bars while the reader is open.
 */
@Composable
fun ReaderScreen(
    work: Work,
    chapter: Chapter,
    urls: List<String>,
    loading: Boolean,
    error: String,
    stored: YomiStore,
    onBack: () -> Unit,
    onModeSelected: (ReadingMode) -> Unit,
    onProgress: (Int) -> Unit,
    onComplete: () -> Unit,
    onNext: () -> Unit,
    hasNext: Boolean,
    onDownload: () -> Unit
) {
    var mode by remember(work.key) {
        mutableStateOf(stored.overrideMode(work) ?: work.defaultMode())
    }
    var controlsVisible by remember(chapter.id) { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val progress = remember(chapter.id) { stored.progress(work) }
    var pageIndex by remember(chapter.id) {
        mutableIntStateOf(
            if (progress?.chapterId == chapter.id) progress.pageIndex else 0
        )
    }
    val scrollState = androidx.compose.runtime.key(chapter.id) {
        rememberLazyListState(initialFirstVisibleItemIndex = pageIndex.coerceAtLeast(0))
    }
    LaunchedEffect(chapter.id, mode, urls.size) {
        if (mode == ReadingMode.VERTICAL && urls.isNotEmpty()) {
            snapshotFlow { scrollState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .collect { index ->
                    val current = index.coerceIn(0, urls.lastIndex)
                    pageIndex = current
                    onProgress(current)
                    if (current >= urls.lastIndex) onComplete()
                }
        }
    }
    fun navigateBy(delta: Int) {
        val next = pageIndex + delta
        if (next in urls.indices) {
            pageIndex = next
            onProgress(next)
            if (next == urls.lastIndex) onComplete()
        } else if (next >= urls.size && hasNext) {
            onNext()
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(mode, urls.size, pageIndex) {
                detectTapGestures { position ->
                    if (mode == ReadingMode.VERTICAL || urls.isEmpty()) {
                        controlsVisible = !controlsVisible
                    } else {
                        when {
                            position.x < size.width * 0.25f ->
                                navigateBy(if (mode == ReadingMode.RTL) -1 else -1)
                            position.x > size.width * 0.75f ->
                                navigateBy(1)
                            else -> controlsVisible = !controlsVisible
                        }
                    }
                }
            }
    ) {
        when {
            loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center), color = Color.White
            )
            error.isNotBlank() && urls.isEmpty() ->
                Text(error, color = Color.White, modifier = Modifier.align(Alignment.Center)
                    .padding(28.dp))
            urls.isEmpty() ->
                Text("No se encontraron páginas para este capítulo.",
                    color = Color.White, modifier = Modifier.align(Alignment.Center).padding(26.dp))
            mode == ReadingMode.VERTICAL -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = scrollState,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(urls) { index, url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Página " + (index + 1),
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 200.dp)
                    )
                }
                item {
                    Column(
                        Modifier.fillMaxWidth().background(Color.Black)
                            .padding(vertical = 30.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Fin del capítulo", color = Color.White)
                        if (hasNext) Button(onClick = onNext) { Text("Siguiente capítulo") }
                    }
                }
            }
            else -> AsyncImage(
                model = urls[pageIndex.coerceIn(0, urls.lastIndex)],
                contentDescription = "Página " + (pageIndex + 1),
                modifier = Modifier.fillMaxSize()
                    .pointerInput(mode, urls.size, pageIndex) {
                        var dragged = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragged = 0f },
                            onHorizontalDrag = { change, offset ->
                                dragged += offset
                                change.consume()
                            },
                            onDragEnd = {
                                if (abs(dragged) >= 70f) {
                                    navigateBy(
                                        if (mode == ReadingMode.RTL) {
                                            if (dragged > 0) 1 else -1
                                        } else {
                                            if (dragged < 0) 1 else -1
                                        }
                                    )
                                }
                            }
                        )
                    },
                contentScale = ContentScale.Fit
            )
        }
        if (controlsVisible) {
            Row(
                Modifier.fillMaxWidth().align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.84f))
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text(work.title, color = Color.White, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall)
                    Text(chapter.title, color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall)
                }
                if (chapter.sourceId != "local") IconButton(onClick = onDownload) {
                    Icon(Icons.Default.Download, "Descargar capítulo", tint = Color.White)
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.Tune, "Modo de lectura", tint = Color.White)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        ReadingMode.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                leadingIcon = {
                                    if (mode == option) Icon(Icons.Default.Check, null)
                                },
                                onClick = {
                                    mode = option
                                    onModeSelected(option)
                                    menuOpen = false
                                }
                            )
                        }
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.86f))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (urls.isNotEmpty()) {
                    LinearProgressIndicator(
                        progress = { ((pageIndex + 1f) / urls.size).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        trackColor = Color.DarkGray
                    )
                }
                Row(
                    Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (urls.isEmpty()) "Sin páginas"
                        else (pageIndex + 1).toString() + " / " + urls.size,
                        color = Color.White, style = MaterialTheme.typography.labelMedium
                    )
                    if (mode != ReadingMode.VERTICAL) Row {
                        IconButton(onClick = { navigateBy(-1) }) {
                            Icon(Icons.Default.ChevronLeft, "Página anterior", tint = Color.White)
                        }
                        IconButton(onClick = { navigateBy(1) }) {
                            Icon(Icons.Default.ChevronRight, "Página siguiente", tint = Color.White)
                        }
                    }
                    if (hasNext && pageIndex >= urls.lastIndex && urls.isNotEmpty()) {
                        TextButton(onClick = onNext) {
                            Text("Siguiente capítulo", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
