package com.davidlg.iptvplayer.ui.vod

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.davidlg.iptvplayer.MainViewModel
import com.davidlg.iptvplayer.OttTab
import com.davidlg.iptvplayer.data.Channel
import com.davidlg.iptvplayer.ui.Poppins
import com.davidlg.iptvplayer.ui.Sans
import com.davidlg.iptvplayer.ui.dlClickable
import org.json.JSONArray

private val SERIES_RX = Regex("""\bs\d{1,2}\s?e\d{1,3}\b|temporada|season|episodio""", RegexOption.IGNORE_CASE)

/** "movie" | "series" | "anime" según grupo y nombre (como ott-vod.js). */
internal fun vodKind(c: Channel): String {
    val g = c.group.lowercase()
    return when {
        g.contains("anime") -> "anime"
        g.contains("serie") || SERIES_RX.containsMatchIn(c.name) || c.url.contains("/series/") -> "series"
        else -> "movie"
    }
}

private fun readFavs(vm: MainViewModel): Set<String> = try {
    val a = JSONArray(vm.prefs.getString("davidlgVodFavorites", "[]")); (0 until a.length()).map { a.getString(it) }.toSet()
} catch (_: Exception) { emptySet() }

private fun writeFavs(vm: MainViewModel, s: Set<String>) = vm.prefs.putString("davidlgVodFavorites", JSONArray(s.toList()).toString())

/** Catálogo VOD. tab: Movie, Series, Anime o Fav (Mi lista). classic=true usa la paleta azul del modo clásico. */
@Composable
fun VodScreen(vm: MainViewModel, tab: OttTab, classic: Boolean = false, onHome: () -> Unit) {
    val all by vm.vod.collectAsState()
    var favs by remember { mutableStateOf(readFavs(vm)) }
    var q by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<Channel?>(null) }
    val acc = if (classic) Color(0xFF56AAFF) else Color(0xFFE0C38A)
    val font: FontFamily = if (classic) Sans else Poppins
    BackHandler(detail != null) { detail = null }

    val items = remember(all, tab, q, favs) {
        all.filter {
            when (tab) { OttTab.Movie -> vodKind(it) == "movie"; OttTab.Series -> vodKind(it) == "series"; OttTab.Anime -> vodKind(it) == "anime"; else -> it.id in favs }
        }.filter { q.isBlank() || it.name.contains(q, true) || it.group.contains(q, true) }
    }
    val rows = remember(items) { items.groupBy { it.group.ifBlank { "Sin categoría" } }.toList() }
    val hero = items.firstOrNull()

    Column(Modifier.fillMaxSize().padding(horizontal = 64.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            var f by remember { mutableStateOf(false) }
            Box(Modifier.width(306.dp).height(56.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x14FFFFFF)).border(if (f) 3.dp else 1.dp, if (f) acc else Color(0x33FFFFFF), RoundedCornerShape(14.dp)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                BasicTextField(q, { q = it }, singleLine = true, textStyle = TextStyle(color = Color.White, fontSize = 18.sp, fontFamily = font), cursorBrush = SolidColor(acc), modifier = Modifier.fillMaxWidth().onFocusChanged { f = it.isFocused })
                if (q.isEmpty()) Text("Buscar título o categoría…", color = Color(0x66FFFFFF), fontSize = 18.sp, fontFamily = font)
            }
            Chip("‹ INICIO", font, onHome)
        }
        if (hero == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (tab == OttTab.Fav) "Aún no tienes títulos en Mi lista" else "No hay contenido en esta sección", color = Color(0x99FFFFFF), fontSize = 28.sp, fontFamily = font)
            }
        } else LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Column(Modifier.padding(top = 54.dp, bottom = 22.dp, start = 0.dp)) {
                    Text("— DESTACADO DE TU LISTA", color = acc, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = font, letterSpacing = 4.sp)
                    Text(hero.name, color = Color(0xFFF6EEE0), fontSize = 64.sp, fontWeight = FontWeight.Bold, fontFamily = font, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 12.dp))
                    Text(vodKind(hero).uppercase().replace("MOVIE", "PELÍCULA").replace("SERIES", "SERIE") + " · " + hero.group, color = Color.White, fontSize = 22.sp, fontFamily = font, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Btn("▶ REPRODUCIR", true, font) { vm.play(hero, items) }
                        Btn("ⓘ MÁS INFORMACIÓN", false, font) { detail = hero }
                    }
                }
            }
            items(rows, key = { it.first }) { (g, list) ->
                Column {
                    Text(g, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = font, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                        items(list.take(150), key = { it.id }) { c -> Poster(c, acc, font) { detail = c } }
                    }
                }
            }
            item { Box(Modifier.height(40.dp)) }
        }
    }

    detail?.let { c ->
        val fav = c.id in favs
        Box(Modifier.fillMaxSize().background(Color(0xE6000000)), contentAlignment = Alignment.Center) {
            Row(Modifier.width(1300.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFF14110C)).border(1.dp, acc.copy(alpha = .3f), RoundedCornerShape(24.dp)).padding(36.dp), horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                Box(Modifier.width(280.dp).height(420.dp).clip(RoundedCornerShape(16.dp))) { PosterArt(c, acc, font, 80) }
                Column(Modifier.weight(1f)) {
                    Text(c.name, color = Color.White, fontSize = 46.sp, fontWeight = FontWeight.Bold, fontFamily = font, maxLines = 3)
                    Text(c.group, color = acc, fontSize = 22.sp, fontFamily = font, modifier = Modifier.padding(vertical = 10.dp))
                    val prog = vm.vodProgress(c.id)
                    Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Btn("▶ REPRODUCIR", true, font) { detail = null; vm.play(c, items, 0L) }
                        if (prog > 10_000) Btn("↻ CONTINUAR", false, font) { detail = null; vm.play(c, items, prog) }
                        Btn(if (fav) "★ EN MI LISTA" else "☆ MI LISTA", false, font) { favs = if (fav) favs - c.id else favs + c.id; writeFavs(vm, favs) }
                        Btn("CERRAR", false, font) { detail = null }
                    }
                }
            }
        }
    }
}

