package com.localmp3.player

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.localmp3.player.data.MusicRepository
import com.localmp3.player.data.PlaybackSessionStore
import com.localmp3.player.data.PlaylistStore

class LocalMp3Application : Application() {

    lateinit var musicRepository: MusicRepository
        private set

    lateinit var playlistStore: PlaylistStore
        private set

    lateinit var playbackSessionStore: PlaybackSessionStore
        private set

    override fun onCreate() {
        super.onCreate()
        musicRepository = MusicRepository(this)
        playlistStore = PlaylistStore(this)
        playbackSessionStore = PlaybackSessionStore(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "playback"
    }
}
