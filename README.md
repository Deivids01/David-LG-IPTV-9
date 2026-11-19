# DavidLG IPTV Player — app nativa Android / Android TV (Kotlin + Jetpack Compose + ExoPlayer)

Versión 4.0.0 · Etapa 1

## Qué incluye
- Interfaz nativa estilo streaming: inicio con "Continúa viendo", favoritos, filas por categoría, tarjetas con EPG y barra de progreso.
- Se adapta solo: **Android TV** (barra superior, foco con control remoto), **tablet/teléfono horizontal** (categorías laterales) y **teléfono vertical** (barra inferior).
- Listas **M3U** (URL), **Xtream Codes** (servidor/usuario/contraseña) y guía **EPG XMLTV** (.xml o .gz).
- **TV en vivo** y **Películas y series (VOD)** separadas (por tipo de lista o detección automática).
- Reproductor **ExoPlayer**: HLS, DASH/MPD, TS directo, **ClearKey** (KODIPROP / kid:key) y **Widevine** (licencia por URL), cabeceras User-Agent/Referer/Origin.
- Control remoto: ↑/↓ y CH+/CH− cambian de canal, ← abre la lista de canales, OK muestra info/pausa, Atrás vuelve.
- Táctil: toque muestra controles; botones de favorito, lista de canales, pausa y cerrar.

## Compilar
GitHub Actions (workflow incluido): sube el proyecto, ve a *Actions → Build APK → Run workflow* y descarga el APK.
También en Android Studio: *Build → Build APK(s)*.

## Pendiente (siguientes etapas)
- Perfiles de usuario, ajustes avanzados de reproducción, pista de audio/subtítulos, catch-up.
- Panel administrativo: acceso a contenido/paquetes por usuario (requiere definir el servidor).
- Detalle de películas/series (sinopsis, temporadas) y reanudar VOD.
