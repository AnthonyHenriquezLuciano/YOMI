# YOMI Global · Android 0.3.0

Lector nativo Kotlin/Jetpack Compose de manga, manhwa y manhua en español.

## Diseño y experiencia

- **Idioma:** búsqueda MangaDex filtrada a obras con capítulos traducidos al español; la lista de capítulos solicita exclusivamente `es`. El idioma *original* de la obra puede ser japonés, coreano o chino.
- **Extensiones:** el directorio Keiyoushi/Nyora/WOX solo indexa fuentes publicadas como `es` o `es-*`; esas extensiones no se instalan ni se ejecutan en YOMI.
- **Comick:** sigue siendo un adaptador de catálogo, pero se omite cuando el filtro obligatorio es español, porque el endpoint consultado no verifica traducciones en español.
- **Detalle renovado:** portada difuminada como fondo superior, contraste de texto, sinopsis plegada, botones grandes y capítulos con tipografía simplificada.
- **Controles:** **Añadir a la biblioteca** (o quitar), **Descargar** con `Completo` (todos los capítulos que cargó la lista) o `Por tramo` (posiciones del más antiguo al más reciente).
- **Actualización:** arrastra hacia abajo en el detalle o pulsa actualizar para volver a solicitar los capítulos.
- **Navegación:** botón físico/gesto Atrás de Android para cerrar lector, detalle o directorio; barra de estado y navegación respetadas con insets de Compose.
- **Lector:** páginas RTL/LTR, lectura webtoon vertical, progreso y descargas CBZ offline.

## Restricciones honestas

La descarga «Completo» abarca únicamente los capítulos cargados en la lista de detalle.
El proveedor MangaDex actualmente limita esa consulta a tres páginas de 100, por lo que
las obras con más de 300 entradas requerirán ampliar su paginación en una versión posterior.
Las descargas de varios capítulos se hacen secuencialmente, se pueden cancelar y funcionan
**mientras la app permanezca abierta**, no como trabajos persistentes del sistema operativo.
Los capítulos que ya están descargados se omiten.

**MangaDex** es la fuente online de lectura actualmente integrada. Los archivos CBZ
importados también pueden leerse. Comick se limita a catálogo y enlace externo, sin
garantía de capítulos en español. La presencia de una fuente en los índices externos
no significa que sea funcional en el buscador o lector.

## Compilación

Ve a [GitHub Actions](https://github.com/AnthonyHenriquezLuciano/YOMI/actions) y descarga
el artefacto **YOMI-debug-apk** de la última ejecución aprobada.
La compilación ejecuta `gradle :app:assembleDebug :app:testDebugUnitTest`.

Android 8.0+, JDK 17, SDK Android 37, Gradle 9.4.1.
Versión de pruebas, no distribuida como release firmada.

## Próximas mejoras

Paginación completa del feed, descargas persistentes con WorkManager, adaptación para
pantallas de diferentes tamaños, pruebas en dispositivos reales, respaldo, sincronización
y más conectores autorizados de contenido.
