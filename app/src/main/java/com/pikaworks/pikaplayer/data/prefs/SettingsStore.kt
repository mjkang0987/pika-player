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
        val THEME = stringPreferencesKey("theme")
        val LIBRARY_SORT = stringPreferencesKey("library_sort")
        val AUTO_PIP = booleanPreferencesKey("auto_pip")
        val CHILD_LOCK = booleanPreferencesKey("child_lock")
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
            theme = p[Keys.THEME] ?: ThemeMode.SYSTEM,
            librarySort = p[Keys.LIBRARY_SORT] ?: SortOrder.DATE_DESC,
            autoPip = p[Keys.AUTO_PIP] ?: false,
            childLock = p[Keys.CHILD_LOCK] ?: false,
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

    suspend fun setTheme(value: String) =
        context.dataStore.edit { it[Keys.THEME] = value }

    suspend fun setLibrarySort(value: String) =
        context.dataStore.edit { it[Keys.LIBRARY_SORT] = value }

    suspend fun setAutoPip(value: Boolean) =
        context.dataStore.edit { it[Keys.AUTO_PIP] = value }

    suspend fun setChildLock(value: Boolean) =
        context.dataStore.edit { it[Keys.CHILD_LOCK] = value }
}

/**
 * 자막 글자 배율. 기준 15sp 에 곱해 쓴다 — PlayerScreen 의 SubtitleText.
 *
 * 가장 큰 값을 1.5 까지 둔 이유: 1.25 는 폰을 조금 멀리 두고 보는 사람에게
 * 여전히 작다는 쪽이었다. 그보다 더 키우면 가로 영상에서 한 줄이 두 줄로
 * 접히기 시작해서, 자막이 화면을 먹는 대가가 읽기 편해지는 이득을 넘는다.
 */
object SubtitleScale {
    const val SMALL = 0.85f
    const val NORMAL = 1.0f
    const val LARGE = 1.25f
    const val EXTRA_LARGE = 1.5f
}

/**
 * 목록 정렬 기준. 보관함과 폴더 안 영상 목록이 같은 값을 쓴다.
 *
 * 두 화면에서 정렬이 따로 놀면 "왜 여기만 순서가 다르지"가 된다.
 */
object SortOrder {
    const val DATE_DESC = "date_desc"
    const val NAME = "name"
    const val SIZE_DESC = "size_desc"
    const val DURATION_DESC = "duration_desc"
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
    val theme: String = ThemeMode.SYSTEM,
    val librarySort: String = SortOrder.DATE_DESC,
    /**
     * 홈 버튼 등으로 앱을 벗어날 때 자동으로 작은 창(PiP)으로 넘어갈지.
     *
     * 기본값이 꺼짐인 이유: 켜져 있으면 영상을 보다 잠깐 다른 앱을 열 때마다
     * 작은 창이 따라붙는다. 원하는 사람에게는 편하지만 원치 않는 사람에게는
     * 매번 닫아야 하는 방해물이다.
     */
    val autoPip: Boolean = false,
    /**
     * 어린이 잠금. 켜면 재생 화면의 잠금을 풀 때 PIN 을 묻고, 잠긴 동안에는
     * 뒤로가기로도 나갈 수 없다. 비공개 폴더와 같은 PIN 을 쓴다.
     */
    val childLock: Boolean = false,
)
