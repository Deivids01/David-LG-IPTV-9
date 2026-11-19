@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.davidlg.iptvplayer.ui.player

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.davidlg.iptvplayer.MainViewModel
import com.davidlg.iptvplayer.data.EpgStore
import com.davidlg.iptvplayer.player.PlaybackFactory
import com.davidlg.iptvplayer.player.ShakaWebView
import com.davidlg.iptvplayer.ui.Sans
import com.davidlg.iptvplayer.ui.dlClickable
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Blue = Color(0xFF56AAFF)
private val Cyan = Color(0xFF7EE7FF)
private val Gold = Color(0xFFE0C38A)

private fun hm(ms: Long): String = SimpleDateFormat("hh:mm a", Locale.US).format(Date(ms)).replace("AM", "A.M.").replace("PM", "P.M.")
private fun clock(): String = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
private fun mmss(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0); val h = s / 3600; val m = (s % 3600) / 60; val x = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, x) else "%02d:%02d".format(m, x)
}

/** Reproductor a pantalla completa: ExoPlayer (único para todo el zapping) con Shaka (WebView) como respaldo. */
@Composable
fun PlayerScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val pp by vm.playbackPrefs.collectAsState()
    val epgVer by vm.epgVersion.collectAsState()
    val player = remember { PlaybackFactory.newPlayer(ctx) }
    val c = vm.current
    var overlay by remember { mutableStateOf(true) }
    var tick by remember { mutableLongStateOf(0L) }
    var buffering by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var digits by remember { mutableStateOf("") }
    var drawer by remember { mutableStateOf(false) }
    var hideAt by remember { mutableLongStateOf(0L) }
    var shakaFor by remember { mutableStateOf<String?>(null) }
    val enginePref = pp.str("engine", "auto")
    val useShaka = c != null && (enginePref == "shaka" || shakaFor == c.id)
    val web = remember {
        ShakaWebView(ctx) { st, msg ->
            when (st) {
                "playing" -> { buffering = false; error = null }
                "buffering" -> buffering = true
                "error" -> { buffering = false; error = msg.ifBlank { "No se pudo reproducir este canal" } }
            }
        }
    }
    var lastZap by remember { mutableLongStateOf(0L) }

    fun show() { overlay = true; hideAt = System.currentTimeMillis() + 5000 }
    fun zap(d: Int) {
        val t = System.currentTimeMillis()
        if (t - lastZap < 120) return
        lastZap = t; vm.zap(d)
    }

    // Preferencias de reproducción (audio/subtítulos) como playbackPrefs de la versión web.
    LaunchedEffect(pp, c?.id) {
        val a = pp.str("audio", "es"); val s = pp.str("subs", "off")
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().apply {
            if (a != "auto") setPreferredAudioLanguage(a)
            if (s == "off") setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true) else { setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false); setPreferredTextLanguage(if (a == "auto") "es" else a) }
            val q = (vm.current?.id?.let { id -> pp.json().optJSONObject("channelQuality")?.optString(id, "") } ?: "").ifBlank { pp.str("quality", "auto") }
            val h = q.toIntOrNull()
            if (h != null) setMaxVideoSize(Int.MAX_VALUE, h) else setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
        }.build()
    }

    DisposableEffect(Unit) {
        val l = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (vm.current?.let { vm.current?.id == it.id } == true) { buffering = state == Player.STATE_BUFFERING; if (state == Player.STATE_READY) error = null }
            }
            override fun onPlayerError(e: PlaybackException) {
                val cur = vm.current
                // ExoPlayer no pudo abrirlo (p. ej. CENC/CBCS): se reintenta con Shaka como en la app de LG.
                if (cur != null && enginePref != "exo" && shakaFor != cur.id) { shakaFor = cur.id; error = null; buffering = true }
                else error = "No se pudo reproducir (${e.errorCodeName})"
            }
        }
        player.addListener(l)
        onDispose {
            vm.current?.let { if (it.isVod) vm.saveVodProgress(it.id, player.currentPosition, player.duration.coerceAtLeast(0)) }
            player.removeListener(l); player.release()
            web.stopPlayback(); web.destroy()
        }
    }

    // Cada cambio de canal reutiliza el mismo motor: sin recrear pantalla ni vista.
    LaunchedEffect(c?.id, useShaka) {
        if (c != null) {
            error = null; buffering = true
            vm.markWatched(c)
            if (useShaka) {
                player.stop()
                web.playJson(PlaybackFactory.shakaJson(c))
            } else {
                web.stopPlayback()
                PlaybackFactory.play(ctx, player, c, vm.startPositionMs)
            }
            vm.startPositionMs = 0L
            show()
        }
    }
    // Vigilante: un canal con DRM que no arranca en 8 s pasa a Shaka.
    LaunchedEffect(c?.id, useShaka) {
        if (c != null && !useShaka && enginePref == "auto" && PlaybackFactory.isDrm(c)) {
            delay(8000)
            if (player.playbackState != Player.STATE_READY && shakaFor != c.id) shakaFor = c.id
        }
    }
    LaunchedEffect(Unit) { while (true) { delay(500); tick++; if (overlay && System.currentTimeMillis() > hideAt && !drawer) overlay = false } }
    // Entrada numérica de canal (como en la TV): se aplica tras 0,7 s sin teclear.
    LaunchedEffect(digits) {
        if (digits.isNotEmpty()) {
            delay(700)
            val n = digits.toIntOrNull(); digits = ""
            vm.queue.firstOrNull { it.number == n }?.let { vm.current = it }
        }
    }
    BackHandler { if (drawer) drawer = false else vm.closePlayer() }

    val isVod = c?.isVod == true

    // Teclas del mando: se reciben a nivel de Activity, sin depender del foco de Compose (CH+/CH− fiables).
    DisposableEffect(Unit) {
        vm.keyHook = hook@{ e ->
            if (drawer) return@hook false
            val code = e.keyCode
            val vodNow = vm.current?.isVod == true
            val dgt = when (code) {
                in android.view.KeyEvent.KEYCODE_0..android.view.KeyEvent.KEYCODE_9 -> code - android.view.KeyEvent.KEYCODE_0
                in android.view.KeyEvent.KEYCODE_NUMPAD_0..android.view.KeyEvent.KEYCODE_NUMPAD_9 -> code - android.view.KeyEvent.KEYCODE_NUMPAD_0
                else -> -1
            }
            val act: (() -> Unit)? = when {
                dgt >= 0 -> ({ if (!vodNow && digits.length < 4) digits += dgt })
                code == android.view.KeyEvent.KEYCODE_CHANNEL_UP || code == android.view.KeyEvent.KEYCODE_PAGE_UP ||
                    code == android.view.KeyEvent.KEYCODE_MEDIA_NEXT -> ({ if (!vodNow) zap(1) })
                code == android.view.KeyEvent.KEYCODE_CHANNEL_DOWN || code == android.view.KeyEvent.KEYCODE_PAGE_DOWN ||
                    code == android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS -> ({ if (!vodNow) zap(-1) })
                code == android.view.KeyEvent.KEYCODE_DPAD_UP -> ({ if (vodNow) show() else zap(1) })
                code == android.view.KeyEvent.KEYCODE_DPAD_DOWN -> ({ if (vodNow) show() else zap(-1) })
                code == android.view.KeyEvent.KEYCODE_DPAD_LEFT -> ({ if (vodNow) { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)); show() } else drawer = true })
                code == android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> ({ if (vodNow) { player.seekTo(player.currentPosition + 10_000); show() } else show() })
                code == android.view.KeyEvent.KEYCODE_DPAD_CENTER || code == android.view.KeyEvent.KEYCODE_ENTER ||
                    code == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> ({
                        if (vodNow) { player.playWhenReady = !player.playWhenReady; show() } else if (overlay) overlay = false else show()
                    })
                code == android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || code == android.view.KeyEvent.KEYCODE_MEDIA_PLAY ||
                    code == android.view.KeyEvent.KEYCODE_MEDIA_PAUSE -> ({ player.playWhenReady = !player.playWhenReady })
                else -> null
            }
            if (act == null) false
            else { if (e.action == android.view.KeyEvent.ACTION_DOWN && (e.repeatCount == 0 || code == android.view.KeyEvent.KEYCODE_CHANNEL_UP || code == android.view.KeyEvent.KEYCODE_CHANNEL_DOWN)) act(); true }
        }
        onDispose { vm.keyHook = null }
    }

    val now = remember(c, epgVer, tick / 20) { c?.let { EpgStore.now(it) } }
    val next = remember(c, epgVer, tick / 20) { c?.let { EpgStore.next(it) } }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .pointerInput(Unit) {
                var dy = 0f
                androidx.compose.foundation.gestures.detectVerticalDragGestures(
                    onDragStart = { dy = 0f }, onDragEnd = { if (vm.current?.isVod != true) { if (dy < -120) zap(1) else if (dy > 120) zap(-1) } },
                ) { _, d -> dy += d }
            }
            .pointerInput(Unit) {
                androidx.compose.foundation.gestures.detectTapGestures(onTap = { if (overlay) overlay = false else show() })
            }
    ) {
        if (!useShaka) AndroidView(
            factory = { x ->
                PlayerView(x).apply {
                    useController = false
                    isFocusable = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    this.player = player
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) else AndroidView(factory = { (web.parent as? ViewGroup)?.removeView(web); web }, modifier = Modifier.fillMaxSize())

        if (buffering && error == null) Box(Modifier.align(Alignment.Center)) {
            androidx.compose.material3.CircularProgressIndicator(color = Blue, strokeWidth = 4.dp, modifier = Modifier.size(64.dp))
        }
        error?.let {
            Column(Modifier.align(Alignment.Center).clip(RoundedCornerShape(18.dp)).background(Color(0xE6101830)).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(it, color = Color.White, fontSize = 26.sp, fontFamily = Sans, fontWeight = FontWeight.Bold)
                Text("Pulsa CH+ / CH− para cambiar de canal o Atrás para salir", color = Color(0xFFB9C7E6), fontSize = 18.sp, fontFamily = Sans, modifier = Modifier.padding(top = 8.dp))
            }
        }
        if (digits.isNotEmpty()) Text(
            digits, color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Black, fontFamily = Sans,
            modifier = Modifier.align(Alignment.TopEnd).padding(48.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xB3000000)).padding(horizontal = 28.dp, vertical = 10.dp),
        )

        if (overlay && c != null) {
            Column(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp).width(1840.dp)
                    .clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(Color(0xE60B1226), Color(0xF2060B18))))
            ) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(Brush.horizontalGradient(listOf(Color(0xFF2F7BFF), Blue, Cyan))))
                Row(Modifier.padding(26.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(Modifier.size(180.dp, 140.dp).clip(RoundedCornerShape(16.dp)).background(Color(0x14FFFFFF)), contentAlignment = Alignment.Center) {
                        if (c.logo.isNotBlank()) AsyncImage(c.logo, null, Modifier.fillMaxSize().padding(12.dp), contentScale = ContentScale.Fit)
                        else Text("TV", color = Blue, fontSize = 40.sp, fontWeight = FontWeight.Black, fontFamily = Sans)
                    }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Badge(if (isVod) "VOD" else "%03d".format(c.number), Blue)
                            Text(c.name, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                            Badge("HD", Color(0xFF9AA7C7))
                            if (!isVod) Badge("LIVE", Color(0xFFFF5252))
                            if (useShaka) Badge("SHAKA", Color(0xFFE0C38A))
                        }
                        if (isVod) {
                            val dur = player.duration.takeIf { it > 0 } ?: 0L
                            val pos = player.currentPosition
                            Text("${mmss(pos)} / ${mmss(dur)}", color = Cyan, fontSize = 24.sp, fontFamily = Sans, modifier = Modifier.padding(top = 14.dp))
                            ProgressBar(if (dur > 0) pos.toFloat() / dur else 0f)
                            Text("◄ −10 s   ·   OK pausa   ·   +10 s ►", color = Color(0xFFB9C7E6), fontSize = 18.sp, fontFamily = Sans, modifier = Modifier.padding(top = 10.dp))
                        } else {
                            ProgRow("AHORA", now?.title ?: "Sin información EPG", now?.let { hm(it.start) + " - " + hm(it.stop) }, now?.let { val m = ((it.stop - System.currentTimeMillis()) / 60000).coerceAtLeast(0); "termina en $m min" }, EpgStore.progress(now), true)
                            ProgRow("DESPUÉS", next?.title ?: "Sin siguiente programa", next?.let { hm(it.start) + " - " + hm(it.stop) }, next?.let { val m = ((it.start - System.currentTimeMillis()) / 60000).coerceAtLeast(0); "en $m minutos" }, 0f, false)
                        }
                    }
                    if (pp.str("clock", "on") != "off") Text(clock(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, modifier = Modifier.align(Alignment.Bottom))
                }
            }
        }

        if (drawer) ChannelDrawer(vm) { drawer = false }
    }
}

