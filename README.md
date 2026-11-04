# YOMI — Manga, Manhwa y Manhua

YOMI es un lector Android nativo en Kotlin y Jetpack Compose. Diseño blanco/negro, biblioteca local, búsqueda en un catálogo unificado y lectura adaptable a manga paginado y webtoon vertical.

## Estado actual — versión 0.1.0 (base funcional)

- Cinco secciones Compose: Inicio, Explorar, Biblioteca, Descargas y Ajustes.
- Buscador global con motor de proveedores concurrentes y agrupación conservadora de títulos.
- **Proveedor remoto inicial: MangaDex** (API pública con atribuciones). No afirmar que todas las webs son compatibles: para sumar proveedores se necesitan conectores propios y condiciones de uso compatibles.
- Exploración de manga, manhwa y manhua con categorías por idioma original; filtro de formato.
- Detalle con sinopsis, fuente, traducciones, lista de capítulos y atribución del grupo.
- Lector vertical continuo y paginado RTL/LTR, modo sugerido según origen/etiqueta Long Strip, override por obra.
- Biblioteca, historial de posición por obra y capítulos leídos almacenados en dispositivo.
- Descarga por capítulo en almacenamiento privado, relectura offline.
- Importación de archivos CBZ mediante selector de Android, sin almacenamiento externo general.
- Compilación automatizada en GitHub Actions con APK de depuración como artifact.

## Descargar APK

1. Abre la pestaña [Actions](https://github.com/AnthonyHenriquezLuciano/YOMI/actions).
2. Selecciona la última ejecución **Android APK** con resultado correcto.
3. Descarga el artifact **YOMI-debug-apk** y extrae `app-debug.apk`.
4. Instala en Android 8.0+ (habilitando instalación de APK para el gestor correspondiente).

> El APK de depuración es para pruebas personales, no es una versión de producción firmada.

## Ejecutar localmente

- JDK 17; Android SDK plataforma 37; Gradle 9.4.1.
- Importa el repositorio en Android Studio compatible con AGP 9.2.1.
- Sin wrapper de Gradle todavía: ejecuta `gradle :app:assembleDebug` o utiliza Gradle desde Android Studio.

## Arquitectura

```text
app.yomi.reader
├── core
│   ├── Models.kt          // Tipos y contrato ContentSource
│   ├── MangaDexSource.kt  // Proveedor remoto
│   └── GlobalCatalog.kt   // Búsqueda concurrente con aislamiento de errores
├── data
│   └── YomiStore.kt       // Biblioteca, progreso, descargas y CBZ
├── ui
│   ├── Screens.kt         // Inicio, Explorar, Biblioteca, Descargas, Ajustes, detalle
│   └── ReaderScreen.kt    // Lector vertical y paginado
└── MainActivity.kt        // Navegación y estado
```

## Límites y próximos hitos

YOMI Global es una **arquitectura extensible**, no un agregador universal de sitios no autorizados. Para incorporar nuevas fuentes hay que implementar `ContentSource` y registrar el proveedor. En esta versión solo se conecta MangaDex y se aceptan CBZ locales.

Pendiente para una versión de lanzamiento: fusionar catálogos de varias fuentes con confirmación de identidad; fallback seguro entre proveedores; migración de respaldos Mihon; sincronización opcional; estadísticas; descarga reanudable con WorkManager; paginación completa de catálogos; panel-to-panel; modos de lectura más sofisticados; mejores herramientas de accesibilidad; pruebas de rendimiento con tiras largas; QA en dispositivos reales y preparación de firma de producción.

## Uso responsable de fuentes

- MangaDex es un proveedor gratuito y **exige atribución a MangaDex y a los grupos de traducción**, además de respetar solicitudes de retirada.
- MangaDex **prohíbe mostrar anuncios o cobrar por la aplicación que consume su API**.
- YOMI no incluye anuncios, pagos, scrapers evasivos ni repositorios de extensiones no verificados.
- El contenido y los derechos de publicación pertenecen a sus titulares respectivos.
- [Documentación y política de uso de MangaDex](https://gitlab.com/mangadex-pub/mangadex-api-docs).

## Desarrollo

Código inicial para pruebas e iteración. No se ha efectuado aún un lanzamiento de producción.
