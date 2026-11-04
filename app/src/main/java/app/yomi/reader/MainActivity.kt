package app.yomi.reader

import android.os.Bundle
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.yomi.reader.core.*
import app.yomi.reader.data.YomiStore
import app.yomi.reader.ui.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import app.yomi.reader.data.*

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
    val libraryMeta = remember { LibraryMetaStore(context.applicationContext) }
    val prefsStore = remember { YomiPreferences(context.applicationContext) }
    val backups = remember { YomiBackup(context.applicationContext) }
    var preferences by remember { mutableStateOf(prefsStore.load()) }
    var showOnboarding by remember { mutableStateOf(!preferences.onboardingCompleted) }
    var showUpdates by remember { mutableStateOf(false) }
    fun updatePreferences(value: YomiUiPreferences) {
        preferences = value
        prefsStore.save(value)
    }
    val remote = remember { MangaDexSource() }
    val localSource = remember { LocalArchiveSource(store) }
    val comick = remember { ComickCatalogSource() }
    val mangaPdf = remember { MangaPdfSource() }
    val bundled = remember { BundledMadaraSources.configs.map(::MadaraNativeSource) }
    val catalog = remember {
        GlobalCatalog(listOf(remote, comick, mangaPdf, localSource) + bundled)
    }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(Tab.HOME) }
    val systemDark = isSystemInDarkTheme()
    val dark = when (preferences.theme) {
        YomiTheme.SYSTEM -> systemDark
        YomiTheme.LIGHT -> false
        YomiTheme.DARK -> true
    }
    var query by remember { mutableStateOf("") }
    var filters by remember {
        mutableStateOf(SearchFilters(sourceIds = preferences.sourceIds))
    }
    var catalogTags by remember { mutableStateOf<List<CatalogTag>>(emptyList()) }
    var catalogTagsError by remember { mutableStateOf("") }
    var catalogTagsRefresh by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var searchWorks by remember { mutableStateOf<List<Work>>(emptyList()) }
    var sourceVariants by remember { mutableStateOf<Map<String, List<Work>>>(emptyMap()) }
    var searching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var showSources by remember { mutableStateOf(false) }
    var selectedWork by remember { mutableStateOf<Work?>(null) }
    var selectedChapter by remember { mutableStateOf<Chapter?>(null) }
    var chapters by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var chapterLoading by remember { mutableStateOf(false) }
    var chapterRefresh by remember { mutableIntStateOf(0) }
    var pages by remember { mutableStateOf<List<String>>(emptyList()) }
    var pageLoading by remember { mutableStateOf(false) }
    var library by remember { mutableStateOf(store.savedWorks()) }
    var libraryRevision by remember { mutableIntStateOf(0) }
    var categories by remember { mutableStateOf(libraryMeta.categories()) }
    var appearance by remember { mutableStateOf(libraryMeta.appearance()) }
    var categoryTarget by remember { mutableStateOf<Work?>(null) }
    var showCategorySettings by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var libraryRefreshing by remember { mutableStateOf(false) }
    var downloads by remember { mutableStateOf(store.downloads()) }
    var downloading by remember { mutableStateOf(false) }
    var downloadPaused by remember { mutableStateOf(false) }
    var downloadProcessed by remember { mutableIntStateOf(0) }
    var downloadTotal by remember { mutableIntStateOf(0) }
    var failedDownloads by remember { mutableStateOf<List<Chapter>>(emptyList()) }
    var failedDownloadWork by remember { mutableStateOf<Work?>(null) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var downloadStatus by remember { mutableStateOf("") }
    var operationMessage by remember { mutableStateOf("") }

    fun refreshLocal() {
        library = store.savedWorks()
        downloads = store.downloads()
        categories = libraryMeta.categories()
        libraryRevision++
    }
    fun updateAppearance(value: LibraryAppearance) {
        appearance = value
        libraryMeta.setAppearance(value)
    }
    fun resume(work: Work) {
        selectedWork = work
        val previous = store.progress(work)?.chapterId
        selectedChapter = libraryMeta.cachedChapters(work)
            ?.chapters?.firstOrNull { it.id == previous }
    }
    fun refreshLibrary() {
        if (libraryRefreshing) return
        libraryRefreshing = true
        scope.launch {
            var completed = 0
            var failed = 0
            try {
                val toUpdate = library.filterNot { it.catalogOnly }
                for (work in toUpdate) {
                    coroutineContext.ensureActive()
                    try {
                        val newChapters = catalog.source(work.sourceId)?.chapters(work).orEmpty()
                        libraryMeta.cacheChapters(work, newChapters)
                        completed++
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        failed++
                    }
                    delay(350)
                }
                operationMessage = "Biblioteca actualizada: " + completed +
                    " obras · Fallidos: " + failed
            } finally {
                libraryRefreshing = false
                refreshLocal()
            }
        }
    }
    val libraryItems = remember(library, downloads, libraryRevision) {
        val downloadedIds = downloads.map { it.chapter.id }.toSet()
        library.map { libraryMeta.item(it, store, downloadedIds) }
    }
    val historyItems = remember(library, downloads, libraryRevision) {
        val allWorks = (library + store.historyWorks()).distinctBy { it.key }
        val downloadedIds = downloads.map { it.chapter.id }.toSet()
        allWorks.map { libraryMeta.item(it, store, downloadedIds) }
    }
    fun queueDownloadBatch(work: Work, selection: List<Chapter>) {
        if (downloading || selection.isEmpty() || work.catalogOnly) return
        if (preferences.downloadOnWifiOnly) {
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as? ConnectivityManager
            val caps = manager?.getNetworkCapabilities(manager.activeNetwork)
            if (caps == null || !(
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
            ) {
                operationMessage = "Conecta Wi-Fi o desactiva la restricción en Ajustes → Descargas."
                return
            }
        }
        downloading = true
        downloadPaused = false
        downloadTotal = selection.size
        downloadProcessed = 0
        failedDownloads = emptyList()
        failedDownloadWork = null
        downloadJob = scope.launch {
            var completed = 0
            var existing = 0
            val failed = mutableListOf<Chapter>()
            try {
                for ((index, chapter) in selection.withIndex()) {
                    coroutineContext.ensureActive()
                    while (downloadPaused) {
                        delay(250)
                        coroutineContext.ensureActive()
                    }
                    downloadStatus = "Capítulo " + (index + 1) + " de " + selection.size
                    if (store.downloadedPages(chapter.id) != null) {
                        existing++
                        downloadProcessed++
                        continue
                    }
                    try {
                        val urls = catalog.source(chapter.sourceId)?.pages(chapter).orEmpty()
                        require(urls.isNotEmpty()) { "Sin imágenes" }
                        store.download(work, chapter, urls) { percent ->
                            downloadStatus = "Capítulo " + (index + 1) +
                                " de " + selection.size + " · " + percent + "%"
                        }
                        completed++
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        failed += chapter
                    }
                    downloadProcessed++
                    delay(650)
                }
                operationMessage = "Guardados: " + completed +
                    " · Ya descargados: " + existing + " · Fallidos: " + failed.size
            } catch (e: CancellationException) {
                operationMessage = "Descarga detenida: " + completed + " guardados"
            } finally {
                failedDownloads = failed.toList()
                failedDownloadWork = if (failed.isEmpty()) null else work
                downloadStatus = ""
                downloading = false
                downloadPaused = false
                downloadJob = null
                refreshLocal()
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
        val enabled = nextFilters.sourceIds.intersect(preferences.sourceIds)
        val requested = nextFilters.copy(
            page = nextPage,
            sourceIds = enabled.ifEmpty { preferences.sourceIds }
        )
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
                libraryMeta.ensureAdded(imported)
                refreshLocal()
                categoryTarget = imported
                tab = Tab.LIBRARY
                operationMessage = "Importado: " + imported.title
            } catch (e: Exception) {
                operationMessage = e.message ?: "No se pudo importar el archivo"
            }
        }
    }
    val backupExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            operationMessage = try {
                val size = backups.exportTo(uri)
                "Respaldo guardado (" + size / 1024 + " KB)"
            } catch (e: Exception) {
                "No se pudo exportar: " + (e.message ?: "Error")
            }
        }
    }
    val backupImport = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            operationMessage = try {
                val count = backups.importFrom(uri)
                preferences = prefsStore.load()
                appearance = libraryMeta.appearance()
                refreshLocal()
                "Respaldo restaurado: " + count + " secciones"
            } catch (e: Exception) {
                "No se pudo importar: " + (e.message ?: "Archivo no válido")
            }
        }
    }
    LaunchedEffect(preferences.sourceIds, preferences.onboardingCompleted) {
        if (preferences.onboardingCompleted) {
            search(query, filters.copy(sourceIds = preferences.sourceIds))
        }
    }
    LaunchedEffect(catalogTagsRefresh) {
        try {
            catalogTags = remote.tags()
            catalogTagsError = ""
        } catch (e: Exception) {
            catalogTagsError = "No se pudieron cargar los géneros y etiquetas."
        }
    }
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
            libraryMeta.cacheChapters(work, chapters)
            if (store.contains(work)) libraryRevision++
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
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (selectedChapter != null) {
                    hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }
    BackHandler(enabled = showCategorySettings || showHistory ||
        showSources ||
        selectedChapter != null || selectedWork != null || tab != Tab.HOME) {
        when {
            selectedChapter != null -> { selectedChapter = null; refreshLocal() }
            selectedWork != null -> { selectedWork = null; error = "" }
            showSources -> showSources = false
            showHistory -> showHistory = false
            showCategorySettings -> showCategorySettings = false
            else -> tab = Tab.HOME
        }
    }
    MaterialTheme(colorScheme = palette, typography = YomiTypography, shapes = YomiShapes) {
        Surface(
            modifier = Modifier.fillMaxSize().then(
                if (selectedChapter != null) Modifier
                else Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
            ),
            color = MaterialTheme.colorScheme.background
        ) {
            val work = selectedWork
            val chapter = selectedChapter
            when {
                showOnboarding -> YomiOnboarding(
                    initial = preferences,
                    onComplete = {
                        updatePreferences(it)
                        showOnboarding = false
                    }
                )
                showCategorySettings -> CategoriesSettingsScreen(
                    categories = categories,
                    onBack = { showCategorySettings = false },
                    onAdd = { name ->
                        val added = libraryMeta.addCategory(name)
                        if (added) refreshLocal()
                        added
                    },
                    onRename = { old, next ->
                        val renamed = libraryMeta.renameCategory(old, next)
                        if (renamed) refreshLocal()
                        renamed
                    },
                    onDelete = { category ->
                        libraryMeta.deleteCategory(category)
                        refreshLocal()
                    }
                )
                showHistory -> ReadingHistoryScreen(
                    items = historyItems,
                    onBack = { showHistory = false },
                    onResume = { work ->
                        showHistory = false
                        resume(work)
                    }
                )
                work != null && chapter != null -> ReaderScreen(
                    work = work, chapter = chapter, urls = pages,
                    loading = pageLoading, error = error,
                    stored = store,
                    preferences = preferences,
                    onBack = { selectedChapter = null; refreshLocal() },
                    onModeSelected = { store.setMode(work, it) },
                    onProgress = { index ->
                        store.setProgress(work, chapter, index)
                        if (libraryMeta.status(work) == ReadingShelf.PENDING)
                            libraryMeta.setStatus(work, ReadingShelf.READING)
                    },
                    onComplete = { store.markRead(chapter.id); libraryRevision++ },
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
                    onTagClick = { tag ->
                        if (work.sourceId == "mangadex") {
                            selectedWork = null
                            selectedChapter = null
                            tab = Tab.EXPLORE
                            search("", SearchFilters(includedTags = setOf(tag.id)))
                        }
                    },
                    onOpenExternal = { link ->
                        if (link.startsWith("https://")) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                        }
                    },
                    error = error,
                    isSaved = library.any { it.key == work.key },
                    isRead = { store.isRead(it) },
                    onBack = { selectedWork = null; error = "" },
                    onBookmark = { categoryTarget = work },
                    onRead = {
                        selectedChapter = it
                        if (store.contains(work)) {
                            libraryMeta.setStatus(work, ReadingShelf.READING)
                            libraryRevision++
                        }
                    },
                    onRefresh = { chapterRefresh++ },
                    onDownloadBatch = { selected -> queueDownloadBatch(work, selected) },
                    downloading = downloading,
                    downloadStatus = downloadStatus,
                    onCancelDownloads = { downloadJob?.cancel() }
                )
                showSources -> BundledSourcesScreen(
                    sources = bundled,
                    onBack = { showSources = false },
                    onOpenSource = { sourceId ->
                        showSources = false
                        tab = Tab.EXPLORE
                        search("", SearchFilters(sourceIds = setOf(sourceId)))
                    },
                    onSearchAll = {
                        showSources = false
                        tab = Tab.EXPLORE
                        search("", SearchFilters(sourceIds = DEFAULT_NATIVE_SOURCE_IDS))
                    }
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
                        Crossfade(
                            targetState = tab,
                            animationSpec = tween(if (preferences.enableAnimations) 170 else 0),
                            label = "Cambio de sección"
                        ) { currentTab ->
                        when (currentTab) {
                            Tab.HOME -> YomiHomeScreen(
                                saved = libraryItems, discovered = searchWorks,
                                loading = searching,
                                onOpen = { selectedWork = it },
                                onResume = { resume(it) },
                                onExplore = { tab = Tab.EXPLORE }
                            )
                            Tab.EXPLORE -> ExploreV2Screen(
                                query = query,
                                onQuery = { query = it },
                                results = searchWorks,
                                loading = searching,
                                error = error,
                                filters = filters,
                                availableTags = catalogTags,
                                tagsError = catalogTagsError,
                                onRetryTags = { catalogTagsRefresh++ },
                                onSources = { showSources = true },
                                hasMore = hasMore,
                                onLoadMore = { search(loadMore = true) },
                                onSearch = { search(query, filters) },
                                onFiltersChanged = { search(query, it) },
                                onOpen = { selectedWork = it },
                                compactCovers = preferences.compactCovers,
                                showBadges = preferences.showContentBadges
                            )
                            Tab.LIBRARY -> LibraryV2Screen(
                                library = libraryItems,
                                categories = categories,
                                appearance = appearance,
                                onAppearance = { updateAppearance(it) },
                                revision = libraryRevision,
                                refreshing = libraryRefreshing,
                                onRefresh = { refreshLibrary() },
                                onOpen = { selectedWork = it },
                                onRandom = { library.randomOrNull()?.let { selectedWork = it } },
                                onHistory = { showHistory = true },
                                onContinue = { resume(it) },
                                onFavorite = {
                                    libraryMeta.toggleFavorite(it)
                                    libraryRevision++
                                },
                                onShelf = { work, shelf ->
                                    libraryMeta.setStatus(work, shelf)
                                    libraryRevision++
                                },
                                onCategories = { categoryTarget = it }
                            )
                            Tab.DOWNLOADS -> YomiDownloadsScreen(
                                downloads = downloads,
                                downloading = downloading,
                                paused = downloadPaused,
                                processed = downloadProcessed,
                                total = downloadTotal,
                                message = downloadStatus,
                                failedCount = failedDownloads.size,
                                onPause = { downloadPaused = true },
                                onResume = { downloadPaused = false },
                                onCancel = { downloadJob?.cancel() },
                                onRetry = {
                                    failedDownloadWork?.let { work ->
                                        queueDownloadBatch(work, failedDownloads)
                                    }
                                },
                                onOpen = {
                                    selectedWork = it.work
                                    selectedChapter = it.chapter
                                },
                                onRemove = {
                                    store.removeDownload(it.chapter.id)
                                    refreshLocal()
                                }
                            )
                            Tab.SETTINGS -> YomiSettingsHub(
                                settings = preferences,
                                onChange = { updatePreferences(it) },
                                libraryCount = library.size,
                                downloadCount = downloads.size,
                                onCategories = { showCategorySettings = true },
                                onHistory = { showHistory = true },
                                onImportCbz = {
                                    importLauncher.launch(arrayOf(
                                        "application/zip", "application/x-cbz",
                                        "application/octet-stream"
                                    ))
                                },
                                onSources = { showSources = true },
                                onExportBackup = {
                                    backupExport.launch("YOMI-respaldo.json")
                                },
                                onImportBackup = {
                                    backupImport.launch(arrayOf("application/json"))
                                },
                                onClearCache = {
                                    val removed = context.cacheDir.listFiles()
                                        ?.count { it.deleteRecursively() } ?: 0
                                    operationMessage = "Caché temporal limpiada (" + removed + " elementos)"
                                },
                                onOpenGitHub = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/AnthonyHenriquezLuciano/YOMI")))
                                },
                                onShowUpdates = { showUpdates = true }
                            )
                        }
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
            if (showUpdates) AlertDialog(
                onDismissRequest = { showUpdates = false },
                title = { Text("Novedades · YOMI 0.8") },
                text = {
                    Text(
                        "Explorar más compacto, Inicio renovado, Ajustes organizados, " +
                        "bienvenida inicial, lector personalizable, mejoras en descargas, " +
                        "categorías y respaldo local de datos."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showUpdates = false }) { Text("Entendido") }
                }
            )
            val target = categoryTarget
            if (target != null) CategoryPicker(
                work = target,
                existing = libraryMeta.assignments(target),
                categories = categories,
                alreadySaved = store.contains(target),
                onAddCategory = { name ->
                    val added = libraryMeta.addCategory(name)
                    if (added) refreshLocal()
                    added
                },
                onSave = { names ->
                    if (!store.contains(target)) store.save(target)
                    libraryMeta.saveAssignments(target, names)
                    libraryMeta.ensureAdded(target)
                    refreshLocal()
                },
                onRemove = {
                    store.remove(target)
                    refreshLocal()
                },
                onDismiss = { categoryTarget = null }
            )
    }
}
