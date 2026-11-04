package app.yomi.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import app.yomi.reader.core.*
import app.yomi.reader.data.YomiStore
import app.yomi.reader.ui.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { YomiApp() }
    }
}

private enum class Tab(val label: String) {
    HOME("Inicio"), EXPLORE("Explorar"), LIBRARY("Biblioteca"),
    DOWNLOADS("Descargas"), SETTINGS("Ajustes")
}

@Composable
fun YomiApp() {
    val context = LocalContext.current
    val store = remember { YomiStore(context.applicationContext) }
    val remote = remember { MangaDexSource() }
    val catalog = remember { GlobalCatalog(listOf(remote)) }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(Tab.HOME) }
    var dark by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<WorkFormat?>(null) }
    var searchWorks by remember { mutableStateOf<List<Work>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var selectedWork by remember { mutableStateOf<Work?>(null) }
    var selectedChapter by remember { mutableStateOf<Chapter?>(null) }
    var chapters by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var chapterLoading by remember { mutableStateOf(false) }
    var pages by remember { mutableStateOf<List<String>>(emptyList()) }
    var pageLoading by remember { mutableStateOf(false) }
    var library by remember { mutableStateOf(store.savedWorks()) }
    var downloads by remember { mutableStateOf(store.downloads()) }
    var downloading by remember { mutableStateOf(false) }
    var operationMessage by remember { mutableStateOf("") }

    fun refreshLocal() {
        library = store.savedWorks()
        downloads = store.downloads()
    }
    fun search(text: String = query, format: WorkFormat? = filter) {
        query = text
        filter = format
        scope.launch {
            searching = true; error = ""
            try {
                val result = catalog.search(text, format)
                searchWorks = result.works
                if (result.errors.isNotEmpty()) error = result.errors.joinToString("\n")
            } catch (e: Exception) {
                error = e.message ?: "No se pudo consultar el catálogo"
            } finally { searching = false }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            operationMessage = "Importando archivo CBZ..."
            try {
                val imported = store.importCbz(uri)
                refreshLocal()
                tab = Tab.LIBRARY
                operationMessage = "Importado: " + imported.title
            } catch (e: Exception) {
                operationMessage = e.message ?: "No se pudo importar el archivo"
            }
        }
    }
    LaunchedEffect(Unit) { search("") }
    LaunchedEffect(selectedWork?.key) {
        val work = selectedWork ?: return@LaunchedEffect
        chapters = emptyList()
        chapterLoading = true
        try {
            chapters = if (work.sourceId == "local") {
                store.downloads().filter { it.work.key == work.key }.map { it.chapter }
            } else {
                catalog.source(work.sourceId)?.chapters(work).orEmpty()
            }
            error = ""
        } catch (e: Exception) {
            error = e.message ?: "No se encontraron capítulos"
        } finally { chapterLoading = false }
    }
    LaunchedEffect(selectedChapter?.id) {
        val chapter = selectedChapter ?: return@LaunchedEffect
        pages = emptyList(); pageLoading = true
        try {
            pages = store.downloadedPages(chapter.id)
                ?: catalog.source(chapter.sourceId)?.pages(chapter).orEmpty()
            error = ""
        } catch (e: Exception) {
            error = e.message ?: "No se pudieron cargar las páginas"
        } finally { pageLoading = false }
    }

    val palette = if (dark) darkColorScheme(
        primary = Color.White, onPrimary = Color.Black,
        background = Color(0xFF101013), surface = Color(0xFF17171A),
        onBackground = Color.White, onSurface = Color.White
    ) else lightColorScheme(
        primary = Color(0xFF131316), onPrimary = Color.White,
        background = Color(0xFFFAFAFA), surface = Color.White,
        onBackground = Color(0xFF151515), onSurface = Color(0xFF151515)
    )
    MaterialTheme(colorScheme = palette) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val work = selectedWork
            val chapter = selectedChapter
            when {
                work != null && chapter != null -> ReaderScreen(
                    work = work, chapter = chapter, urls = pages,
                    loading = pageLoading, error = error,
                    stored = store,
                    onBack = { selectedChapter = null; refreshLocal() },
                    onModeSelected = { store.setMode(work, it) },
                    onProgress = { index -> store.setProgress(work, chapter, index) },
                    onComplete = { store.markRead(chapter.id) },
                    onNext = {
                        val index = chapters.indexOfFirst { it.id == chapter.id }
                        if (index > 0) selectedChapter = chapters[index - 1]
                    },
                    hasNext = chapters.indexOfFirst { it.id == chapter.id } > 0,
                    onDownload = {
                        if (!downloading && pages.isNotEmpty() && chapter.sourceId != "local") {
                            scope.launch {
                                downloading = true; operationMessage = "Descargando capítulo..."
                                try {
                                    store.download(work, chapter, pages)
                                    refreshLocal()
                                    operationMessage = "Capítulo descargado"
                                } catch (e: Exception) {
                                    operationMessage = e.message ?: "Error de descarga"
                                } finally { downloading = false }
                            }
                        }
                    }
                )
                work != null -> DetailScreen(
                    work = work, chapters = chapters, loading = chapterLoading,
                    error = error, isSaved = library.any { it.key == work.key },
                    isRead = { store.isRead(it) },
                    onBack = { selectedWork = null; error = "" },
                    onBookmark = {
                        if (store.contains(work)) store.remove(work) else store.save(work)
                        refreshLocal()
                    },
                    onRead = { selectedChapter = it },
                    onContinue = {
                        val previous = store.progress(work)?.chapterId
                        selectedChapter = chapters.firstOrNull { it.id == previous } ?: chapters.lastOrNull()
                    },
                    onDownload = { c ->
                        if (!downloading) {
                            scope.launch {
                                downloading = true
                                operationMessage = "Descargando " + c.title
                                try {
                                    val urls = catalog.source(c.sourceId)?.pages(c).orEmpty()
                                    store.download(work, c, urls)
                                    refreshLocal()
                                    operationMessage = "Descarga completada"
                                } catch (e: Exception) {
                                    operationMessage = e.message ?: "Error al descargar"
                                } finally { downloading = false }
                            }
                        }
                    }
                )
                else -> Scaffold(
                    bottomBar = {
                        NavigationBar {
                            Tab.entries.forEach { item ->
                                val icon = when (item) {
                                    Tab.HOME -> Icons.Default.Home
                                    Tab.EXPLORE -> Icons.Default.Search
                                    Tab.LIBRARY -> Icons.Default.Bookmarks
                                    Tab.DOWNLOADS -> Icons.Default.Download
                                    Tab.SETTINGS -> Icons.Default.Settings
                                }
                                NavigationBarItem(
                                    selected = tab == item,
                                    onClick = { tab = item; error = "" },
                                    icon = { Icon(icon, contentDescription = item.label) },
                                    label = { Text(item.label) }
                                )
                            }
                        }
                    }
                ) { inner ->
                    Column(modifier = Modifier.fillMaxSize().padding(inner)) {
                        when (tab) {
                            Tab.HOME -> HomeScreen(
                                library = library, discovered = searchWorks,
                                progress = { store.progress(it) },
                                onOpen = { selectedWork = it },
                                onExplore = { tab = Tab.EXPLORE }
                            )
                            Tab.EXPLORE -> ExploreScreen(
                                query = query, onQuery = { query = it },
                                results = searchWorks, loading = searching,
                                error = error, filter = filter,
                                onSearch = { search(query, filter) },
                                onFilter = { search(query, it) },
                                onOpen = { selectedWork = it }
                            )
                            Tab.LIBRARY -> LibraryScreen(
                                library = library,
                                progress = { store.progress(it) },
                                onOpen = { selectedWork = it },
                                onImport = {
                                    importLauncher.launch(arrayOf("application/zip",
                                        "application/x-cbz", "application/octet-stream"))
                                }
                            )
                            Tab.DOWNLOADS -> DownloadsScreen(
                                downloads = downloads,
                                onOpen = {
                                    selectedWork = it.work
                                    selectedChapter = it.chapter
                                },
                                onRemove = { store.removeDownload(it.chapter.id); refreshLocal() }
                            )
                            Tab.SETTINGS -> SettingsScreen(
                                dark = dark, onDark = { dark = it },
                                onImport = {
                                    importLauncher.launch(arrayOf("application/zip",
                                        "application/x-cbz", "application/octet-stream"))
                                },
                                libraryCount = library.size,
                                downloadCount = downloads.size
                            )
                        }
                        if (operationMessage.isNotBlank()) {
                            Snackbar(
                                modifier = Modifier.padding(horizontal = androidx.compose.ui.unit.Dp(12f)),
                                action = {
                                    TextButton(onClick = { operationMessage = "" }) { Text("Cerrar") }
                                }
                            ) { Text(operationMessage) }
                        }
                    }
                }
            }
        }
    }
}
