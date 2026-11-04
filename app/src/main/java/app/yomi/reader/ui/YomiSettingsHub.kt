package app.yomi.reader.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import app.yomi.reader.R
import app.yomi.reader.core.BundledMadaraSources
import app.yomi.reader.core.ReadingMode
import app.yomi.reader.data.*

@Composable
fun YomiSettingsHub(
    settings: YomiUiPreferences,
    onChange: (YomiUiPreferences) -> Unit,
    libraryCount: Int,
    downloadCount: Int,
    onCategories: () -> Unit,
    onImportCbz: () -> Unit,
    onSources: () -> Unit,
    onHistory: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onClearCache: () -> Unit,
    onOpenGitHub: () -> Unit,
    onShowUpdates: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf("appearance") }
    var showReset by remember { mutableStateOf(false) }
    var showCache by remember { mutableStateOf(false) }
    if (showReset) AlertDialog(
        onDismissRequest = { showReset = false },
        title = { Text("Restablecer preferencias") },
        text = { Text("Solo se restablecerán las preferencias visuales y de lectura. No se borrarán obras ni categorías.") },
        confirmButton = {
            TextButton(onClick = {
                onChange(YomiUiPreferences(onboardingCompleted = true))
                showReset = false
            }) { Text("Restablecer") }
        },
        dismissButton = { TextButton(onClick = { showReset = false }) { Text("Cancelar") } }
    )
    if (showCache) AlertDialog(
        onDismissRequest = { showCache = false },
        title = { Text("Vaciar caché temporal") },
        text = { Text("Se limpiarán archivos temporales del dispositivo. Las obras descargadas y la biblioteca se conservarán.") },
        confirmButton = {
            TextButton(onClick = { onClearCache(); showCache = false }) { Text("Limpiar") }
        },
        dismissButton = { TextButton(onClick = { showCache = false }) { Text("Cancelar") } }
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item("hero") {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(82.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.yomi_mark),
                        contentDescription = "Logotipo de YOMI",
                        modifier = Modifier.fillMaxSize().padding(5.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("YOMI", style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black)
                Text("Tu universo de manga, manhwa y manhua",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(7.dp))
                Text("VERSION 0.8.0", style = MaterialTheme.typography.labelSmall,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(1.2f, androidx.compose.ui.unit.TextUnitType.Sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SettingsStat(libraryCount.toString(), "En biblioteca")
                        VerticalDivider(Modifier.height(36.dp))
                        SettingsStat(downloadCount.toString(), "Descargados")
                        VerticalDivider(Modifier.height(36.dp))
                        SettingsStat(BundledMadaraSources.configs.size.toString(), "Fuentes nativas")
                    }
                }
            }
        }
        item("intro") {
            Text("PREFERENCIAS", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 10.dp, top = 10.dp))
        }
        item("appearance") {
            SettingsPanel(
                title = "Apariencia",
                subtitle = "Tema, portadas y estilo de interfaz",
                symbol = { Icon(Icons.Default.Palette, null) },
                open = expanded == "appearance",
                onToggle = { expanded = if (expanded == "appearance") "" else "appearance" }
            ) {
                Text("Tema de la aplicación", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    YomiTheme.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.theme == mode,
                            onClick = { onChange(settings.copy(theme = mode)) },
                            label = { Text(mode.label) }
                        )
                    }
                }
                PanelToggle("Portadas compactas", "Muestra más obras por pantalla",
                    settings.compactCovers) { onChange(settings.copy(compactCovers = it)) }
                PanelToggle("Etiquetas en las tarjetas", "Formato, capítulos e idioma",
                    settings.showContentBadges) { onChange(settings.copy(showContentBadges = it)) }
                PanelToggle("Animaciones suaves", "Transiciones y estados visuales",
                    settings.enableAnimations) { onChange(settings.copy(enableAnimations = it)) }
            }
        }
        item("reader") {
            SettingsPanel(
                title = "Lectura",
                subtitle = "Gestos, márgenes y modos de visualización",
                symbol = { Icon(Icons.Default.MenuBook, null) },
                open = expanded == "reader",
                onToggle = { expanded = if (expanded == "reader") "" else "reader" }
            ) {
                Text("Manga · modo inicial",
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                ReaderModeOptions(settings.defaultMangaMode) {
                    onChange(settings.copy(defaultMangaMode = it))
                }
                Text("Manhwa y manhua · modo inicial",
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                ReaderModeOptions(settings.defaultWebtoonMode) {
                    onChange(settings.copy(defaultWebtoonMode = it))
                }
                PanelToggle("Toque lateral para pasar página",
                    "Centro para mostrar y ocultar controles", settings.tapZones) {
                    onChange(settings.copy(tapZones = it))
                }
                PanelToggle("Mantener pantalla encendida",
                    "Solo mientras se lee", settings.keepScreenOn) {
                    onChange(settings.copy(keepScreenOn = it))
                }
                PanelToggle("Precarga de imágenes", "Carga la página siguiente anticipadamente",
                    settings.prefetch) { onChange(settings.copy(prefetch = it)) }
                Text("Márgenes del lector · " + settings.margins + " dp",
                    style = MaterialTheme.typography.labelMedium)
                Slider(value = settings.margins.toFloat(),
                    onValueChange = { onChange(settings.copy(margins = it.toInt().coerceIn(0, 24))) },
                    valueRange = 0f..24f, steps = 5)
                PanelToggle("Brillo del sistema",
                    "Desactiva para establecer un brillo propio",
                    settings.readerBrightness < 0f) {
                    onChange(settings.copy(readerBrightness = if (it) -1f else 0.60f))
                }
                if (settings.readerBrightness >= 0) {
                    Slider(
                        value = settings.readerBrightness,
                        onValueChange = { onChange(settings.copy(readerBrightness = it)) },
                        valueRange = 0.05f..1f
                    )
                }
            }
        }
        item("library") {
            SettingsPanel(
                title = "Biblioteca",
                subtitle = "Categorías, historial y organización",
                symbol = { Icon(Icons.Default.CollectionsBookmark, null) },
                open = expanded == "library",
                onToggle = { expanded = if (expanded == "library") "" else "library" }
            ) {
                PanelLink("Gestionar categorías", Icons.Default.FolderOpen, onCategories)
                PanelLink("Historial de lectura", Icons.Default.History, onHistory)
                PanelLink("Importar archivo CBZ", Icons.Default.FileOpen, onImportCbz)
                Text("Al añadir una obra podrás elegir sus categorías.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item("sources") {
            SettingsPanel(
                title = "Fuentes",
                subtitle = "Todos los conectores incluidos en el APK",
                symbol = { Icon(Icons.Default.Hub, null) },
                open = expanded == "sources",
                onToggle = { expanded = if (expanded == "sources") "" else "sources" }
            ) {
                PanelLink("Ver y buscar fuentes integradas", Icons.Default.Search, onSources)
                Text("Selecciona las fuentes para el catálogo global.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                SourcePreferenceRow("MangaDex", "mangadex", settings, onChange)
                BundledMadaraSources.configs.forEach { config ->
                    SourcePreferenceRow(config.title, "mdr_" + config.id, settings, onChange)
                }
                SourcePreferenceRow("Archivos locales", "local", settings, onChange)
            }
        }
        item("downloads") {
            SettingsPanel(
                title = "Descargas y almacenamiento",
                subtitle = "Conexión y archivos temporales",
                symbol = { Icon(Icons.Default.Download, null) },
                open = expanded == "downloads",
                onToggle = { expanded = if (expanded == "downloads") "" else "downloads" }
            ) {
                PanelToggle(
                    "Descargar solo por Wi-Fi",
                    "Evita utilizar datos móviles en descargas por lotes",
                    settings.downloadOnWifiOnly
                ) { onChange(settings.copy(downloadOnWifiOnly = it)) }
                PanelLink("Vaciar caché temporal", Icons.Default.CleaningServices) {
                    showCache = true
                }
                Text("Las descargas completas se almacenan en el espacio privado de YOMI.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item("backup") {
            SettingsPanel(
                title = "Datos y respaldo",
                subtitle = "Exportar, restaurar o reiniciar",
                symbol = { Icon(Icons.Default.Backup, null) },
                open = expanded == "backup",
                onToggle = { expanded = if (expanded == "backup") "" else "backup" }
            ) {
                PanelLink("Exportar copia de seguridad", Icons.Default.UploadFile, onExportBackup)
                PanelLink("Importar copia de seguridad", Icons.Default.Restore, onImportBackup)
                PanelLink("Restablecer preferencias", Icons.Default.RestartAlt) {
                    showReset = true
                }
                Text("El respaldo conserva biblioteca, historial, categorías y preferencias. " +
                    "No incluye imágenes de capítulos descargados.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item("about") {
            SettingsPanel(
                title = "Acerca de YOMI",
                subtitle = "Versión, cambios y atribuciones",
                symbol = { Icon(Icons.Default.Info, null) },
                open = expanded == "about",
                onToggle = { expanded = if (expanded == "about") "" else "about" }
            ) {
                PanelLink("Novedades de esta versión", Icons.Default.NewReleases, onShowUpdates)
                PanelLink("Código fuente en GitHub", Icons.Default.Code, onOpenGitHub)
                Text(
                    "YOMI es gratuito y sin anuncios. No está afiliado a las fuentes. " +
                        "Las obras pertenecen a sus creadores y titulares. El acceso está " +
                        "sujeto a la disponibilidad y las condiciones de cada proveedor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Text("Creado para leer. Diseñado para disfrutar.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 18.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun SettingsStat(value: String, caption: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(caption, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsPanel(
    title: String,
    subtitle: String,
    symbol: @Composable () -> Unit,
    open: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle)
                    .padding(start = 15.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) { symbol() }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Icon(if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    if (open) "Contraer" else "Ampliar")
            }
            androidx.compose.animation.AnimatedVisibility(visible = open) {
                Column(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    content()
                }
            }
        }
    }
}

@Composable
private fun PanelToggle(
    title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }
        .padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun PanelLink(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(title, modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium)
        Icon(Icons.Default.ChevronRight, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReaderModeOptions(selected: ReadingMode, onChange: (ReadingMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ReadingMode.entries.forEach { mode ->
            Row(Modifier.fillMaxWidth().clickable { onChange(mode) }
                .heightIn(min = 38.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected == mode, onClick = { onChange(mode) },
                    modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(6.dp))
                Text(mode.label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SourcePreferenceRow(
    title: String, id: String, prefs: YomiUiPreferences,
    onChange: (YomiUiPreferences) -> Unit
) {
    val enabled = id in prefs.sourceIds
    Row(Modifier.fillMaxWidth().heightIn(min = 36.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall)
        Checkbox(
            checked = enabled,
            onCheckedChange = { checked ->
                val chosen = if (checked) prefs.sourceIds + id else prefs.sourceIds - id
                if (chosen.isNotEmpty()) onChange(prefs.copy(sourceIds = chosen))
            }
        )
    }
}
