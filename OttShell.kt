package com.davidlg.iptvplayer.ui.ott

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidlg.iptvplayer.MainViewModel
import com.davidlg.iptvplayer.OttTab
import com.davidlg.iptvplayer.R
import com.davidlg.iptvplayer.Screen
import com.davidlg.iptvplayer.ui.Poppins
import com.davidlg.iptvplayer.ui.dlClickable
import com.davidlg.iptvplayer.ui.profiles.ProfileAvatar
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Modo OTT (el "nuevo"): barra superior + Inicio / TV en vivo / VOD / Mi lista. */
@Composable
fun OttShell(vm: MainViewModel) {
    val contentFr = remember { androidx.compose.ui.focus.FocusRequester() }
    // Al cambiar de sección el foco entra al contenido (como en la app de LG).
    LaunchedEffect(vm.ottTab) { delay(150); runCatching { contentFr.requestFocus() } }
    androidx.activity.compose.BackHandler(vm.ottTab != OttTab.Start) { vm.ottTab = OttTab.Start }
    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF14110C), Color(0xFF0A0908), Color(0xFF050505)))
        )
    ) {
        OttTopBar(vm)
        Box(Modifier.fillMaxWidth().weight(1f).focusRequester(contentFr).focusGroup()) {
            when (vm.ottTab) {
                OttTab.Start -> OttStart(vm)
                OttTab.Live -> OttLive(vm)
                OttTab.Movie -> OttVod(vm, "movie", "Películas")
                OttTab.Series -> OttVod(vm, "series", "Series")
                OttTab.Anime -> OttVod(vm, "anime", "Anime")
                OttTab.Fav -> OttVod(vm, "fav", "Mi lista")
            }
        }
    }
}

private data class TabDef(val tab: OttTab, val label: String, val ico: List<String>)

private val TABS = listOf(
    TabDef(OttTab.Start, "Inicio", Ico.home),
    TabDef(OttTab.Movie, "Películas", Ico.movie),
    TabDef(OttTab.Series, "Series", Ico.series),
    TabDef(OttTab.Anime, "Anime", Ico.anime),
    TabDef(OttTab.Live, "TV en vivo", Ico.live),
    TabDef(OttTab.Fav, "Mi lista", Ico.fav),
)

@Composable
private fun OttTopBar(vm: MainViewModel) {
    val profile by vm.activeProfile.collectAsState()
    val playlists by vm.playlists.collectAsState()
    val filter by vm.sourceFilter.collectAsState()
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(1000) } }

    Row(
        Modifier.fillMaxWidth().height(92.dp)
            .background(Color(0xEB0A0908))
            .padding(start = 40.dp, end = 34.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.logo_davidlg), "DAVIDLG IPTV PLAYER", Modifier.size(210.dp, 76.dp), contentScale = ContentScale.Fit)
        Spacer(Modifier.width(69.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            for (t in TABS) {
                val sel = vm.ottTab == t.tab
                var foc by remember { mutableStateOf(false) }
                Row(
                    Modifier.height(55.dp)
                        .dlClickable(RoundedCornerShape(50), Color.White, 2.dp, 1f, { foc = it }) { vm.ottTab = t.tab }
                        .background(if (sel) Color(0x24E0C38A) else Color.Transparent)
                        .then(if (sel) Modifier.border(1.dp, Color(0x73E0C38A), RoundedCornerShape(50)) else Modifier)
                        .padding(horizontal = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    SvgIcon(t.ico, 24.dp, if (sel) Color.White else OttText.copy(alpha = 0.78f), 1.8f)
                    Text(
                        t.label, color = if (sel) Color.White else OttText.copy(alpha = 0.78f),
                        fontSize = 19.sp, fontWeight = FontWeight.SemiBold, fontFamily = Poppins, maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        // Reloj
        Column(Modifier.width(237.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                SimpleDateFormat("EEEE · d MMMM", ES).format(now).uppercase(), color = OttGoldDim,
                fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, letterSpacing = 2.6.sp, maxLines = 1,
            )
            Text(
                SimpleDateFormat("hh:mm:ss a", Locale.US).format(now).uppercase(), color = Color.White,
                fontSize = 34.sp, fontWeight = FontWeight.Black, fontFamily = Poppins, letterSpacing = 1.4.sp, maxLines = 1,
            )
        }
        Spacer(Modifier.width(14.dp))
        // Botón FUENTE
        val srcName = when {
            playlists.isEmpty() -> "Sin listas"
            filter == "all" -> "Todas las listas · ${playlists.size}"
            else -> playlists.firstOrNull { it.id == filter }?.name ?: "Todas las listas"
        }
        Row(
            Modifier.width(257.dp).height(55.dp)
                .dlClickable(RoundedCornerShape(50), Color.White, 2.dp, 1f) {
                    val ids = listOf("all") + playlists.map { it.id }
                    vm.setSourceFilter(ids[(ids.indexOf(filter).coerceAtLeast(0) + 1) % ids.size])
                }
                .background(Brush.linearGradient(listOf(Color(0x38E0C38A), Color(0x0AFFFFFF))))
                .border(1.dp, Color(0x80E0C38A), RoundedCornerShape(50))
                .padding(start = 22.dp, end = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FUENTE", color = OttGoldDim, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, fontFamily = Poppins, letterSpacing = 2.6.sp)
                Text(srcName, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = Poppins, maxLines = 1, letterSpacing = 1.sp)
            }
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(50)).background(Color(0x4DE0C38A)), contentAlignment = Alignment.Center) {
                Text("↻", color = Color.White, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.width(14.dp))
        // Perfil
        Box(
            Modifier.size(60.dp).dlClickable(RoundedCornerShape(18.dp), Color.White, 3.dp, 1.06f) { vm.showProfileSwitcher() }
                .border(2.dp, Color(0x38FFFFFF), RoundedCornerShape(18.dp))
        ) { ProfileAvatar(profile.av, 60.dp) }
        Spacer(Modifier.width(12.dp))
        // Ajustes
        Box(
            Modifier.size(60.dp).dlClickable(RoundedCornerShape(50), Color.White, 3.dp, 1.06f) { vm.screen = Screen.Settings }
                .background(Brush.linearGradient(listOf(Color(0x1FFFFFFF), Color(0x08FFFFFF))))
                .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center,
        ) { SvgIcon(Ico.gear, 28.dp, OttText, 1.8f) }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x1FE0C38A)))
}
