package com.localmp3.player.ui.util

fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
