package com.davidlg.iptvplayer.ui.ott

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.davidlg.iptvplayer.MainViewModel
import com.davidlg.iptvplayer.data.Channel
import com.davidlg.iptvplayer.data.EpgStore
import com.davidlg.iptvplayer.ui.Poppins
import com.davidlg.iptvplayer.ui.dlClickable

private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun OttVod(vm: MainViewModel, kind: String, title: String) {
    val tab = when (kind) {
        "movie" -> com.davidlg.iptvplayer.OttTab.Movie
        "series" -> com.davidlg.iptvplayer.OttTab.Series
        "anime" -> com.davidlg.iptvplayer.OttTab.Anime
        else -> com.davidlg.iptvplayer.OttTab.Fav
    }
    com.davidlg.iptvplayer.ui.vod.VodScreen(vm, tab, false) { vm.ottTab = com.davidlg.iptvplayer.OttTab.Start }
}

/** #liveView — categorías + banner de vista previa + carrusel de canales + guía de programación. */
@Composable
fun OttLive(vm: MainViewModel) {
    val live by vm.live.collectAsState()
    val epgVer by vm.epgVersion.collectAsState()
    val groups = remember(live) { live.groupBy { it.group.ifBlank { "Sin grupo" } } }
    val names = remember(groups) { listOf("TODOS") + groups.keys.toList() }
    var group by remember { mutableStateOf("TODOS") }
    val list = remember(live, group) { if (group == "TODOS") live else groups[group].orEmpty() }
    var sel by remember { mutableStateOf<Channel?>(null) }
    val cur = sel?.takeIf { c -> list.any { it.id == c.id } } ?: list.firstOrNull()
    val prog = remember(cur, epgVer) { cur?.let { EpgStore.now(it) } }
    val next = remember(cur, epgVer) { cur?.let { EpgStore.next(it) } }

    Row(Modifier.fillMaxSize().padding(start = 42.dp, end = 42.dp, top = 14.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        // ------------ Categorías ------------
        Column(
            Modifier.width(340.dp).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(Color(0xEB0E0C22))
                .border(1.dp, Color(0x29E0C38A), RoundedCornerShape(20.dp)).padding(14.dp)
        ) {
            Text("CATEGORÍAS", color = OttGoldSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, letterSpacing = 3.64.sp, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
            Text("Explora todos nuestros canales por categoría", color = OttText.copy(alpha = 0.6f), fontSize = 13.sp, fontFamily = Poppins, modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(names) { g ->
                    val n = if (g == "TODOS") live.size else groups[g]?.size ?: 0
                    val on = g == group
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        Modifier.fillMaxWidth().height(54.dp)
                            .dlClickable(shape, Color.White, 3.dp, 1f, null) { group = g; sel = null }
                            .background(if (on) Color(0x29E0C38A) else Color(0x0DFFFFFF))
                            .border(1.dp, if (on) OttGold else Color.Transparent, shape)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(g, Modifier.weight(1f), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black, fontFamily = Poppins, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0x26E0C38A)).padding(horizontal = 10.dp, vertical = 2.dp)) {
                            Text(n.toString(), color = OttGold, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins)
                        }
                    }
                }
            }
        }
        // ------------ Columna derecha ------------
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Banner / vista previa
            Box(
                Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF0C1020), Color(0xFF1B1F3D), Color(0xFF0C1020))))
                    .border(1.dp, Color(0x38E0C38A), RoundedCornerShape(22.dp))
            ) {
                if (cur != null) {
                    val art = prog?.icon.orEmpty()
                    if (art.isNotBlank()) {
                        AsyncImage(art, null, Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(900.dp), contentScale = ContentScale.Crop)
                        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color(0xFF0C1020), 0.45f to Color(0xE60C1020), 1f to Color(0x330C1020))))
                    }
                    Row(Modifier.padding(start = 31.dp, top = 28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(68.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x33FFFFFF)), contentAlignment = Alignment.Center) {
                            if (cur.logo.isNotBlank()) AsyncImage(cur.logo, null, Modifier.fillMaxSize().padding(6.dp), contentScale = ContentScale.Fit)
                            else Text("TV", color = OttGold, fontWeight = FontWeight.Black, fontFamily = Poppins, fontSize = 22.sp)
                        }
                        Text(cur.name, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Poppins, maxLines = 1, modifier = Modifier.width(520.dp), overflow = TextOverflow.Ellipsis)
                    }
                    Box(Modifier.align(Alignment.BottomStart).padding(start = 31.dp, bottom = 26.dp)) {
                        GoldPill("▶  Ver en vivo", 22, 36, 16, Modifier.width(234.dp), 0f) { vm.play(cur, list) }
                    }
                    Column(Modifier.align(Alignment.CenterEnd).padding(end = 36.dp).width(740.dp)) {
                        Text(prog?.title ?: "Sin información EPG", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 54.sp)
                        Text(
                            (prog?.let { fmtAmPm(it.start) + " – " + fmtAmPm(it.stop) + " · " } ?: "") + cur.group,
                            color = OttCream, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, fontFamily = Poppins, maxLines = 1, modifier = Modifier.padding(top = 6.dp),
                        )
                        if (art.isBlank() && prog != null) Text("EPG: este programa no trae imagen", color = Color(0xFF9A93D6), fontSize = 15.sp, fontFamily = Poppins, modifier = Modifier.padding(top = 4.dp))
                        Text(prog?.desc.orEmpty(), color = OttText.copy(alpha = 0.78f), fontSize = 24.sp, fontFamily = Poppins, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                    }
                    Row(Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 30.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(Color(0xFF3DDC84)))
                        Text("EN VIVO " + pad3(cur.number), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = Poppins, letterSpacing = 2.sp, modifier = Modifier.padding(start = 10.dp))
                    }
                    Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Color(0x1FFFFFFF))) {
                        Box(Modifier.fillMaxWidth(EpgStore.progress(prog)).fillMaxHeight().background(OttGold))
                    }
                } else {
                    Text("Sin canales. Agrega una lista en Ajustes.", color = OttText, fontSize = 26.sp, fontFamily = Poppins, modifier = Modifier.align(Alignment.Center))
                }
            }
            // Carrusel de canales
            val rowState = rememberLazyListState()
            LazyRow(Modifier.fillMaxWidth().height(176.dp), state = rowState, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(list.take(400), key = { it.id }) { c ->
                    Column(
                        Modifier.width(190.dp).height(158.dp)
                            .dlClickable(CardShape, OttGold, 3.dp, 1.05f, { if (it) sel = c }) { vm.play(c, list) }
                            .background(Brush.verticalGradient(listOf(Color(0xFF1F2747), Color(0xFF121731))))
                            .border(1.dp, Color(0x29E0C38A), CardShape).padding(10.dp)
                    ) {
                        Text(pad3(c.number), color = OttGold, fontSize = 15.sp, fontWeight = FontWeight.Black, fontFamily = Poppins)
                        Box(Modifier.fillMaxWidth().height(84.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x1FFFFFFF)), contentAlignment = Alignment.Center) {
                            if (c.logo.isNotBlank()) AsyncImage(c.logo, null, Modifier.fillMaxSize().padding(6.dp), contentScale = ContentScale.Fit)
                            else Text("TV", color = OttGold, fontWeight = FontWeight.Black, fontFamily = Poppins, fontSize = 26.sp)
                        }
                        Text(c.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black, fontFamily = Poppins, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            // Guía de programación
            Column(
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp)).background(Color(0xEB0E0C22))
                    .border(1.dp, Color(0x29E0C38A), RoundedCornerShape(20.dp)).padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("▣", color = OttGold, fontSize = 22.sp)
                    Text("Guía de Programación", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black, fontFamily = Poppins)
                    Pill(group)
                    Box(Modifier.weight(1f))
                    Pill("${list.size} CANALES · GUÍA EN VIVO")
                    Pill(esDate("EEE d 'DE' MMM").uppercase())
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
                    Text("CANAL", Modifier.width(370.dp), color = OttGoldDim, fontSize = 12.sp, fontFamily = Poppins, letterSpacing = 2.6.sp, fontWeight = FontWeight.Bold)
                    Text("AHORA", Modifier.width(523.dp), color = OttGoldDim, fontSize = 12.sp, fontFamily = Poppins, letterSpacing = 2.6.sp, fontWeight = FontWeight.Bold)
                    Text("SIGUIENTE", color = OttGoldDim, fontSize = 12.sp, fontFamily = Poppins, letterSpacing = 2.6.sp, fontWeight = FontWeight.Bold)
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(list.take(300), key = { it.id }) { c -> EpgRow(vm, c, list, epgVer) { sel = c } }
                }
            }
        }
    }
}

