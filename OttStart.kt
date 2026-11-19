package com.davidlg.iptvplayer.ui.ott

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.davidlg.iptvplayer.MainViewModel
import com.davidlg.iptvplayer.OttTab
import com.davidlg.iptvplayer.R
import com.davidlg.iptvplayer.Screen
import com.davidlg.iptvplayer.data.EpgStore
import com.davidlg.iptvplayer.ui.Poppins
import com.davidlg.iptvplayer.ui.dlClickable

internal fun greetingNow(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return if (h in 5..11) "Buenos días" else if (h in 12..18) "Buenas tardes" else "Buenas noches"
}

/** #startView — réplica del inicio OTT (hero + 4 tarjetas + resumen). */
@Composable
fun OttStart(vm: MainViewModel) {
    val live by vm.live.collectAsState()
    val vod by vm.vod.collectAsState()
    val recents by vm.recents.collectAsState()
    val epgVer by vm.epgVersion.collectAsState()
    val last = remember(recents, live) { vm.recentChannels().firstOrNull() }
    val prog = remember(last, epgVer) { last?.let { EpgStore.now(it) } }
    val groupsCount = remember(live) { live.map { it.group }.distinct().size }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Column(Modifier.fillMaxSize().padding(start = 84.dp, end = 84.dp, top = 46.dp), verticalArrangement = Arrangement.spacedBy(34.dp)) {
        // ---------- Hero ----------
        Box(
            Modifier.fillMaxWidth().height(395.dp).clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF0B0A09))
                .border(1.dp, Color(0x29E0C38A), RoundedCornerShape(28.dp))
        ) {
            Image(
                painterResource(R.drawable.logo_mark), null,
                Modifier.align(Alignment.CenterEnd).padding(end = 10.dp).size(380.dp).alpha(0.9f), contentScale = ContentScale.Fit,
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        0f to Color(0xF5080706), 0.46f to Color(0xC7080706), 1f to Color(0x26080706),
                    )
                )
            )
            Column(Modifier.padding(start = 64.dp, top = 50.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(38.dp).height(2.dp).background(OttGoldDim))
                    Text(
                        "DAVIDLG IPTV · " + esDate("EEEE, d 'de' MMMM").uppercase(),
                        color = OttGoldDim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = Poppins,
                        letterSpacing = 3.64.sp, modifier = Modifier.padding(start = 14.dp),
                    )
                }
                Text(
                    greetingNow(), fontSize = 76.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, letterSpacing = (-1.9).sp,
                    style = TextStyle(brush = Brush.verticalGradient(0.15f to Color.White, 1f to OttCream)),
                    modifier = Modifier.padding(top = 14.dp, bottom = 10.dp),
                )
                Text(
                    "Accede rápidamente a tus canales y continúa viendo televisión en vivo.",
                    color = OttText.copy(alpha = 0.72f), fontSize = 24.sp, lineHeight = 36.sp, fontFamily = Poppins,
                    modifier = Modifier.width(760.dp).padding(bottom = 30.dp),
                )
                GoldPill(
                    "▶  CONTINUAR VIENDO", 19, 38, 19, Modifier.focusRequester(first), 1.14f,
                ) {
                    val c = last ?: live.firstOrNull()
                    if (c != null) vm.play(c, live) else vm.ottTab = OttTab.Live
                }
            }
            if (last != null) {
                Row(
                    Modifier.align(Alignment.BottomEnd).padding(end = 52.dp, bottom = 46.dp).width(580.dp).height(160.dp)
                        .clip(RoundedCornerShape(22.dp)).background(Color(0xC7080706))
                        .border(1.dp, Color(0x4DE0C38A), RoundedCornerShape(22.dp))
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Box(
                        Modifier.size(96.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xE6080706))
                            .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (last.logo.isNotBlank()) AsyncImage(last.logo, null, Modifier.fillMaxSize().padding(10.dp), contentScale = ContentScale.Fit)
                        else Text(last.name.take(2).uppercase(), color = OttGold, fontSize = 36.sp, fontWeight = FontWeight.Black, fontFamily = Poppins)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("ÚLTIMO CANAL", color = OttGoldDim, fontSize = 13.sp, fontFamily = Poppins, letterSpacing = 3.38.sp)
                        Text(last.name, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            prog?.title ?: "En vivo", color = OttText.copy(alpha = 0.82f), fontSize = 21.sp, fontFamily = Poppins,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp, bottom = 10.dp),
                        )
                        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x2EFFFFFF))) {
                            Box(Modifier.fillMaxWidth(EpgStore.progress(prog)).fillMaxHeight().background(OttGold))
                        }
                    }
                }
            }
        }
        // ---------- Tarjetas ----------
        Row(Modifier.fillMaxWidth().height(218.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            StartCard(Ico.tv, "TV EN VIVO", "Explora categorías y canales", Modifier.weight(1f)) { vm.ottTab = OttTab.Live }
            StartCard(Ico.play, "PELÍCULAS Y SERIES", if (vod.isEmpty()) "Explora tu catálogo VOD" else "${vod.size} películas y series disponibles", Modifier.weight(1f)) { vm.ottTab = OttTab.Movie }
            StartCard(Ico.fav, "MIS CANALES", if (recents.isEmpty()) "Sin canales recientes" else "${recents.size} canales recientes", Modifier.weight(1f)) { vm.ottTab = OttTab.Fav }
            StartCard(Ico.gear, "AJUSTES", "Configura M3U y EPG", Modifier.weight(1f)) { vm.screen = Screen.Settings }
        }
        // ---------- Resumen ----------
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x1AE0C38A)))
        Row(Modifier.fillMaxWidth().padding(top = 0.dp)) {
            Stat("ÚLTIMO CANAL", last?.name ?: "Ninguno", Modifier.weight(838f))
            Box(Modifier.width(1.dp).height(72.dp).background(Color(0x33E0C38A)))
            Stat("CANALES CARGADOS", "%,d".format(live.size).replace(",", ","), Modifier.weight(437f).padding(start = 34.dp))
            Box(Modifier.width(1.dp).height(72.dp).background(Color(0x33E0C38A)))
            Stat("GRUPOS", groupsCount.toString(), Modifier.weight(476f).padding(start = 34.dp))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier.padding(top = 0.dp)) {
        Text(label, color = OttGoldDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = Poppins, letterSpacing = 2.64.sp)
        Text(value, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, fontFamily = Poppins, maxLines = 1, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun StartCard(ico: List<String>, title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier.fillMaxHeight()
            .dlClickable(shape, Color.White, 3.dp, 1.03f, null, onClick)
            .background(Brush.linearGradient(listOf(Color(0xFF1A1512), Color(0xFF15110E), Color(0xFF0B0A09)), tileMode = TileMode.Clamp))
            .border(1.dp, Color(0xFF3A3126), shape)
    ) {
        Column(Modifier.padding(start = 30.dp, top = 30.dp, end = 30.dp, bottom = 28.dp)) {
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Color(0x3DE0C38A), Color(0x0FE0C38A))))
                    .border(1.dp, Color(0x57E0C38A), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) { SvgIcon(ico, 34.dp, OttCream, 1.7f) }
            Box(Modifier.weight(1f))
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, letterSpacing = 0.88.sp, maxLines = 1)
            Text(sub, color = OttText.copy(alpha = 0.58f), fontSize = 15.sp, fontFamily = Poppins, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 28.dp, bottom = 28.dp).size(42.dp).clip(RoundedCornerShape(50))
                .border(1.dp, Color(0x59E0C38A), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) { Text("›", color = OttCream, fontSize = 22.sp) }
    }
}
