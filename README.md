# YOMI Global · Android 0.2.0

Aplicación nativa Kotlin/Jetpack Compose para manga, manhwa y manhua.
Explorar combina fuentes compatibles en un catálogo global, con lector adaptable, biblioteca,
descargas y archivos CBZ locales.

## Integraciones de contenido

| Integración | Búsqueda | Capítulos | Lectura dentro de YOMI |
|---|---|---|---|
| MangaDex | Sí, por API pública | Sí | Sí, cuando el proveedor ofrece páginas |
| Comick | Sí, catálogo público | No habilitado | No, abre su sitio externo |
| CBZ local | Sí, títulos importados | Sí | Sí, sin conexión |

**No hay un conector universal que permita buscar y leer directamente desde todas las webs.**
Cada proveedor necesita una API compatible o un adaptador con mantenimiento y permisos.

## Directorio de repositorios de extensiones

En **Ajustes → Explorar repositorios** se pueden consultar *metadatos* de repositorios
Mihon/Tachiyomi. Están precargados:

- Keiyoushi (su índice público actual puede mostrar únicamente mensajes de actualización de app).
- Nyora SFW (el índice puede agrupar cientos de fuentes bajo una sola extensión sin listar cada fuente).
- WOX Tadami manga (indexación de nombres, idiomas y enlaces declarados por el repositorio).

También se pueden añadir índices `index.min.json` y `index.json` alojados en GitHub o
GitHub Pages mediante HTTPS. Se filtran las entradas marcadas como NSFW por el índice.

**AVISO: solo se leen datos JSON.** Los índices pueden ser erróneos u obsoletos.
YOMI **no instala ni ejecuta** las APK de los repositorios.
Esas fuentes **NO se añaden al buscador de títulos** por el hecho de aparecer en el directorio.
Las extensiones son código Android de terceros y requerirían un motor aislado,
análisis de seguridad y compatibilidad antes de utilizarse como fuentes funcionales.

Validaciones de seguridad: HTTPS, lista de dominios permitidos, sin redirecciones,
máximo 3 MiB por índice y sin credenciales embebidas ni descarga de ejecutables.

## Funciones de YOMI 0.2

- Inicio, Explorar, Biblioteca, Descargas, Ajustes y directorio de repositorios.
- Búsqueda global de MangaDex y catálogo de Comick con aislamiento de errores.
- Búsqueda paginada, filtros y resultados por formato.
- Sinopsis plegada por defecto.
- Lector vertical, RTL y LTR con progreso persistente y gestos.
- Descargas privadas por capítulo, importador CBZ y lectura local.
- Catálogos alternativos enlazados a una misma obra sin perder la procedencia.
- Créditos de MangaDex y grupos de traducción.

## Compilar

GitHub Actions: `Android APK`. El workflow ejecuta
`gradle :app:assembleDebug :app:testDebugUnitTest` con JDK 17 y Android SDK.

[Ver compilaciones](https://github.com/AnthonyHenriquezLuciano/YOMI/actions).
Descarga el artefacto `YOMI-debug-apk` de la última ejecución **correcta**.

Para compilar localmente se necesitan Gradle 9.4.1, Android Gradle Plugin 9.2.1
y plataforma Android 37. La versión 0.2.0 es de pruebas, no release firmada.

## Pendientes

- Verificar comportamiento de Comick con su API en Android y completar lector solo si su
  API pública lo permite.
- Habilitar otras fuentes mediante conectores dedicados.
- Opciones para activar/desactivar conectores por usuario.
- Sincronización de lectura y migración de respaldos Mihon.
- Descargas resilientes, manejo de 429 y pruebas de memoria en webtoons largos.
- Tests en teléfonos y tabletas reales.

No afiliado a Mihon, MangaDex, Comick, Keiyoushi ni otros proveedores.
Se respetan sus políticas, licencias y derechos de autor.