@Composable private fun Chip(t: String, font: FontFamily, onClick: () -> Unit) {
    val s = RoundedCornerShape(50)
    Text(t, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = font,
        modifier = Modifier.dlClickable(s, Color.White, 3.dp, 1.03f, null, onClick).background(Color(0x14FFFFFF)).border(1.dp, Color(0x33FFFFFF), s).padding(horizontal = 24.dp, vertical = 14.dp))
}

@Composable private fun Btn(t: String, primary: Boolean, font: FontFamily, onClick: () -> Unit) {
    val s = RoundedCornerShape(8.dp)
    Text(t, color = if (primary) Color.Black else Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = font,
        modifier = Modifier.dlClickable(s, Color(0xFFE0C38A), 3.dp, 1.04f, null, onClick).background(if (primary) Color.White else Color(0xFF555555)).padding(horizontal = 26.dp, vertical = 16.dp))
}

@Composable private fun PosterArt(c: Channel, acc: Color, font: FontFamily, fs: Int) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3A2C2A), Color(0xFF1E1412)))), contentAlignment = Alignment.Center) {
        if (c.logo.isNotBlank()) AsyncImage(c.logo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(c.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase(), color = acc, fontSize = fs.sp, fontWeight = FontWeight.Black, fontFamily = font)
    }
}

@Composable private fun Poster(c: Channel, acc: Color, font: FontFamily, onClick: () -> Unit) {
    val s = RoundedCornerShape(12.dp)
    Box(Modifier.size(210.dp, 315.dp).dlClickable(s, acc, 3.dp, 1.06f, null, onClick)) {
        PosterArt(c, acc, font, 72)
        Text(vodKind(c).uppercase().replace("MOVIE", "PELÍCULA").replace("SERIES", "SERIE"), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = font,
            modifier = Modifier.padding(10.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xCC000000)).padding(horizontal = 8.dp, vertical = 3.dp))
        Text(c.name, color = Color.White, fontSize = 16.sp, fontFamily = font, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))).padding(10.dp))
    }
}
