package com.localmp3.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localmp3.player.R
import com.localmp3.player.data.PlayMode
import com.localmp3.player.data.Song
import com.localmp3.player.ui.components.AlbumArt
import com.localmp3.player.ui.theme.Copper
import com.localmp3.player.ui.theme.JadeDeep
import com.localmp3.player.ui.theme.JadeSoft
import com.localmp3.player.ui.theme.Mist
import com.localmp3.player.ui.theme.MistWarm
import com.localmp3.player.ui.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    song: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    playMode: PlayMode,
    playQueue: List<Song>,
    queueTitle: String,
    librarySongs: List<Song>,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleMode: () -> Unit,
    onOpenLibrary: () -> Unit,
    onEnsureQueue: () -> Unit,
    onQueueSongClick: (Song) -> Unit,
) {
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }
    var showQueue by remember { mutableStateOf(false) }
    val duration = durationMs.coerceAtLeast(0L)
    val sliderValue = if (scrubbing) {
        scrubValue
    } else if (duration > 0L) {
        (positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val playScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "playScale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MistWarm, Mist, JadeDeep.copy(alpha = 0.08f)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.now_playing),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onOpenLibrary) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = JadeDeep,
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = stringResource(R.string.switch_to_library),
                        color = JadeDeep,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = song?.id to song?.albumArtUri,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "artwork",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { (_, artUri) ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    AlbumArt(
                        artUri = artUri,
                        contentDescription = song?.title,
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .aspectRatio(1f)
                            .scale(playScale),
                        cornerRadius = 32.dp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedContent(
                targetState = song?.id to (song?.title to song?.artist),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "meta",
            ) { (_, meta) ->
                val (title, artist) = meta
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title ?: stringResource(R.string.nothing_playing),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = artist ?: stringResource(R.string.library),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Slider(
                value = sliderValue,
                onValueChange = {
                    scrubbing = true
                    scrubValue = it
                },
                onValueChangeFinished = {
                    scrubbing = false
                    onSeek((scrubValue * duration).toLong())
                },
                enabled = song != null && duration > 0L,
                colors = SliderDefaults.colors(
                    thumbColor = Copper,
                    activeTrackColor = JadeDeep,
                    inactiveTrackColor = JadeDeep.copy(alpha = 0.18f),
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatDuration(
                        if (scrubbing) (scrubValue * duration).toLong() else positionMs,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCycleMode) {
                    Icon(
                        imageVector = when (playMode) {
                            PlayMode.SEQUENTIAL -> Icons.Rounded.Repeat
                            PlayMode.REPEAT_ONE -> Icons.Rounded.RepeatOne
                            PlayMode.SHUFFLE -> Icons.Rounded.Shuffle
                        },
                        contentDescription = when (playMode) {
                            PlayMode.SEQUENTIAL -> stringResource(R.string.mode_sequential)
                            PlayMode.REPEAT_ONE -> stringResource(R.string.mode_repeat_one)
                            PlayMode.SHUFFLE -> stringResource(R.string.mode_shuffle)
                        },
                        tint = if (playMode == PlayMode.SEQUENTIAL) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            Copper
                        },
                        modifier = Modifier.size(28.dp),
                    )
                }

                IconButton(onClick = onPrevious, enabled = song != null) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = stringResource(R.string.previous),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(40.dp),
                    )
                }

                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(76.dp)
                        .background(JadeDeep, CircleShape),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(
                            if (isPlaying) R.string.pause else R.string.play,
                        ),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(40.dp),
                    )
                }

                IconButton(onClick = onNext, enabled = song != null) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.next),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(40.dp),
                    )
                }

                IconButton(
                    onClick = {
                        onEnsureQueue()
                        showQueue = true
                    },
                    enabled = song != null || playQueue.isNotEmpty(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlaylistPlay,
                        contentDescription = stringResource(R.string.view_queue),
                        tint = if (song != null || playQueue.isNotEmpty()) {
                            JadeDeep
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }

    if (showQueue) {
        LaunchedEffect(showQueue) {
            onEnsureQueue()
        }
        // Local fallback so the sheet is never empty for one frame while ViewModel catches up.
        val sheetSongs = playQueue.ifEmpty {
            song?.let { current ->
                librarySongs.filter { it.folderPath == current.folderPath }.ifEmpty { listOf(current) }
            }.orEmpty()
        }
        val sheetTitle = queueTitle.ifBlank {
            song?.folderName?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.current_queue)
        }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = sheetState,
            containerColor = Mist,
        ) {
            QueueSheetContent(
                title = sheetTitle,
                songs = sheetSongs,
                currentSongId = song?.id,
                onSongClick = {
                    onQueueSongClick(it)
                    showQueue = false
                },
            )
        }
    }
}

@Composable
private fun QueueSheetContent(
    title: String,
    songs: List<Song>,
    currentSongId: Long?,
    onSongClick: (Song) -> Unit,
) {
    val listState = rememberLazyListState()
    val currentIndex = remember(songs, currentSongId) {
        songs.indexOfFirst { it.id == currentSongId }
    }
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    // Avoid fillMaxHeight(fraction) inside ModalBottomSheet — unbounded parent
    // height can measure to 0 and the sheet appears as a no-op / empty flash.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp, max = 560.dp)
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        Text(
            text = stringResource(R.string.song_count, songs.size),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.queue_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
            ) {
                itemsIndexed(songs, key = { _, s -> s.id }) { index, item ->
                    val selected = item.id == currentSongId
                    val background by animateColorAsState(
                        targetValue = if (selected) JadeSoft else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        label = "queueRow",
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(background)
                            .clickable { onSongClick(item) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "%02d".format(index + 1),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selected) JadeDeep else MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = item.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = Copper,
                                modifier = Modifier.size(20.dp),
                            )
                        } else {
                            Text(
                                text = formatDuration(item.durationMs),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
