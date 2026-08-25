package com.pikaworks.pikaplayer.ui.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.VideoSize
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
    /** 같은 폴더에서 이 영상 뒤에 오는 것들. 하단 목록과 자동 재생이 함께 쓴다. */
    val upNext: List<VideoItem> = emptyList(),
    /** 대기열에서 이 영상 앞에 오는 것. 없으면 이전 버튼을 만들지 않는다. */
    val previous: VideoItem? = null,
    /** 한 편 반복. 켜면 끝나도 넘어가지 않고 처음부터 다시 튼다. */
    val repeatEnabled: Boolean = false,
    /** 구간 반복의 시작점. 찍기 전에는 null. */
    val abStartMs: Long? = null,
    /** 구간 반복의 끝점. 둘 다 있어야 반복이 돈다. */
    val abEndMs: Long? = null,
    /**
     * 영상 자체의 가로/세로 비. 아직 모르면 null.
     *
     * 전체보기에서 화면을 돌릴지 말지를 이 값으로 가른다. 세로 영상을 돌리면
     * 좌우가 다 검게 남아 오히려 작아진다.
     */
    val videoAspect: Float? = null,
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
    /** 마지막 위치 저장용. [savePosition] 의 주석 참고. */
    private val persistScope: CoroutineScope,
) : ViewModel() {

    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var track: SubtitleTrack = SubtitleTrack.EMPTY
    private var subtitleOffsetMs: Long = 0L
    private var currentVideo: VideoItem? = null
    private var matches: List<SubtitleMatcher.Match> = emptyList()
    private var queue: List<VideoItem> = emptyList()
    /**
     * 대기열을 사용자가 직접 정했는가(재생목록).
     *
     * 보관함에서 튼 경우의 대기열은 "기기의 모든 영상" 이라 그대로 쓰면 다음
     * 영상이 엉뚱한 폴더로 넘어간다. 그래서 폴더로 한 번 거른다. 재생목록은
     * 반대다 — 여러 폴더에서 모은 것이 곧 사용자의 뜻이므로 거르면 안 된다.
     */
    private var explicitQueue: Boolean = false
    /** 마지막까지 가면 처음으로 돌아간다. 재생목록에서만 켠다. */
    private var loopQueue: Boolean = false
    private var autoPlayNext: Boolean = true
    private var resumePlayback: Boolean = true
    /** 지금 연 영상에 딸린 작업(이어보기 탐색·자막 찾기). 영상을 바꾸면 끊는다. */
    private var videoJob: Job? = null
    /** 자막 파일 읽기. 다른 자막이나 인코딩을 고르면 앞의 것을 끊는다. */
    private var subtitleJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
            if (!isPlaying) savePosition()
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            // 픽셀이 정사각형이 아닌 영상이 있다. 그 보정을 곱해야 실제로 보이는 비다.
            val width = videoSize.width * videoSize.pixelWidthHeightRatio
            val height = videoSize.height.toFloat()
            _uiState.update {
                it.copy(videoAspect = if (width > 0f && height > 0f) width / height else null)
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) {
                _uiState.update { it.copy(durationMs = player.duration.coerceAtLeast(0L)) }
            }
            if (state == Player.STATE_ENDED) {
                savePosition() // 끝까지 본 위치를 남겨야 '최근'에 뜬다
                if (autoPlayNext) _uiState.value.upNext.firstOrNull()?.let(::playNext)
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
     *
     * [speed] 와 [charset] 은 설정 화면의 기본값이다. 아래에서 상태를 통째로
     * 새로 만들기 때문에, 기본값을 여기서 같이 받지 않으면 영상을 열 때마다
     * 사용자가 정한 값이 무시된다.
     */
    fun open(
        video: VideoItem,
        resume: Boolean,
        speed: Float = 1.0f,
        charset: String = SubtitleEncoding.AUTO,
        queue: List<VideoItem> = emptyList(),
        /** 재생목록처럼 사용자가 직접 세운 대기열인가. 폴더로 거르지 않는다. */
        explicitQueue: Boolean = false,
        /** 마지막까지 가면 처음으로 돌아갈지. */
        loopQueue: Boolean = false,
    ) {
        this.queue = queue
        this.explicitQueue = explicitQueue
        this.loopQueue = loopQueue
        if (currentVideo?.uri == video.uri) return

        // 앞 영상의 자막 읽기나 이어보기 탐색이 남아 있으면 새 영상에 끼어든다.
        videoJob?.cancel()
        subtitleJob?.cancel()

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
            upNext = upNextOf(video),
            previous = previousOf(video),
            // 첫 프레임이 오기 전에도 알아야 한다. MediaStore 가 준 값으로 먼저
            // 채우고, onVideoSizeChanged 가 오면 그 값으로 바로잡는다.
            videoAspect = if (video.width > 0 && video.height > 0) {
                video.width.toFloat() / video.height
            } else null,
        )

        player.setPlaybackSpeed(speed)
        // 화면비·잠금과 같은 결로 영상마다 초기화한다. 반복을 켠 채 다른 영상을
        // 열었을 때 모르는 사이에 계속 돌고 있으면 곤란하다.
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.setMediaItem(MediaItem.fromUri(video.uri))
        player.prepare()

        videoJob = viewModelScope.launch {
            if (resume) {
                positionDao.find(video.uri.toString())?.let { saved ->
                    if (saved.positionMs > 0) player.seekTo(saved.positionMs)
                }
            }
            player.play()
            loadSubtitle(video)
        }
    }

    /**
     * 화면을 벗어날 때.
     *
     * 이 ViewModel 은 Activity 에 매여 있어 플레이어 화면을 닫아도 정리되지 않는다.
     * 여기서 멈추지 않으면 목록으로 돌아간 뒤에도 소리가 계속 난다.
     */
    fun close() {
        videoJob?.cancel()
        subtitleJob?.cancel()
        savePosition()
        // stop() 이 리스너를 깨워 한 번 더 저장하지 않도록 먼저 지운다.
        currentVideo = null
        player.stop()
        player.clearMediaItems()
        queue = emptyList()
        _uiState.value = PlayerUiState()
    }

    /**
     * 같은 폴더에서 이 영상 다음에 오는 것들.
     *
     * 보관함은 여러 폴더의 영상을 한 줄로 섞어 보여준다. 거기서 그대로 "다음"을
     * 집으면 관계없는 폴더로 건너뛴다 — 기획서 7.2 는 같은 폴더로 못박았다.
     * SAF 로 연 파일은 folderName 이 없는데, 그때는 고른 폴더 하나뿐이라 맞다.
     */
    /** 지금 영상 기준으로 대기열을 다시 세운다. 폴더 거르기와 목록 반복이 여기 걸린다. */
    private fun poolFor(video: VideoItem): List<VideoItem> =
        if (explicitQueue) queue else queue.filter { it.folderName == video.folderName }

    private fun upNextOf(video: VideoItem): List<VideoItem> {
        val pool = poolFor(video)
        val index = pool.indexOfFirst { it.uri == video.uri }
        if (index < 0) return emptyList()
        val after = pool.drop(index + 1)
        // 목록 반복이면 끝에서 처음으로 이어 붙인다. 자기 자신은 넣지 않는다 —
        // 한 편 반복은 따로 있고, 여기서 겹치면 "다음 영상" 에 지금 것이 뜬다.
        val chained = if (loopQueue) after + pool.take(index) else after
        return chained.take(UP_NEXT_LIMIT)
    }

    private fun previousOf(video: VideoItem): VideoItem? {
        val pool = poolFor(video)
        val index = pool.indexOfFirst { it.uri == video.uri }
        if (index < 0) return null
        return pool.getOrNull(index - 1) ?: if (loopQueue) pool.lastOrNull()?.takeIf { it.uri != video.uri } else null
    }

    /** 이전 영상으로. 없으면 아무 일도 하지 않는다. */
    fun playPrevious() {
        _uiState.value.previous?.let(::playNext)
    }

    /** 목록에서 고르거나 자동 재생으로 넘어갈 때. 재생속도·인코딩은 이어받는다. */
    fun playNext(video: VideoItem) {
        val current = _uiState.value
        open(
            video = video,
            resume = resumePlayback,
            speed = current.speed,
            charset = current.subtitleCharset,
            queue = queue,
            explicitQueue = explicitQueue,
            loopQueue = loopQueue,
        )
    }

    /** 설정에서 온 값들. 상태를 갈아끼우는 [open] 과 무관하게 유지돼야 한다. */
    fun setResumePlayback(enabled: Boolean) {
        resumePlayback = enabled
    }

    fun setAutoPlayNext(enabled: Boolean) {
        autoPlayNext = enabled
    }

    private suspend fun loadSubtitle(video: VideoItem) {
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
            return
        }
        _uiState.update {
            it.copy(subtitleOptions = matches.map { m -> SubtitleOption(m.displayName, m.format.label) })
        }
        applySubtitle(0)
    }

    /**
     * [index] 가 -1 이면 자막을 끈다.
     *
     * 파일 읽기가 끝나기 전에 다른 자막을 고를 수 있다. 앞의 것을 끊지 않으면
     * 늦게 끝난 쪽이 나중에 고른 것을 덮어쓴다.
     */
    fun selectSubtitle(index: Int) {
        subtitleJob?.cancel()
        subtitleJob = viewModelScope.launch { applySubtitle(index) }
    }

    private suspend fun applySubtitle(index: Int) {
        if (index < 0 || index >= matches.size) {
            track = SubtitleTrack.EMPTY
            _uiState.update {
                it.copy(selectedSubtitle = -1, subtitleEnabled = false, subtitleLabel = "자막 끔", cue = null)
            }
            return
        }
        val match = matches[index]
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
                // 조건을 걸지 않는다. 일시정지 중에 탐색하면 자막도 따라와야 하고,
                // 값이 그대로면 StateFlow 가 알아서 흘리지 않는다(데이터 클래스 동등성).
                val pos = player.currentPosition.coerceAtLeast(0L)

                // 구간 반복. 재생을 멈추지 않고 되감기만 한다.
                //
                // 여기서 처리하는 이유: ExoPlayer 에는 구간을 지정하는 기능이 없고,
                // ClippingMediaSource 로 자르면 구간 밖으로 나갈 수 없어서 해제할 때
                // 미디어를 다시 만들어야 한다. 200ms 마다 도는 이 고리에서 보면
                // 최대 200ms 넘어갔다가 돌아온다 — 연습 용도로는 그 정도면 된다.
                val snapshot = _uiState.value
                val abStart = snapshot.abStartMs
                val abEnd = snapshot.abEndMs
                if (abStart != null && abEnd != null && pos >= abEnd) {
                    player.seekTo(abStart)
                }

                _uiState.update { state ->
                    state.copy(
                        positionMs = pos,
                        cue = if (state.subtitleEnabled) track.cueAt(pos, subtitleOffsetMs) else null,
                    )
                }
                delay(200)
            }
        }
    }

    fun togglePlay() = when {
        player.isPlaying -> player.pause()
        // 끝까지 본 영상은 처음부터 다시 튼다. 그냥 play() 를 부르면 끝 지점에
        // 멈춰 있어 눌러도 아무 일이 없는 것처럼 보인다.
        player.playbackState == Player.STATE_ENDED -> {
            player.seekTo(0)
            player.play()
        }
        else -> player.play()
    }

    /**
     * 구간 반복 지점을 차례로 찍는다. A → B → 해제.
     *
     * B 가 A 보다 앞이면 순서가 뒤집힌 것이다. 오류를 내는 것보다 방금 찍은 곳을
     * 새 A 로 삼는 편이 손이 덜 간다 — 되감아서 다시 찍으려던 참일 테니.
     */
    fun markAb() {
        val now = player.currentPosition.coerceAtLeast(0L)
        _uiState.update { s ->
            val start = s.abStartMs
            when {
                start == null -> s.copy(abStartMs = now)
                s.abEndMs != null -> s.copy(abStartMs = null, abEndMs = null)
                now > start -> s.copy(abEndMs = now)
                else -> s.copy(abStartMs = now)
            }
        }
    }

    /**
     * 한 편 반복.
     *
     * 켜 두면 ExoPlayer 가 안에서 되감아 STATE_ENDED 에 닿지 않는다. 다음
     * 영상으로 자동으로 넘어가는 경로도 그래서 같이 멈춘다 — 반복을 켠 사람이
     * 바라는 동작이다.
     */
    fun toggleRepeat() {
        val next = !_uiState.value.repeatEnabled
        player.repeatMode = if (next) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        _uiState.update { it.copy(repeatEnabled = next) }
    }

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

    companion object {
        val RESIZE_MODE_LABELS = listOf("맞춤", "채움", "늘이기")

        /** 하단 목록에 쓰는 개수. 화면에 다 보이지도 않는 분량을 들고 있을 이유가 없다. */
        private const val UP_NEXT_LIMIT = 20
    }

    /**
     * 재생 위치를 남긴다.
     *
     * `persistScope` 를 쓰는 이유: `onCleared()` 가 불릴 때 `viewModelScope` 는
     * 이미 취소돼 있다. 거기서 띄운 코루틴은 실행되지 않아 마지막 위치가 사라진다.
     */
    private fun savePosition() {
        val video = currentVideo ?: return
        val pos = player.currentPosition
        val dur = player.duration
        if (dur <= 0) return
        persistScope.launch {
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
        private val persistScope: CoroutineScope,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlayerViewModel(context, positionDao, subtitleMatcher, persistScope) as T
    }
}
