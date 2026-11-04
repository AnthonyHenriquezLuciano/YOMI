package app.yomi.reader.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.yomi.reader.core.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Ten integrated sources, with transparent connection capabilities. */
@Composable
fun ConnectedSourcesScreen(
    onBack: () -> Unit,
    onNative: () -> Unit,
    onWeb: (ConnectedSource) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val visible = ConnectedSources.builtIn.filter { src ->
        query.isBlank() || src.title.contains(query, true) ||
            src.subtitle.contains(query, true)
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Volver")
            }
            Column(Modifier.weight(1f)) {
                Text("Fuentes integradas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Text("10 conexiones nuevas · Sin extensiones APK",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            label = { Text("Buscar fuente") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true
        )
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                "MangaPDF ofrece un lector conectado por API. Las otras nueve " +
                    "plataformas se consultan en su propia web, dentro de YOMI. " +
                    "Conservan sus cuentas, pagos y restricciones. " +
                    "Las búsquedas de esos portales no se mezclan con el catálogo nativo.",
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(visible, key = { it.id }) { source ->
                Surface(
                    onClick = {
                        if (source.kind == ConnectionKind.NATIVE_API) onNative()
                        else onWeb(source)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = 1.dp
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (source.kind == ConnectionKind.NATIVE_API)
                                Icons.Default.MenuBook else Icons.Default.Language,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(source.title, fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleSmall)
                            Text(source.subtitle, style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (source.kind == ConnectionKind.NATIVE_API) "API y lector de YOMI"
                                else "Sitio oficial dentro de YOMI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(source.note, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        }
    }
}

/**
 * Official website rendered as a normal web browser: no data extraction,
 * injected JavaScript, forced access to paid chapters, or image downloading.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SourceWebsiteScreen(source: ConnectedSource, onBack: () -> Unit) {
    val context = LocalContext.current
    var loading by remember(source.id) { mutableStateOf(true) }
    var error by remember(source.id) { mutableStateOf("") }
    var canBack by remember(source.id) { mutableStateOf(false) }
    var currentUrl by remember(source.id) { mutableStateOf(source.homeUrl) }
    val webView = remember(source.id) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.mediaPlaybackRequiresUserGesture = true
            settings.setSupportMultipleWindows(false)
            isHorizontalScrollBarEnabled = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?, request: WebResourceRequest?
                ): Boolean {
                    val target = request?.url?.toString() ?: return true
                    return !source.isAllowed(target)
                }

                override fun onPageStarted(
                    view: WebView?, url: String?, favicon: Bitmap?
                ) {
                    loading = true
                    error = ""
                    canBack = view?.canGoBack() == true
                    currentUrl = url ?: source.homeUrl
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    loading = false
                    canBack = view?.canGoBack() == true
                    currentUrl = url ?: currentUrl
                }

                override fun onReceivedError(
                    view: WebView?, request: WebResourceRequest?, failure: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        error = "Esta página no está disponible en el navegador integrado."
                        loading = false
                    }
                }
            }
            loadUrl(source.homeUrl)
        }
    }
    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }
    BackHandler(enabled = canBack) {
        webView.goBack()
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (webView.canGoBack()) webView.goBack() else onBack()
            }) { Icon(Icons.Default.ArrowBack, "Volver") }
            Column(Modifier.weight(1f)) {
                Text(source.title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text("Navegación integrada · Sitio del proveedor",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { webView.reload() }) {
                Icon(Icons.Default.Refresh, "Actualizar")
            }
            IconButton(onClick = {
                // Explicit user action only, useful when the service blocks WebView.
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.homeUrl)))
            }) {
                Icon(Icons.Default.OpenInBrowser, "Abrir web oficial")
            }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error.isNotBlank()) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { webView.loadUrl(source.homeUrl) }) {
                    Text("Volver a intentar")
                }
            }
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { webView },
            update = {  }
        )
    }
}

@Composable
fun MangaPdfBrowserScreen(
    source: MangaPdfSource,
    onBack: () -> Unit,
    onOpen: (Work) -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<Work>>(emptyList()) }
    var page by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var job by remember { mutableStateOf<Job?>(null) }

    fun fetch(loadMore: Boolean = false) {
        if (loading && loadMore) return
        job?.cancel()
        val request = query
        val targetPage = if (loadMore) page + 1 else 0
        job = scope.launch {
            loading = true
            if (!loadMore) items = emptyList()
            error = ""
            try {
                val result = source.searchPage(
                    request,
                    SearchFilters(
                        page = targetPage,
                        pageSize = 40,
                        sort = CatalogSort.POPULAR,
                        verifyChapters = false
                    )
                )
                items = if (loadMore) (items + result.works).distinctBy { it.key }
                    else result.works
                page = targetPage
                hasMore = result.hasMore
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                error = e.message ?: "No se pudo consultar MangaPDF"
            } finally { loading = false }
        }
    }
    LaunchedEffect(source) { fetch() }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(5.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Volver")
            }
            Text("MangaPDF · API",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Text(
                "Conector de lectura nativa. Este proveedor no informa el idioma de traducción; " +
                    "los títulos y capítulos no están garantizados en español. " +
                    "Los contenidos con cero capítulos no se podrán leer.",
                modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall
            )
        }
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            label = { Text("Buscar en MangaPDF") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                IconButton(onClick = { fetch() }) { Icon(Icons.Default.ArrowForward, "Buscar") }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { fetch() })
        )
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error.isNotBlank()) {
            Text(error, modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.error)
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.key }) { work ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(work) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WorkCover(work, Modifier.width(70.dp).height(96.dp))
                    Column(Modifier.weight(1f)) {
                        Text(work.title, maxLines = 2, fontWeight = FontWeight.SemiBold)
                        Text("Idioma sin verificar · API", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
                HorizontalDivider()
            }
            if (hasMore) item {
                OutlinedButton(onClick = { fetch(true) }, enabled = !loading,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("Cargar más obras")
                }
            }
        }
    }
}
