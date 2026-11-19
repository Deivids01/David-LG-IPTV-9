package com.davidlg.iptvplayer.ui.classic

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidlg.iptvplayer.MainViewModel
import com.davidlg.iptvplayer.Screen
import com.davidlg.iptvplayer.data.Channel
import com.davidlg.iptvplayer.data.EpgStore
import com.davidlg.iptvplayer.ui.Sans
import com.davidlg.iptvplayer.ui.dlClickable
import com.davidlg.iptvplayer.ui.profiles.ProfileAvatar
import com.davidlg.iptvplayer.ui.vod.ClassicVodTab
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class CTab(val label: String) { Live("TV en vivo"), Guide("Guía"), Vod("Películas y series"), Settings("Ajustes") }

/** Modo clásico (azul, estilo TV tradicional). */
@Composable
fun ClassicShell(vm: MainViewModel) {
    var tab by remember { mutableStateOf(CTab.Live) }
    val profile by vm.activeProfile.collectAsState()
    val live by vm.live.collectAsState()
    val ids by vm.activeIds.collectAsState()
    val src by vm.sourceFilter.collectAsState()
    var clock by remember { mutableStateOf("") }
    val contentFr = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(tab) { delay(150); runCatching { contentFr.requestFocus() } }
    androidx.activity.compose.BackHandler(tab != CTab.Live) { tab = CTab.Live }
    LaunchedEffect(Unit) { while (true) { clock = SimpleDateFormat("HH:mm", Locale.US).format(Date()); delay(1000) } }

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B1E33), Color(0xFF070F1B), Color(0xFF050A12))))) {
        Row(Modifier.fillMaxWidth().height(100.dp).padding(horizontal = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            CTab.values().forEach { t ->
                val on = t == tab
                Column(
                    Modifier.padding(end = 28.dp).dlClickable(RoundedCornerShape(10.dp), Color.White, 3.dp, 1f, { if (it && t != CTab.Settings) tab = t }) { if (t == CTab.Settings) vm.screen = Screen.Settings else tab = t }
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                ) {
                    Text(t.label, color = if (on) Color.White else Color(0xFFA0AEBF), fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                    Box(Modifier.padding(top = 4.dp).width(if (on) 152.dp else 0.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(ClassicAcc))
                }
            }
            Box(Modifier.weight(1f))
            Box(
                Modifier.dlClickable(RoundedCornerShape(14.dp), Color.White, 3.dp, 1f, null) { vm.cycleSource() }
                    .background(Color(0x1AFFFFFF)).padding(horizontal = 24.dp, vertical = 10.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("FUENTE", color = ClassicDim, fontSize = 13.sp, fontFamily = Sans, letterSpacing = 3.sp)
                    Text(if (src == "all") "Todas las listas · ${ids.size}" else (vm.playlists.value.firstOrNull { it.id == src }?.name ?: "Lista"), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                }
            }
            Text(clock, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, modifier = Modifier.padding(horizontal = 30.dp))
            Row(
                Modifier.dlClickable(RoundedCornerShape(14.dp), Color.White, 3.dp, 1f, null) { vm.showProfileSwitcher() }
                    .background(Color(0x1AFFFFFF)).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileAvatar(profile.av, 44.dp)
                Text(profile.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
            }
        }
        Box(Modifier.fillMaxSize().focusRequester(contentFr).focusGroup()) {
            when (tab) {
                CTab.Live -> ClassicLive(vm, live)
                CTab.Guide -> ClassicGuide(vm, live)
                CTab.Vod -> ClassicVodTab(vm)
                CTab.Settings -> {}
            }
        }
    }
}

@Composable
private fun ClassicLive(vm: MainViewModel, live: List<Channel>) {
    val epgVer by vm.epgVersion.collectAsState()
    val groups = remember(live) { live.groupBy { it.group.ifBlank { "Sin grupo" } } }
    var group by remember { mutableStateOf("") }
    val list = remember(live, group) { if (group.isEmpty()) live else groups[group].orEmpty() }
    var sel by remember { mutableStateOf<Channel?>(null) }
    val cur = sel?.takeIf { c -> list.any { it.id == c.id } } ?: list.firstOrNull()
    val now = remember(cur, epgVer) { cur?.let { EpgStore.now(it) } }
    val next = remember(cur, epgVer) { cur?.let { EpgStore.next(it) } }

    Column(Modifier.fillMaxSize().padding(horizontal = 56.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(Modifier.fillMaxWidth().height(315.dp), horizontalArrangement = Arrangement.spacedBy(46.dp)) {
            Box(
                Modifier.width(560.dp).fillMaxHeight().clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(cur?.let { classicHue(it.name) } ?: Color(0xFF334), Color(0xFF1A0F1E))))
            ) {
                if (!now?.icon.isNullOrBlank()) coil.compose.AsyncImage(now!!.icon, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                Text("▶  Pulsa OK para ver", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, modifier = Modifier.align(Alignment.BottomStart).padding(24.dp))
            }
            if (cur != null) Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Text(classicPad(cur.number), color = ClassicAcc, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, modifier = Modifier.border(2.dp, ClassicAcc, RoundedCornerShape(10.dp)).padding(horizontal = 18.dp, vertical = 4.dp))
                    Text(cur.name, color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                    Text(cur.group, color = Color(0xFFB5C2D3), fontSize = 22.sp, fontFamily = Sans, modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x1AFFFFFF)).padding(horizontal = 14.dp, vertical = 4.dp))
                }
                Text(now?.title ?: "Sin información EPG", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, modifier = Modifier.padding(top = 22.dp), maxLines = 1)
                if (now != null) {
                    Text(classicHm(now.start) + " – " + classicHm(now.stop), color = Color(0xFFB5C2D3), fontSize = 26.sp, fontFamily = Sans, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    ClassicBar(classicProgress(now.start, now.stop), 8.dp, Modifier.width(760.dp))
                    Text(now.desc, color = Color(0xFFB5C2D3), fontSize = 24.sp, fontFamily = Sans, maxLines = 2, modifier = Modifier.padding(top = 14.dp))
                }
                next?.let { Text("A continuación · ${classicHm(it.start)} ${it.title}", color = ClassicAcc, fontSize = 26.sp, fontFamily = Sans, modifier = Modifier.padding(top = 16.dp)) }
            }
        }
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            LazyColumn(Modifier.width(410.dp).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(Color(0x990A1424))) {
                itemsIndexed(listOf("") + groups.keys.toList()) { _, g ->
                    val on = g == group
                    Row(
                        Modifier.fillMaxWidth().height(74.dp).dlClickable(RoundedCornerShape(0.dp), Color.White, 3.dp, 1f, { if (it) { group = g; sel = null } }) { group = g }
                            .background(if (on) Color(0xFF16324F) else Color.Transparent).padding(horizontal = 30.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (g.isEmpty()) "Todos los canales" else g, Modifier.weight(1f), color = if (on) Color.White else Color(0xFFA9B6C6), fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text((if (g.isEmpty()) live.size else groups[g]?.size ?: 0).toString(), color = ClassicDim, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                    }
                }
            }
            val st = rememberLazyListState()
            LazyColumn(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(Color(0x66081019)), state = st) {
                itemsIndexed(list, key = { _, c -> c.id }) { _, c ->
                    val p = remember(c, epgVer) { EpgStore.now(c) }
                    val on = c.id == cur?.id
                    Row(
                        Modifier.fillMaxWidth().height(102.dp).dlClickable(RoundedCornerShape(0.dp), ClassicAcc, 3.dp, 1f, { if (it) sel = c }) { vm.play(c, list) }
                            .background(if (on) Color(0xFF16324F) else Color.Transparent).padding(horizontal = 30.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(30.dp),
                    ) {
                        Text(classicPad(c.number), Modifier.width(72.dp), color = if (on) Color.White else Color(0xFFA9B6C6), fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                        ClassicLogo(c.logo, c.name, 92.dp, 70.dp, 36f)
                        Column(Modifier.weight(1f)) {
                            Text(c.name, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(p?.let { classicHm(it.start) + " " + it.title } ?: "Sin información EPG", color = Color(0xFFA9B6C6), fontSize = 24.sp, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (p != null) ClassicBar(classicProgress(p.start, p.stop), 5.dp, Modifier.width(560.dp).padding(top = 4.dp))
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
                }
            }
        }
    }
}

@Composable
private fun ClassicGuide(vm: MainViewModel, live: List<Channel>) {
    val epgVer by vm.epgVersion.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 56.dp)) {
        itemsIndexed(live.take(400), key = { _, c -> c.id }) { _, c ->
            val p = remember(c, epgVer) { EpgStore.now(c) }
            val n = remember(c, epgVer) { EpgStore.next(c) }
            Row(
                Modifier.fillMaxWidth().height(96.dp).dlClickable(RoundedCornerShape(10.dp), ClassicAcc, 3.dp, 1f, null) { vm.play(c, live) }.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                Text(classicPad(c.number), Modifier.width(80.dp), color = ClassicDim, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = Sans)
                Text(c.name, Modifier.width(380.dp), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(p?.let { classicHm(it.start) + " " + it.title } ?: "Sin información", Modifier.width(560.dp), color = Color.White, fontSize = 26.sp, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(n?.let { classicHm(it.start) + " " + it.title } ?: "", color = Color(0xFFA9B6C6), fontSize = 24.sp, fontFamily = Sans, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x14FFFFFF)))
        }
    }
}
