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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 끝났을 때 무엇을 반복할지. 셋 중 하나다.
 *
 * 한 편 반복과 목록 반복을 따로 켜고 끄던 때가 있었는데, 둘 다 켜면 무엇이
 * 이기는지 화면만 봐서는 알 수 없었다(한 편이 이긴다 — 영상이 끝나지 않으니
 * 목록으로 넘어갈 일이 없다). 고르는 것으로 바꾸면 그 상태 자체가 생기지 않는다.
 */
object RepeatMode {
    const val OFF = "off"
    /** 지금 영상을 끝나면 다시 처음부터. */
    const val ONE = "one"
    /** 목록의 마지막 다음에 처음으로. */
    const val ALL = "all"
}

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
    /** 같은 폴더에서 이 영상 뒤에 오는 것들. 다음 버튼과 자동 재생이 함께 쓴다. */
    val upNext: List<VideoItem> = emptyList(),
    /**
     * 지금 틀고 있는 목록 전체. 재생목록 시트가 이걸 그린다.
     *
     * [upNext] 와 따로 두는 이유: 시트는 지나온 것까지 보여 주고 그 안에서 지금
     * 어디쯤인지 짚어 준다. 뒤에 남은 것만 보여 주면 목록의 마지막에 왔을 때
     * 빈 시트가 뜬다 — 무엇을 보고 있었는지조차 사라진다.
     */
    val queue: List<VideoItem> = emptyList(),
    /** 목록에서 지금 것을 짚기 위한 값. */
    val playingUri: String? = null,
    /** 대기열에서 이 영상 앞에 오는 것. 없으면 이전 버튼을 만들지 않는다. */
    val previous: VideoItem? = null,
    /** [RepeatMode] 값. 셋 중 하나만 고를 수 있다. */
    val repeatMode: String = RepeatMode.OFF,
    /**
     * 사용자가 목록 하나를 통째로 튼 것인가 — 재생목록, 또는 폴더.
     *
     * 목록 반복과 랜덤은 그럴 때만 뜻이 있다. 보관함에서 한 편을 누른 대기열은
     * "기기의 모든 영상" 이라, 거기서 섞어 봐야 사용자가 정한 순서를 흐트러
     * 뜨리는 것이 아니라 그냥 아무 영상이나 트는 것이 된다.
     */
    val explicitQueue: Boolean = false,
    /** 랜덤. 지금 영상 뒤를 섞는다. */
    val shuffleEnabled: Boolean = false,
    /** 끝나면 다음 영상으로 넘어갈지. 설정과 같은 값을 본다. */
    val autoPlayNextEnabled: Boolean = true,
    /** 구간 반복의 시작점. 찍기 전에는 null. */
    val abStartMs: Long? = null,
    /** 구간 반복의 끝점. 둘 다 있어야 반복이 돈다. */
    val abEndMs: Long? = null,
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
    /**
     * 사용자가 정한 원래 순서. 랜덤을 끄면 여기로 되돌린다.
     *
     * 섞은 결과만 들고 있으면 되돌릴 방법이 없다 — 다시 섞으면 원래와 다른
     * 또 하나의 순서가 나올 뿐이다.
     */
    private var orderedQueue: List<VideoItem> = emptyList()
    /** 지금 쓰는 순서. 랜덤이 꺼져 있으면 [orderedQueue] 와 같다. */
    private var queue: List<VideoItem> = emptyList()
    /**
     * 대기열을 사용자가 직접 정했는가(재생목록).
     *
     * 보관함에서 튼 경우의 대기열은 "기기의 모든 영상" 이라 그대로 쓰면 다음
     * 영상이 엉뚱한 폴더로 넘어간다. 그래서 폴더로 한 번 거른다. 재생목록은
     * 반대다 — 여러 폴더에서 모은 것이 곧 사용자의 뜻이므로 거르면 안 된다.
     */
    private var explicitQueue: Boolean = false
    private var repeatMode: String = RepeatMode.OFF
    /** 마지막까지 가면 처음으로 돌아간다. 재생목록·폴더에서만 뜻이 있다. */
    private val loopQueue: Boolean get() = repeatMode == RepeatMode.ALL
    private var shuffle: Boolean = false
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

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) {
                _uiState.update { it.copy(durationMs = player.duration.coerceAtLeast(0L)) }
            }
            if (state == Player.STATE_ENDED) {
                savePosition() // 끝까지 본 위치를 남겨야 '최근'에 뜬다
                // 목록 반복을 고른 것은 "이 목록을 계속 돌려라" 는 말이다. 자동
                // 재생이 꺼져 있다고 거기서 멈추면, 골라 놓고 아무 일도 일어나지
                // 않는 상태가 된다.
                if (autoPlayNext || repeatMode == RepeatMode.ALL) {
                    _uiState.value.upNext.firstOrNull()?.let(::playNext)
                }
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
    ) {
        this.orderedQueue = queue
        this.queue = queue
        this.explicitQueue = explicitQueue
        // 새 재생이다. 지난번에 켜 둔 랜덤·목록 반복은 여기서 끊는다 — 목록마다
        // 기억하지 않는 값이라, 남아 있으면 어디서 켠 것인지 알 수 없다.
        this.repeatMode = RepeatMode.OFF
        this.shuffle = false
        if (currentVideo?.uri == video.uri) {
            // 같은 영상이면 다시 틀지 않는다. 다만 방금 되돌린 값이 화면에는
            // 그대로 켜져 보이지 않도록 상태만 맞춰 둔다.
            _uiState.update {
                it.copy(
                    explicitQueue = explicitQueue,
                    repeatMode = RepeatMode.OFF,
                    shuffleEnabled = false,
                )
            }
            refreshQueue()
            return
        }
        start(video, resume, speed, charset)
    }

    /** 미디어를 실제로 갈아끼우는 부분. 대기열은 건드리지 않는다. */
    private fun start(video: VideoItem, resume: Boolean, speed: Float, charset: String) {
        // 앞 영상의 자막 읽기나 이어보기 탐색이 남아 있으면 새 영상에 끼어든다.
        videoJob?.cancel()
        subtitleJob?.cancel()

        savePosition() // 이전 영상의 위치를 먼저 남긴다
        track = SubtitleTrack.EMPTY
        subtitleOffsetMs = 0L
        matches = emptyList()
        currentVideo = video
        // 이전 영상의 자막·화면비·잠금 상태가 넘어오지 않도록 초기화한다.
        // 대기열 설정(랜덤·목록 반복·자동 재생)은 재생 한 판을 통틀어 유지되는
        // 값이라 여기서 다시 실어 준다.
        _uiState.value = PlayerUiState(
            title = video.baseName,
            durationMs = video.durationMs,
            speed = speed,
            subtitleCharset = charset,
            upNext = upNextOf(video),
            queue = poolFor(video),
            playingUri = video.uri.toString(),
            previous = previousOf(video),
            explicitQueue = explicitQueue,
            repeatMode = repeatMode,
            shuffleEnabled = shuffle,
            autoPlayNextEnabled = autoPlayNext,
        )

        player.setPlaybackSpeed(speed)
        // 재생 한 판을 통틀어 유지되는 값이라 여기서 다시 걸어 준다. open() 이
        // 새 재생을 열 때만 끊는다.
        player.repeatMode = exoRepeatMode()
        player.setMediaItem(MediaItem.fromUri(video.uri))
        player.prepare()

        videoJob = viewModelScope.launch {
            if (resume) {
                positionDao.find(video.uri.toString())?.let { saved ->
                    // 끝까지 본 영상은 이어보지 않고 처음부터 튼다. 저장된 위치로
                    // 가면 열자마자 끝나 버려서, 자동으로 넘어온 다음 영상이
                    // 눈 깜짝할 사이에 지나간다.
                    if (saved.positionMs > 0 && !watchedToEnd(saved)) player.seekTo(saved.positionMs)
                }
            }
            player.play()
            loadSubtitle(video)
        }
    }

    /**
     * 사실상 끝까지 본 영상인가.
     *
     * 기준을 보관함의 '이어보기' 와 같은 값으로 맞춘다. 거기서 이어볼 것으로
     * 세지 않는 영상이 여기서는 이어져 열린다면 앞뒤가 맞지 않는다.
     */
    private fun watchedToEnd(saved: PlaybackPosition): Boolean =
        saved.durationMs > 0 && saved.positionMs.toFloat() / saved.durationMs >= WATCHED_RATIO

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
        orderedQueue = emptyList()
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

    /**
     * 목록에서 고르거나 자동 재생으로 넘어갈 때. 재생속도·인코딩은 이어받는다.
     *
     * [open] 이 아니라 [start] 를 부른다. 같은 재생 한 판 안에서의 이동이라
     * 대기열도, 켜 둔 랜덤·목록 반복도 그대로 두어야 한다.
     */
    fun playNext(video: VideoItem) {
        val current = _uiState.value
        start(video, resumePlayback, current.speed, current.subtitleCharset)
    }

    /**
     * 랜덤.
     *
     * 지금 보는 것을 맨 앞에 두고 나머지를 섞는다. 자리를 그대로 두고 뒤만
     * 섞으면 이미 지나온 영상들이 앞쪽에 남아 목록 반복을 켰을 때 다시 나온다.
     */
    fun toggleShuffle() {
        shuffle = !shuffle
        val video = currentVideo
        queue = when {
            !shuffle -> orderedQueue
            video == null -> orderedQueue.shuffled()
            else -> listOf(video) + orderedQueue.filter { it.uri != video.uri }.shuffled()
        }
        _uiState.update { it.copy(shuffleEnabled = shuffle) }
        refreshQueue()
    }

    /** 목록 반복. 마지막 영상 다음에 처음으로 돌아간다. */
    /** 반복 방식을 고른다. 목록이 바뀌므로 앞·뒤 영상도 다시 센다. */
    fun setRepeatMode(mode: String) {
        repeatMode = mode
        player.repeatMode = exoRepeatMode()
        _uiState.update { it.copy(repeatMode = mode) }
        refreshQueue()
    }

    /**
     * 목록 반복은 ExoPlayer 에 맡기지 않는다.
     *
     * REPEAT_MODE_ALL 은 플레이어에 담긴 항목을 도는 것인데, 우리는 한 번에 한
     * 편만 담고 대기열은 우리가 들고 있다. 넘어가는 것은 upNextOf 가 정한다.
     */
    private fun exoRepeatMode(): Int =
        if (repeatMode == RepeatMode.ONE) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF

    /** 대기열이 바뀌면 앞·뒤 영상을 다시 계산한다. 버튼이 그 자리에서 생기고 사라진다. */
    private fun refreshQueue() {
        val video = currentVideo ?: return
        _uiState.update {
            it.copy(
                upNext = upNextOf(video),
                queue = poolFor(video),
                playingUri = video.uri.toString(),
                previous = previousOf(video),
            )
        }
    }

    /** 설정에서 온 값들. 상태를 갈아끼우는 [open] 과 무관하게 유지돼야 한다. */
    fun setResumePlayback(enabled: Boolean) {
        resumePlayback = enabled
    }

    fun setAutoPlayNext(enabled: Boolean) {
        autoPlayNext = enabled
        _uiState.update { it.copy(autoPlayNextEnabled = enabled) }
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


    /**
     * 그 자리로 옮긴다. 재생 여부는 건드리지 않는다.
     *
     * 보고 있었으면 옮긴 자리에서 계속 나오고, 세워 뒀으면 세워 둔 채로 자리만
     * 바뀐다. 어디쯤인지 보려고 시크바를 만졌을 뿐인데 소리가 나기 시작하면
     * 곤란하다.
     */
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

        /** 여기까지 봤으면 다 본 것으로 친다. 보관함의 '이어보기' 와 같은 값. */
        private const val WATCHED_RATIO = 0.97f
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
                    // 기본값 false 로 덮어쓴다. 이어보기 줄에서 뺐던 영상이라도
                    // 다시 보기 시작했으면 그 줄에 도로 나와야 한다.
                    dismissedFromContinue = false,
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
