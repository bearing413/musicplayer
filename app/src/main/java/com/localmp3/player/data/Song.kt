package com.localmp3.player.data

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val contentUri: Uri,
    val albumArtUri: Uri?,
    val album: String?,
    /** Normalized parent path used for grouping (never blank). */
    val folderPath: String,
    /** Short display name for the parent folder. */
    val folderName: String,
)

data class SongFolder(
    val path: String,
    val name: String,
    val songs: List<Song>,
) {
    val trackCount: Int get() = songs.size
}

fun List<Song>.groupByFolder(): List<SongFolder> =
    groupBy { it.folderPath }
        .map { (path, songs) ->
            SongFolder(
                path = path,
                name = songs.first().folderName,
                songs = songs,
            )
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

enum class PlayMode {
    SEQUENTIAL,
    REPEAT_ONE,
    SHUFFLE,
}

fun PlayMode.next(): PlayMode = when (this) {
    PlayMode.SEQUENTIAL -> PlayMode.REPEAT_ONE
    PlayMode.REPEAT_ONE -> PlayMode.SHUFFLE
    PlayMode.SHUFFLE -> PlayMode.SEQUENTIAL
}
