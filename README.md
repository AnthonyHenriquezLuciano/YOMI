# YOMI Global · Android 0.7

Android reader for manga, manhwa, manhua, with Spanish-first global search.
Kotlin + Jetpack Compose; no advertisements or embedded web browser.

## Bundled extensions — installed as part of the APK

**Ten native Spanish source adapters**, derived from the Apache-2.0
Keiyoushi Madara source patterns, are compiled into YOMI:

| Source | Language | Engine |
|---|---|---|
| KazokuDen | es | Madara HTML |
| Inventario Oculto | es | Madara AJAX |
| Hades no Fansub | es | Madara AJAX |
| Manhwa Latino | es | Madara HTML |
| Monopoly Scan | es | Madara AJAX |
| Taurus Fansub | es | Madara AJAX |
| House of Otakus | es | Madara AJAX |
| Marmota | es | Madara AJAX |
| Ragnarok Scanlation | es | Madara AJAX |
| Dragon Translation | es | Madara HTML |

Open **Explorar → Fuentes** to see the sources, search one source, or search
them all together with MangaDex. You can select individual sources from
**Explorar → Filtros → Fuentes disponibles**.

**These are not downloaded or installed as separate extension APKs.** Their
configuration and native adapter code are distributed *inside the YOMI APK*.
The only network calls happen when searching catalogs, retrieving chapters,
reading image pages, or downloading chapters offline.

Each source uses YOMI's built-in search/reader interfaces. This implementation
covers common WordPress/Madara HTML and AJAX layouts. Site-specific features
may require adjustments. An unavailable site may return an error or empty
results; integration with the app does not guarantee all sources are live.
The source engine rejects external website navigation and never displays a
WebView, executes remote code or bypasses anti-bot measures.

The list of third-party source names and URLs is based on the Keiyoushi
repository source code, which is licensed Apache-2.0. See [NOTICE](NOTICE.md).

## Other YOMI sources

- **MangaDex**: public API, Spanish chapters, native reader.
- **Local CBZ**: offline reader.
- **MangaPDF**: native API adapter retained, but *excluded from the
  Spanish-only global feed* because its API lacks a reliable language flag.
- **Comick**: historical metadata-only connector, excluded from the Spanish
  global feed; it is not a reader.

## Reader, library and downloads

- Adaptive manga RTL, page LTR and vertical webtoon viewing.
- Immersive reader: controls shown by tapping; Android system bars hidden.
- Persistent library, categories, sorting, groups, reading history.
- Full/batch downloads of loaded chapter lists while the app is open.
- Spanish synopsis when supplied or optional ML Kit on-device translation.

## Validation

GitHub Actions builds with Gradle and runs unit tests for native adapters.
**Not yet verified end-to-end on real Android devices against all ten live
websites**. Websites may use login, protections or inconsistent chapters
which YOMI will not bypass. Check each source's functionality in the app.

Download the latest successful APK through
[GitHub Actions](https://github.com/AnthonyHenriquezLuciano/YOMI/actions)
→ Android APK → Artifacts → \`YOMI-debug-apk\`.
