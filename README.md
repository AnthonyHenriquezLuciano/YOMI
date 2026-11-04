# YOMI Global · Android 0.4.0

App Android de manga, manhwa y manhua en español con diseño blanco/negro, Kotlin y Jetpack Compose.

## Novedades 0.4

### Lector inmersivo
- Por defecto oculta los controles superior e inferior.
- Android oculta temporalmente barras del sistema durante la lectura.
- Toca el **centro** para mostrar u ocultar controles.
- Lectura webtoon vertical y manga por páginas, con gestos de navegación.
- Barra de progreso y selector de modo solo visibles al abrir controles.
- Usa Atrás de Android para volver sin depender de barras permanentes.

### Biblioteca
- Barra de búsqueda, botón de filtros y menú **⋮**.
- Menú: **Actualizar biblioteca**, **Abrir un elemento al azar**, **Historial**.
- Filtros: **Descargados**, **Sin leer**, **Empezados**, **Favoritos**, **Completados**.
- Orden: **Alfabéticamente**, **Número de capítulos**, **Última lectura**, **Restantes**, **Capítulos más recientes**, **Fecha de obtención**, **Antigüedad**, **Al azar**; opción descendente.
- Apariencia: **Cuadrícula compacta**, **Cuadrícula amplia**, **Solo portadas**, **Lista**.
- Deslizador para títulos por fila en cuadrículas compacta/portadas.
- Superposiciones independientes: descargados, sin leer, local, idioma y botón **Seguir leyendo**.
- Pestañas opcionales de categorías y recuentos.
- Agrupación (opción única) por **Categorías**, **Fuentes**, **Estado** o **Desagrupado**.
- Menú por obra: favorito, asignar categorías y cambiar estado (pendiente, en lectura, completado).

### Categorías
- **Ajustes → Gestionar categorías**: crear, renombrar y eliminar.
- Al pulsar **Añadir a la biblioteca**, aparece una hoja para seleccionar una o varias categorías, crear una nueva o guardar sin categoría.
- Las categorías pueden cambiarse desde el menú de cada obra.
- Al importar un CBZ también se ofrece categorizarlo.
- Eliminar una categoría **no borra** las obras guardadas.

### Datos y actualización
- Preferencias y categorías guardadas localmente en el dispositivo.
- Los recuentos de capítulos se calculan a partir de listas consultadas y guardadas; se muestran según los datos **disponibles**, sin inventar totales.
- **Actualizar biblioteca** revisa obras conocidas una por una; necesita conexión para fuentes online y la aplicación abierta.
- El historial usa las marcas de lectura registradas en el dispositivo.
- El agrupamiento por categorías puede mostrar una obra en más de una sección si tiene varias categorías.

## Fuentes e idioma

- **MangaDex:** consulta de títulos con traducción disponible en español y feed de capítulos exclusivamente en español, cuando los hay.
- **CBZ local:** importación y lectura offline.
- **Comick:** directorio público no habilitado para lectura; al exigir verificación del español no se mezcla contenido sin idioma comprobado.
- **Repositorios Mihon/Tachiyomi (Keiyoushi, Nyora, WOX):** solo índice de fuentes etiquetadas en español; **NO son conectores automáticos ni APK ejecutadas**.

## Descargar APK

GitHub Actions → **Android APK** → última ejecución verde → artefacto \`YOMI-debug-apk\`.
Se compila y prueban casos unitarios con JDK 17 y Android SDK.
Instalador de pruebas, no versión release firmada.

## Límites

- Los contadores y el orden por capítulos dependen de los metadatos almacenados de la última actualización, no del universo completo de fuentes externas.
- Para actualizar toda la biblioteca hay que conservar YOMI abierta durante el proceso actual.
- No hay aún restauración de respaldo Mihon ni sincronización entre dispositivos.
- Las fuentes de extensión de terceros no funcionan hasta integrar adaptadores compatibles con autorización.
- Pendiente validar experiencia táctil y rendimiento en móviles y tabletas reales.

La aplicación no está afiliada a los proveedores de contenido. Se respetan licencias y condiciones de las APIs integradas.
