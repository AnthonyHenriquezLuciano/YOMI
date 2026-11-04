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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.yomi.reader.core.*

/**
 * Pure native source selector. The reader APK contains all source adaptations.
 * No Android WebView, APK sideloading, or dynamic class loading.
 */
@Composable
fun BundledSourcesScreen(
    sources: List<MadaraNativeSource>,
    onBack: () -> Unit,
    onOpenSource: (String) -> Unit,
    onSearchAll: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val visible = sources.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true)
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
            Column(Modifier.weight(1f)) {
                Text("Fuentes integradas", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold)
                Text(sources.size.toString() + " adaptadores nativos · Español",
                    style = MaterialTheme.typography.labelSmall)
            }
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp)) {
                Text("Las fuentes se incluyen en esta APK y utilizan el lector de YOMI. " +
                    "No hay páginas web integradas ni extensiones que debas instalar. " +
                    "La disponibilidad depende de cada proveedor.",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onSearchAll, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Search, null)
                Spacer(Modifier.width(8.dp))
                Text("Buscar en todas las fuentes")
            }
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Buscar fuente") },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(visible, key = { it.id }) { source ->
                Surface(
                    onClick = { onOpenSource(source.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.CollectionsBookmark, null, Modifier.size(26.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(source.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (source.config.mixedContent)
                                    "Extensión adaptada · Catálogo mixto"
                                else "Extensión adaptada · Español",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("Búsqueda y lectura nativas · Sujeto a disponibilidad",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        }
    }
}
