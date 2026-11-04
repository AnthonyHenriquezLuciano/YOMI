# YOMI Global · Android 0.6.0

Lector Android Kotlin + Jetpack Compose con biblioteca, categorías, filtros y
lector nativo para manga, manhwa y manhua. Diseño blanco/negro.

## 10 nuevas conexiones integradas

**Explorar → 10 fuentes** o **Ajustes → Abrir 10 fuentes conectadas**.

| Nueva conexión | Integración | Español |
|---|---|---|
| MangaPDF | API pública, búsqueda, lista de capítulos y páginas en lector YOMI | Idioma **no confirmado** por la API |
| MANGA Plus (Shueisha) | Navegación del sitio oficial dentro de YOMI | Catálogo español |
| WEBTOON | Navegación oficial dentro de YOMI | Catálogo español |
| MangaToon | Navegación oficial dentro de YOMI | Catálogo español |
| Toomics | Navegación oficial dentro de YOMI | Catálogo español |
| Lezhin Comics ES | Navegación oficial dentro de YOMI | Catálogo español |
| NamiComi | Navegación oficial dentro de YOMI | Soporta español, obras multilingües |
| GlobalComix | Apartado español del sitio dentro de YOMI | Catálogo español |
| MANGA Plus Creators | Navegación oficial dentro de YOMI | Obras en inglés y español |
| Izneo | Navegación del sitio dentro de YOMI (puede bloquear WebView) | Idioma variable |

Las 10 conexiones **no requieren extensiones APK**. No descargamos ni ejecutamos código de
repositorios de terceros, y no extraemos páginas de webs cerradas ni eludimos DRM.

### Diferencia importante entre las conexiones

- **MangaPDF** cuenta con API para terceros documentada en
  https://docs.mangapdf.org/guide/api/. Se usa la cabecera pública
  \`X-Client: api-consumer\`, sin tokens, cookies robadas ni endpoints privados.
  Desde su buscador nativo se abre el detalle y el lector de YOMI.
- Las **otras nueve** conexiones usan Android WebView: se navega, busca, inicia
  sesión y lee mediante la web de cada proveedor. La interfaz, capítulos,
  pagos y acceso dependen de ese proveedor. Sus obras **NO aparecen aún en la
  búsqueda unificada**, ni ofrecen descarga con el motor de YOMI.
- El **buscador global** conserva por defecto la comprobación de capítulos
  en español mediante MangaDex y permite archivos CBZ locales.
  MangaPDF está en su propio apartado porque su API no incluye un filtro
  verificable por idioma español.
- Algunos sitios podrían bloquear WebView o exigir su app o navegador:
  hay una opción de abrir la página oficial externamente.

### Seguridad de navegación

El WebView habilita JavaScript y almacenamiento DOM solo para mostrar páginas
del proveedor, pero deshabilita lectura de archivos locales, contenido del
dispositivo, contenido mixto y acceso universal desde archivos. Las
navegaciones fuera de los dominios permitidos se bloquean en el WebView.
No se inyectan scripts ni se interceptan sesiones o imágenes protegidas.

## Funcionalidades anteriores

- MangaDex en español con búsqueda por géneros, etiquetas, demografía,
  estado y año; verificación del feed de capítulos en español.
- Sinopsis española o traducción ML Kit en el dispositivo con Wi-Fi inicial.
- Lector inmersivo vertical y RTL/LTR, descargas por tramo y lector CBZ.
- Biblioteca personalizada con categorías, historial, filtros y portadas.
- Índices de extensiones Mihon/Tachiyomi visibles como **directorios**,
  no como fuentes ejecutables.

## Descargar

En [Actions](https://github.com/AnthonyHenriquezLuciano/YOMI/actions), abre
la última ejecución aprobada del flujo \`Android APK\`, descarga el artefacto
\`YOMI-debug-apk\`, descomprime e instala \`app-debug.apk\`.

Versión de prueba sin firma de distribución. Pendiente validar cada web en
un dispositivo Android real y adaptar posibles cambios en sus sitios.
