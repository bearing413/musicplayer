package com.localmp3.player.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.localmp3.player.R
import com.localmp3.player.data.MusicRepository
import com.localmp3.player.data.Song
import com.localmp3.player.data.SongFolder
import com.localmp3.player.data.UserPlaylist
import com.localmp3.player.data.groupByFolder
import com.localmp3.player.ui.components.AlbumArt
import com.localmp3.player.ui.theme.Copper
import com.localmp3.player.ui.theme.JadeDeep
import com.localmp3.player.ui.theme.JadeSoft
import com.localmp3.player.ui.theme.Mist
import com.localmp3.player.ui.theme.MistWarm
import com.localmp3.player.ui.util.formatDuration

private enum class LibraryTab { All, Folders, Playlists }

private enum class LibraryPane { AllSongs, FolderList, FolderSongs, PlaylistList, PlaylistSongs, AddSongs }

@Composable
fun SongListScreen(
    songs: List<Song>,
    playlists: List<UserPlaylist>,
    currentSongId: Long?,
    isLoading: Boolean,
    onSongClick: (Song) -> Unit,
    onPlayPlaylistSong: (Song, List<Song>, String, String) -> Unit,
    onCreatePlaylist: (String) -> UserPlaylist,
    onDeletePlaylist: (String) -> Unit,
    onSetPlaylistSongs: (String, List<Long>) -> Unit,
    onRemoveSongFromPlaylist: (String, Long) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(LibraryTab.All) }
    var selectedFolderPath by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var addingToPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val folders = remember(songs) { songs.groupByFolder() }
    val selectedFolder = remember(folders, selectedFolderPath) {
        folders.find { it.path == selectedFolderPath }
    }
    val selectedPlaylist = remember(playlists, selectedPlaylistId) {
        playlists.find { it.id == selectedPlaylistId }
    }
    val playlistSongs = remember(selectedPlaylist, songs) {
        selectedPlaylist?.withSongs(songs).orEmpty()
    }

    LaunchedEffect(folders, selectedFolderPath) {
        if (selectedFolderPath != null && selectedFolder == null) {
            selectedFolderPath = null
        }
    }
    LaunchedEffect(playlists, selectedPlaylistId) {
        if (selectedPlaylistId != null && selectedPlaylist == null) {
            selectedPlaylistId = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Mist, MistWarm)))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        LibraryHeader(
            tab = tab,
            selectedFolder = selectedFolder,
            selectedPlaylist = selectedPlaylist,
            addingSongs = addingToPlaylistId != null,
            songCount = songs.size,
            folderCount = folders.size,
            playlistCount = playlists.size,
            playlistSongCount = playlistSongs.size,
            onBack = {
                when {
                    addingToPlaylistId != null -> addingToPlaylistId = null
                    selectedFolder != null -> selectedFolderPath = null
                    selectedPlaylist != null -> selectedPlaylistId = null
                    else -> onBack()
                }
            },
            onRefresh = onRefresh,
            onCreatePlaylist = { showCreateDialog = true },
            showCreate = tab == LibraryTab.Playlists &&
                selectedPlaylist == null &&
                addingToPlaylistId == null &&
                !isLoading,
        )

        val showTabs = !isLoading &&
            songs.isNotEmpty() &&
            selectedFolder == null &&
            selectedPlaylist == null &&
            addingToPlaylistId == null

        if (showTabs) {
            LibraryTabBar(
                tab = tab,
                onTabChange = { next ->
                    tab = next
                    selectedFolderPath = null
                    selectedPlaylistId = null
                    addingToPlaylistId = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        when {
            isLoading -> LoadingState()
            songs.isEmpty() -> EmptyLibrary(onRefresh = onRefresh)
            else -> {
                AnimatedContent(
                    targetState = when {
                        addingToPlaylistId != null -> LibraryPane.AddSongs to addingToPlaylistId
                        selectedFolderPath != null -> LibraryPane.FolderSongs to selectedFolderPath
                        selectedPlaylistId != null -> LibraryPane.PlaylistSongs to selectedPlaylistId
                        tab == LibraryTab.Folders -> LibraryPane.FolderList to null
                        tab == LibraryTab.Playlists -> LibraryPane.PlaylistList to null
                        else -> LibraryPane.AllSongs to null
                    },
                    transitionSpec = {
                        (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 5 } + fadeOut())
                    },
                    label = "libraryContent",
                    modifier = Modifier.fillMaxSize(),
                ) { (pane, key) ->
                    when (pane) {
                        LibraryPane.AllSongs -> SongList(
                            songs = songs,
                            currentSongId = currentSongId,
                            onSongClick = onSongClick,
                        )

                        LibraryPane.FolderList -> FolderList(
                            folders = folders,
                            onFolderClick = { selectedFolderPath = it.path },
                        )

                        LibraryPane.FolderSongs -> {
                            val folderSongs = folders.find { it.path == key }?.songs.orEmpty()
                            SongList(
                                songs = folderSongs,
                                currentSongId = currentSongId,
                                onSongClick = onSongClick,
                            )
                        }

                        LibraryPane.PlaylistList -> PlaylistList(
                            playlists = playlists,
                            songs = songs,
                            onPlaylistClick = { selectedPlaylistId = it.id },
                            onDelete = onDeletePlaylist,
                        )

                        LibraryPane.PlaylistSongs -> {
                            val pl = playlists.find { it.id == key }
                            val list = pl?.withSongs(songs).orEmpty()
                            PlaylistDetail(
                                songs = list,
                                currentSongId = currentSongId,
                                onSongClick = { song ->
                                    if (pl != null) {
                                        onPlayPlaylistSong(song, list, pl.name, pl.id)
                                    }
                                },
                                onRemove = { songId ->
                                    if (key != null) onRemoveSongFromPlaylist(key, songId)
                                },
                                onAddSongs = { if (key != null) addingToPlaylistId = key },
                            )
                        }

                        LibraryPane.AddSongs -> {
                            AddSongsScreen(
                                songs = songs,
                                alreadySelected = playlists.find { it.id == key }?.songIds.orEmpty().toSet(),
                                onDone = { picked ->
                                    if (key != null) {
                                        onSetPlaylistSongs(key, picked)
                                    }
                                    addingToPlaylistId = null
                                },
                                onCancel = { addingToPlaylistId = null },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                val created = onCreatePlaylist(name)
                showCreateDialog = false
                tab = LibraryTab.Playlists
                selectedPlaylistId = created.id
            },
        )
    }
}

@Composable
private fun LibraryHeader(
    tab: LibraryTab,
    selectedFolder: SongFolder?,
    selectedPlaylist: UserPlaylist?,
    addingSongs: Boolean,
    songCount: Int,
    folderCount: Int,
    playlistCount: Int,
    playlistSongCount: Int,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onCreatePlaylist: () -> Unit,
    showCreate: Boolean,
) {
    val title = when {
        addingSongs -> stringResource(R.string.select_songs_title)
        selectedFolder != null -> selectedFolder.name
        selectedPlaylist != null -> selectedPlaylist.name
        else -> stringResource(R.string.library)
    }
    val subtitle = when {
        addingSongs -> stringResource(R.string.song_count, songCount)
        selectedFolder != null -> stringResource(R.string.folder_track_count, selectedFolder.trackCount)
        selectedPlaylist != null -> stringResource(R.string.folder_track_count, playlistSongCount)
        tab == LibraryTab.Folders -> stringResource(R.string.folder_count, folderCount)
        tab == LibraryTab.Playlists -> stringResource(R.string.playlist_count, playlistCount)
        else -> stringResource(R.string.song_count, songCount)
    }
    val backDescription = when {
        addingSongs || selectedPlaylist != null -> stringResource(R.string.back_to_playlists)
        selectedFolder != null -> stringResource(R.string.back_to_folders)
        else -> stringResource(R.string.switch_to_now_playing)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = backDescription,
                tint = JadeDeep,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showCreate) {
            IconButton(onClick = onCreatePlaylist) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.create_playlist),
                    tint = JadeDeep,
                )
            }
        }
        IconButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = stringResource(R.string.retry_scan),
                tint = JadeDeep,
            )
        }
    }
}

