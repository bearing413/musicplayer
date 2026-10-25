package com.localmp3.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.localmp3.player.data.Song
import com.localmp3.player.data.UserPlaylist
import com.localmp3.player.ui.theme.LocalMp3Theme

@Composable
fun PlayerApp(
    state: PlayerUiState,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onRefreshLibrary: () -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayPlaylistSong: (Song, List<Song>, String, String) -> Unit,
    onEnsureQueue: () -> Unit,
    onQueueSongClick: (Song) -> Unit,
    onCreatePlaylist: (String) -> UserPlaylist,
    onDeletePlaylist: (String) -> Unit,
    onSetPlaylistSongs: (String, List<Long>) -> Unit,
    onRemoveSongFromPlaylist: (String, Long) -> Unit,
    onDeleteSongFile: (Song) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleMode: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenNowPlaying: () -> Unit,
) {
    LocalMp3Theme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (state.permissionStatus) {
                PermissionStatus.Unknown -> {
                    PermissionRequestScreen(onGrant = onGrantPermission)
                }

                PermissionStatus.Denied -> {
                    PermissionDeniedScreen(
                        permanentlyDenied = false,
                        onGrant = onGrantPermission,
                        onOpenSettings = onOpenSettings,
                    )
                }

                PermissionStatus.PermanentlyDenied -> {
                    PermissionDeniedScreen(
                        permanentlyDenied = true,
                        onGrant = onGrantPermission,
                        onOpenSettings = onOpenSettings,
                    )
                }

                PermissionStatus.Granted -> {
                    AnimatedContent(
                        targetState = state.screen,
                        transitionSpec = {
                            if (targetState == Screen.Library) {
                                (slideInHorizontally { it } + fadeIn()) togetherWith
                                    (slideOutHorizontally { -it / 4 } + fadeOut())
                            } else {
                                (slideInHorizontally { -it } + fadeIn()) togetherWith
                                    (slideOutHorizontally { it / 4 } + fadeOut())
                            }
                        },
                        label = "screen",
                    ) { screen ->
                        when (screen) {
                            Screen.NowPlaying -> NowPlayingScreen(
                                song = state.currentSong,
                                isPlaying = state.isPlaying,
                                positionMs = state.positionMs,
                                durationMs = state.durationMs.takeIf { it > 0 }
                                    ?: state.currentSong?.durationMs ?: 0L,
                                playMode = state.playMode,
                                playQueue = state.playQueue,
                                queueTitle = state.queueTitle,
                                librarySongs = state.songs,
                                onPlayPause = onPlayPause,
                                onPrevious = onPrevious,
                                onNext = onNext,
                                onSeek = onSeek,
                                onCycleMode = onCycleMode,
                                onOpenLibrary = onOpenLibrary,
                                onEnsureQueue = onEnsureQueue,
                                onQueueSongClick = onQueueSongClick,
                                onDeleteSongFile = onDeleteSongFile,
                            )

                            Screen.Library -> SongListScreen(
                                songs = state.songs,
                                playlists = state.playlists,
                                currentSongId = state.currentSong?.id,
                                isLoading = state.isLoading,
                                onSongClick = onSongClick,
                                onPlayPlaylistSong = onPlayPlaylistSong,
                                onCreatePlaylist = onCreatePlaylist,
                                onDeletePlaylist = onDeletePlaylist,
                                onSetPlaylistSongs = onSetPlaylistSongs,
                                onRemoveSongFromPlaylist = onRemoveSongFromPlaylist,
                                onDeleteSongFile = onDeleteSongFile,
                                onBack = onOpenNowPlaying,
                                onRefresh = onRefreshLibrary,
                            )
                        }
                    }
                }
            }
        }
    }
}
