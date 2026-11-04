package app.yomi.reader.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.yomi.reader.data.*
import app.yomi.reader.core.Work

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryV2Screen(
    library: List<LibraryItem>,
    categories: List<String>,
    appearance: LibraryAppearance,
    onAppearance: (LibraryAppearance) -> Unit,
    revision: Int,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onOpen: (Work) -> Unit,
    onRandom: () -> Unit,
    onHistory: () -> Unit,
    onContinue: (Work) -> Unit,
    onFavorite: (Work) -> Unit,
    onShelf: (Work, ReadingShelf) -> Unit,
    onCategories: (Work) -> Unit
) {
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    var activeCategory by remember { mutableStateOf<String?>(null) }
    var randomSeed by remember { mutableIntStateOf(0) }
    val selected = remember(library, appearance, query, activeCategory, randomSeed, revision) {
        selectLibrary(library, query, appearance, activeCategory, randomSeed)
    }
    val count = selected.sumOf { it.items.size }
    if (sheet) {
        ModalBottomSheet(onDismissRequest = { sheet = false }) {
            LibraryFilterSheet(
                appearance = appearance,
                onAppearance = onAppearance,
                onShuffle = { randomSeed++ }
            )
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 10.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Biblioteca", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
                Text(count.toString() + " obras",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { searchVisible = !searchVisible; if (!searchVisible) query = "" }) {
                Icon(Icons.Default.Search, "Buscar en la biblioteca")
            }
            IconButton(onClick = { sheet = true }) {
                Icon(Icons.Default.Tune, "Filtrar y ordenar")
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, "Opciones de biblioteca")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Actualizar biblioteca") },
                        leadingIcon = { Icon(Icons.Default.Refresh, null) },
                        onClick = { menu = false; onRefresh() }
                    )
                    DropdownMenuItem(
                        text = { Text("Abrir un elemento al azar") },
                        leadingIcon = { Icon(Icons.Default.Shuffle, null) },
                        onClick = { menu = false; onRandom() },
                        enabled = library.isNotEmpty()
                    )
                    DropdownMenuItem(
                        text = { Text("Historial") },
                        leadingIcon = { Icon(Icons.Default.History, null) },
                        onClick = { menu = false; onHistory() }
                    )
                }
            }
        }
        if (searchVisible) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text("Buscar entre tus obras...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Limpiar") }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
        }
        if (appearance.showCategoryTabs && categories.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tabs = listOf<String?>(null) + categories
                tabs.forEach { category ->
                    val label = category ?: "Todas"
                    val total = if (category == null) library.size
                        else library.count { category in it.categories }
                    FilterChip(
                        selected = activeCategory == category,
                        onClick = { activeCategory = category },
                        label = {
                            Text(label + if (appearance.showCounts) " (" + total + ")" else "")
                        }
                    )
                }
            }
        }
        if (refreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (selected.all { it.items.isEmpty() }) {
            Column(
                Modifier.fillMaxSize().padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.CollectionsBookmark, null, Modifier.size(46.dp))
                Spacer(Modifier.height(12.dp))
                Text(if (library.isEmpty()) "Tu biblioteca está vacía"
                    else "No hay resultados para estos filtros",
                    style = MaterialTheme.typography.titleMedium)
                Text(
                    if (library.isEmpty()) "Añade una obra desde Explorar para comenzar."
                    else "Prueba otra categoría, búsqueda o filtro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (library.isNotEmpty()) TextButton(onClick = {
                    onAppearance(LibraryAppearance(
                        view = appearance.view, columns = appearance.columns,
                        showCategoryTabs = appearance.showCategoryTabs))
                    activeCategory = null
                    query = ""
                }) { Text("Restablecer filtros") }
            }
        } else if (appearance.view == LibraryView.LIST) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                selected.forEach { section ->
                    if (section.title.isNotBlank()) item(key = "head_" + section.title) {
                        GroupHeading(section.title, section.items.size, appearance.showCounts)
                    }
                    items(section.items, key = { it.work.key }) { item ->
                        LibraryRow(
                            item, appearance,
                            onOpen = { onOpen(item.work) },
                            onContinue = { onContinue(item.work) },
                            onFavorite = { onFavorite(item.work) },
                            onShelf = { onShelf(item.work, it) },
                            onCategories = { onCategories(item.work) }
                        )
                    }
                }
            }
        } else {
            val cols = if (appearance.view == LibraryView.WIDE) 2
                else appearance.columns.coerceIn(2, 6)
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                selected.forEach { section ->
                    if (section.title.isNotBlank()) item(
                        key = "group_" + section.title, span = { GridItemSpan(maxLineSpan) }
                    ) {
                        GroupHeading(section.title, section.items.size, appearance.showCounts)
                    }
                    gridItems(section.items, key = { it.work.key }) { item ->
                        LibraryTile(
                            item, appearance,
                            onOpen = { onOpen(item.work) },
                            onContinue = { onContinue(item.work) },
                            onFavorite = { onFavorite(item.work) },
                            onShelf = { onShelf(item.work, it) },
                            onCategories = { onCategories(item.work) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeading(name: String, count: Int, showCount: Boolean) {
    Text(
        name + if (showCount) "  ·  " + count else "",
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryTile(
    item: LibraryItem,
    settings: LibraryAppearance,
    onOpen: () -> Unit,
    onContinue: () -> Unit,
    onFavorite: () -> Unit,
    onShelf: (ReadingShelf) -> Unit,
    onCategories: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.fillMaxWidth()
                .aspectRatio(0.69f)
                .clip(RoundedCornerShape(10.dp))
                .combinedClickable(onClick = onOpen, onLongClick = { menu = true })
        ) {
            WorkCover(item.work, Modifier.fillMaxSize())
            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (settings.badgeUnread && item.unread > 0) LabelBadge(item.unread.toString() + " sin leer")
                if (settings.badgeDownloads && item.downloaded > 0)
                    LabelBadge(item.downloaded.toString() + " ↓")
                if (settings.badgeLocal && item.work.sourceId == "local")
                    LabelBadge("Local")
                if (settings.badgeLanguage) LabelBadge("Español")
                if (item.favorite) LabelBadge("★")
            }
            Box(Modifier.align(Alignment.BottomEnd)) {
                IconButton(
                    onClick = { menu = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.MoreVert, "Acciones de obra",
                        tint = Color.White)
                }
                WorkActions(menu, { menu = false }, onFavorite, item.favorite,
                    onShelf, onCategories)
            }
        }
        if (settings.view != LibraryView.COVERS) {
            Text(
                item.work.title, maxLines = if (settings.view == LibraryView.WIDE) 3 else 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (settings.showContinue && item.lastReadAt > 0) {
            TextButton(onClick = onContinue, contentPadding = PaddingValues(0.dp),
                modifier = Modifier.heightIn(min = 28.dp)) {
                Text("Seguir leyendo", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LibraryRow(
    item: LibraryItem,
    settings: LibraryAppearance,
    onOpen: () -> Unit,
    onContinue: () -> Unit,
    onFavorite: () -> Unit,
    onShelf: (ReadingShelf) -> Unit,
    onCategories: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        WorkCover(item.work, Modifier.width(74.dp).height(100.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.work.title, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(item.shelf.label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            val overlays = buildList {
                if (settings.badgeUnread && item.unread > 0) add(item.unread.toString() + " sin leer")
                if (settings.badgeDownloads && item.downloaded > 0) add(item.downloaded.toString() + " descargados")
                if (settings.badgeLocal && item.work.sourceId == "local") add("Local")
                if (settings.badgeLanguage) add("Español")
            }
            if (overlays.isNotEmpty()) Text(
                overlays.joinToString(" · "), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (settings.showContinue && item.lastReadAt > 0) {
                TextButton(onClick = onContinue, contentPadding = PaddingValues(0.dp)) {
                    Text("Seguir leyendo")
                }
            }
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Default.MoreVert, "Acciones de obra")
            }
            WorkActions(menu, { menu = false }, onFavorite, item.favorite, onShelf, onCategories)
        }
    }
    HorizontalDivider()
}

@Composable
private fun LabelBadge(label: String) {
    Surface(color = Color.Black.copy(alpha = 0.82f), shape = RoundedCornerShape(6.dp)) {
        Text(
            label, color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun WorkActions(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onFavorite: () -> Unit,
    isFavorite: Boolean,
    onShelf: (ReadingShelf) -> Unit,
    onCategories: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(if (isFavorite) "Quitar de favoritos" else "Marcar favorito") },
            leadingIcon = { Icon(Icons.Default.Star, null) },
            onClick = { onDismiss(); onFavorite() }
        )
        DropdownMenuItem(
            text = { Text("Organizar categorías") },
            leadingIcon = { Icon(Icons.Default.FolderOpen, null) },
            onClick = { onDismiss(); onCategories() }
        )
        ReadingShelf.entries.forEach { shelf ->
            DropdownMenuItem(
                text = { Text("Estado: " + shelf.label) },
                onClick = { onDismiss(); onShelf(shelf) }
            )
        }
    }
}

@Composable
private fun LibraryFilterSheet(
    appearance: LibraryAppearance,
    onAppearance: (LibraryAppearance) -> Unit,
    onShuffle: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Personalizar biblioteca", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        HorizontalDivider()
        Text("Filtrar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        SwitchSetting("Descargados", appearance.onlyDownloaded) {
            onAppearance(appearance.copy(onlyDownloaded = it))
        }
        SwitchSetting("Sin leer", appearance.onlyUnread) {
            onAppearance(appearance.copy(onlyUnread = it))
        }
        SwitchSetting("Empezados", appearance.onlyStarted) {
            onAppearance(appearance.copy(onlyStarted = it))
        }
        SwitchSetting("Favoritos", appearance.onlyFavorite) {
            onAppearance(appearance.copy(onlyFavorite = it))
        }
        SwitchSetting("Completados", appearance.onlyCompleted) {
            onAppearance(appearance.copy(onlyCompleted = it))
        }
        HorizontalDivider()
        Text("Ordenar por", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LibrarySort.entries.forEach { sort ->
            Row(
                Modifier.fillMaxWidth().clickable {
                    onAppearance(appearance.copy(sort = sort))
                    if (sort == LibrarySort.RANDOM) onShuffle()
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = appearance.sort == sort, onClick = {
                    onAppearance(appearance.copy(sort = sort))
                    if (sort == LibrarySort.RANDOM) onShuffle()
                })
                Text(sort.label, style = MaterialTheme.typography.bodyMedium)
            }
        }
        SwitchSetting("Orden descendente", appearance.descending) {
            onAppearance(appearance.copy(descending = it))
        }
        HorizontalDivider()
        Text("Apariencia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Modo de visualización", style = MaterialTheme.typography.labelLarge)
        LibraryView.entries.forEach { view ->
            Row(
                Modifier.fillMaxWidth().clickable { onAppearance(appearance.copy(view = view)) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = appearance.view == view, onClick = {
                    onAppearance(appearance.copy(view = view))
                })
                Text(view.label, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text("Títulos por fila · " + appearance.columns,
            style = MaterialTheme.typography.labelLarge)
        Slider(
            value = appearance.columns.toFloat(),
            onValueChange = { onAppearance(appearance.copy(columns = it.toInt().coerceIn(2, 6))) },
            valueRange = 2f..6f, steps = 3,
            enabled = appearance.view == LibraryView.COMPACT ||
                appearance.view == LibraryView.COVERS
        )
        Text("Superposición", style = MaterialTheme.typography.labelLarge)
        SwitchSetting("Capítulos descargados", appearance.badgeDownloads) {
            onAppearance(appearance.copy(badgeDownloads = it))
        }
        SwitchSetting("Capítulos por leer", appearance.badgeUnread) {
            onAppearance(appearance.copy(badgeUnread = it))
        }
        SwitchSetting("Contenido local", appearance.badgeLocal) {
            onAppearance(appearance.copy(badgeLocal = it))
        }
        SwitchSetting("Idioma", appearance.badgeLanguage) {
            onAppearance(appearance.copy(badgeLanguage = it))
        }
        SwitchSetting("Mostrar el botón de seguir leyendo", appearance.showContinue) {
            onAppearance(appearance.copy(showContinue = it))
        }
        HorizontalDivider()
        Text("Pestañas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        SwitchSetting("Mostrar pestañas de categorías", appearance.showCategoryTabs) {
            onAppearance(appearance.copy(showCategoryTabs = it))
        }
        SwitchSetting("Mostrar el número de elementos", appearance.showCounts) {
            onAppearance(appearance.copy(showCounts = it))
        }
        HorizontalDivider()
        Text("Grupo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        LibraryGroup.entries.forEach { group ->
            SwitchSetting(group.label, appearance.group == group) { enabled ->
                if (enabled || group == LibraryGroup.NONE)
                    onAppearance(appearance.copy(group = group))
                else if (appearance.group == group)
                    onAppearance(appearance.copy(group = LibraryGroup.NONE))
            }
        }
        Text(
            "Los recuentos de capítulos dependen de la última actualización de cada obra.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun SwitchSetting(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChecked(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPicker(
    work: Work,
    existing: Set<String>,
    categories: List<String>,
    alreadySaved: Boolean,
    onAddCategory: (String) -> Boolean,
    onSave: (Set<String>) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember(work.key, existing) { mutableStateOf(existing) }
    var newName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(if (alreadySaved) "Organizar en categorías" else "Añadir a la biblioteca",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(work.title, maxLines = 2,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Selecciona una o varias categorías, o guarda sin categoría.",
                style = MaterialTheme.typography.bodySmall)
            categories.forEach { category ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        selected = if (category in selected) selected - category
                            else selected + category
                    }, verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = category in selected,
                        onCheckedChange = {
                            selected = if (it) selected + category else selected - category
                        }
                    )
                    Text(category)
                }
            }
            OutlinedTextField(
                value = newName, onValueChange = { newName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nueva categoría") },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        enabled = newName.isNotBlank(),
                        onClick = {
                            val value = newName.trim()
                            if (onAddCategory(value)) {
                                selected += value
                                newName = ""
                                message = ""
                            } else message = "Categoría repetida o nombre no válido"
                        }
                    ) { Icon(Icons.Default.Add, "Crear categoría") }
                }
            )
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
            Button(onClick = { onSave(selected); onDismiss() },
                modifier = Modifier.fillMaxWidth()) {
                Text(if (alreadySaved) "Guardar categorías" else "Añadir a la biblioteca")
            }
            if (alreadySaved) TextButton(
                onClick = { onRemove(); onDismiss() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DeleteOutline, null)
                Spacer(Modifier.width(8.dp))
                Text("Quitar de la biblioteca", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
fun CategoriesSettingsScreen(
    categories: List<String>,
    onBack: () -> Unit,
    onAdd: (String) -> Boolean,
    onRename: (String, String) -> Boolean,
    onDelete: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var confirmDeletion by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf("") }
    if (renameTarget != null) AlertDialog(
        onDismissRequest = { renameTarget = null },
        title = { Text("Renombrar categoría") },
        text = {
            OutlinedTextField(value = renameText, onValueChange = { renameText = it },
                label = { Text("Nombre") }, singleLine = true)
        },
        confirmButton = { TextButton(onClick = {
            if (onRename(renameTarget.orEmpty(), renameText)) {
                renameTarget = null; error = ""
            } else error = "Nombre repetido o inválido"
        }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancelar") } }
    )
    if (confirmDeletion != null) AlertDialog(
        onDismissRequest = { confirmDeletion = null },
        title = { Text("Eliminar categoría") },
        text = { Text("Las obras permanecerán en tu biblioteca, pero perderán esta categoría.") },
        confirmButton = { TextButton(onClick = {
            confirmDeletion?.let(onDelete)
            confirmDeletion = null
        }) { Text("Eliminar") } },
        dismissButton = { TextButton(onClick = { confirmDeletion = null }) { Text("Cancelar") } }
    )
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
            Text("Categorías", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Crea categorías para organizar tus lecturas.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Nombre de la categoría") }
            )
            Button(
                onClick = {
                    if (onAdd(text)) {
                        text = ""; error = ""
                    } else error = "El nombre ya existe o no es válido"
                },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Añadir categoría")
            }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            HorizontalDivider()
            categories.forEach { category ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderOpen, null)
                    Spacer(Modifier.width(12.dp))
                    Text(category, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    IconButton(onClick = {
                        renameTarget = category; renameText = category
                    }) { Icon(Icons.Default.Edit, "Renombrar") }
                    IconButton(onClick = { confirmDeletion = category }) {
                        Icon(Icons.Default.DeleteOutline, "Eliminar")
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun ReadingHistoryScreen(
    items: List<LibraryItem>,
    onBack: () -> Unit,
    onResume: (Work) -> Unit
) {
    val history = items.filter { it.lastReadAt > 0 }.sortedByDescending { it.lastReadAt }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
            Text("Historial de lectura", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
        }
        if (history.isEmpty()) Box(
            Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            Text("Aún no hay lecturas registradas.")
        } else LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            items(history, key = { it.work.key }) { item ->
                Row(
                    Modifier.fillMaxWidth().clickable { onResume(item.work) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WorkCover(item.work, Modifier.width(68.dp).height(95.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.work.title, maxLines = 2, fontWeight = FontWeight.SemiBold)
                        Text("Última lectura: " +
                            java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("es"))
                                .format(java.util.Date(item.lastReadAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Seguir leyendo", style = MaterialTheme.typography.labelMedium)
                    }
                    Icon(Icons.Default.ChevronRight, null)
                }
                HorizontalDivider()
            }
        }
    }
}
