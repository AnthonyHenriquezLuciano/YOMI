# YOMI Global — Android 0.8.0

Lector nativo de manga, manhwa y manhua en español, basado en Kotlin y Jetpack Compose. Una sola APK, sin WebView y sin instalar extensiones adicionales.

## Rediseño visual 0.8

- **Explorar compacto:** campo de búsqueda de 46 dp, filtros en icono, formatos rápidos, menos encabezados y más espacio para las portadas.
- **Inicio renovado:** obra destacada, continuar leyendo, colección reciente y sugerencias basadas en el catálogo cargado y los géneros de tu biblioteca. No se inventan valoraciones ni posiciones de popularidad.
- **Ajustes rediseñados:** logo YOMI arriba, estadísticas y paneles plegables de Apariencia, Lectura, Biblioteca, Fuentes, Descargas, Datos y Acerca de.
- **Tipografía:** jerarquía y espaciados coherentes, tarjetas con portadas y metadatos discretos, animación de sinopsis.
- **Carga elegante:** tarjetas esqueleto animadas al consultar el catálogo.
- **Transiciones:** fundidos suaves entre secciones, configurables en Ajustes.
- **Bienvenida en tres pasos:** tema, modo de lectura y selección de fuentes, en el primer inicio.

## Preferencias funcionales

- Tema automático, claro u oscuro.
- Lector: modo inicial manga/webtoon, zonas de toque, márgenes, brillo propio, pantalla despierta durante lectura y precarga de siguiente página.
- Fuentes: activar o desactivar cualquiera de los diez adaptadores Madara junto a MangaDex y archivos locales. Lo seleccionado determina los proveedores consultados por Explorar.
- Descargas: opción de usar solamente Wi-Fi; muestra una advertencia si se intenta descargar usando otra red.
- Biblioteca: categorías, favoritos, filtros, grupos y navegación ya existentes.

## Descargas mejoradas

La pantalla agrupa las descargas por obra, muestra páginas y espacio ocupado, y permite eliminar capítulos con confirmación. La cola de descargas admite **pausa entre páginas/capítulos**, reanudación, cancelación y reintento de capítulos fallidos mientras la app sigue abierta. El progreso del capítulo procede del descargador existente.

**Límite:** las descargas de lote aún requieren mantener YOMI abierta. No son trabajos persistentes de WorkManager y no se promete que continúen al cerrar la app.

## Respaldo local

Desde **Ajustes → Datos y respaldo** puedes exportar/restaurar un JSON mediante el selector de archivos Android. Conserva información de biblioteca, categorías, historial, preferencias y datos de lectura; **no incluye archivos CBZ ni imágenes descargadas**. Importar un respaldo fusiona las entradas de datos en el dispositivo. No hay almacenamiento en nube.

## Fuentes y límites

El lector integra MangaDex y diez adaptadores nativos para catálogos con contenido en español, utilizando las APIs o selectores correspondientes. Los proveedores pueden cambiar de estructura, restringir acceso o quedar temporalmente fuera de servicio: compilar correctamente no demuestra que todas sus obras abran.

Pendiente para una versión de producción:
- Descargas persistentes con WorkManager y reintento después de reinicios.
- Pruebas de interacción, accesibilidad y rendimiento en dispositivos reales.
- Sincronización opcional y migración de respaldos Mihon.
- Validación continua de cada adaptador contra su sitio en vivo.

## Compilar e instalar

En [GitHub Actions](https://github.com/AnthonyHenriquezLuciano/YOMI/actions), abre **Android APK** y descarga el artefacto **YOMI-debug-apk** de una ejecución verde. Dentro del ZIP estará \`app-debug.apk\`.

Uso personal; respeta las políticas, licencias y derechos de autores y proveedores.
