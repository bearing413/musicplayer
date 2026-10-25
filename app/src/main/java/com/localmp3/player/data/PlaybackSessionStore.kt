package com.localmp3.player.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PlaybackSession(
    val songIds: List<Long>,
    val currentIndex: Int,
    val positionMs: Long,
    val playMode: PlayMode,
    val queueTitle: String,
    /** `folder`, `playlist`, or `unknown`. */
    val sourceType: String,
    /** Folder path or user playlist id. */
    val sourceKey: String,
)

class PlaybackSessionStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): PlaybackSession? {
        val raw = prefs.getString(KEY_SESSION, null) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            val idsJson = obj.optJSONArray("songIds") ?: JSONArray()
            val ids = buildList {
                for (i in 0 until idsJson.length()) add(idsJson.getLong(i))
            }
            if (ids.isEmpty()) return null
            val modeName = obj.optString("playMode", PlayMode.SEQUENTIAL.name)
            val mode = runCatching { PlayMode.valueOf(modeName) }.getOrDefault(PlayMode.SEQUENTIAL)
            PlaybackSession(
                songIds = ids,
                currentIndex = obj.optInt("currentIndex", 0).coerceAtLeast(0),
                positionMs = obj.optLong("positionMs", 0L).coerceAtLeast(0L),
                playMode = mode,
                queueTitle = obj.optString("queueTitle", ""),
                sourceType = obj.optString("sourceType", SOURCE_UNKNOWN),
                sourceKey = obj.optString("sourceKey", ""),
            )
        }.getOrNull()
    }

    fun save(session: PlaybackSession, sync: Boolean = false) {
        if (session.songIds.isEmpty()) {
            clear(sync)
            return
        }
        val obj = JSONObject().apply {
            put("songIds", JSONArray(session.songIds))
            put("currentIndex", session.currentIndex)
            put("positionMs", session.positionMs)
            put("playMode", session.playMode.name)
            put("queueTitle", session.queueTitle)
            put("sourceType", session.sourceType)
            put("sourceKey", session.sourceKey)
        }
        val editor = prefs.edit().putString(KEY_SESSION, obj.toString())
        if (sync) editor.commit() else editor.apply()
    }

    fun clear(sync: Boolean = false) {
        val editor = prefs.edit().remove(KEY_SESSION)
        if (sync) editor.commit() else editor.apply()
    }

    companion object {
        const val SOURCE_FOLDER = "folder"
        const val SOURCE_PLAYLIST = "playlist"
        const val SOURCE_UNKNOWN = "unknown"

        private const val PREFS_NAME = "qingyin_playback_session"
        private const val KEY_SESSION = "session_json"
    }
}
