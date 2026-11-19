package com.davidlg.iptvplayer.data

/** contentType igual que en la versión web: "mixed" (TV + VOD), "tv" (solo TV en vivo) o "vod" (solo VOD). */
data class Playlist(
    val id: String,
    val name: String,
    val m3uUrl: String,
    val epgUrl: String = "",
    val contentType: String = "mixed",
    val epgShift: Int = 0,
)

data class Channel(
    val id: String,
    val number: Int,
    val name: String,
    val tvgId: String,
    val tvgName: String,
    val group: String,
    val logo: String,
    val url: String,
    val props: Map<String, String>,
    val isVod: Boolean,
    val sourceId: String,
    val sourceName: String = "",
)

data class Program(
    val title: String,
    val desc: String,
    val start: Long,
    val stop: Long,
    val icon: String = "",
)

data class Profile(val id: String, val name: String, val av: Int)
