package app.yomi.reader.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.yomi.reader.core.*
import coil3.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import app.yomi.reader.R

@Composable
private fun Heading(title: String, subtitle: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.MenuBook, null, modifier = Modifier.size(42.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun WorkCover(work: Work, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.clip(RoundedCornerShape(14.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant) {
        if (work.coverUrl.isNotBlank()) {
            AsyncImage(
                model = work.coverUrl, contentDescription = "Portada de " + work.title,
                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MenuBook, null, modifier = Modifier.size(40.dp))
            }
        }
    }
}

@Composable
fun WorkCard(work: Work, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clickable(onClick = onOpen), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        WorkCover(work, Modifier.fillMaxWidth().aspectRatio(0.70f))
        Text(work.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(work.format.label + if (work.catalogOnly) " · Catálogo" else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HomeScreen(
    library: List<Work>, discovered: List<Work>,
    progress: (Work) -> ReadingProgress?, onOpen: (Work) -> Unit, onExplore: () -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("YOMI", style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black)
                    Text("Tu universo de lectura", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Image(painterResource(R.drawable.yomi_mark), "Logo de YOMI", modifier = Modifier.size(40.dp))
            }
        }
        val inProgress = library.filter { progress(it) != null }
            .sortedByDescending { progress(it)?.updatedAt ?: 0L }
        if (inProgress.isNotEmpty()) {
            item { Text("Continuar leyendo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(inProgress.take(8), key = { it.key }) { work ->
                        WorkCard(work, { onOpen(work) }, Modifier.width(140.dp))
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Descubre nuevas historias", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                TextButton(onClick = onExplore) { Text("Ver todo") }
            }
        }
        if (discovered.isEmpty()) {
            item { EmptyState("Un universo por explorar", "Abre Explorar para buscar obras en el catálogo.") }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(discovered.take(12), key = { it.key }) { work ->
                        WorkCard(work, { onOpen(work) }, Modifier.width(140.dp))
                    }
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Manga · Manhwa · Manhua",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Tu biblioteca, tus fuentes y tu forma de leer. Todo en un solo lugar.",
                        style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onExplore) { Text("Explorar catálogo") }
                }
            }
        }
    }
}

@Composable
fun ExploreScreen(
    query: String, onQuery: (String) -> Unit, results: List<Work>,
    loading: Boolean, error: String, filters: SearchFilters,
    hasMore: Boolean, onLoadMore: () -> Unit,
    onSearch: () -> Unit, onFiltersChanged: (SearchFilters) -> Unit,
    onOpen: (Work) -> Unit
) {
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Heading("Explorar", "Manga, manhwa y manhua desde un solo buscador")
            OutlinedTextField(
                value = query, onValueChange = onQuery, singleLine = true,
                placeholder = { Text("Buscar título o nombre alternativo...") },
                label = { Text("Buscar obras") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    IconButton(onClick = {
                        if (query.isNotBlank()) onQuery("") else onSearch()
                    }) {
                        Icon(if (query.isNotBlank()) Icons.Default.Close
                            else Icons.Default.ArrowForward,
                            contentDescription = if (query.isNotBlank()) "Vaciar texto" else "Buscar")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp),
                shape = RoundedCornerShape(18.dp)
            )
            Row(
                Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onSearch, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Buscar")
                }
                OutlinedButton(onClick = { showAdvanced = !showAdvanced }) {
                    Icon(Icons.Default.Tune, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Filtros" + if (filters.activeCount() > 0)
                        " (" + filters.activeCount() + ")" else "")
                    Icon(if (showAdvanced) Icons.Default.ExpandLess
                        else Icons.Default.ExpandMore, null)
                }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filters.format == null,
                    onClick = { onFiltersChanged(filters.copy(format = null)) },
                    label = { Text("Todos") }
                )
                listOf(WorkFormat.MANGA, WorkFormat.MANHWA, WorkFormat.MANHUA).forEach { fmt ->
                    FilterChip(
                        selected = filters.format == fmt,
                        onClick = { onFiltersChanged(filters.copy(format = fmt)) },
                        label = { Text(fmt.label) }
                    )
                }
            }
            if (showAdvanced) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Ordenar por", fontWeight = FontWeight.SemiBold)
                        Row(Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CatalogSort.entries.forEach { sort ->
                                FilterChip(
                                    selected = filters.sort == sort,
                                    onClick = { onFiltersChanged(filters.copy(sort = sort)) },
                                    label = { Text(sort.label) }
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Idioma: Español", fontWeight = FontWeight.SemiBold)
                        }
                        Text("Estado de publicación", fontWeight = FontWeight.SemiBold)
                        Row(Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                null to "Todos", "ongoing" to "En curso",
                                "completed" to "Finalizado", "hiatus" to "Pausado",
                                "cancelled" to "Cancelado"
                            ).forEach { (status, label) ->
                                FilterChip(
                                    selected = filters.status == status,
                                    onClick = { onFiltersChanged(filters.copy(status = status)) },
                                    label = { Text(label) }
                                )
                            }
                        }
                        TextButton(
                            onClick = { onFiltersChanged(SearchFilters()) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.RestartAlt, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Restablecer")
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                Text("YOMI Global", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("Contenido en español", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text(results.size.toString() + " títulos", style = MaterialTheme.typography.labelSmall)
            }
            if (loading && results.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error.isNotBlank()) Text(error, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
        }
        if (!loading && results.isEmpty()) {
            EmptyState("Sin resultados", "Prueba otro título o cambia los filtros.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(135.dp), modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                gridItems(results, key = { it.key }) { work ->
                    WorkCard(work, { onOpen(work) })
                }
                if (hasMore || (loading && results.isNotEmpty())) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (loading) CircularProgressIndicator()
                            else OutlinedButton(onClick = onLoadMore) {
                                Icon(Icons.Default.ExpandMore, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Cargar más resultados")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(
    library: List<Work>, progress: (Work) -> ReadingProgress?,
    onOpen: (Work) -> Unit, onImport: () -> Unit
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Heading("Mi biblioteca", library.size.toString() + " obras guardadas")
            IconButton(onClick = onImport) { Icon(Icons.Default.Add, "Importar CBZ") }
        }
        if (library.isEmpty()) {
            EmptyState("Tu biblioteca está vacía",
                "Guarda tus obras favoritas desde Explorar o importa un CBZ.")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(135.dp), modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                gridItems(library, key = { it.key }) { work ->
                    Column {
                        WorkCard(work, { onOpen(work) })
                        if (progress(work) != null) {
                            Text("En lectura", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadsScreen(
    downloads: List<DownloadedChapter>,
    onOpen: (DownloadedChapter) -> Unit, onRemove: (DownloadedChapter) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(20.dp)) {
            Heading("Descargas", downloads.size.toString() + " capítulos disponibles sin conexión")
        }
        if (downloads.isEmpty()) EmptyState("No hay descargas",
            "Abre un capítulo y pulsa descargar para leerlo sin conexión.")
        else LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
            items(downloads, key = { it.chapter.id }) { item ->
                Row(Modifier.fillMaxWidth().clickable { onOpen(item) }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    WorkCover(item.work, Modifier.size(width = 54.dp, height = 70.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.work.title, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.chapter.title, style = MaterialTheme.typography.bodySmall)
                        Text(item.paths.size.toString() + " páginas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onRemove(item) }) {
                        Icon(Icons.Default.DeleteOutline, "Eliminar descarga")
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun SettingsScreen(
    dark: Boolean, onDark: (Boolean) -> Unit, onImport: () -> Unit,
    libraryCount: Int, downloadCount: Int,
    onRepositories: () -> Unit,
    onCategories: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Heading("Ajustes", "Personaliza tu experiencia")
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DarkMode, null)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Modo oscuro", fontWeight = FontWeight.Medium)
                Text("Tema blanco o negro", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = dark, onCheckedChange = onDark)
        }
        HorizontalDivider()
        Text("Archivos y biblioteca", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = onCategories, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FolderOpen, null)
            Spacer(Modifier.width(8.dp))
            Text("Gestionar categorías")
        }
        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.FileOpen, null)
            Spacer(Modifier.width(8.dp))
            Text("Importar manga desde CBZ")
        }
        Text(libraryCount.toString() + " obras guardadas · " +
            downloadCount.toString() + " capítulos descargados",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Text("Fuentes integradas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("MangaDex · Lector integrado", fontWeight = FontWeight.Medium)
        Text("Comick · Catálogo pendiente de verificación de idioma español", fontWeight = FontWeight.Medium)
        Text("CBZ local · Importación desde el dispositivo", fontWeight = FontWeight.Medium)
        OutlinedButton(onClick = onRepositories, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.AccountTree, null)
            Spacer(Modifier.width(8.dp))
            Text("Explorar repositorios de extensiones")
        }
        Text("Los índices muestran proveedores disponibles. Las extensiones APK no se ejecutan ni se instalan en YOMI.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Text("Atribución y uso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("YOMI es un lector sin anuncios. El contenido remoto se consulta mediante la API de MangaDex. " +
            "Las obras y traducciones pertenecen a sus respectivos autores y grupos. " +
            "Se respetan solicitudes de retirada y términos de cada proveedor.",
            style = MaterialTheme.typography.bodyMedium)
        Text("YOMI · Versión 0.3.0", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DetailScreen(
    work: Work, chapters: List<Chapter>, loading: Boolean, error: String,
    sourceChoices: List<Work>, onSourceChange: (Work) -> Unit,
    onOpenExternal: (String) -> Unit,
    isSaved: Boolean, isRead: (String) -> Boolean,
    onBack: () -> Unit, onBookmark: () -> Unit,
    onRead: (Chapter) -> Unit, onContinue: () -> Unit,
    onDownload: (Chapter) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 36.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
                Text("Detalle de obra", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onBookmark) {
                    Icon(if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        "Guardar o quitar de biblioteca")
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                WorkCover(work, Modifier.size(width = 126.dp, height = 180.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(work.title, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black)
                    Text(work.format.label, style = MaterialTheme.typography.labelLarge)
                    if (work.status.isNotBlank()) Text(work.status,
                        style = MaterialTheme.typography.bodySmall)
                    Text("Fuente: " + when (work.sourceId) {
                        "mangadex" -> "MangaDex"
                        "comick" -> "Comick (solo catálogo)"
                        else -> "Local"
                    },
                        style = MaterialTheme.typography.labelMedium)
                    Text("Idioma original: " + work.language,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (sourceChoices.size > 1) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Fuentes disponibles", fontWeight = FontWeight.Bold)
                    Row(Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        sourceChoices.forEach { option ->
                            FilterChip(selected = option.key == work.key,
                                onClick = { onSourceChange(option) },
                                label = { Text(when(option.sourceId) {
                                    "mangadex" -> "MangaDex"
                                    "comick" -> "Comick · Catálogo"
                                    else -> "CBZ local"
                                }) })
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (work.catalogOnly) {
                    Text("Este proveedor permite descubrir obras. Para leer, abre la página del proveedor o selecciona una fuente compatible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (work.catalogUrl.startsWith("https://")) {
                        OutlinedButton(
                            onClick = { onOpenExternal(work.catalogUrl) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Abrir en el proveedor")
                        }
                    }
                } else {
                    Button(onClick = onContinue, enabled = chapters.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.MenuBook, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Continuar / Comenzar")
                    }
                }
                if (work.description.isNotBlank()) {
                    var synopsisExpanded by rememberSaveable(work.key) { mutableStateOf(false) }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { synopsisExpanded = !synopsisExpanded }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sinopsis", style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (synopsisExpanded) "Ocultar" else "Mostrar",
                                        style = MaterialTheme.typography.labelMedium)
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        if (synopsisExpanded) Icons.Default.ExpandLess
                                        else Icons.Default.ExpandMore,
                                        contentDescription = if (synopsisExpanded)
                                            "Ocultar sinopsis" else "Mostrar sinopsis"
                                    )
                                }
                            }
                            if (synopsisExpanded) {
                                Text(
                                    work.description, modifier = Modifier.padding(
                                        start = 16.dp, end = 16.dp, bottom = 16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                HorizontalDivider()
                Text("Capítulos (" + chapters.size + ")",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (work.sourceId == "mangadex") {
                    Text("Capítulos proporcionados por MangaDex. Créditos de los grupos en cada capítulo.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        }
        if (!loading && chapters.isEmpty()) {
            item { EmptyState("Sin capítulos", "Prueba otro título o revisa tu conexión.") }
        }
        items(chapters, key = { it.id }) { chapter ->
            Row(Modifier.fillMaxWidth().clickable { onRead(chapter) }
                .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isRead(chapter.id)) Icons.Default.CheckCircle else Icons.Default.ChevronRight,
                    null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(chapter.title, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold, maxLines = 2)
                    Text(chapter.language.uppercase() + " · " +
                        chapter.scanlationGroup.ifBlank { "Grupo no indicado" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (work.sourceId != "local") IconButton(onClick = { onDownload(chapter) }) {
                    Icon(Icons.Default.Download, "Descargar")
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 20.dp))
        }
    }
}
