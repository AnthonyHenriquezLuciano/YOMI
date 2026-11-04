package app.yomi.reader.core

import java.net.URI

/**
 * Connections built into YOMI. One native API; nine official reading sites,
 * each navigated INSIDE a sandboxed Android WebView.
 *
 * A web connection is not a native ContentSource and does not grant
 * extraction/download permission. Do not represent it as a manga API.
 */
enum class ConnectionKind { NATIVE_API, IN_APP_WEBSITE }

data class ConnectedSource(
    val id: String,
    val title: String,
    val homeUrl: String,
    val subtitle: String,
    val kind: ConnectionKind,
    val spanishCatalog: Boolean,
    val note: String,
    val permittedHosts: Set<String>
) {
    val host: String get() = URI(homeUrl).host.lowercase()
    fun isAllowed(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.userInfo == null && uri.host?.lowercase()?.let { target ->
                permittedHosts.any { target == it || target.endsWith(".$it") }
            } == true
    }.getOrDefault(false)
}

object ConnectedSources {
    val builtIn: List<ConnectedSource> = listOf(
        ConnectedSource(
            id = "mangapdf", title = "MangaPDF",
            homeUrl = "https://mangapdf.org",
            subtitle = "API de manga y capítulos",
            kind = ConnectionKind.NATIVE_API,
            spanishCatalog = false,
            note = "Buscador y lector nativo. El idioma de cada título no está verificado.",
            permittedHosts = setOf("mangapdf.org")
        ),
        ConnectedSource(
            id = "mangaplus", title = "MANGA Plus",
            homeUrl = "https://mangaplus.shueisha.co.jp/updates",
            subtitle = "Manga oficial · Español",
            kind = ConnectionKind.IN_APP_WEBSITE,
            spanishCatalog = true,
            note = "Lectura en la web oficial. Algunas obras pueden variar por región.",
            permittedHosts = setOf("mangaplus.shueisha.co.jp")
        ),
        ConnectedSource(
            id = "webtoon", title = "WEBTOON",
            homeUrl = "https://www.webtoons.com/es/",
            subtitle = "Manhwa y webtoon · Español",
            kind = ConnectionKind.IN_APP_WEBSITE,
            spanishCatalog = true,
            note = "Usa el catálogo y lector de WEBTOON sin descargar capítulos.",
            permittedHosts = setOf("webtoons.com")
        ),
        ConnectedSource(
            id = "mangatoon", title = "MangaToon",
            homeUrl = "https://mangatoon.mobi/es",
            subtitle = "Manhwa y manhua · Español",
            kind = ConnectionKind.IN_APP_WEBSITE,
            spanishCatalog = true,
            note = "Los capítulos y accesos dependen del servicio.",
            permittedHosts = setOf("mangatoon.mobi")
        ),
        ConnectedSource(
            id = "toomics", title = "Toomics",
            homeUrl = "https://toomics.com/esp",
            subtitle = "Webtoons · Español",
            kind = ConnectionKind.IN_APP_WEBSITE,
            spanishCatalog = true,
            note = "Algunas obras requieren cuenta, monedas o suscripción.",
            permittedHosts = setOf("toomics.com", "toomics.uk")
        ),
        ConnectedSource(
            id = "lezhin", title = "Lezhin Comics ES",
            homeUrl = "https://www.lezhin.es/",
            subtitle = "Manhwa oficial · Español",
            kind = ConnectionKind.IN_APP_WEBSITE,
            spanishCatalog = true,
            note = "Obras de pago y capítulos gratuitos según la editorial.",
            permittedHosts = setOf("lezhin.es")
        ),
        ConnectedSource(
            id = "namicomi", title = "NamiComi",
            homeUrl = "https://namicomi.com/es-la/discover/comics",
            subtitle = "Cómics independientes · Interfaz en español",
            kind = ConnectionKind.IN_APP_WEBSITE,
            spanishCatalog = false,
            note = "La plataforma soporta español, pero no todas las obras lo ofrecen.",
            permittedHosts = setOf("namicomi.com")
        ),
        ConnectedSource(
            id = "globalcomix", title = "GlobalComix",
            homeUrl = "https://globalcomix.com/browse/es/comics",
            subtitle = "Historietas y manga · Catálogo español",
            kind = ConnectionKind.IN_APP_WEBSITE,
            spanishCatalog = true,
            note = "Solo el apartado español. Algunas obras requieren suscripción.",
            permittedHosts = setOf("globalcomix.com")
        ),
        ConnectedSource(
            id = "mangapluscreators", title = "MANGA Plus Creators",
            homeUrl = "https://service-b.mangaplus-creators.jp/",
            subtitle = "Manga independiente · Inglés y español",
            kind = ConnectionKind.IN_APP_WEBSITE,
            spanishCatalog = false,
            note = "Elige idioma español dentro de la plataforma cuando esté disponible.",
            permittedHosts = setOf("mangaplus-creators.jp")
        ),
        ConnectedSource(
            id = "izneo", title = "Izneo",
            homeUrl = "https://www.izneo.com/es",
            subtitle = "Manga y cómics oficiales · Portal europeo",
            kind = ConnectionKind.IN_APP_WEBSITE,
            spanishCatalog = false,
            note = "El acceso web puede estar restringido por el proveedor. Hay opción de abrir externamente.",
            permittedHosts = setOf("izneo.com")
        )
    )
    init {
        require(builtIn.size == 10)
        require(builtIn.distinctBy { it.id }.size == 10)
        require(builtIn.all { it.homeUrl.startsWith("https://") })
    }
}
