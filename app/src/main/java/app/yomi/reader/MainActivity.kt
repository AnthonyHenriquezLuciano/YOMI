package app.yomi.reader

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
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
import androidx.core.view.WindowCompat
import app.yomi.reader.core.*
import app.yomi.reader.data.YomiStore
import app.yomi.reader.ui.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Job

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
    val localSource = remember { LocalArchiveSource(store) }
    val comick = remember { ComickCatalogSource() }
    val catalog = remember { GlobalCatalog(listOf(remote, comick, localSource)) }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(Tab.HOME) }
    var dark by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var filters by remember { mutableStateOf(SearchFilters()) }
    var page by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var searchWorks by remember { mutableStateOf<List<Work>>(emptyList()) }
    var sourceVariants by remember { mutableStateOf<Map<String, List<Work>>>(emptyMap()) }
    var searching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var showRepositories by remember { mutableStateOf(false) }
    var selectedWork by remember { mutableStateOf<Work?>(null) }
    var selectedChapter by remember { mutableStateOf<Chapter?>(null) }
    var chapters by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var chapterLoading by remember { mutableStateOf(false) }
    var chapterRefresh by remember { mutableIntStateOf(0) }
    var pages by remember { mutableStateOf<List<String>>(emptyList()) }
    var pageLoading by remember { mutableStateOf(false) }
    var library by remember { mutableStateOf(store.savedWorks()) }
    var downloads by remember { mutableStateOf(store.downloads()) }
    var downloading by remember { mutableStateOf(false) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var downloadStatus by remember { mutableStateOf("") }
    var operationMessage by remember { mutableStateOf("") }

    fun refreshLocal() {
        library = store.savedWorks()
        downloads = store.downloads()
    }
    fun queueDownloadBatch(work: Work, selection: List<Chapter>) {
        if (downloading || selection.isEmpty() || work.catalogOnly) return
        downloading = true
        downloadJob = scope.launch {
            var completed = 0
            var existing = 0
            var failed = 0
            try {
                for ((index, chapter) in selection.withIndex()) {
                    coroutineContext.ensureActive()
                    downloadStatus = "Capítulo " + (index + 1) + " de " + selection.size
                    if (store.downloadedPages(chapter.id) != null) {
                        existing++
                        continue
                    }
                    try {
                        val urls = catalog.source(chapter.sourceId)?.pages(chapter).orEmpty()
                        require(urls.isNotEmpty()) { "Sin imágenes" }
                        store.download(work, chapter, urls)
                        completed++
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        failed++
                    }
                    delay(650) // Reduce continuous API requests
                }
                operationMessage = "Guardados: " + completed + " · Ya descargados: " +
                    existing + " · Fallidos: " + failed
            } catch (e: CancellationException) {
                operationMessage = "Descarga detenida: " + completed + " capítulos guardados"
            } finally {
                downloadStatus = ""
                downloading = false
                refreshLocal()
                downloadJob = null
            }
        }
    }
    fun search(
        text: String = query,
        nextFilters: SearchFilters = filters,
        loadMore: Boolean = false
    ) {
        if (loadMore && (searching || !hasMore)) return
        val nextPage = if (loadMore) page + 1 else 0
        val requested = nextFilters.copy(page = nextPage)
        searchJob?.cancel()
        if (!loadMore) {
            query = text
            filters = requested.copy(page = 0)
            page = 0
            hasMore = false
            searchWorks = emptyList()
            sourceVariants = emptyMap()
        }
        searchJob = scope.launch {
            searching = true
            error = ""
            try {
                val result = catalog.search(text, requested)
                if (loadMore) {
                    searchWorks = (searchWorks + result.works)
                        .distinctBy { normalizedTitle(it.title) + "|" + it.format.name }
                    sourceVariants = sourceVariants + result.alternatives
                } else {
                    searchWorks = result.works
                    sourceVariants = result.alternatives
                }
                if (result.errors.isNotEmpty()) error = result.errors.joinToString("\n")
                if (result.errors.isEmpty() || result.works.isNotEmpty()) {
                    page = nextPage
                    hasMore = result.hasMore
                }
            } catch (e: Exception) {
                error = e.message ?: "No se pudo consultar el catálogo"
            } finally {
                searching = false
            }
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
    LaunchedEffect(selectedWork?.key, chapterRefresh) {
        val work = selectedWork ?: return@LaunchedEffect
        chapters = emptyList()
        chapterLoading = true
        try {
            chapters = if (work.catalogOnly) {
                emptyList()
            } else if (work.sourceId == "local") {
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
    val activity = context as? ComponentActivity
    SideEffect {
        activity?.let {
            WindowCompat.getInsetsController(it.window, it.window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    BackHandler(enabled = showRepositories || selectedChapter != null ||
        selectedWork != null || tab != Tab.HOME) {
        when {
            showRepositories -> showRepositories = false
            selectedChapter != null -> { selectedChapter = null; refreshLocal() }
            selectedWork != null -> { selectedWork = null; error = "" }
            else -> tab = Tab.HOME
        }
    }
    MaterialTheme(colorScheme = palette) {
        Surface(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            color = MaterialTheme.colorScheme.background
        ) {
            val work = selectedWork
            val chapter = selectedChapter
            when {
                showRepositories -> RepositoryDirectoryScreen(
                    onBack = { showRepositories = false }
                )
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
                        if (chapter.sourceId != "local") {
                            queueDownloadBatch(work, listOf(chapter))
                        }
                    }
                )
                work != null -> YomiDetailScreen(
                    work = work, chapters = chapters, loading = chapterLoading,
                    sourceChoices = sourceVariants.values.firstOrNull { group ->
                        group.any { it.key == work.key }
                    } ?: listOf(work),
                    onSourceChange = { selectedWork = it },
                    onOpenExternal = { link ->
                        if (link.startsWith("https://")) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                        }
                    },
                    error = error,
                    isSaved = library.any { it.key == work.key },
                    isRead = { store.isRead(it) },
                    onBack = { selectedWork = null; error = "" },
                    onBookmark = {
                        if (store.contains(work)) store.remove(work) else store.save(work)
                        refreshLocal()
                    },
                    onRead = { selectedChapter = it },
                    onRefresh = { chapterRefresh++ },
                    onDownloadBatch = { selected -> queueDownloadBatch(work, selected) },
                    downloading = downloading,
                    downloadStatus = downloadStatus,
                    onCancelDownloads = { downloadJob?.cancel() }
                )
                else -> Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0)) {
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
                                error = error, filters = filters,
                                hasMore = hasMore,
                                onLoadMore = { search(loadMore = true) },
                                onSearch = { search(query, filters) },
                                onFiltersChanged = { search(query, it) },
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
                                downloadCount = downloads.size,
                                onRepositories = { showRepositories = true }
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
