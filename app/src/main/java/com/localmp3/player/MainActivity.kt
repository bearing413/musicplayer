package com.localmp3.player

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.localmp3.player.data.PlaybackSessionStore
import com.localmp3.player.ui.PlayerApp
import com.localmp3.player.ui.PlayerViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val permission = PlayerViewModel.requiredPermission()
        val shouldShow = ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
        viewModel.onPermissionResult(granted, shouldShow)
        if (granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* optional for media notification on API 33+ */ }

    private val deleteConsentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onDeleteConsentResult(result.resultCode == Activity.RESULT_OK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.refreshPermissionStatus()
            }
        }

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                viewModel.savePlaybackSession(sync = true)
            }

            override fun onStop(owner: LifecycleOwner) {
                viewModel.savePlaybackSession(sync = true)
            }
        })

        setContent {
            val state by viewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                viewModel.deleteConsentRequests.collect { request ->
                    deleteConsentLauncher.launch(
                        IntentSenderRequest.Builder(request.intentSender).build(),
                    )
                }
            }

            PlayerApp(
                state = state,
                onGrantPermission = {
                    permissionLauncher.launch(PlayerViewModel.requiredPermission())
                },
                onOpenSettings = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null),
                    )
                    startActivity(intent)
                },
                onRefreshLibrary = viewModel::loadLibrary,
                onSongClick = { song -> viewModel.playSong(song) },
                onPlayPlaylistSong = { song, queue, title, playlistId ->
                    viewModel.playSong(
                        song = song,
                        queue = queue,
                        queueTitle = title,
                        sourceType = PlaybackSessionStore.SOURCE_PLAYLIST,
                        sourceKey = playlistId,
                    )
                },
                onEnsureQueue = viewModel::ensurePlayQueue,
                onQueueSongClick = viewModel::jumpInQueue,
                onCreatePlaylist = viewModel::createPlaylist,
                onDeletePlaylist = viewModel::deletePlaylist,
                onSetPlaylistSongs = viewModel::setPlaylistSongs,
                onRemoveSongFromPlaylist = viewModel::removeSongFromPlaylist,
                onDeleteSongFile = viewModel::deleteSongFile,
                onPlayPause = viewModel::togglePlayPause,
                onPrevious = viewModel::skipPrevious,
                onNext = viewModel::skipNext,
                onSeek = viewModel::seekTo,
                onCycleMode = viewModel::cyclePlayMode,
                onOpenLibrary = viewModel::showLibrary,
                onOpenNowPlaying = viewModel::showNowPlaying,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissionStatus()
    }
}
