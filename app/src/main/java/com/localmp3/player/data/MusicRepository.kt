package com.localmp3.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MusicRepository(private val context: Context) {

    suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.IS_MUSIC,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection += MediaStore.Audio.Media.RELATIVE_PATH
        }
        @Suppress("DEPRECATION")
        projection += MediaStore.Audio.Media.DATA

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf("1000")
        // Sort in memory by filesystem display name; MediaStore TITLE is not used for UI.
        val sortOrder = "${MediaStore.Audio.Media._ID} ASC"

        val unknownFolder = context.getString(com.localmp3.player.R.string.unknown_folder)
        val unknownTitle = context.getString(com.localmp3.player.R.string.unknown_title)

        context.contentResolver.query(
            collection,
            projection.toTypedArray(),
            selection,
            selectionArgs,
            sortOrder,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val displayNameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val relativePathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                -1
            }
            @Suppress("DEPRECATION")
            val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val mediaStoreTitle = cursor.getString(titleCol)
                val displayName = if (displayNameCol >= 0) cursor.getString(displayNameCol) else null
                val dataPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                val title = resolveDisplayTitle(displayName, dataPath, mediaStoreTitle, unknownTitle)

                val artist = cursor.getString(artistCol)?.takeIf {
                    it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true)
                } ?: context.getString(com.localmp3.player.R.string.unknown_artist)
                val album = cursor.getString(albumCol)
                val duration = cursor.getLong(durationCol).coerceAtLeast(0L)
                val albumId = cursor.getLong(albumIdCol)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id,
                )
                val albumArtUri = if (albumId > 0) {
                    ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId,
                    )
                } else {
                    null
                }

                val relativePath = if (relativePathCol >= 0) cursor.getString(relativePathCol) else null
                val (folderPath, folderName) = resolveFolder(relativePath, dataPath, unknownFolder)

                songs += Song(
                    id = id,
                    title = title,
                    artist = artist,
                    durationMs = duration,
                    contentUri = contentUri,
                    albumArtUri = albumArtUri,
                    album = album,
                    folderPath = folderPath,
                    folderName = folderName,
                )
            }
        }

        songs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    }

    companion object {
        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "flac", "m4a", "wav", "aac", "ogg", "opus", "wma",
            "aiff", "aif", "alac", "ape", "dsf", "dff", "m4b", "mp4", "3gp",
        )

        /**
         * UI title = filesystem filename without extension.
         * Example: `002-韩国电影的使命.mp3` → `002-韩国电影的使命`
         */
        internal fun resolveDisplayTitle(
            displayName: String?,
            dataPath: String?,
            mediaStoreTitle: String?,
            unknownTitle: String,
        ): String {
            val fromDisplay = displayName?.trim()?.takeIf { it.isNotEmpty() }?.let { stripAudioExtension(it) }
            if (!fromDisplay.isNullOrBlank()) return fromDisplay

            val fromData = dataPath?.trim()?.takeIf { it.isNotEmpty() }?.let { path ->
                stripAudioExtension(File(path).name)
            }
            if (!fromData.isNullOrBlank()) return fromData

            return mediaStoreTitle?.trim()?.takeIf { it.isNotEmpty() } ?: unknownTitle
        }

        internal fun stripAudioExtension(fileName: String): String {
            val trimmed = fileName.trim()
            val dot = trimmed.lastIndexOf('.')
            if (dot <= 0) return trimmed
            val ext = trimmed.substring(dot + 1).lowercase()
            return if (ext in AUDIO_EXTENSIONS) trimmed.substring(0, dot) else trimmed
        }

        internal fun resolveFolder(
            relativePath: String?,
            dataPath: String?,
            unknownFolder: String,
        ): Pair<String, String> {
            val relative = relativePath?.trim()?.trimEnd('/')
            if (!relative.isNullOrEmpty()) {
                val name = relative.substringAfterLast('/').ifBlank { relative }
                return relative to name
            }

            if (!dataPath.isNullOrBlank()) {
                val parent = File(dataPath).parentFile
                val path = parent?.absolutePath?.trimEnd('/')
                if (!path.isNullOrBlank()) {
                    val name = parent.name.ifBlank { path }
                    return path to name
                }
            }

            return UNKNOWN_FOLDER_KEY to unknownFolder
        }

        const val UNKNOWN_FOLDER_KEY = "__unknown__"
    }
}
