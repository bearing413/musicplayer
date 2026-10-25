package com.localmp3.player.data

data class UserPlaylist(
    val id: String,
    val name: String,
    val songIds: List<Long>,
) {
    fun withSongs(library: List<Song>): List<Song> {
        val byId = library.associateBy { it.id }
        return songIds.mapNotNull { byId[it] }
    }
}
