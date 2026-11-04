# YOMI Global · Android 0.5.0

Lector Android nativo para manga, manhwa y manhua, priorizando **capítulos en español**.
Kotlin + Jetpack Compose. Sin anuncios y sin instalación de APK de extensiones externas.

## Novedades de Explorar 0.5

- **Filtro completo:** formato (manga, manhwa, manhua), estado (publicando, finalizado, pausado, cancelado), público (shōnen, shōjo, seinen, josei), año de publicación, contenido sugerente, fuente y ordenamiento.
- **Géneros y etiquetas:** descargados desde \`GET /manga/tag\` de MangaDex, organizados en géneros, temas, formatos y contenido; nombres priorizados en español, con traducción conocida de géneros comunes.
- Selección **múltiple** de etiquetas para **incluir o excluir**. Coincidencia **todas (Y)** o **cualquiera (O)**.
- Las etiquetas se muestran en la ficha de la obra, debajo de la sinopsis; pulsarlas inicia una búsqueda de títulos relacionados.
- **Títulos localizados:** prioridad al título oficial en español, luego inglés o romanizado. Se evita presentar solo caracteres CJK cuando existe un nombre latino.
- **Sinopsis en español:** si el proveedor ofrece una descripción en español se utiliza directamente; cuando solo aporta otra lengua, YOMI traduce **en el dispositivo** con ML Kit al desplegar la sinopsis. La primera traducción necesita Wi-Fi para descargar el modelo, es automática y puede contener errores. El resultado se guarda en caché local.
- **Capítulos limpios:** nunca presenta la palabra literal \`null\` como número, título, volumen o grupo.
- **Sin obras vacías:** búsqueda MangaDex filtrada por traducción en español y disponibilidad; adicionalmente se verifica con el feed que haya al menos un capítulo en español antes de mostrar el resultado. Una comprobación fallida no se presenta como una obra verificadamente legible.
- Los resultados admiten paginación y se muestran los recuentos de capítulos informados por el feed.

## Fuentes de contenido activas

| Proveedor | Buscar títulos | Abrir capítulos |
|---|---|---|
| MangaDex · español | Sí | Sí, sujeto a disponibilidad |
| CBZ locales | Sí | Sí, offline |
| Comick | No en filtro español (no verifica traducción disponible) | No |

El objetivo de YOMI sigue siendo buscar todos los proveedores compatibles desde un único lugar.
**Los índices de extensiones no son conectores funcionales.** No ejecutamos código
externo ni instalamos APK desconocidas por la seguridad del usuario.

## Directorios de extensiones disponibles

En **Ajustes → Repositorios** hay índices de Keiyoushi, Nyora, WOX y tres
archivos comunitarios adicionales: **Hycnyl**, **GeorgyDesign** y **MiguelMA3**.
Se indexan solo entradas que declaran idioma español. Algunos índices son archivos
históricos con entradas duplicadas o desactualizadas; YOMI no garantiza su
funcionamiento. No se añaden automáticamente a la búsqueda o lectura.

## Funcionalidades anteriores

- Biblioteca con categorías, favoritas, varios modos de visualización, filtros,
  agrupación y ordenamiento; historial y actualización.
- Detalle con fondo basado en la portada, sinopsis plegada y etiquetas interactivas.
- Descarga completa o por tramos de los capítulos cargados en la ficha.
- Lector vertical, RTL/LTR, con barras ocultas y controles por toque.
- Importación CBZ, seguimiento de progreso y lectura sin conexión.

## Instalar

1. Entra a [GitHub Actions](https://github.com/AnthonyHenriquezLuciano/YOMI/actions).
2. Abre la última ejecución **Android APK** aprobada.
3. Descarga \`YOMI-debug-apk\`, extrae el ZIP e instala \`app-debug.apk\`.

Versión para pruebas internas, no release de producción firmada.

## Límites técnicos

- Comprobar en dispositivo real la velocidad del listado con verificaciones de
  capítulos y disponibilidad de traducciones.
- Las verificaciones y el contador dependen de MangaDex y sus límites de API.
- ML Kit descarga el modelo de traducción desde Google la primera vez por Wi-Fi.
- Si el origen no tiene un título español y tampoco romanización, no es posible
  garantizar un nombre de marca oficial en español.
- La traducción automática de sinopsis puede contener errores.
- No se ha integrado un motor seguro de ejecución de extensiones Tachiyomi.
- Falta sistema de descargas persistente con WorkManager y migración de respaldos.

Fuentes de contenido y traducciones son propiedad de sus respectivos titulares.
YOMI respeta las condiciones de uso de MangaDex y otros proveedores.
