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
import com.pikaworks.pikaplayer.data.prefs.SubtitleEncoding
import com.pikaworks.pikaplayer.data.subtitle.SubtitleMatcher
import com.pikaworks.pikaplayer.subtitle.SubtitleCue
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
    /** 영상 옆에서 찾은 자막 파일들 */
    val subtitleOptions: List<SubtitleOption> = emptyList(),
    /** 선택된 자막. -1 이면 끔 */
    val selectedSubtitle: Int = -1,
    /** 사용자가 지정한 인코딩. "auto" 면 자동 판별 */
    val subtitleCharset: String = SubtitleEncoding.AUTO,
    val subtitleOffsetMs: Long = 0L,
    val controlsVisible: Boolean = true,
    val locked: Boolean = false,
    /** AspectRatioFrameLayout 의 resize mode. 화면비 버튼이 순환시킨다. */
    val resizeMode: Int = 0,
) {
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

data class SubtitleOption(
    val name: String,
    val formatLabel: String,
)

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
    private var matches: List<SubtitleMatcher.Match> = emptyList()

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

    /**
     * 다른 영상으로 갈아탄다.
     *
     * 영상마다 ViewModel 을 새로 만들면 ExoPlayer 인스턴스가 쌓인다. 코덱은
     * 기기당 개수가 제한돼 있어 몇 개만 새면 재생이 실패한다. ViewModel 은
     * 하나만 두고 여기서 갈아끼운다.
     */
    /**
     * [speed] 와 [charset] 은 설정 화면의 기본값이다. 아래에서 상태를 통째로
     * 새로 만들기 때문에, 기본값을 여기서 같이 받지 않으면 영상을 열 때마다
     * 사용자가 정한 값이 무시된다.
     */
    fun open(
        video: VideoItem,
        resume: Boolean,
        speed: Float = 1.0f,
        charset: String = SubtitleEncoding.AUTO,
    ) {
        if (currentVideo?.uri == video.uri) return

        savePosition() // 이전 영상의 위치를 먼저 남긴다
        track = SubtitleTrack.EMPTY
        subtitleOffsetMs = 0L
        matches = emptyList()
        currentVideo = video
        // 이전 영상의 자막·화면비·잠금 상태가 넘어오지 않도록 초기화한다.
        _uiState.value = PlayerUiState(
            title = video.baseName,
            durationMs = video.durationMs,
            speed = speed,
            subtitleCharset = charset,
        )

        player.setPlaybackSpeed(speed)
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
            matches = subtitleMatcher.findFor(video)
            if (matches.isEmpty()) {
                track = SubtitleTrack.EMPTY
                _uiState.update {
                    it.copy(
                        subtitleLabel = "자막 없음",
                        subtitleEnabled = false,
                        subtitleOptions = emptyList(),
                        selectedSubtitle = -1,
                    )
                }
                return@launch
            }
            _uiState.update {
                it.copy(subtitleOptions = matches.map { m -> SubtitleOption(m.displayName, m.format.label) })
            }
            selectSubtitle(0)
        }
    }

    /** [index] 가 -1 이면 자막을 끈다. */
    fun selectSubtitle(index: Int) {
        if (index < 0 || index >= matches.size) {
            track = SubtitleTrack.EMPTY
            _uiState.update {
                it.copy(selectedSubtitle = -1, subtitleEnabled = false, subtitleLabel = "자막 끔", cue = null)
            }
            return
        }
        val match = matches[index]
        viewModelScope.launch {
            val charsetName = _uiState.value.subtitleCharset.takeIf { it != SubtitleEncoding.AUTO }
            track = subtitleMatcher.load(match, charsetName) ?: SubtitleTrack.EMPTY
            _uiState.update {
                it.copy(
                    selectedSubtitle = index,
                    subtitleEnabled = !track.isEmpty(),
                    // 외부 자막 파일에는 언어 정보가 없다. 형식명으로 표기한다.
                    subtitleLabel = "자막 · ${match.format.label}",
                )
            }
        }
    }

    /**
     * 인코딩을 사람이 직접 지정한다.
     *
     * 자동 판별이 틀리는 경우가 있고, 그때 되돌릴 방법이 없으면 사용자는
     * "자막이 깨져서 못 본다"로 받아들인다 — 기획서 7.2.
     */
    fun setSubtitleCharset(name: String) {
        _uiState.update { it.copy(subtitleCharset = name) }
        val selected = _uiState.value.selectedSubtitle
        if (selected >= 0) selectSubtitle(selected) // 같은 파일을 새 인코딩으로 다시 읽는다
    }

    fun adjustSubtitleOffset(deltaMs: Long) {
        subtitleOffsetMs += deltaMs
        _uiState.update { it.copy(subtitleOffsetMs = subtitleOffsetMs) }
    }

    fun resetSubtitleOffset() {
        subtitleOffsetMs = 0L
        _uiState.update { it.copy(subtitleOffsetMs = 0L) }
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