@Composable private fun Badge(t: String, col: Color) {
    Text(t, color = col, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = Sans,
        modifier = Modifier.border(1.5.dp, col, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 2.dp))
}

@Composable private fun ProgressBar(f: Float) {
    Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x33FFFFFF))) {
        Box(Modifier.fillMaxWidth(f.coerceIn(0f, 1f)).fillMaxHeight().background(Gold))
    }
}

@Composable private fun ProgRow(tag: String, title: String, range: String?, extra: String?, prog: Float, bar: Boolean) {
    Column(Modifier.padding(top = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(tag, color = Blue, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = Sans, modifier = Modifier.width(92.dp))
            Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (range != null) Text(range, color = Color(0xFFB9C7E6), fontSize = 18.sp, fontFamily = Sans)
            if (extra != null) Text(extra, color = Cyan, fontSize = 18.sp, fontFamily = Sans)
        }
        if (bar) ProgressBar(prog)
    }
}

@Composable
private fun ChannelDrawer(vm: MainViewModel, onClose: () -> Unit) {
    val q = vm.queue
    val idx = q.indexOfFirst { it.id == vm.current?.id }.coerceAtLeast(0)
    val st = androidx.compose.foundation.lazy.rememberLazyListState(idx)
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
    Row(Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClose() }) {
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.width(520.dp).fillMaxHeight().background(Color(0xF2070C1A)).padding(16.dp), state = st, verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            androidx.compose.foundation.lazy.itemsIndexed(q) { i, ch ->
                val shape = RoundedCornerShape(12.dp)
                Row(
                    Modifier.fillMaxWidth().height(64.dp)
                        .then(if (i == idx) Modifier.focusRequester(fr) else Modifier)
                        .dlClickable(shape, Color.White, 3.dp, 1f, null) { vm.current = ch; onClose() }
                        .background(if (i == idx) Color(0x3356AAFF) else Color(0x14FFFFFF)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("%03d".format(ch.number), color = Blue, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = Sans)
                    Text(ch.name, color = Color.White, fontSize = 22.sp, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
