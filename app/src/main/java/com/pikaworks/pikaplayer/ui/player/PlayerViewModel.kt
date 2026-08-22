package com.pikaworks.pikaplayer.ui.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.pikaworks.pikaplayer.data.db.PlaybackPosition
import com.pikaworks.pikaplayer.data.db.PlaybackPositionDao
import com.pikaworks.pikaplayer.data.media.VideoItem
import com.pikaworks.pikaplayer.data.subtitle.SubtitleMatcher
import com.pikaworks.pikaplayer.subtitle.SubtitleCue
import com.pikaworks.pikaplayer.subtitle.SubtitleFormat
import com.pikaworks.pikaplayer.subtitle.SubtitleTrack
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUiState(
    val title: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1.0f,
    val cue: SubtitleCue? = null,
    /** 자막 상태 표기 — 기획서 7.2 규칙. 외부 파일은 언어를 알 수 없어 형식명을 쓴다. */
    val subtitleLabel: String = "자막 없음",
    val subtitleEnabled: Boolean = false,
    val controlsVisible: Boolean = true,
    val locked: Boolean = false,
    /** AspectRatioFrameLayout 의 resize mode. 화면비 버튼이 순환시킨다. */
    val resizeMode: Int = 0,
) {
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

@OptIn(UnstableApi::class)
class PlayerViewModel(
    context: Context,
    private val positionDao: PlaybackPositionDao,
    private val subtitleMatcher: SubtitleMatcher,
) : ViewModel() {

    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var track: SubtitleTrack = SubtitleTrack.EMPTY
    private var subtitleOffsetMs: Long = 0L
    private var currentVideo: VideoItem? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (!isPlaying) savePosition()
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) {
                _uiState.update { it.copy(durationMs = player.duration.coerceAtLeast(0L)) }
            }
        }
    }

    init {
        player.addListener(listener)
        startPositionTicker()
    }

    fun open(video: VideoItem, resume: Boolean) {
        currentVideo = video
        _uiState.update { it.copy(title = video.baseName, durationMs = video.durationMs) }

        player.setMediaItem(MediaItem.fromUri(video.uri))
        player.prepare()

        viewModelScope.launch {
            if (resume) {
                positionDao.find(video.uri.toString())?.let { saved ->
                    if (saved.positionMs > 0) player.seekTo(saved.positionMs)
                }
            }
            player.play()
        }

        loadSubtitle(video)
    }

    private fun loadSubtitle(video: VideoItem) {
        viewModelScope.launch {
            val match = subtitleMatcher.findFor(video).firstOrNull()
            if (match == null) {
                track = SubtitleTrack.EMPTY
                _uiState.update { it.copy(subtitleLabel = "자막 없음", subtitleEnabled = false) }
                return@launch
            }
            track = subtitleMatcher.load(match) ?: SubtitleTrack.EMPTY
            _uiState.update {
                it.copy(
                    // 외부 자막 파일에는 언어 정보가 없다. 형식명으로 표기한다.
                    subtitleLabel = "자막 · ${match.format.label}",
                    subtitleEnabled = !track.isEmpty(),
                )
            }
        }
    }

    /**
     * 재생 위치는 플레이어가 알려주지 않는다. 주기적으로 읽어야 한다.
     * 200ms 면 시크바가 끊겨 보이지 않으면서 자막 타이밍도 충분히 따라간다.
     */
    private fun startPositionTicker() {
        viewModelScope.launch {
            while (isActive) {
                if (player.isPlaying || _uiState.value.positionMs == 0L) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    _uiState.update { state ->
                        state.copy(
                            positionMs = pos,
                            cue = if (state.subtitleEnabled) track.cueAt(pos, subtitleOffsetMs) else null,
                        )
                    }
                }
                delay(200)
            }
        }
    }

    fun togglePlay() = if (player.isPlaying) player.pause() else player.play()

    fun seekTo(ms: Long) {
        player.seekTo(ms.coerceIn(0L, player.duration.coerceAtLeast(0L)))
        _uiState.update { it.copy(positionMs = ms) }
    }

    fun skip(deltaMs: Long) = seekTo(player.currentPosition + deltaMs)

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
        _uiState.update { it.copy(speed = speed) }
    }

    fun setSubtitleOffset(offsetMs: Long) {
        subtitleOffsetMs = offsetMs
    }

    fun toggleSubtitle() {
        _uiState.update { it.copy(subtitleEnabled = !it.subtitleEnabled && !track.isEmpty()) }
    }

    fun toggleControls() {
        _uiState.update { it.copy(controlsVisible = !it.controlsVisible) }
    }

    fun toggleLock() {
        // 잠그면 컨트롤도 같이 숨긴다. 잠금 해제 버튼 하나만 남는다.
        _uiState.update { it.copy(locked = !it.locked, controlsVisible = it.locked) }
    }

    /** 맞춤 → 채움 → 늘이기 순환 */
    fun cycleResizeMode() {
        _uiState.update { it.copy(resizeMode = (it.resizeMode + 1) % RESIZE_MODE_LABELS.size) }
    }

    private val speedPresets = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    fun cycleSpeed() {
        val next = speedPresets[(speedPresets.indexOf(_uiState.value.speed).let {
            if (it < 0) speedPresets.indexOf(1.0f) else it
        } + 1) % speedPresets.size]
        setSpeed(next)
    }

    companion object {
        val RESIZE_MODE_LABELS = listOf("맞춤", "채움", "늘이기")
    }

    private fun savePosition() {
        val video = currentVideo ?: return
        val pos = player.currentPosition
        val dur = player.duration
        if (dur <= 0) return
        viewModelScope.launch {
            positionDao.upsert(
                PlaybackPosition(
                    uri = video.uri.toString(),
                    positionMs = pos,
                    durationMs = dur,
                    updatedAtMs = System.currentTimeMillis(),
                )
            )
        }
    }

    override fun onCleared() {
        savePosition()
        player.removeListener(listener)
        player.release()
        super.onCleared()
    }

    class Factory(
        private val context: Context,
        private val positionDao: PlaybackPositionDao,
        private val subtitleMatcher: SubtitleMatcher,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlayerViewModel(context, positionDao, subtitleMatcher) as T
    }
}