@Composable
private fun LibraryTabBar(
    tab: LibraryTab,
    onTabChange: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(LibraryTab.All, LibraryTab.Folders, LibraryTab.Playlists)
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = tab == option,
                onClick = { onTabChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = JadeSoft,
                    activeContentColor = JadeDeep,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(
                    text = stringResource(
                        when (option) {
                            LibraryTab.All -> R.string.tab_all_songs
                            LibraryTab.Folders -> R.string.tab_folders
                            LibraryTab.Playlists -> R.string.tab_playlists
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = JadeDeep)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.loading_library),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyLibrary(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(JadeSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = JadeDeep,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.empty_library_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.empty_library_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onRefresh) {
                Text(
                    text = stringResource(R.string.retry_scan),
                    color = Copper,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun SongList(
    songs: List<Song>,
    currentSongId: Long?,
    onSongClick: (Song) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongRow(
                index = index + 1,
                song = song,
                selected = song.id == currentSongId,
                onClick = { onSongClick(song) },
            )
        }
    }
}

@Composable
private fun FolderList(
    folders: List<SongFolder>,
    onFolderClick: (SongFolder) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(folders, key = { it.path }) { folder ->
            FolderRow(folder = folder, onClick = { onFolderClick(folder) })
        }
    }
}

@Composable
private fun FolderRow(folder: SongFolder, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(JadeSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                tint = JadeDeep,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            val pathHint = folder.path.takeIf {
                it != MusicRepository.UNKNOWN_FOLDER_KEY && it != folder.name
            }
            if (pathHint != null) {
                Text(
                    text = pathHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = stringResource(R.string.folder_track_count, folder.trackCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlaylistList(
    playlists: List<UserPlaylist>,
    songs: List<Song>,
    onPlaylistClick: (UserPlaylist) -> Unit,
    onDelete: (String) -> Unit,
) {
    if (playlists.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = JadeDeep,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.empty_playlists_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.empty_playlists_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(playlists, key = { it.id }) { playlist ->
            val count = playlist.withSongs(songs).size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                    .clickable { onPlaylistClick(playlist) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(JadeSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = null,
                        tint = JadeDeep,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.folder_track_count, count),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onDelete(playlist.id) }) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.delete_playlist),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetail(
    songs: List<Song>,
    currentSongId: Long?,
    onSongClick: (Song) -> Unit,
    onRemove: (Long) -> Unit,
    onAddSongs: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onAddSongs) {
                Text(
                    text = stringResource(R.string.add_songs),
                    color = Copper,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.empty_playlist_songs),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            SongRow(
                                index = index + 1,
                                song = song,
                                selected = song.id == currentSongId,
                                onClick = { onSongClick(song) },
                            )
                        }
                        IconButton(onClick = { onRemove(song.id) }) {
                            Icon(
                                imageVector = Icons.Rounded.RemoveCircleOutline,
                                contentDescription = stringResource(R.string.remove_from_playlist),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddSongsScreen(
    songs: List<Song>,
    alreadySelected: Set<Long>,
    onDone: (List<Long>) -> Unit,
    onCancel: () -> Unit,
) {
    var selected by remember { mutableStateOf(alreadySelected) }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.selected_count, selected.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onDone(selected.toList()) }) {
                    Text(stringResource(R.string.done_add_songs), color = Copper)
                }
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(songs, key = { it.id }) { song ->
                val checked = song.id in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .clickable {
                            selected = if (checked) selected - song.id else selected + song.id
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = if (checked) JadeDeep else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_playlist)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.playlist_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.confirm), color = Copper)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun SongRow(
    index: Int,
    song: Song,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (selected) JadeSoft else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        label = "rowBg",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(
            artUri = song.albumArtUri,
            contentDescription = song.title,
            modifier = Modifier.size(52.dp),
            cornerRadius = 14.dp,
            elevated = false,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) JadeDeep else MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = Copper,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(
                    text = "%02d".format(index),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            Text(
                text = formatDuration(song.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
