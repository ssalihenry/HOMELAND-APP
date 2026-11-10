package com.homeland.app

data class Episode(
    val season: String = "",
    val episode: String = "",
    val url: String = "",
    val subtitleUrl: String = "",
    val streamToken: String = ""
)

data class Movie(
    val id: String = "",
    val title: String = "",
    val poster: String = "",
    val videoUrl: String = "",
    val category: String = "",
    val videoTag: String = "",
    val year: String = "",
    val type: String = "Movie",
    val isVip: Boolean = false,
    val isFeatured: Boolean = false,
    val subtitleUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val episodes: List<Episode> = emptyList(),
    val isAdult: Boolean = false,
    val yocId: String = "",
    val streamToken: String = ""
)

data class TvChannel(
    val id: String = "",
    val name: String = "",
    val logo: String = "",
    val streamUrl: String = "",
    val category: String = "",
    val isVip: Boolean = false,
    val isFeatured: Boolean = false
)
