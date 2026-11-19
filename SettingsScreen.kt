package com.davidlg.iptvplayer.ui.settings

import android.media.MediaDrm
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidlg.iptvplayer.MainViewModel
import com.davidlg.iptvplayer.data.Channel
import com.davidlg.iptvplayer.data.EpgStore
import com.davidlg.iptvplayer.data.Playlist
import com.davidlg.iptvplayer.ui.Sans
import com.davidlg.iptvplayer.ui.dlClickable
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.util.UUID

private val Gold = Color(0xFFDFC48E)
private val GoldTxt = Color(0xFFE0C38A)
private val Dim = Color(0xFF9AA3B8)
private val Danger = Color(0xFFFF8A8A)
private const val VER = "3.2.162"

private enum class Sec(val label: String) { Listas("Listas"), Reproductor("Reproductor"), Apariencia("Apariencia"), Vod("Catálogo VOD"), Sistema("Sistema") }
private enum class End { Chev, Check, On, Off }
private typealias Opts = List<Pair<String, String>>

private sealed class SR {
    class Hdr(val t: String) : SR()
    class Info(val t: String) : SR()
    class Item(
        val label: String, val value: String, val end: End = End.Chev,
        val primary: Boolean = false, val danger: Boolean = false, val plus: Boolean = false, val onClick: () -> Unit,
    ) : SR()
}

private sealed class Dlg {
    class Pick(val title: String, val desc: String, val labels: List<String>, val cur: Int, val onPick: (Int) -> Unit) : Dlg()
    class Txt(val title: String, val desc: String, val initial: String, val secret: Boolean, val onOk: (String) -> Unit) : Dlg()
    class Confirm(val title: String, val msg: String, val yes: String, val onYes: () -> Unit) : Dlg()
    object Diag : Dlg()
}

private data class Form(
    val id: String? = null, val name: String = "", val type: String = "mixed", val m3u: String = "", val epg: String = "",
    val shift: Int = 0, val xtServer: String = "", val xtUser: String = "", val xtPass: String = "", val xtOut: String = "m3u8",
)

private val OnOff: Opts = listOf("on" to "Activado", "off" to "Desactivado")
private val Quality: Opts = listOf("auto" to "Automática", "highest" to "Máxima disponible", "2160" to "4K · 2160p", "1080" to "FHD · 1080p", "720" to "HD · 720p", "480" to "SD · 480p")
private val Opacity: Opts = (0..100 step 10).map { it.toString() to (if (it == 0) "0% · Transparente" else if (it == 100) "100% · Opaca" else "$it%") }
private val Colors: Opts = listOf("white" to "Blanco perla", "platinum" to "Plata platino", "champagne" to "Dorado champán")
private val Types: Opts = listOf("tv" to "Solo TV en vivo", "vod" to "Solo contenido VOD", "mixed" to "TV en vivo + VOD")

