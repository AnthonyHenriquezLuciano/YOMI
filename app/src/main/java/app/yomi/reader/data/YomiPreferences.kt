package app.yomi.reader.data

import android.content.Context
import app.yomi.reader.core.BundledMadaraSources
import app.yomi.reader.core.ReadingMode
import org.json.JSONArray
import org.json.JSONObject

enum class YomiTheme(val label: String) {
    SYSTEM("Automático"), LIGHT("Claro"), DARK("Oscuro")
}
data class YomiUiPreferences(
    val theme: YomiTheme = YomiTheme.SYSTEM,
    val keepScreenOn: Boolean = false,
    val tapZones: Boolean = true,
    val margins: Int = 0,
    val readerBrightness: Float = -1f,
    val defaultMangaMode: ReadingMode = ReadingMode.RTL,
    val defaultWebtoonMode: ReadingMode = ReadingMode.VERTICAL,
    val prefetch: Boolean = true,
    val downloadOnWifiOnly: Boolean = true,
    val showContentBadges: Boolean = true,
    val compactCovers: Boolean = false,
    val enableAnimations: Boolean = true,
    val sourceIds: Set<String> = defaultSources,
    val onboardingCompleted: Boolean = false
) {
    companion object {
        val defaultSources: Set<String> =
            setOf("mangadex", "local") + BundledMadaraSources.configs.map { "mdr_" + it.id }
    }
}

class YomiPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("yomi-ui-settings-v1", Context.MODE_PRIVATE)

    fun load(): YomiUiPreferences {
        val valid = YomiUiPreferences.defaultSources
        val selected = prefs.getStringSet("sourceIds", null)
        fun mode(key: String, default: ReadingMode): ReadingMode =
            ReadingMode.entries.firstOrNull { it.name == prefs.getString(key, "") } ?: default
        return YomiUiPreferences(
            theme = YomiTheme.entries.firstOrNull {
                it.name == prefs.getString("theme", YomiTheme.SYSTEM.name)
            } ?: YomiTheme.SYSTEM,
            keepScreenOn = prefs.getBoolean("keepScreenOn", false),
            tapZones = prefs.getBoolean("tapZones", true),
            margins = prefs.getInt("margins", 0).coerceIn(0, 24),
            readerBrightness = prefs.getFloat("readerBrightness", -1f).coerceIn(-1f, 1f),
            defaultMangaMode = mode("defaultMangaMode", ReadingMode.RTL),
            defaultWebtoonMode = mode("defaultWebtoonMode", ReadingMode.VERTICAL),
            prefetch = prefs.getBoolean("prefetch", true),
            downloadOnWifiOnly = prefs.getBoolean("downloadOnWifiOnly", true),
            showContentBadges = prefs.getBoolean("showContentBadges", true),
            compactCovers = prefs.getBoolean("compactCovers", false),
            enableAnimations = prefs.getBoolean("enableAnimations", true),
            sourceIds = (selected ?: valid).intersect(valid).ifEmpty { setOf("mangadex") },
            onboardingCompleted = prefs.getBoolean("onboardingCompleted", false)
        )
    }
    fun save(value: YomiUiPreferences) {
        prefs.edit()
            .putString("theme", value.theme.name)
            .putBoolean("keepScreenOn", value.keepScreenOn)
            .putBoolean("tapZones", value.tapZones)
            .putInt("margins", value.margins.coerceIn(0, 24))
            .putFloat("readerBrightness", value.readerBrightness.coerceIn(-1f, 1f))
            .putString("defaultMangaMode", value.defaultMangaMode.name)
            .putString("defaultWebtoonMode", value.defaultWebtoonMode.name)
            .putBoolean("prefetch", value.prefetch)
            .putBoolean("downloadOnWifiOnly", value.downloadOnWifiOnly)
            .putBoolean("showContentBadges", value.showContentBadges)
            .putBoolean("compactCovers", value.compactCovers)
            .putBoolean("enableAnimations", value.enableAnimations)
            .putStringSet("sourceIds", value.sourceIds)
            .putBoolean("onboardingCompleted", value.onboardingCompleted)
            .apply()
    }
}
