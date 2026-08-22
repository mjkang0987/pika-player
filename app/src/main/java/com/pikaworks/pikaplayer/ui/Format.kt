package com.pikaworks.pikaplayer.ui

import java.util.Locale

/** 1:40:36 / 08:55 */
fun formatDuration(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}

/** 38분 남음 / 1시간 24분 남음 */
fun formatRemaining(durationMs: Long, positionMs: Long): String {
    val remainSec = ((durationMs - positionMs).coerceAtLeast(0)) / 1000
    val h = remainSec / 3600
    val m = (remainSec % 3600) / 60
    return when {
        h > 0 -> "${h}시간 ${m}분 남음"
        m > 0 -> "${m}분 남음"
        else -> "곧 끝남"
    }
}

/** 3.1 GB / 142 MB */
fun formatSize(bytes: Long): String {
    val gb = 1024.0 * 1024 * 1024
    val mb = 1024.0 * 1024
    return when {
        bytes >= gb -> String.format(Locale.US, "%.1f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.0f MB", bytes / mb)
        else -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    }
}
