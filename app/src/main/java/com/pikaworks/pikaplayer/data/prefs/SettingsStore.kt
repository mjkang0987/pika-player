package com.pikaworks.pikaplayer.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** 설정 화면(S6)의 값들. 단순 키-값이라 Room 이 아니라 DataStore 를 쓴다. */
class SettingsStore(private val context: Context) {

    private object Keys {
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val RESUME = booleanPreferencesKey("resume_playback")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val SUBTITLE_ENCODING = stringPreferencesKey("subtitle_encoding")
        val GESTURES = booleanPreferencesKey("brightness_volume_gestures")
        val DOUBLE_TAP_SEEK = booleanPreferencesKey("double_tap_seek")
        val FOLLOW_AUTO_ROTATE = booleanPreferencesKey("follow_auto_rotate")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            playbackSpeed = p[Keys.PLAYBACK_SPEED] ?: 1.0f,
            resumePlayback = p[Keys.RESUME] ?: true,
            autoPlayNext = p[Keys.AUTO_PLAY_NEXT] ?: true,
            subtitleEncoding = p[Keys.SUBTITLE_ENCODING] ?: SubtitleEncoding.AUTO,
            gesturesEnabled = p[Keys.GESTURES] ?: true,
            doubleTapSeekEnabled = p[Keys.DOUBLE_TAP_SEEK] ?: true,
            followAutoRotate = p[Keys.FOLLOW_AUTO_ROTATE] ?: true,
        )
    }

    suspend fun setResumePlayback(value: Boolean) =
        context.dataStore.edit { it[Keys.RESUME] = value }

    suspend fun setAutoPlayNext(value: Boolean) =
        context.dataStore.edit { it[Keys.AUTO_PLAY_NEXT] = value }

    suspend fun setGesturesEnabled(value: Boolean) =
        context.dataStore.edit { it[Keys.GESTURES] = value }

    suspend fun setDoubleTapSeekEnabled(value: Boolean) =
        context.dataStore.edit { it[Keys.DOUBLE_TAP_SEEK] = value }

    suspend fun setFollowAutoRotate(value: Boolean) =
        context.dataStore.edit { it[Keys.FOLLOW_AUTO_ROTATE] = value }

    suspend fun setSubtitleEncoding(value: String) =
        context.dataStore.edit { it[Keys.SUBTITLE_ENCODING] = value }
}

object SubtitleEncoding {
    const val AUTO = "auto"
    const val UTF_8 = "UTF-8"
    const val CP949 = "CP949"
    const val EUC_KR = "EUC-KR"
    const val SHIFT_JIS = "Shift_JIS"
}

data class Settings(
    val playbackSpeed: Float,
    val resumePlayback: Boolean,
    val autoPlayNext: Boolean,
    val subtitleEncoding: String,
    val gesturesEnabled: Boolean,
    val doubleTapSeekEnabled: Boolean,
    val followAutoRotate: Boolean,
)
