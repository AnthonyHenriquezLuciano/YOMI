package app.yomi.reader.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.yomi.reader.core.*

/** Compact discovery layout: content is primary, not the search controls. */
@Composable
fun ExploreV2Screen(
    query: String, onQuery: (String) -> Unit, results: List<Work>,
    loading: Boolean, error: String, filters: SearchFilters,
    availableTags: List<CatalogTag>, tagsError: String, onRetryTags: () -> Unit,
    onSources: () -> Unit,
    hasMore: Boolean, onLoadMore: () -> Unit,
    onSearch: () -> Unit, onFiltersChanged: (SearchFilters) -> Unit,
    onOpen: (Work) -> Unit,
    compactCovers: Boolean = false,
    showBadges: Boolean = true
) {
    var filtersOpen by remember { mutableStateOf(false) }
    if (filtersOpen) FullExploreFilterSheet(
        current = filters, tags = availableTags, tagsError = tagsError,
        onRetryTags = onRetryTags,
        onDismiss = { filtersOpen = false },
        onApply = { filtersOpen = false; onFiltersChanged(it) }
    )
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Explorar", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onSources,
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Hub, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Fuentes", style = MaterialTheme.typography.labelMedium)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).height(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .77f))
                        .padding(horizontal = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, Modifier.size(19.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(9.dp))
                    BasicTextField(
                        value = query, onValueChange = onQuery,
                        singleLine = true, modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                        decorationBox = { field ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isBlank()) Text("Buscar una historia...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                field()
                            }
                        }
                    )
                    if (query.isNotBlank()) {
                        IconButton(onClick = { onQuery("") }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Close, "Limpiar", Modifier.size(16.dp))
                        }
                    }
                }
                Surface(
                    onClick = { filtersOpen = true },
                    shape = RoundedCornerShape(13.dp),
                    color = if (filters.activeCount() > 0)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Tune, "Filtros completos",
                            tint = if (filters.activeCount() > 0)
                                MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(21.dp))
                    }
                }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                listOf(
                    null to "Todos", WorkFormat.MANGA to "Manga",
                    WorkFormat.MANHWA to "Manhwa", WorkFormat.MANHUA to "Manhua"
                ).forEach { (format, label) ->
                    FilterChip(
                        selected = filters.format == format,
                        onClick = { onFiltersChanged(filters.copy(format = format)) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            if (filters.includedTags.isNotEmpty() || filters.excludedTags.isNotEmpty()) {
                val names = availableTags.associateBy { it.id }
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    filters.includedTags.forEach { id ->
                        InputChip(
                            selected = true,
                            onClick = { onFiltersChanged(filters.copy(
                                includedTags = filters.includedTags - id)) },
                            label = { Text(names[id]?.label ?: "Etiqueta",
                                style = MaterialTheme.typography.labelSmall) },
                            trailingIcon = { Icon(Icons.Default.Close, "Quitar", Modifier.size(13.dp)) }
                        )
                    }
                    filters.excludedTags.forEach { id ->
                        InputChip(
                            selected = true,
                            onClick = { onFiltersChanged(filters.copy(
                                excludedTags = filters.excludedTags - id)) },
                            label = { Text("Sin " + (names[id]?.label ?: "etiqueta"),
                                style = MaterialTheme.typography.labelSmall) },
                            trailingIcon = { Icon(Icons.Default.Close, "Quitar", Modifier.size(13.dp)) }
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("EN ESPAÑOL", style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (filters.activeCount() > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text(filters.activeCount().toString() + " filtros activos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                Text(results.size.toString() + " títulos", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (error.isNotBlank()) Text(error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
        }
        when {
            loading && results.isEmpty() -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(if (compactCovers) 112.dp else 143.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(8) { ShimmerWorkCard() }
                }
            }
            !loading && results.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.AutoStories, null, Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Ninguna obra coincide con la búsqueda",
                        style = MaterialTheme.typography.titleMedium)
                    Text("Prueba otra palabra, género o fuente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = { onFiltersChanged(SearchFilters()) }) {
                        Text("Restablecer búsqueda")
                    }
                }
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(if (compactCovers) 112.dp else 143.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                gridItems(results, key = { it.key }) { work ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        WorkCard(work, { onOpen(work) })
                        if (showBadges && work.chapterCount > 0) {
                            Text(work.chapterCount.toString() + " caps. · Español",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (hasMore || (loading && results.isNotEmpty())) item(
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center) {
                        if (loading) CircularProgressIndicator()
                        else OutlinedButton(onClick = onLoadMore) { Text("Ver más títulos") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FullExploreFilterSheet(
    current: SearchFilters, tags: List<CatalogTag>, tagsError: String,
    onRetryTags: () -> Unit, onDismiss: () -> Unit,
    onApply: (SearchFilters) -> Unit
) {
    var draft by remember(current) { mutableStateOf(current) }
    var yearText by remember(current) { mutableStateOf(current.year?.toString().orEmpty()) }
    var tagQuery by remember { mutableStateOf("") }
    var excluding by remember { mutableStateOf(false) }
    var tagGroup by remember { mutableStateOf("genre") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.90f)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Filtros avanzados", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                TextButton(onClick = { draft = SearchFilters(); yearText = ""; tagQuery = "" }) {
                    Text("Restablecer")
                }
            }
            HorizontalDivider()
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FilterHeading("Idioma y disponibilidad")
                Text("Español · Con al menos un capítulo verificado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilterHeading("Tipo de obra")
                ExploreChoices(
                    listOf(null to "Todos", WorkFormat.MANGA to "Manga",
                        WorkFormat.MANHWA to "Manhwa", WorkFormat.MANHUA to "Manhua"),
                    draft.format
                ) { draft = draft.copy(format = it) }
                FilterHeading("Estado de publicación")
                ExploreChoices(
                    listOf(null to "Todos", "ongoing" to "En publicación",
                        "completed" to "Finalizado", "hiatus" to "En pausa",
                        "cancelled" to "Cancelado"), draft.status
                ) { draft = draft.copy(status = it) }
                FilterHeading("Público")
                ExploreChoices(
                    listOf(null to "Todos", "shounen" to "Shōnen",
                        "shoujo" to "Shōjo", "seinen" to "Seinen",
                        "josei" to "Josei", "none" to "Sin clasificación"),
                    draft.demographic
                ) { draft = draft.copy(demographic = it) }
                FilterHeading("Año")
                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it.filter(Char::isDigit).take(4) },
                    singleLine = true, label = { Text("Año de publicación") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                FilterHeading("Géneros y etiquetas")
                ExploreChoices(listOf(false to "Incluir", true to "Excluir"),
                    excluding) { excluding = it }
                Text(
                    if (excluding) "Excluir obras con las etiquetas seleccionadas."
                    else "Mostrar obras con las etiquetas seleccionadas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("genre" to "Géneros", "theme" to "Temas",
                        "format" to "Formatos", "content" to "Contenido").forEach { (group, name) ->
                        FilterChip(selected = tagGroup == group,
                            onClick = { tagGroup = group }, label = { Text(name) })
                    }
                }
                OutlinedTextField(
                    value = tagQuery, onValueChange = { tagQuery = it },
                    label = { Text("Encontrar etiqueta") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                if (tagsError.isNotBlank()) {
                    Text(tagsError, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetryTags) { Text("Volver a cargar etiquetas") }
                }
                val visible = tags.filter { it.group == tagGroup &&
                    (tagQuery.isBlank() || it.label.contains(tagQuery, true)) }
                if (visible.isEmpty()) Text(
                    if (tags.isEmpty()) "Cargando etiquetas..."
                    else "Sin coincidencias de etiquetas.",
                    style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    visible.forEach { tag ->
                        val selected = if (excluding) tag.id in draft.excludedTags
                            else tag.id in draft.includedTags
                        FilterChip(
                            selected = selected,
                            onClick = {
                                draft = if (excluding) draft.copy(
                                    excludedTags = if (selected) draft.excludedTags - tag.id
                                        else draft.excludedTags + tag.id,
                                    includedTags = draft.includedTags - tag.id
                                ) else draft.copy(
                                    includedTags = if (selected) draft.includedTags - tag.id
                                        else draft.includedTags + tag.id,
                                    excludedTags = draft.excludedTags - tag.id
                                )
                            },
                            label = { Text(tag.label) }
                        )
                    }
                }
                Text("Combinar etiquetas incluidas", fontWeight = FontWeight.Medium)
                ExploreChoices(listOf("AND" to "Todas (Y)", "OR" to "Cualquiera (O)"),
                    draft.includeTagsMode) { draft = draft.copy(includeTagsMode = it) }
                Text("Combinar etiquetas excluidas", fontWeight = FontWeight.Medium)
                ExploreChoices(listOf("OR" to "Cualquiera (O)", "AND" to "Todas (Y)"),
                    draft.excludeTagsMode) { draft = draft.copy(excludeTagsMode = it) }
                FilterHeading("Clasificación de contenido")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Permitir contenido sugerente",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = draft.allowSuggestive,
                        onCheckedChange = { draft = draft.copy(allowSuggestive = it) })
                }
                FilterHeading("Ordenar por")
                ExploreChoices(CatalogSort.entries.map { it to it.label },
                    draft.sort) { draft = draft.copy(sort = it) }
                FilterHeading("Fuentes disponibles")
                val sourceOptions = listOf(
                    "mangadex" to "MangaDex · Español",
                    "local" to "Mis archivos CBZ"
                ) + BundledMadaraSources.configs.map { cfg ->
                    "mdr_" + cfg.id to (cfg.title + " · Español")
                }
                sourceOptions.forEach { (id, label) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = id in draft.sourceIds, onCheckedChange = { checked ->
                            val updated = if (checked) draft.sourceIds + id
                                else draft.sourceIds - id
                            if (updated.isNotEmpty()) draft = draft.copy(sourceIds = updated)
                        })
                        Text(label)
                    }
                }
                Text(
                    "Las diez fuentes españolas vienen incluidas en YOMI. " +
                        "Cada proveedor puede dejar de funcionar si cambia su estructura o limita el acceso.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        onApply(draft.copy(
                            year = yearText.toIntOrNull()?.takeIf { it in 1900..2100 },
                            translatedLanguage = "es", verifyChapters = true, page = 0
                        ))
                    },
                    modifier = Modifier.weight(1.4f)
                ) { Text("Aplicar filtros") }
            }
        }
    }
}

@Composable
private fun FilterHeading(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ExploreChoices(
    choices: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        choices.forEach { (value, label) ->
            FilterChip(selected = selected == value, onClick = { onSelect(value) },
                label = { Text(label) })
        }
    }
}
