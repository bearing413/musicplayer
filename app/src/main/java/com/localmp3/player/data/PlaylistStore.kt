package com.localmp3.player.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PlaylistStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<UserPlaylist> {
        val raw = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val idsJson = obj.optJSONArray("songIds") ?: JSONArray()
                    val ids = buildList {
                        for (j in 0 until idsJson.length()) {
                            add(idsJson.getLong(j))
                        }
                    }
                    add(
                        UserPlaylist(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            songIds = ids,
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(playlists: List<UserPlaylist>) {
        val array = JSONArray()
        playlists.forEach { playlist ->
            array.put(
                JSONObject().apply {
                    put("id", playlist.id)
                    put("name", playlist.name)
                    put("songIds", JSONArray(playlist.songIds))
                },
            )
        }
        prefs.edit().putString(KEY_PLAYLISTS, array.toString()).apply()
    }

    fun create(name: String): UserPlaylist {
        val trimmed = name.trim().ifBlank { "未命名歌单" }
        val playlist = UserPlaylist(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            songIds = emptyList(),
        )
        save(load() + playlist)
        return playlist
    }

    fun update(playlist: UserPlaylist) {
        val next = load().map { if (it.id == playlist.id) playlist else it }
        save(next)
    }

    fun delete(id: String) {
        save(load().filterNot { it.id == id })
    }

    companion object {
        private const val PREFS_NAME = "qingyin_playlists"
        private const val KEY_PLAYLISTS = "playlists_json"
    }
}
