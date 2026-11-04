package app.yomi.reader.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.yomi.reader.core.Work
import app.yomi.reader.core.WorkFormat
import app.yomi.reader.data.LibraryItem
import coil3.compose.AsyncImage

@Composable
fun YomiHomeScreen(
    saved: List<LibraryItem>,
    discovered: List<Work>,
    loading: Boolean,
    animationsEnabled: Boolean = true,
    onOpen: (Work) -> Unit,
    onResume: (Work) -> Unit,
    onExplore: () -> Unit
) {
    val continuing = remember(saved) {
        saved.filter { it.lastReadAt > 0 }
            .sortedByDescending { it.lastReadAt }.take(10)
    }
    val recent = remember(saved) { saved.sortedByDescending { it.addedAt }.take(10) }
    val recommendation = remember(saved, discovered) {
        val favoriteFormats = saved.groupingBy { it.work.format }.eachCount()
        val favoriteTags = saved.flatMap { it.work.tags }.groupingBy { it.id }.eachCount()
        discovered.filterNot { work -> saved.any { it.work.key == work.key } }
            .sortedWith(compareByDescending<Work> { work ->
                favoriteFormats.getOrDefault(work.format, 0) +
                    work.tags.sumOf { favoriteTags.getOrDefault(it.id, 0) }
            }.thenBy { it.title }).take(12)
    }
    val sections = listOf(
        WorkFormat.MANGA to "Manga",
        WorkFormat.MANHWA to "Manhwa",
        WorkFormat.MANHUA to "Manhua"
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item("title") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("YOMI", style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black)
                    Text("Un mundo de historias para ti",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    onClick = onExplore,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Search, "Explorar")
                    }
                }
            }
        }
        val feature = discovered.firstOrNull()
        if (feature != null) item("feature") {
            FeaturedWork(feature, onClick = { onOpen(feature) })
        }
        if (continuing.isNotEmpty()) {
            item("continueHeader") {
                HomeHeading("Seguir leyendo", "Retoma tus historias donde las dejaste", onExplore)
            }
            item("continueItems") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(continuing, key = { it.work.key }) { item ->
                        ContinueCard(item, onClick = { onResume(item.work) })
                    }
                }
            }
        }
        if (saved.isNotEmpty()) {
            item("myLibrary") {
                HomeHeading("Mi colección", "Últimas obras que guardaste", onExplore)
            }
            item("recentBooks") {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recent, key = { it.work.key }) { item ->
                        WorkCard(item.work, { onOpen(item.work) }, Modifier.width(138.dp))
                    }
                }
            }
        }
        if (recommendation.isNotEmpty()) {
            item("recommendedTitle") {
                HomeHeading(
                    if (saved.isEmpty()) "Descubre nuevas historias" else "Para tu próxima lectura",
                    "Selección basada en tus obras y etiquetas disponibles",
                    onExplore
                )
            }
            item("recommended") {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recommendation, key = { it.key }) { work ->
                        WorkCard(work, { onOpen(work) }, Modifier.width(138.dp))
                    }
                }
            }
        }
        sections.forEach { (format, label) ->
            val titles = discovered.filter { it.format == format }.take(12)
            if (titles.isNotEmpty()) {
                item("head_" + format.name) {
                    HomeHeading(label, "Explora este formato", onExplore)
                }
                item("works_" + format.name) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(titles, key = { it.key }) { work ->
                            WorkCard(work, { onOpen(work) }, Modifier.width(138.dp))
                        }
                    }
                }
            }
        }
        if (loading && discovered.isEmpty()) {
            item("skeleton") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeHeading("Explorando el catálogo", "Preparando las obras disponibles", onExplore)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(5) { ShimmerWorkCard(Modifier.width(138.dp), animated = animationsEnabled) }
                    }
                }
            }
        }
        if (!loading && discovered.isEmpty() && saved.isEmpty()) {
            item("welcome") {
                Surface(
                    onClick = onExplore,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoStories, null, Modifier.size(36.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Tu próxima historia empieza aquí",
                                fontWeight = FontWeight.Bold)
                            Text("Busca obras y crea tu biblioteca personal.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ArrowForward, null)
                    }
                }
            }
        }
        item("footnote") {
            Text("Las sugerencias provienen de las fuentes habilitadas y de los datos disponibles.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 22.dp))
        }
    }
}

@Composable
private fun HomeHeading(title: String, subtitle: String, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onOpen) { Text("Explorar") }
    }
}

@Composable
private fun FeaturedWork(work: Work, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(205.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.onSurface
    ) {
        Box(Modifier.fillMaxSize()) {
            if (work.coverUrl.isNotBlank()) AsyncImage(
                model = work.coverUrl, contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(18.dp)
            )
            Box(Modifier.fillMaxSize().background(
                Brush.horizontalGradient(
                    listOf(Color.Black.copy(alpha = .96f),
                        Color.Black.copy(alpha = .78f), Color.Black.copy(alpha = .24f))
                )
            ))
            Row(
                Modifier.fillMaxSize().padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("UNA HISTORIA PARA DESCUBRIR",
                        color = Color.White.copy(alpha = .74f),
                        style = MaterialTheme.typography.labelSmall)
                    Text(work.title, color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black, maxLines = 3,
                        overflow = TextOverflow.Ellipsis)
                    Text(work.format.label + " · Español",
                        color = Color.White.copy(alpha = .80f),
                        style = MaterialTheme.typography.bodySmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ver obra", color = Color.White, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(5.dp))
                        Icon(Icons.Default.ArrowForward, null,
                            tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                WorkCover(work, Modifier.width(100.dp).height(148.dp))
            }
        }
    }
}

@Composable
private fun ContinueCard(item: LibraryItem, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(240.dp).height(126.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .70f),
        shape = RoundedCornerShape(17.dp)
    ) {
        Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            WorkCover(item.work, Modifier.width(76.dp).fillMaxHeight())
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.work.title, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 3,
                    overflow = TextOverflow.Ellipsis)
                Text("Seguir leyendo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ShimmerWorkCard(modifier: Modifier = Modifier, animated: Boolean = true) {
    val alpha = if (animated) {
        val transition = rememberInfiniteTransition(label = "Carga")
        val value by transition.animateFloat(
            initialValue = 0.35f, targetValue = 0.82f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "Brillo de carga"
        )
        value
    } else 0.60f
    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(.70f)
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)))
        Box(Modifier.fillMaxWidth(.92f).height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)))
        Box(Modifier.fillMaxWidth(.62f).height(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)))
    }
}