@Composable
private fun Pill(t: String) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(Color(0x1FE0C38A)).border(1.dp, Color(0x40E0C38A), RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 5.dp)) {
        Text(t, color = OttGold, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, letterSpacing = 1.sp, maxLines = 1)
    }
}

@Composable
private fun EpgRow(vm: MainViewModel, c: Channel, list: List<Channel>, ver: Int, onSel: () -> Unit) {
    val p = remember(c, ver) { EpgStore.now(c) }
    val n = remember(c, ver) { EpgStore.next(c) }
    val shape = RoundedCornerShape(15.dp)
    Row(
        Modifier.fillMaxWidth().height(82.dp)
            .dlClickable(shape, OttGold, 3.dp, 1f, { if (it) onSel() }) { vm.play(c, list) },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.width(370.dp).fillMaxHeight().background(Color(0x14FFFFFF), shape).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(pad3(c.number), color = OttGold, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = Poppins)
            Box(Modifier.size(88.dp, 70.dp).clip(RoundedCornerShape(15.dp)).background(Color(0x1FFFFFFF)), contentAlignment = Alignment.Center) {
                if (c.logo.isNotBlank()) AsyncImage(c.logo, null, Modifier.fillMaxSize().padding(6.dp), contentScale = ContentScale.Fit)
                else Text("TV", color = OttGold, fontWeight = FontWeight.Black, fontFamily = Poppins, fontSize = 22.sp)
            }
            Text(c.name, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black, fontFamily = Poppins, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(Modifier.width(523.dp).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF3A4678), Color(0xFF2A3562))), shape).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.Center) {
            if (p != null) {
                Text(fmtAmPm(p.start) + " – " + fmtAmPm(p.stop), color = Color(0xFFDFF5FF), fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = Poppins)
                Text(p.title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black, fontFamily = Poppins, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else Text("Sin información EPG", color = OttText.copy(alpha = 0.7f), fontSize = 24.sp, fontFamily = Poppins)
        }
        Column(Modifier.weight(1f).fillMaxHeight().background(Color(0xEB181E38), shape).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.Center) {
            if (n != null) {
                Text(fmtAmPm(n.start) + " – " + fmtAmPm(n.stop), color = Color(0xFFDFF5FF), fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = Poppins)
                Text(n.title, color = Color.White.copy(alpha = 0.9f), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Poppins, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else Text("Sin siguiente programa", color = OttText.copy(alpha = 0.6f), fontSize = 22.sp, fontFamily = Poppins)
        }
    }
}