@Composable
fun SettingsScreen(vm: MainViewModel, onClose: () -> Unit) {
    val pp by vm.playbackPrefs.collectAsState()
    val vs by vm.vodSettings.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val activeIds by vm.activeIds.collectAsState()
    val live by vm.live.collectAsState()
    val profile by vm.activeProfile.collectAsState()

    var sec by remember { mutableStateOf(Sec.Listas) }
    var zoneRight by remember { mutableStateOf(false) }
    var form by remember { mutableStateOf<Form?>(null) }
    var dlg by remember { mutableStateOf<Dlg?>(null) }
    var toast by remember { mutableStateOf("") }
    var testUrl by remember { mutableStateOf("") }
    var chSel by remember { mutableStateOf("all") }
    var lastRow by remember { mutableStateOf(0) }
    val catFr = remember { List(Sec.values().size) { FocusRequester() } }
    val rowFr = remember { HashMap<Int, FocusRequester>() }
    fun fr(i: Int) = rowFr.getOrPut(i) { FocusRequester() }
    fun say(t: String) { toast = t }
    LaunchedEffect(toast) { if (toast.isNotEmpty()) { delay(3800); toast = "" } }
    LaunchedEffect(Unit) { delay(60); runCatching { catFr[0].requestFocus() } }
    LaunchedEffect(dlg == null) {
        if (dlg == null) { delay(60); runCatching { if (zoneRight) fr(lastRow).requestFocus() else catFr[sec.ordinal].requestFocus() } }
    }

    fun setPref(vod: Boolean, k: String, v: String) {
        val value: Any = if (k == "navOpacity" || k == "epgOpacity") v.toInt() else v
        if (vod) vm.putVodSetting(k, value) else vm.putPlaybackPref(k, value)
    }
    fun pick(label: String, desc: String, opts: Opts, cur: String, onPick: (String) -> Unit): SR.Item {
        val idx = opts.indexOfFirst { it.first == cur }.coerceAtLeast(0)
        return SR.Item(label, opts[idx].second) { dlg = Dlg.Pick(label, desc, opts.map { it.second }, idx) { i -> onPick(opts[i].first) } }
    }
    fun pref(vod: Boolean, key: String, label: String, desc: String, opts: Opts, def: String): SR.Item {
        val cur = (if (vod) vs else pp).str(key, def)
        val sw = opts.size == 2 && opts.map { it.first }.sorted() == listOf("off", "on")
        if (sw) return SR.Item(label, opts.first { it.first == cur }.second, if (cur == "on") End.On else End.Off) { setPref(vod, key, if (cur == "on") "off" else "on") }
        return pick(label, desc, opts, cur) { setPref(vod, key, it) }
    }
    fun txt(label: String, desc: String, value: String, secret: Boolean = false, onOk: (String) -> Unit): SR.Item =
        SR.Item(label, if (value.isEmpty()) "Sin definir" else if (secret) "•".repeat(minOf(value.length, 16)) else value) { dlg = Dlg.Txt(label, desc, value, secret, onOk) }

    // ---------------- filas por categoría ----------------
    val rows: List<SR> = run {
        val f = form
        if (f != null) {
            val shiftOpts: Opts = (-12..12).map { it.toString() to (if (it == 0) "Sin desfase" else if (it > 0) "+$it h" else "−${-it} h") }
            listOf(
                SR.Hdr("Datos de la lista"),
                txt("Nombre de la lista", "Cómo quieres verla en el reproductor", f.name) { form = f.copy(name = it) },
                pick("Tipo de contenido", "", Types, f.type) { form = f.copy(type = it) },
                SR.Hdr("Enlaces"),
                txt("URL de la lista M3U", "Dirección de tu lista .m3u / .m3u8", f.m3u) { form = f.copy(m3u = it) },
                txt("URL de la guía EPG (opcional)", "Puedes poner varias separadas por espacio; admite .xml y .xml.gz", f.epg) { form = f.copy(epg = it) },
                pick("Desfase horario de la guía", "Corrige la hora de la guía de esta lista", shiftOpts, f.shift.toString()) { form = f.copy(shift = it.toInt()) },
                SR.Hdr("Proveedor Xtream Codes (opcional)"),
                txt("Servidor", "Por ejemplo http://servidor:puerto", f.xtServer) { form = f.copy(xtServer = it) },
                txt("Usuario", "", f.xtUser) { form = f.copy(xtUser = it) },
                txt("Contraseña", "", f.xtPass, true) { form = f.copy(xtPass = it) },
                pick("Formato de los canales", "", listOf("m3u8" to "HLS (m3u8) · recomendado", "ts" to "MPEG-TS"), f.xtOut) { form = f.copy(xtOut = it) },
                SR.Item("Generar enlaces Xtream", "Rellena la lista M3U y la guía EPG con los datos de arriba") {
                    var s = f.xtServer.trim().trimEnd('/')
                    if (s.isNotEmpty()) {
                        if (!s.startsWith("http", true)) s = "http://$s"
                        val u = f.xtUser.trim(); val p = f.xtPass.trim()
                        form = f.copy(
                            m3u = "$s/get.php?username=$u&password=$p&type=m3u_plus&output=${f.xtOut}",
                            epg = "$s/xmltv.php?username=$u&password=$p",
                            name = f.name.ifBlank { "Xtream" },
                        )
                        say("Enlaces generados")
                    } else say("Escribe primero el servidor")
                },
                SR.Item(if (f.id == null) "Guardar lista" else "Guardar cambios", "Guarda la lista y vuelve a Mis listas", primary = true) {
                    if (f.m3u.isBlank()) say("Escribe la URL M3U o genera los enlaces Xtream") else {
                        val nm = f.name.ifBlank { "Lista ${playlists.size + 1}" }
                        if (f.id == null) vm.addPlaylist(Playlist(UUID.randomUUID().toString().take(8), nm, f.m3u.trim(), f.epg.trim(), f.type, f.shift))
                        else vm.updatePlaylist(Playlist(f.id, nm, f.m3u.trim(), f.epg.trim(), f.type, f.shift))
                        form = null; say("Lista guardada")
                    }
                },
            )
        } else when (sec) {
            Sec.Listas -> buildList<SR> {
                add(SR.Item("Añadir lista", "M3U, Xtream Codes o guía XMLTV", primary = true, plus = true) { form = Form() })
                add(SR.Hdr("Mis listas · ${activeIds.size}/5 activas · ${playlists.size} guardadas"))
                if (playlists.isEmpty()) add(SR.Info("Todavía no hay listas guardadas. Pulsa «Añadir lista» para empezar."))
                playlists.forEach { p ->
                    val on = p.id in activeIds
                    val badge = when (p.contentType) { "vod" -> "Solo VOD"; "tv" -> "Solo TV en vivo"; else -> "TV en vivo + VOD" }
                    add(SR.Item(p.name, (if (on) "Incluida · " else "No incluida · ") + badge + " · " + (if (p.epgUrl.isNotBlank()) "EPG configurada" else "Sin EPG propia"), if (on) End.Check else End.Chev) {
                        dlg = Dlg.Pick(p.name, "", listOf(if (on) "Quitar de la combinación" else "Incluir en la combinación", "Editar lista", "Eliminar lista"), -1) { i ->
                            when (i) {
                                0 -> vm.toggleActive(p.id)
                                1 -> form = Form(p.id, p.name, p.contentType, p.m3uUrl, p.epgUrl, p.epgShift)
                                2 -> dlg = Dlg.Confirm("Eliminar lista", "¿Eliminar «${p.name}»? Se quitará de este televisor.", "Eliminar") { vm.removePlaylist(p.id) }
                            }
                        }
                    })
                }
                add(SR.Hdr("Herramientas"))
                add(txt("Prueba rápida de señal", "Escribe la URL de un canal y ábrelo", testUrl) { u ->
                    testUrl = u
                    if (u.startsWith("http", true)) {
                        val c = Channel("test", 1, "Prueba de señal", "", "", "Prueba", "", u, emptyMap(), false, "test")
                        vm.play(c, listOf(c))
                    }
                })
                add(SR.Item("Abrir diagnóstico", "Revisa reproducción, almacenamiento, fuentes y guía EPG") { dlg = Dlg.Diag })
                add(SR.Item("Borrar fuentes guardadas", "Elimina todas las listas de este televisor", danger = true) {
                    dlg = Dlg.Confirm("Borrar fuentes guardadas", "Se eliminarán todas las listas y datos guardados en este televisor. Esta acción no se puede deshacer.", "Borrar todo") { vm.clearAllSources(); say("Fuentes borradas") }
                })
            }
            Sec.Reproductor -> {
                val chOpts: Opts = listOf("all" to "Todos los canales") + live.take(1500).map { it.id to (pad(it.number) + " " + it.name) }
                val cq = pp.json().optJSONObject("channelQuality")?.optString(chSel, "auto") ?: "auto"
                listOf(
                    SR.Hdr("Reproducción"),
                    pref(false, "engine", "Motor de reproducción", "Automático usa ExoPlayer y cambia a Shaka si un canal no abre", listOf("auto" to "Automático (ExoPlayer + Shaka)", "exo" to "Solo ExoPlayer", "shaka" to "Solo Shaka Player"), "auto"),
                    pref(false, "clock", "Reloj del reproductor", "Muestra la hora mientras ves un canal", OnOff, "on"),
                    pref(false, "audio", "Idioma de audio preferido", "Se usa cuando la señal ofrece varios idiomas", listOf("es" to "Español", "en" to "English", "auto" to "Automático"), "es"),
                    pref(false, "subs", "Mostrar subtítulos", "Activa las pistas de subtítulos si existen", listOf("off" to "Desactivados", "on" to "Activados"), "off"),
                    pref(true, "fastZap", "Cambio rápido de canal", "Precarga el servidor de los canales vecinos", OnOff, "on"),
                    SR.Hdr("Calidad de vídeo"),
                    pref(false, "quality", "Calidad general", "", Quality, "auto"),
                    pref(false, "fps", "Fotogramas por segundo (FPS)", "Solo tiene efecto si el canal ofrece varias versiones.", listOf("auto" to "Automático", "30" to "30 fps", "60" to "60 fps", "120" to "120 fps", "max" to "El máximo que ofrezca el canal"), "auto"),
                    pick("Canal para ajuste individual", "Elige un canal para darle su propia calidad", chOpts, chSel) { chSel = it },
                    pick("Calidad de ese canal", "", listOf("auto" to "Usar calidad general") + Quality.drop(1), cq) { q ->
                        if (chSel != "all") {
                            val o = pp.json().optJSONObject("channelQuality") ?: JSONObject()
                            o.put(chSel, q); vm.putPlaybackPref("channelQuality", o)
                        }
                    },
                )
            }
            Sec.Apariencia -> listOf(
                SR.Hdr("Barra lateral del reproductor"),
                pref(false, "navOpacity", "Opacidad del fondo", "", Opacity, "100"),
                pref(false, "navTextColor", "Color de las letras", "", Colors, "white"),
                SR.Hdr("Barra de guía (EPG)"),
                pref(false, "epgOpacity", "Opacidad del fondo", "", Opacity, "100"),
                pref(false, "epgTextColor", "Color de las letras", "", Colors, "white"),
                SR.Hdr("Estilo de la aplicación"),
                pref(true, "style", "Estilo visual", "", listOf("ott" to "OTT (nuevo)", "classic" to "Clásico"), "ott"),
                pref(true, "theme", "Tema de color (OTT)", "", listOf("champan" to "Medianoche y champán", "violeta" to "Violeta nocturno", "esmeralda" to "Esmeralda", "burdeos" to "Burdeos y oro rosa", "grafito" to "Grafito y plata"), "champan"),
                pref(true, "bannerArt", "Imagen del banner (TV en vivo)", "", listOf("right" to "Completa, a la derecha", "full" to "Pantalla completa"), "right"),
                pref(true, "bannerFade", "Degradado del banner (TV en vivo)", "", listOf("rtl" to "Invertido: imagen a la izquierda, color de derecha a izquierda", "ltr" to "Clásico: imagen a la derecha, color de izquierda a derecha", "ltr2" to "Clásico 2: imagen a la derecha, color de derecha a izquierda", "rtl2" to "Invertido 2: imagen a la izquierda, color de izquierda a derecha"), "rtl"),
                pref(true, "titles", "Títulos de los pósters (VOD)", "", listOf("focus" to "Solo al enfocar (estilo Netflix)", "always" to "Siempre visibles"), "focus"),
            )
            Sec.Vod -> listOf(
                SR.Hdr("Catálogo"),
                pref(true, "landing", "Pantalla inicial", "", listOf("hero" to "Portada y filas de contenido", "catalog" to "Ir directamente al catálogo"), "hero"),
                pref(true, "kind", "Mostrar contenido", "", listOf("all" to "Películas, series y animes", "movies" to "Solo películas", "series" to "Solo series", "anime" to "Solo animes"), "all"),
                pref(true, "sort", "Orden del catálogo", "", listOf("source" to "Orden de la lista", "az" to "Nombre: A–Z", "recent" to "Añadidos recientemente", "year" to "Año: más nuevo primero"), "source"),
                SR.Hdr("Datos de películas (TMDB)"),
                txt("API key de TMDB", "Para carátulas, sinopsis y reparto", vs.str("tmdbKey", ""), true) { vm.putVodSetting("tmdbKey", it.trim()) },
            )
            Sec.Sistema -> listOf(
                SR.Hdr("Perfil"),
                SR.Item("Cambiar de perfil", "Ahora: ${profile.name}") { vm.showProfileSwitcher() },
                SR.Hdr("Sistema"),
                SR.Item("Diagnóstico del sistema", "Reproducción, almacenamiento, fuentes y capacidades de la TV") { dlg = Dlg.Diag },
                SR.Item("Cerrar ajustes", "Volver al inicio") { onClose() },
                SR.Hdr("Acerca de"),
                SR.Info("DavidLG IPTV Player · versión $VER"),
            )
        }
    }

    BackHandler {
        when {
            dlg != null -> dlg = null
            form != null -> { form = null; zoneRight = true }
            zoneRight -> { zoneRight = false; runCatching { catFr[sec.ordinal].requestFocus() } }
            else -> onClose()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0E1426), Color(0xFF070A14)))).focusProperties { canFocus = dlg == null },
        ) {
            // ---- Menú izquierdo ----
            Column(Modifier.width(540.dp).fillMaxHeight().background(Color(0x66000000)).padding(start = 52.dp, end = 34.dp, top = 56.dp)) {
                Text("Ajustes", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, modifier = Modifier.padding(start = 20.dp))
                Text("DavidLG IPTV Player · v$VER", color = Dim, fontSize = 20.sp, fontFamily = Sans, letterSpacing = 1.sp, modifier = Modifier.padding(start = 20.dp, bottom = 38.dp))
                Sec.values().forEach { s ->
                    val sel = s == sec
                    var foc by remember { mutableStateOf(false) }
                    val shape = RoundedCornerShape(16.dp)
                    Box(
                        Modifier.fillMaxWidth().height(94.dp).padding(bottom = 8.dp)
                            .focusRequester(catFr[s.ordinal])
                            .onPreviewKeyEvent { e ->
                                if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionRight || e.key == Key.DirectionCenter || e.key == Key.Enter)) {
                                    runCatching { fr(0).requestFocus() }.onFailure { runCatching { fr(1).requestFocus() } }; zoneRight = true; true
                                } else false
                            }
                            .dlClickable(shape, Color.Transparent, 0.dp, 1f, { f -> foc = f; if (f) { sec = s; zoneRight = false; form = null } }) { zoneRight = true; runCatching { fr(0).requestFocus() } }
                            .background(if (foc) Gold else if (sel) Color(0xFF1C2033) else Color.Transparent),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (sel && !foc) Box(Modifier.width(5.dp).height(50.dp).background(Gold, RoundedCornerShape(3.dp)))
                        Text(s.label, Modifier.padding(start = 30.dp), color = if (foc) Color(0xFF1B140A) else if (sel) GoldTxt else Color(0xFFC3C9D6), fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                    }
                }
            }
            // ---- Contenido ----
            Column(Modifier.weight(1f).fillMaxHeight().padding(start = 78.dp, end = 70.dp, top = 56.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(if (form != null) "Añadir o editar lista" else sec.label, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                    if (form != null) Text("Listas › Lista", color = Dim, fontSize = 22.sp, fontFamily = Sans, modifier = Modifier.padding(start = 24.dp, bottom = 8.dp))
                }
                Box(Modifier.padding(top = 22.dp).fillMaxWidth().height(1.dp).background(Color(0x1FFFFFFF)))
                LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                    itemsIndexed(rows) { i, r ->
                        when (r) {
                            is SR.Hdr -> Text(r.t.uppercase(), color = GoldTxt, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, letterSpacing = 4.sp, modifier = Modifier.padding(start = 30.dp, top = 34.dp, bottom = 10.dp))
                            is SR.Info -> Text(r.t, color = Dim, fontSize = 22.sp, fontFamily = Sans, modifier = Modifier.padding(start = 30.dp, top = 10.dp, bottom = 10.dp))
                            is SR.Item -> SRow(r, Modifier.focusRequester(fr(i)).onPreviewKeyEvent { e ->
                                if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft) {
                                    if (form != null) form = null
                                    zoneRight = false; runCatching { catFr[sec.ordinal].requestFocus() }; true
                                } else false
                            }) { zoneRight = true; lastRow = i }
                        }
                    }
                    item { Box(Modifier.height(80.dp)) }
                }
            }
        }

        when (val d = dlg) {
            is Dlg.Pick -> PickDialog(d) { dlg = null }
            is Dlg.Txt -> TxtDialog(d) { dlg = null }
            is Dlg.Confirm -> ConfirmDialog(d) { dlg = null }
            Dlg.Diag -> DiagDialog(vm) { dlg = null }
            null -> {}
        }
        if (toast.isNotEmpty()) Text(
            toast, color = Color.White, fontSize = 24.sp, fontFamily = Sans, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).clip(RoundedCornerShape(50)).background(Color(0xF0182038)).border(1.dp, Color(0x40E0C38A), RoundedCornerShape(50)).padding(horizontal = 36.dp, vertical = 14.dp),
        )
    }
}

