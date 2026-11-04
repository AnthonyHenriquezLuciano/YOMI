package app.yomi.reader.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.yomi.reader.core.*

@Composable
fun ExploreV2Screen(
    query: String, onQuery: (String) -> Unit, results: List<Work>,
    loading: Boolean, error: String, filters: SearchFilters,
    availableTags: List<CatalogTag>, tagsError: String, onRetryTags: () -> Unit,
    hasMore: Boolean, onLoadMore: () -> Unit,
    onSearch: () -> Unit, onFiltersChanged: (SearchFilters) -> Unit,
    onOpen: (Work) -> Unit
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
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Explorar", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold)
                    Text("Solo obras con capítulos en español",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("YOMI Global", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedTextField(
                value = query, onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text("Buscar título o nombre alternativo...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotBlank()) IconButton(onClick = { onQuery("") }) {
                        Icon(Icons.Default.Close, "Limpiar")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() })
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onSearch, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Buscar")
                }
                OutlinedButton(onClick = { filtersOpen = true }) {
                    Icon(Icons.Default.Tune, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Filtros" + if (filters.activeCount() > 0)
                        " (" + filters.activeCount() + ")" else "")
                }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    null to "Todos", WorkFormat.MANGA to "Manga",
                    WorkFormat.MANHWA to "Manhwa", WorkFormat.MANHUA to "Manhua"
                ).forEach { (format, label) ->
                    FilterChip(
                        selected = filters.format == format,
                        onClick = { onFiltersChanged(filters.copy(format = format)) },
                        label = { Text(label) }
                    )
                }
            }
            if (filters.includedTags.isNotEmpty() || filters.excludedTags.isNotEmpty()) {
                val names = availableTags.associateBy { it.id }
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    filters.includedTags.forEach { id ->
                        InputChip(
                            selected = true,
                            onClick = { onFiltersChanged(filters.copy(includedTags =
                                filters.includedTags - id)) },
                            label = { Text(names[id]?.label ?: "Etiqueta") },
                            trailingIcon = { Icon(Icons.Default.Close, "Eliminar", Modifier.size(14.dp)) }
                        )
                    }
                    filters.excludedTags.forEach { id ->
                        InputChip(
                            selected = true,
                            onClick = { onFiltersChanged(filters.copy(excludedTags =
                                filters.excludedTags - id)) },
                            label = { Text("Sin " + (names[id]?.label ?: "etiqueta")) },
                            trailingIcon = { Icon(Icons.Default.Close, "Eliminar", Modifier.size(14.dp)) }
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Catálogo en español",
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(results.size.toString() + " títulos", style = MaterialTheme.typography.labelSmall)
            }
            if (loading && results.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
        if (!loading && results.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                Text("No se encontraron obras con capítulos en español.\n" +
                    "Prueba otra búsqueda o modifica los filtros.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(135.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                gridItems(results, key = { it.key }) { work ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        WorkCard(work, { onOpen(work) })
                        if (work.chapterCount > 0) Text(
                            work.chapterCount.toString() + " capítulos en español",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (hasMore || (loading && results.isNotEmpty())) item(
                    span = { GridItemSpan(maxLineSpan) }
                ) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center) {
                        if (loading) CircularProgressIndicator()
                        else OutlinedButton(onClick = onLoadMore) {
                            Text("Cargar más resultados")
                        }
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
                listOf("mangadex" to "MangaDex · Español",
                    "local" to "Mis archivos CBZ").forEach { (id, label) ->
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
                    "Los repositorios de extensiones aparecen en Ajustes. " +
                        "No se consideran fuentes de lectura hasta tener un conector verificado.",
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
