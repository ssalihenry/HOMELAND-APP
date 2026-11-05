package com.homeland.app

data class Movie(
    val id: String = "",
    val title: String = "",
    val poster: String = "",
    val videoUrl: String = "",
    val category: String = "",
    val videoTag: String = "",
    val year: String = "",
    val type: String = "Movie" // "Movie" or "TV Show"
)

data class TvChannel(
    val id: String = "",
    val name: String = "",
    val logo: String = "",
    val streamUrl: String = "",
    val category: String = ""
)
