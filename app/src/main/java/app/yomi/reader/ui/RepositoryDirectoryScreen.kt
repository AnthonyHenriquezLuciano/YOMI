package app.yomi.reader.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import app.yomi.reader.core.*
import kotlinx.coroutines.launch

@Composable
fun RepositoryDirectoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val directory = remember { ExtensionRepositoryDirectory(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var listings by remember { mutableStateOf<List<RepositoryListing>>(emptyList()) }
    var refreshing by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    fun refresh() {
        scope.launch {
            refreshing = true
            try { listings = directory.refresh() }
            finally { refreshing = false }
        }
    }
    LaunchedEffect(Unit) { refresh() }
    val sources = listings.flatMap { it.sources }
        .filter { ExtensionRepositoryDirectory.isSpanish(it.language) }
        .filter {
            query.isBlank() || it.name.contains(query, true) ||
                it.repository.contains(query, true) || it.language.contains(query, true)
        }
        .distinctBy { it.repository + it.name + it.language + it.originalUrl }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Volver")
            }
            Text("Repositorios", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { refresh() }, enabled = !refreshing) {
                Icon(Icons.Default.Refresh, "Actualizar índices")
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Directorio de extensiones", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Mostramos únicamente las fuentes etiquetadas en español. " +
                            "Los índices pertenecen a repositorios de Mihon/Tachiyomi. " +
                            "Estas extensiones NO están instaladas ni son compatibles automáticamente " +
                            "con el lector de YOMI. Solo se importan sus nombres e idiomas.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(listings.size.toString() + " repositorios",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = { showAdd = !showAdd }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Añadir índice")
                }
            }
            if (showAdd) {
                OutlinedTextField(
                    value = urlInput, onValueChange = { urlInput = it },
                    label = { Text("URL del índice JSON de GitHub") },
                    placeholder = { Text("https://raw.githubusercontent.com/.../index.min.json") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                Button(onClick = {
                    try {
                        directory.add(urlInput)
                        urlInput = ""
                        showAdd = false
                        message = "Índice guardado"
                        refresh()
                    } catch (e: Exception) {
                        message = e.message ?: "Enlace inválido"
                    }
                }) { Text("Guardar repositorio") }
            }
            if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
            listings.forEach { listing ->
                Row(
                    Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(listing.repository.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (listing.error == null)
                                listing.sources.size.toString() + " fuentes indexadas"
                            else "Error: " + listing.error,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (listing.error != null) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!listing.repository.builtIn) {
                        TextButton(onClick = {
                            directory.remove(listing.repository.url)
                            refresh()
                        }) { Text("Quitar") }
                    }
                }
            }
            HorizontalDivider()
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                label = { Text("Filtrar fuentes indexadas") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Text("Solo fuentes en español",
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.labelLarge)
            Text(sources.size.toString() + " entradas de directorio",
                style = MaterialTheme.typography.labelMedium)
            if (refreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(sources) { source ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Extension, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(source.name, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text(source.repository + " · " + source.language,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Solo índice · Requiere adaptador compatible",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
