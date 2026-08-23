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
        /** 사용자가 SAF 로 고른 폴더. 권한을 거부했을 때의 경로. */
        val FOLDER_TREE_URI = stringPreferencesKey("folder_tree_uri")
        val SUBTITLE_SCALE = floatPreferencesKey("subtitle_scale")
        val SUBTITLE_POSITION = stringPreferencesKey("subtitle_position")
        val THEME = stringPreferencesKey("theme")
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
            folderTreeUri = p[Keys.FOLDER_TREE_URI],
            subtitleScale = p[Keys.SUBTITLE_SCALE] ?: SubtitleScale.NORMAL,
            subtitlePosition = p[Keys.SUBTITLE_POSITION] ?: SubtitlePosition.IN_VIDEO,
            theme = p[Keys.THEME] ?: ThemeMode.SYSTEM,
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

    suspend fun setFolderTreeUri(value: String) =
        context.dataStore.edit { it[Keys.FOLDER_TREE_URI] = value }

    suspend fun setPlaybackSpeed(value: Float) =
        context.dataStore.edit { it[Keys.PLAYBACK_SPEED] = value }

    suspend fun setSubtitleScale(value: Float) =
        context.dataStore.edit { it[Keys.SUBTITLE_SCALE] = value }

    suspend fun setSubtitlePosition(value: String) =
        context.dataStore.edit { it[Keys.SUBTITLE_POSITION] = value }

    suspend fun setTheme(value: String) =
        context.dataStore.edit { it[Keys.THEME] = value }
}

object SubtitleScale {
    const val SMALL = 0.85f
    const val NORMAL = 1.0f
    const val LARGE = 1.25f
}

/**
 * 자막을 영상 프레임 안에 그릴지, 아래 레터박스 영역으로 내릴지.
 * 시네마스코프 영상에서 화면을 가리지 않으려는 수요가 있다 — 기획서 7.2.
 */
object SubtitlePosition {
    const val IN_VIDEO = "in_video"
    const val LETTERBOX = "letterbox"
}

object ThemeMode {
    const val SYSTEM = "system"
    const val DARK = "dark"
    const val LIGHT = "light"
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
    val folderTreeUri: String? = null,
    val subtitleScale: Float = SubtitleScale.NORMAL,
    val subtitlePosition: String = SubtitlePosition.IN_VIDEO,
    val theme: String = ThemeMode.SYSTEM,
)
