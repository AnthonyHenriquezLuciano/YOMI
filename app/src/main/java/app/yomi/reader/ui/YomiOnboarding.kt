package app.yomi.reader.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.yomi.reader.R
import app.yomi.reader.core.BundledMadaraSources
import app.yomi.reader.core.ReadingMode
import app.yomi.reader.data.*

@Composable
fun YomiOnboarding(
    initial: YomiUiPreferences,
    onComplete: (YomiUiPreferences) -> Unit
) {
    var page by remember { mutableIntStateOf(0) }
    var settings by remember { mutableStateOf(initial) }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (page < 2) TextButton(onClick = {
                onComplete(settings.copy(onboardingCompleted = true))
            }) { Text("Omitir") }
        }
        Spacer(Modifier.weight(0.4f))
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(94.dp)
        ) {
            Image(painterResource(R.drawable.yomi_mark), "Logo de YOMI",
                Modifier.fillMaxSize().padding(6.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("YOMI", style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            repeat(3) { index ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (page == index) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.width(if (page == index) 28.dp else 7.dp).height(7.dp)
                ) {}
            }
        }
        Spacer(Modifier.height(24.dp))
        AnimatedContent(targetState = page, label = "Bienvenida", modifier = Modifier.weight(2f),
            transitionSpec = {
                androidx.compose.animation.fadeIn(animationSpec = tween(220)) togetherWith
                    androidx.compose.animation.fadeOut(animationSpec = tween(140))
            }) { step ->
            Column(Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (step) {
                    0 -> {
                        Text("Todo lo que quieres leer", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(
                            "Manga, manhwa y manhua en una sola biblioteca. " +
                                "Sin extensiones que instalar y con tus lecturas siempre a mano.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Elige cómo quieres ver YOMI",
                            fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            YomiTheme.entries.forEach { theme ->
                                FilterChip(
                                    selected = settings.theme == theme,
                                    onClick = { settings = settings.copy(theme = theme) },
                                    label = { Text(theme.label) }
                                )
                            }
                        }
                    }
                    1 -> {
                        Text("Tu manera de leer", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(
                            "El modo se adapta a la obra y puedes cambiarlo en cualquier capítulo.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Manga", fontWeight = FontWeight.SemiBold)
                        ReadingMode.entries.forEach { mode ->
                            FilterChip(selected = settings.defaultMangaMode == mode,
                                onClick = { settings = settings.copy(defaultMangaMode = mode) },
                                label = { Text(mode.label) })
                        }
                        Text("Manhwa y manhua", fontWeight = FontWeight.SemiBold)
                        ReadingMode.entries.forEach { mode ->
                            FilterChip(selected = settings.defaultWebtoonMode == mode,
                                onClick = { settings = settings.copy(defaultWebtoonMode = mode) },
                                label = { Text(mode.label) })
                        }
                    }
                    else -> {
                        Text("Tus fuentes, ya incluidas",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(
                            "Selecciona los catálogos que deseas consultar. Podrás modificarlos en Ajustes.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            val options = listOf("mangadex" to "MangaDex") +
                                BundledMadaraSources.configs.map {
                                    "mdr_" + it.id to it.title
                                }
                            options.forEach { (id, title) ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(checked = id in settings.sourceIds, onCheckedChange = {
                                        val updated = if (it) settings.sourceIds + id
                                        else settings.sourceIds - id
                                        if (updated.isNotEmpty())
                                            settings = settings.copy(sourceIds = updated)
                                    })
                                    Text(title, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Button(onClick = {
            if (page < 2) page++ else onComplete(settings.copy(onboardingCompleted = true))
        }, modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(15.dp)) {
            Text(if (page == 2) "Empezar a leer" else "Continuar")
            Spacer(Modifier.width(8.dp))
            Icon(if (page == 2) Icons.Default.Check else Icons.Default.ArrowForward, null)
        }
        if (page > 0) TextButton(onClick = { page-- }) { Text("Anterior") }
    }
}