private fun pad(n: Int) = n.toString().padStart(3, '0')

@Composable
private fun SRow(r: SR.Item, modifier: Modifier, onFocus: () -> Unit) {
    var foc by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(16.dp)
    Column(Modifier.padding(horizontal = 12.dp)) {
        Row(
            modifier.fillMaxWidth().padding(vertical = 2.dp)
                .dlClickable(shape, Gold, 3.dp, 1f, { foc = it; if (it) onFocus() }, r.onClick)
                .background(if (foc) Brush.horizontalGradient(listOf(Color(0xFF55504F), Color(0xFF2B2A31))) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (r.plus) Text("+", color = GoldTxt, fontSize = 44.sp, fontWeight = FontWeight.Light, fontFamily = Sans, modifier = Modifier.width(72.dp))
            Column(Modifier.weight(1f)) {
                Text(r.label, color = if (r.danger) Danger else if (r.primary) GoldTxt else Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (r.value.isNotEmpty()) Text(r.value, color = Color(0xFFA6AEC2), fontSize = 22.sp, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            when (r.end) {
                End.Chev -> Text("›", color = if (foc) GoldTxt else Dim, fontSize = 40.sp, fontFamily = Sans, modifier = Modifier.padding(start = 16.dp))
                End.Check -> Text("✓", color = GoldTxt, fontSize = 38.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, modifier = Modifier.padding(start = 16.dp))
                End.On, End.Off -> {
                    val on = r.end == End.On
                    Box(Modifier.width(84.dp).height(46.dp).clip(RoundedCornerShape(50)).background(if (on) Gold else Color(0xFF3A3F52)).padding(5.dp)) {
                        Box(Modifier.align(if (on) Alignment.CenterEnd else Alignment.CenterStart).size(36.dp).clip(RoundedCornerShape(50)).background(if (on) Color(0xFF1B140A) else Color(0xFFB4BBCB)))
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
    }
}

// ---------------- Diálogos ----------------
@Composable
private fun DlgBox(width: Int, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(width.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF111A30)).border(1.dp, Color(0x33E0C38A), RoundedCornerShape(24.dp)).padding(36.dp)

        ) { content() }
    }
}

@Composable
private fun PickDialog(d: Dlg.Pick, close: () -> Unit) {
    val fr = remember { FocusRequester() }
    val st = androidx.compose.foundation.lazy.rememberLazyListState(maxOf(0, d.cur - 3))
    LaunchedEffect(Unit) { delay(80); runCatching { fr.requestFocus() } }
    DlgBox(1100) {
        Text(d.title, color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (d.desc.isNotEmpty()) Text(d.desc, color = Dim, fontSize = 22.sp, fontFamily = Sans, modifier = Modifier.padding(top = 6.dp))
        LazyColumn(Modifier.padding(top = 18.dp).height(minOf(d.labels.size, 7) * 72.dp), state = st) {
            itemsIndexed(d.labels) { i, l ->
                var foc by remember { mutableStateOf(false) }
                val shape = RoundedCornerShape(14.dp)
                Row(
                    Modifier.fillMaxWidth().height(68.dp).padding(vertical = 2.dp).then(if (i == maxOf(0, d.cur)) Modifier.focusRequester(fr) else Modifier)
                        .dlClickable(shape, Gold, 3.dp, 1f, { foc = it }) { close(); d.onPick(i) }
                        .background(if (foc) Color(0xFF3A3638) else Color(0x0DFFFFFF)).padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(26.dp).border(3.dp, if (i == d.cur) Gold else Color(0xFF6B7287), RoundedCornerShape(50)).padding(5.dp)) {
                        if (i == d.cur) Box(Modifier.fillMaxSize().background(Gold, RoundedCornerShape(50)))
                    }
                    Text(l, Modifier.padding(start = 20.dp), color = Color.White, fontSize = 26.sp, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (d.labels.size > 7) Text("${maxOf(0, d.cur) + 1} / ${d.labels.size}", color = Dim, fontSize = 18.sp, fontFamily = Sans, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun TxtDialog(d: Dlg.Txt, close: () -> Unit) {
    var v by remember { mutableStateOf(d.initial) }
    val kb = LocalSoftwareKeyboardController.current
    val fieldFr = remember { FocusRequester() }
    var ff by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(80); runCatching { fieldFr.requestFocus() } }
    DlgBox(1100) {
        Text(d.title, color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
        Text(d.desc.ifEmpty { "Pulsa OK sobre el campo para escribir" }, color = Dim, fontSize = 22.sp, fontFamily = Sans, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
        Box(
            Modifier.fillMaxWidth().height(68.dp).background(Color(0x14FFFFFF), RoundedCornerShape(12.dp))
                .border(if (ff) 3.dp else 1.dp, if (ff) Gold else Color(0x33FFFFFF), RoundedCornerShape(12.dp)).padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                v, { v = it }, singleLine = true, textStyle = TextStyle(color = Color.White, fontSize = 28.sp, fontFamily = Sans), cursorBrush = SolidColor(Gold),
                visualTransformation = if (d.secret) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions.Default,
                modifier = Modifier.fillMaxWidth().focusRequester(fieldFr).onFocusChanged { ff = it.isFocused }
                    .onPreviewKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown && (e.key == Key.DirectionCenter || e.key == Key.Enter)) { kb?.show(); false } else false
                    },
            )
        }
        Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DBtn("Aceptar", true) { close(); d.onOk(v.trim()) }
            DBtn("Cancelar", false) { close() }
        }
    }
}

@Composable
private fun ConfirmDialog(d: Dlg.Confirm, close: () -> Unit) {
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(80); runCatching { fr.requestFocus() } }
    DlgBox(900) {
        Text(d.title, color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
        Text(d.msg, color = Dim, fontSize = 24.sp, fontFamily = Sans, modifier = Modifier.padding(top = 10.dp, bottom = 26.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DBtn(d.yes, false, danger = true) { close(); d.onYes() }
            DBtn("Cancelar", true, Modifier.focusRequester(fr)) { close() }
        }
    }
}

@Composable
private fun DBtn(t: String, primary: Boolean, modifier: Modifier = Modifier, danger: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(
        t, color = if (primary) Color(0xFF1B140A) else if (danger) Danger else Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = Sans,
        modifier = modifier.dlClickable(shape, Color.White, 3.dp, 1.04f, null, onClick).background(if (primary) Gold else Color(0x1FFFFFFF)).padding(horizontal = 36.dp, vertical = 16.dp),
    )
}

@Composable
private fun DiagDialog(vm: MainViewModel, close: () -> Unit) {
    val live by vm.live.collectAsState(); val v by vm.vod.collectAsState(); val st by vm.status.collectAsState(); val er by vm.errors.collectAsState()
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(80); runCatching { fr.requestFocus() } }
    fun drm(u: java.util.UUID) = try { MediaDrm.isCryptoSchemeSupported(u) } catch (_: Throwable) { false }
    val wv = try { android.webkit.WebView.getCurrentWebViewPackage()?.versionName ?: "?" } catch (_: Throwable) { "?" }
    val lines = listOf(
        "Versión" to VER,
        "Dispositivo" to "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}",
        "Canales en vivo" to live.size.toString(),
        "Películas y series" to v.size.toString(),
        "Estado" to st.ifBlank { "Listo" },
        "Widevine" to if (drm(androidx.media3.common.C.WIDEVINE_UUID)) "Sí" else "No",
        "ClearKey" to if (drm(androidx.media3.common.C.CLEARKEY_UUID)) "Sí" else "No",
        "WebView (Shaka)" to wv,
        "Perfil" to vm.activeProfile.value.name,
    )
    DlgBox(1200) {
        Text("Diagnóstico del sistema", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, modifier = Modifier.padding(bottom = 14.dp))
        lines.forEach { (k, x) ->
            Row(Modifier.padding(vertical = 5.dp)) {
                Text(k, Modifier.width(320.dp), color = Dim, fontSize = 24.sp, fontFamily = Sans)
                Text(x, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        er.take(4).forEach { Text("• $it", color = Danger, fontSize = 20.sp, fontFamily = Sans, modifier = Modifier.padding(top = 4.dp), maxLines = 2) }
        Row(Modifier.padding(top = 22.dp)) { DBtn("Cerrar", true, Modifier.focusRequester(fr)) { close() } }
    }
}
