package com.localmp3.player.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.localmp3.player.LocalMp3Application
import com.localmp3.player.data.PlayMode
import com.localmp3.player.data.PlaybackSession
import com.localmp3.player.data.PlaybackSessionStore
import com.localmp3.player.data.Song
import com.localmp3.player.data.UserPlaylist
import com.localmp3.player.data.next
import com.localmp3.player.playback.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class Screen { NowPlaying, Library }

enum class PermissionStatus { Unknown, Granted, Denied, PermanentlyDenied }

data class PlayerUiState(
    val permissionStatus: PermissionStatus = PermissionStatus.Unknown,
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val playlists: List<UserPlaylist> = emptyList(),
    val playQueue: List<Song> = emptyList(),
    val queueTitle: String = "",
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playMode: PlayMode = PlayMode.SEQUENTIAL,
    val screen: Screen = Screen.NowPlaying,
    val errorMessage: String? = null,
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LocalMp3Application
    private val repository = app.musicRepository
    private val playlistStore = app.playlistStore
    private val sessionStore = app.playbackSessionStore

    private val _uiState = MutableStateFlow(PlayerUiState(playlists = playlistStore.load()))
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionJob: Job? = null
    private var sessionSaveJob: Job? = null
    private var shuffleOrder: List<Int> = emptyList()
    private var shuffleCursor: Int = 0

    private var queueSourceType: String = PlaybackSessionStore.SOURCE_UNKNOWN
    private var queueSourceKey: String = ""
    private var didAttemptRestore: Boolean = false
    private var pendingRestorePositionMs: Long? = null
    private var pendingRestorePaused: Boolean = false

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startPositionUpdates()
                startPeriodicSessionSave()
            } else {
                stopPositionUpdates()
                stopPeriodicSessionSave()
                persistSession(sync = false)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncCurrentSongFromPlayer()
            refreshProgress()
            persistSession(sync = false)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            refreshProgress()
            if (playbackState == Player.STATE_ENDED) {
                onTrackEnded()
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                events.contains(Player.EVENT_TIMELINE_CHANGED) ||
                events.contains(Player.EVENT_IS_PLAYING_CHANGED)
            ) {
                syncCurrentSongFromPlayer()
                refreshProgress()
            }
        }
    }

    init {
        refreshPermissionStatus()
        connectController()
    }

    fun refreshPermissionStatus() {
        val granted = hasAudioPermission(getApplication())
        _uiState.update { state ->
            val nextStatus = when {
                granted -> PermissionStatus.Granted
                state.permissionStatus == PermissionStatus.PermanentlyDenied ->
                    PermissionStatus.PermanentlyDenied
                state.permissionStatus == PermissionStatus.Denied -> PermissionStatus.Denied
                else -> PermissionStatus.Unknown
            }
            state.copy(permissionStatus = nextStatus)
        }
        if (granted && _uiState.value.songs.isEmpty() && !_uiState.value.isLoading) {
            loadLibrary()
        }
    }

    fun onPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        val status = when {
            granted -> PermissionStatus.Granted
            !shouldShowRationale -> PermissionStatus.PermanentlyDenied
            else -> PermissionStatus.Denied
        }
        _uiState.update { it.copy(permissionStatus = status) }
        if (granted) loadLibrary()
    }

    fun loadLibrary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.loadSongs() }
                .onSuccess { songs ->
                    val playlists = playlistStore.load()
                    val hadQueue = _uiState.value.playQueue.isNotEmpty()
                    val prevQueueIds = _uiState.value.playQueue.map { it.id }
                    val refreshedQueue = prevQueueIds.mapNotNull { id -> songs.find { it.id == id } }
                    val current = _uiState.value.currentSong?.let { cur ->
                        songs.find { s -> s.id == cur.id }
                    } ?: _uiState.value.currentSong
                    val nextQueue = when {
                        refreshedQueue.isNotEmpty() -> refreshedQueue
                        current != null && hadQueue -> {
                            songs.filter { it.folderPath == current.folderPath }.ifEmpty { listOf(current) }
                        }
                        else -> emptyList()
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            songs = songs,
                            playlists = playlists,
                            playQueue = if (nextQueue.isNotEmpty()) nextQueue else it.playQueue,
                            queueTitle = when {
                                it.queueTitle.isNotBlank() && refreshedQueue.isNotEmpty() -> it.queueTitle
                                current != null && nextQueue.isNotEmpty() -> current.folderName
                                else -> it.queueTitle
                            },
                            currentSong = current,
                            screen = if (songs.isEmpty()) Screen.Library else it.screen,
                        )
                    }
                    if (nextQueue.isNotEmpty()) {
                        rebuildShuffleOrder(_uiState.value.playQueue.size)
                    }

                    if (!didAttemptRestore) {
                        didAttemptRestore = true
                        if (_uiState.value.playQueue.isEmpty()) {
                            restoreFromSession(songs)
                        }
                    } else if (hadQueue && refreshedQueue.isEmpty() && current == null) {
                        sessionStore.clear()
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message)
                    }
                }
        }
    }

    fun showLibrary() {
        _uiState.update { it.copy(screen = Screen.Library) }
    }

    fun showNowPlaying() {
        _uiState.update { it.copy(screen = Screen.NowPlaying) }
    }

    /**
     * Start playback. Default queue = all songs in the same folder as [song].
     * Pass [queue] / [queueTitle] when playing from a user playlist.
     */
    fun playSong(
        song: Song,
        queue: List<Song>? = null,
        queueTitle: String? = null,
        sourceType: String? = null,
        sourceKey: String? = null,
    ) {
        val library = _uiState.value.songs
        val activeQueue = when {
            !queue.isNullOrEmpty() -> queue
            else -> {
                val folderSongs = library.filter { it.folderPath == song.folderPath }
                folderSongs.ifEmpty { listOf(song) }
            }
        }
        val title = queueTitle
            ?: song.folderName.ifBlank {
                getApplication<Application>().getString(com.localmp3.player.R.string.current_queue)
            }
        queueSourceType = sourceType
            ?: if (queue.isNullOrEmpty()) {
                PlaybackSessionStore.SOURCE_FOLDER
            } else {
                PlaybackSessionStore.SOURCE_UNKNOWN
            }
        queueSourceKey = sourceKey
            ?: if (queueSourceType == PlaybackSessionStore.SOURCE_FOLDER) song.folderPath else ""
        val index = activeQueue.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } ?: 0
        pendingRestorePaused = false
        pendingRestorePositionMs = null
        startQueue(activeQueue, index, title, playWhenReady = true, startPositionMs = 0L)
    }

    fun jumpInQueue(song: Song) {
        ensurePlayQueue()
        val queue = _uiState.value.playQueue
        val index = queue.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } ?: return
        jumpToQueueIndex(index, goToNowPlaying = false)
    }

    fun ensurePlayQueue() {
        val state = _uiState.value
        if (state.playQueue.isNotEmpty()) return

        val current = state.currentSong
        val fromPlayer = queueFromPlayer(state.songs)
        val folderQueue = current?.let { song ->
            state.songs.filter { it.folderPath == song.folderPath }.ifEmpty { listOf(song) }
        }.orEmpty()
        val queue = when {
            fromPlayer.isNotEmpty() -> fromPlayer
            folderQueue.isNotEmpty() -> folderQueue
            else -> return
        }
        val title = when {
            state.queueTitle.isNotBlank() -> state.queueTitle
            current != null -> current.folderName
            else -> getApplication<Application>().getString(com.localmp3.player.R.string.current_queue)
        }
        if (queueSourceType == PlaybackSessionStore.SOURCE_UNKNOWN && current != null) {
            queueSourceType = PlaybackSessionStore.SOURCE_FOLDER
            queueSourceKey = current.folderPath
        }
        _uiState.update {
            it.copy(
                playQueue = queue,
                queueTitle = title.ifBlank {
                    getApplication<Application>().getString(com.localmp3.player.R.string.current_queue)
                },
                currentSong = current ?: queue.firstOrNull() ?: it.currentSong,
            )
        }
        rebuildShuffleOrder(queue.size)
        persistSession(sync = false)
    }

    fun createPlaylist(name: String): UserPlaylist {
        val playlist = playlistStore.create(name)
        refreshPlaylists()
        return playlist
    }

    fun deletePlaylist(id: String) {
        playlistStore.delete(id)
        refreshPlaylists()
    }

    fun setPlaylistSongs(playlistId: String, songIds: List<Long>) {
        val current = playlistStore.load().find { it.id == playlistId } ?: return
        playlistStore.update(current.copy(songIds = songIds.distinct()))
        refreshPlaylists()
    }

    fun addSongsToPlaylist(playlistId: String, songIds: List<Long>) {
        val current = playlistStore.load().find { it.id == playlistId } ?: return
        playlistStore.update(current.copy(songIds = (current.songIds + songIds).distinct()))
        refreshPlaylists()
    }

    fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        val current = playlistStore.load().find { it.id == playlistId } ?: return
        playlistStore.update(current.copy(songIds = current.songIds.filterNot { it == songId }))
        refreshPlaylists()
    }

    private fun refreshPlaylists() {
        _uiState.update { it.copy(playlists = playlistStore.load()) }
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.mediaItemCount == 0) {
            val queue = _uiState.value.playQueue
            if (queue.isNotEmpty()) {
                val song = _uiState.value.currentSong ?: queue.first()
                val index = queue.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } ?: 0
                val position = _uiState.value.positionMs
                startQueue(
                    queue = queue,
                    index = index,
                    title = _uiState.value.queueTitle.ifBlank { song.folderName },
                    playWhenReady = true,
                    startPositionMs = position,
                )
                return
            }
            val first = _uiState.value.songs.firstOrNull() ?: return
            playSong(first)
            return
        }
        if (player.isPlaying) player.pause() else player.play()
    }

    fun skipNext() {
        val player = controller ?: return
        val queue = _uiState.value.playQueue
        if (queue.isEmpty()) return

        when (_uiState.value.playMode) {
            PlayMode.SHUFFLE -> {
                if (shuffleOrder.isEmpty()) rebuildShuffleOrder(queue.size)
                shuffleCursor = (shuffleCursor + 1) % shuffleOrder.size
                jumpToQueueIndex(shuffleOrder[shuffleCursor])
            }
            PlayMode.SEQUENTIAL, PlayMode.REPEAT_ONE -> {
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.play()
                } else if (queue.isNotEmpty()) {
                    player.seekTo(0, 0L)
                    player.play()
                }
            }
        }
        persistSession(sync = false)
    }

    fun skipPrevious() {
        val player = controller ?: return
        val queue = _uiState.value.playQueue
        if (queue.isEmpty()) return

        if (player.currentPosition > 3000L) {
            player.seekTo(0L)
            persistSession(sync = false)
            return
        }

        when (_uiState.value.playMode) {
            PlayMode.SHUFFLE -> {
                if (shuffleOrder.isEmpty()) rebuildShuffleOrder(queue.size)
                shuffleCursor = if (shuffleCursor - 1 < 0) shuffleOrder.lastIndex else shuffleCursor - 1
                jumpToQueueIndex(shuffleOrder[shuffleCursor])
            }
            PlayMode.SEQUENTIAL, PlayMode.REPEAT_ONE -> {
                if (player.hasPreviousMediaItem()) {
                    player.seekToPreviousMediaItem()
                    player.play()
                } else {
                    player.seekTo(queue.lastIndex, 0L)
                    player.play()
                }
            }
        }
        persistSession(sync = false)
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
        _uiState.update { it.copy(positionMs = positionMs) }
        persistSession(sync = false)
    }

    fun cyclePlayMode() {
        val nextMode = _uiState.value.playMode.next()
        _uiState.update { it.copy(playMode = nextMode) }
        controller?.let { applyRepeatMode(it, nextMode) }
        if (nextMode == PlayMode.SHUFFLE) {
            val queue = _uiState.value.playQueue
            rebuildShuffleOrder(queue.size)
            val currentId = _uiState.value.currentSong?.id
            val currentIndex = queue.indexOfFirst { it.id == currentId }
            shuffleCursor = shuffleOrder.indexOf(currentIndex).takeIf { it >= 0 } ?: 0
        }
        persistSession(sync = false)
    }

    /** Persist for process death / background. Prefer sync when leaving the app. */
    fun savePlaybackSession(sync: Boolean = true) {
        persistSession(sync = sync)
    }

    private fun restoreFromSession(library: List<Song>) {
        val session = sessionStore.load() ?: return
        val byId = library.associateBy { it.id }
        val queue = session.songIds.mapNotNull { byId[it] }
        if (queue.isEmpty()) {
            sessionStore.clear()
            return
        }
        val preferredId = session.songIds.getOrNull(session.currentIndex)
        val index = queue.indexOfFirst { it.id == preferredId }.takeIf { it >= 0 }
            ?: session.currentIndex.coerceIn(0, queue.lastIndex)
        val title = session.queueTitle.ifBlank { queue[index].folderName }
        queueSourceType = session.sourceType
        queueSourceKey = session.sourceKey
        pendingRestorePaused = true
        pendingRestorePositionMs = session.positionMs

        _uiState.update {
            it.copy(
                playQueue = queue,
                queueTitle = title,
                currentSong = queue[index],
                positionMs = session.positionMs,
                durationMs = queue[index].durationMs,
                playMode = session.playMode,
                screen = Screen.NowPlaying,
                isPlaying = false,
            )
        }
        rebuildShuffleOrder(queue.size)
        if (session.playMode == PlayMode.SHUFFLE) {
            shuffleCursor = shuffleOrder.indexOf(index).takeIf { it >= 0 } ?: 0
        }

        val player = controller
        if (player != null) {
            applyQueueToPlayer(
                player = player,
                queue = queue,
                index = index,
                startPositionMs = session.positionMs,
                playWhenReady = false,
                playMode = session.playMode,
            )
            pendingRestorePaused = false
            pendingRestorePositionMs = null
        }
    }

    private fun startQueue(
        queue: List<Song>,
        index: Int,
        title: String,
        playWhenReady: Boolean,
        startPositionMs: Long,
    ) {
        if (queue.isEmpty()) return
        val safeIndex = index.coerceIn(0, queue.lastIndex)
        _uiState.update {
            it.copy(
                playQueue = queue,
                queueTitle = title,
                currentSong = queue[safeIndex],
                screen = Screen.NowPlaying,
                isPlaying = playWhenReady,
                positionMs = startPositionMs.coerceAtLeast(0L),
            )
        }
        rebuildShuffleOrder(queue.size)
        if (_uiState.value.playMode == PlayMode.SHUFFLE) {
            shuffleCursor = shuffleOrder.indexOf(safeIndex).takeIf { it >= 0 } ?: 0
        }
        persistSession(sync = false)
        val player = controller ?: return
        applyQueueToPlayer(
            player = player,
            queue = queue,
            index = safeIndex,
            startPositionMs = startPositionMs,
            playWhenReady = playWhenReady,
            playMode = _uiState.value.playMode,
        )
    }

    private fun applyQueueToPlayer(
        player: MediaController,
        queue: List<Song>,
        index: Int,
        startPositionMs: Long,
        playWhenReady: Boolean,
        playMode: PlayMode,
    ) {
        player.setMediaItems(queue.map { it.toMediaItem() }, index, startPositionMs.coerceAtLeast(0L))
        player.prepare()
        applyRepeatMode(player, playMode)
        if (playWhenReady) player.play() else player.pause()
    }

    private fun queueFromPlayer(library: List<Song>): List<Song> {
        val player = controller ?: return emptyList()
        if (player.mediaItemCount <= 0) return emptyList()
        val byId = library.associateBy { it.id }
        return buildList {
            for (i in 0 until player.mediaItemCount) {
                val id = player.getMediaItemAt(i).mediaId.toLongOrNull() ?: continue
                byId[id]?.let { add(it) }
            }
        }
    }

    private fun applyPendingQueueIfNeeded(player: MediaController) {
        val state = _uiState.value
        if (state.playQueue.isEmpty()) return
        if (player.mediaItemCount > 0 && !pendingRestorePaused) return

        val song = state.currentSong ?: state.playQueue.firstOrNull() ?: return
        val index = state.playQueue.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } ?: 0
        val position = pendingRestorePositionMs ?: state.positionMs
        val playWhenReady = if (pendingRestorePaused) false else state.isPlaying
        applyQueueToPlayer(
            player = player,
            queue = state.playQueue,
            index = index,
            startPositionMs = position,
            playWhenReady = playWhenReady,
            playMode = state.playMode,
        )
        pendingRestorePaused = false
        pendingRestorePositionMs = null
        _uiState.update { it.copy(isPlaying = playWhenReady, positionMs = position) }
    }

    private fun jumpToQueueIndex(index: Int, goToNowPlaying: Boolean = true) {
        val player = controller ?: return
        val queue = _uiState.value.playQueue
        if (index !in queue.indices) return
        val song = queue[index]
        if (player.mediaItemCount != queue.size) {
            player.setMediaItems(queue.map { it.toMediaItem() }, index, 0L)
            player.prepare()
        } else {
            player.seekTo(index, 0L)
        }
        player.play()
        applyRepeatMode(player, _uiState.value.playMode)
        _uiState.update {
            it.copy(
                currentSong = song,
                isPlaying = true,
                positionMs = 0L,
                screen = if (goToNowPlaying) Screen.NowPlaying else it.screen,
            )
        }
        if (_uiState.value.playMode == PlayMode.SHUFFLE) {
            shuffleCursor = shuffleOrder.indexOf(index).takeIf { it >= 0 } ?: shuffleCursor
        }
        persistSession(sync = false)
    }

    private fun onTrackEnded() {
        when (_uiState.value.playMode) {
            PlayMode.REPEAT_ONE -> {
                controller?.seekTo(0L)
                controller?.play()
            }
            PlayMode.SHUFFLE -> skipNext()
            PlayMode.SEQUENTIAL -> {
                val player = controller ?: return
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.play()
                } else {
                    player.pause()
                    player.seekTo(0, 0L)
                }
            }
        }
        persistSession(sync = false)
    }

    private fun applyRepeatMode(player: Player, mode: PlayMode) {
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.shuffleModeEnabled = false
        if (mode == PlayMode.REPEAT_ONE) {
            player.repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    private fun rebuildShuffleOrder(size: Int) {
        shuffleOrder = (0 until size).shuffled()
        shuffleCursor = 0
    }

    private fun persistSession(sync: Boolean) {
        val state = _uiState.value
        if (state.playQueue.isEmpty()) {
            sessionStore.clear(sync)
            return
        }
        val index = state.playQueue.indexOfFirst { it.id == state.currentSong?.id }
            .takeIf { it >= 0 } ?: 0
        val position = controller?.currentPosition?.coerceAtLeast(0L) ?: state.positionMs
        sessionStore.save(
            PlaybackSession(
                songIds = state.playQueue.map { it.id },
                currentIndex = index,
                positionMs = position,
                playMode = state.playMode,
                queueTitle = state.queueTitle,
                sourceType = queueSourceType,
                sourceKey = queueSourceKey,
            ),
            sync = sync,
        )
    }

    private fun startPeriodicSessionSave() {
        if (sessionSaveJob?.isActive == true) return
        sessionSaveJob = viewModelScope.launch {
            while (isActive) {
                delay(5_000L)
                if (_uiState.value.isPlaying) {
                    persistSession(sync = false)
                }
            }
        }
    }

    private fun stopPeriodicSessionSave() {
        sessionSaveJob?.cancel()
        sessionSaveJob = null
    }

    private fun connectController() {
        val context = getApplication<Application>()
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, token).buildAsync().also { future ->
            future.addListener(
                {
                    val mediaController = runCatching { future.get() }.getOrNull() ?: return@addListener
                    controller = mediaController
                    mediaController.addListener(playerListener)
                    applyPendingQueueIfNeeded(mediaController)
                    ensurePlayQueue()
                    syncCurrentSongFromPlayer()
                    refreshProgress()
                    applyRepeatMode(mediaController, _uiState.value.playMode)
                },
                MoreExecutors.directExecutor(),
            )
        }
    }

    private fun syncCurrentSongFromPlayer() {
        val mediaId = controller?.currentMediaItem?.mediaId ?: return
        val fromQueue = _uiState.value.playQueue.find { it.id.toString() == mediaId }
        val song = fromQueue ?: _uiState.value.songs.find { it.id.toString() == mediaId }
        if (song != null) {
            _uiState.update { it.copy(currentSong = song) }
            if (_uiState.value.playQueue.isEmpty()) {
                ensurePlayQueue()
            }
        }
    }

    private fun refreshProgress() {
        val player = controller ?: return
        _uiState.update {
            it.copy(
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.takeIf { d -> d > 0 } ?: it.currentSong?.durationMs ?: 0L,
                isPlaying = player.isPlaying,
            )
        }
    }

    private fun startPositionUpdates() {
        if (positionJob?.isActive == true) return
        positionJob = viewModelScope.launch {
            while (isActive) {
                refreshProgress()
                delay(400L)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionJob?.cancel()
        positionJob = null
        refreshProgress()
    }

    override fun onCleared() {
        persistSession(sync = true)
        stopPeriodicSessionSave()
        stopPositionUpdates()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onCleared()
    }

    companion object {
        fun hasAudioPermission(context: Context): Boolean {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_AUDIO
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }
            return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        }

        fun requiredPermission(): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.Manifest.permission.READ_MEDIA_AUDIO
            } else {
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }
    }
}

private fun Song.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(albumArtUri)
        .build()
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(contentUri)
        .setMediaMetadata(metadata)
        .build()
}
