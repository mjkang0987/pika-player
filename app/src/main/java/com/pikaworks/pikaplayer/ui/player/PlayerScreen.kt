package com.pikaworks.pikaplayer.ui.player

import android.view.LayoutInflater
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.pikaworks.pikaplayer.data.media.VideoItem
import com.pikaworks.pikaplayer.R
import com.pikaworks.pikaplayer.ui.AppIcons
import com.pikaworks.pikaplayer.ui.IconTap
import com.pikaworks.pikaplayer.ui.OptionSheet
import com.pikaworks.pikaplayer.ui.SheetToggle
import com.pikaworks.pikaplayer.ui.ToggleSheet
import com.pikaworks.pikaplayer.ui.SPEED_OPTIONS
import com.pikaworks.pikaplayer.ui.formatDuration
import com.pikaworks.pikaplayer.ui.theme.PikaTheme
import kotlin.math.abs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.animateFloatAsState
import com.pikaworks.pikaplayer.ui.UpNextSheet
import androidx.compose.ui.text.style.TextOverflow

/**
 * 플레이어(S3, 세로).
 *
 * 시안 구조: 상단바 → 영상(컨트롤 오버레이) → 시크바 → 보조 버튼
 * 컨트롤은 영상 위에 얹는다. 영상 아래 따로 두면 세로 화면에서 빈 공간이 크게 남는다.
 */
/** 가만두면 컨트롤이 사라지기까지. 짧으면 읽기 전에 없어지고, 길면 계속 떠 있는 것과 같다. */
private const val CONTROLS_HIDE_MS = 3_000L

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    player: ExoPlayer,
    state: PlayerUiState,
    onTogglePlay: () -> Unit,
    onSkip: (Long) -> Unit,
    onSeek: (Long) -> Unit,
    onToggleControls: () -> Unit,
    onSelectSubtitle: (Int) -> Unit,
    onSelectCharset: (String) -> Unit,
    onAdjustSubtitleOffset: (Long) -> Unit,
    onResetSubtitleOffset: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleLoopQueue: () -> Unit,
    onSetAutoPlayNext: (Boolean) -> Unit,
    onMarkAb: () -> Unit,
    onCycleResize: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onToggleFullscreen: () -> Unit,
    onBrightnessDelta: (Float) -> Float,
    onVolumeDelta: (Float) -> Float,
    onPlayVideo: (VideoItem) -> Unit,
    /** null 이면 PiP 버튼을 아예 그리지 않는다 — 기기가 지원하지 않는 경우. */
    onEnterPip: (() -> Unit)?,
    onBack: () -> Unit,
    isFullscreen: Boolean = false,
    /** 설정 '밝기 · 볼륨 스와이프' */
    brightnessVolumeGestures: Boolean = true,
    /** 설정 '더블탭 10초 이동' */
    doubleTapSeek: Boolean = true,
    subtitleScale: Float = 1f,
    /** PiP 창 안에서는 영상만 그린다. 좁은 창에 컨트롤을 얹으면 영상이 안 보인다. */
    pipMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (pipMode) {
        VideoSurface(
            player = player,
            resizeMode = state.resizeMode,
            modifier = modifier.fillMaxSize().background(Color.Black),
        )
        return
    }

    val colors = PikaTheme.colors
    var feedback by remember { mutableStateOf<GestureFeedback?>(null) }
    var subtitleSheetVisible by remember { mutableStateOf(false) }
    // 속도는 순환 버튼이었다. 0.5 에서 2.0 으로 가려면 다섯 번을 눌러야 했고,
    // 지금 몇 배인지 보려고 또 눌러 보게 된다. 목록에서 고르게 바꾼다.
    var speedSheetVisible by remember { mutableStateOf(false) }
    // 반복·랜덤·자동 재생은 모두 "이 영상이 끝나면 무엇이 일어나는가" 한 가지
    // 질문의 답이다. 흩어 놓으면 서로 어떻게 맞물리는지 알기 어려워 한데 묶었다.
    var playbackSheetVisible by remember { mutableStateOf(false) }

    // 만지면 다시 보이고, 가만두면 사라진다. 컨트롤이 계속 떠 있으면 영상 위에
    // 반투명 막이 남아 어두운 장면이 늘 뿌옇다.
    //
    // nudge 는 "방금 뭔가 만졌다" 는 신호다. 이것이 없으면 시크바를 끌거나 10초를
    // 옮기는 동안 손 밑에서 컨트롤이 사라진다 — controlsVisible 은 그대로라서
    // 아래 효과가 다시 시작되지 않기 때문이다.
    var nudge by remember { mutableIntStateOf(0) }
    val seek: (Long) -> Unit = { nudge++; onSeek(it) }
    var upNextSheetVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state.controlsVisible, state.isPlaying, state.locked, nudge) {
        if (state.controlsVisible && state.isPlaying && !state.locked) {
            delay(CONTROLS_HIDE_MS)
            onToggleControls()
        }
    }

    if (upNextSheetVisible) {
        UpNextSheet(
            videos = state.queue,
            playingUri = state.playingUri,
            onClick = {
                upNextSheetVisible = false
                onPlayVideo(it)
            },
            onDismiss = { upNextSheetVisible = false },
        )
    }

    if (playbackSheetVisible) {
        ToggleSheet(
            title = "반복과 순서",
            toggles = listOfNotNull(
                SheetToggle(
                    label = "한 편 반복",
                    on = state.repeatEnabled,
                    description = "지금 영상이 끝나면 처음부터 다시 틉니다.",
                    onToggle = onToggleRepeat,
                ),
                // 목록을 통째로 튼 것이 아니면(보관함에서 한 편) 뒤에 올 목록 자체가 없다.
                SheetToggle(
                    label = "목록 반복",
                    on = state.loopQueueEnabled,
                    description = "마지막 영상 다음에 목록의 처음으로 돌아갑니다.",
                    onToggle = onToggleLoopQueue,
                ).takeIf { state.explicitQueue },
                SheetToggle(
                    label = "랜덤",
                    on = state.shuffleEnabled,
                    description = "지금 영상 뒤의 순서를 섞습니다.",
                    onToggle = onToggleShuffle,
                ).takeIf { state.explicitQueue },
                SheetToggle(
                    label = "자동 재생",
                    on = state.autoPlayNextEnabled,
                    description = "끝나면 다음 영상으로 넘어갑니다. 이미 끝까지 본 영상은 처음부터 틉니다.",
                    onToggle = { onSetAutoPlayNext(!state.autoPlayNextEnabled) },
                ),
            ),
            onDismiss = { playbackSheetVisible = false },
            onMedia = true,
        )
    }

    if (speedSheetVisible) {
        OptionSheet(
            title = "재생속도",
            options = SPEED_OPTIONS,
            selected = state.speed,
            onSelect = onSelectSpeed,
            onDismiss = { speedSheetVisible = false },
            onMedia = true,
        )
    }

    if (subtitleSheetVisible) {
        SubtitleSheet(
            state = state,
            onSelectSubtitle = onSelectSubtitle,
            onSelectCharset = onSelectCharset,
            onAdjustOffset = onAdjustSubtitleOffset,
            onResetOffset = onResetSubtitleOffset,
            onDismiss = { subtitleSheetVisible = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {

        // 영상 자리는 화면 전체다. 제목 줄을 위에 쌓아 두면 그만큼 영상이 작아지는데,
        // 세로 영상에서는 그 몫이 그대로 세로 크기다. 제목도 컨트롤과 함께 영상 위에
        // 얹고 같이 걷히게 한다.
        //
        // 시스템 바 인셋은 얹히는 것들이 각자 갖는다. 여기서 한 번에 주면 영상까지
        // 밀려 들어와 위아래로 검은 띠가 남는다.
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
                // '채움' 은 넘치는 부분을 잘라내는 게 목적이다. 잘리지 않으면
                // 영상이 아래 버튼 줄 위로 흘러나온다.
                .clipToBounds()
                .playerGestures(
                    enabled = !state.locked,
                    brightnessVolumeEnabled = brightnessVolumeGestures,
                    doubleTapSeekEnabled = doubleTapSeek,
                    durationMs = state.durationMs,
                    currentPositionMs = { state.positionMs },
                    onTap = onToggleControls,
                    onDoubleTapSeek = onSkip,
                    onSeekCommit = onSeek,
                    onBrightnessDelta = { d -> feedback = GestureFeedback.Brightness(onBrightnessDelta(d)) },
                    onVolumeDelta = { d -> feedback = GestureFeedback.Volume(onVolumeDelta(d)) },
                    onFeedback = { feedback = it },
                ),
        ) {
            VideoSurface(player = player, resizeMode = state.resizeMode, modifier = Modifier.fillMaxSize())

            if (state.locked) {
                // 잠금 중에는 해제 버튼 하나만. 실수로 눌리는 것을 막는 게 목적이다.
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 20.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(onClick = onToggleLock),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.Lock, "잠금 해제", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            } else {
                // 사라질 때 툭 꺼지면 영상이 깜빡인 것처럼 보인다. 스크림과 버튼이
                // 같이 옅어져야 "가려져 있던 것이 걷혔다" 로 읽힌다.
                //
                // AnimatedVisibility 를 쓰지 않는다. 여기는 Box 안이지만 바깥에
                // Column 이 있어서 같은 이름의 ColumnScope 확장이 먼저 잡히고,
                // 그쪽은 이 자리에서 부를 수 없다. 알파를 직접 다루는 편이 짧다.
                //
                // 다 옅어지면 아예 그리지 않는다. 투명한 채로 남으면 안 보이는
                // 버튼이 화면을 덮어, 영상을 눌러도 컨트롤이 다시 나오지 않는다.
                val controlsAlpha by animateFloatAsState(
                    targetValue = if (state.controlsVisible) 1f else 0f,
                    label = "controls",
                )
                if (controlsAlpha > 0f) {
                    Box(Modifier.fillMaxSize().alpha(controlsAlpha)) {
                        // 밝은 장면에서 흰 아이콘이 묻히지 않도록 스크림을 깐다.
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.30f)))
                        // 가운데에는 재생 묶음만 둔다. 다른 것을 같이 세우면 그
                        // 있고 없음에 따라 재생 버튼이 위아래로 움직인다 — 눈 감고도
                        // 닿아야 하는 버튼이다.
                        TransportControls(
                            isPlaying = state.isPlaying,
                            onTogglePlay = onTogglePlay,
                            onSkip = { nudge++; onSkip(it) },
                            onPrevious = state.previous?.let { { onPlayVideo(it) } },
                            onNext = state.upNext.firstOrNull()?.let { { onPlayVideo(it) } },
                            modifier = Modifier.align(Alignment.Center),
                        )

                        // 재생목록은 제목 줄 오른쪽 끝에 붙인다. 글자 수가 바뀌어도
                        // 오른쪽에 매달려 있어 자리가 흔들리지 않는다.
                        PlayerTopBar(
                            title = state.title,
                            onBack = onBack,
                            modifier = Modifier.align(Alignment.TopStart),
                            trailing = {
                                UpNextButton(onClick = { upNextSheetVisible = true })
                            },
                        )

                        // 세로든 가로든 컨트롤은 영상 위에 얹는다. 아래에 따로
                        // 두면 그만큼 영상이 위로 밀려 올라가고, 컨트롤이 사라져도
                        // 그 자리는 검게 남는다. 얹어 두면 걷혔을 때 영상만 남는다.
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                // 3버튼 내비게이션에 가리지 않게. 전체화면에서는
                                // 시스템 바를 감춘 상태라 이 값이 0 이 된다.
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
                        ) {
                            SeekBar(state = state, onSeek = seek, onMarkAb = onMarkAb)
                            Spacer(Modifier.height(2.dp))
                            SecondaryControls(
                                state = state,
                                isFullscreen = isFullscreen,
                                onOpenSubtitleSheet = { subtitleSheetVisible = true },
                                onCycleResize = onCycleResize,
                                onOpenSpeedSheet = { speedSheetVisible = true },
                                onToggleFullscreen = onToggleFullscreen,
                                onToggleLock = onToggleLock,
                                onOpenPlaybackSheet = { playbackSheetVisible = true },
                                onEnterPip = onEnterPip,
                            )
                        }
                    }
                }
            }

            // 자막은 늘 영상 위에 얹는다.
            //
            // 영상 아래에 띠를 만들어 거기 그리는 선택지가 있었다. 영상을 가리지
            // 않는 대신 띠 높이만큼 영상이 작아졌는데, 영상이 화면을 다 쓰게 된
            // 지금은 그 대가가 그대로 손해다. 게다가 가로 영상은 이미 위아래가
            // 검게 남아서, 아래에 얹은 자막이 대개 그 검은 자리에 놓인다.
            //
            // 자리는 고정한다. 컨트롤이 뜰 때 비켜서게 해 봤는데, 자막이 화면
            // 한가운데까지 뛰어올랐다가 3초 뒤에 도로 내려온다. 겹쳐서 한 줄 못
            // 읽는 것보다 눈이 자막을 따라다녀야 하는 쪽이 더 거슬린다.
            state.cue?.let { cue ->
                SubtitleText(
                    text = cue.text,
                    scale = subtitleScale,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 26.dp, end = 26.dp, bottom = 28.dp),
                )
            }

            feedback?.let { GestureIndicator(it, state.positionMs, Modifier.align(Alignment.Center)) }
        }

    }
}

/**
 * 지금 틀고 있는 목록을 여는 문.
 *
 * 글자가 늘 같다. 개수나 남은 편수를 달아 두면 영상이 넘어갈 때마다 폭이 변해서,
 * 제목이 잘리는 자리가 계속 달라진다. 목록의 마지막에 와 있어도 그대로 둔다 —
 * 지금 무엇을 틀고 있는지는 여기서 여전히 볼 수 있다.
 */
@Composable
private fun UpNextButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PikaTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Text(
        "재생목록",
        fontSize = 12.sp,
        color = colors.onMediaText,
        // 폭을 채우지 않는다. 제목 줄 오른쪽 끝에 매달리는 것이라 글자만큼만
        // 차지해야 제목에 줄 폭이 남는다.
        modifier = modifier
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.22f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/**
 * 자막 한 줄.
 *
 * 크기는 15sp 를 기준으로 배율만 곱한다. 설정에서 고른 값이 두 위치(영상 안·레터박스)에
 * 똑같이 적용되어야 해서 한 곳에 모았다. 그림자를 넣는 이유는 밝은 장면 위에서
 * 흰 글자가 사라지기 때문이다.
 */
@Composable
private fun SubtitleText(text: String, scale: Float, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        fontSize = (15 * scale).sp,
        lineHeight = (21 * scale).sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        style = LocalTextStyle.current.copy(
            shadow = Shadow(color = Color.Black.copy(alpha = 0.75f), blurRadius = 6f),
        ),
        modifier = modifier,
    )
}

@Composable
private fun PlayerTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** 오른쪽 끝에 붙는 것. 지금은 재생목록 버튼. */
    trailing: @Composable () -> Unit = {},
) {
    Row(
        // 왼쪽·위아래 여백은 IconTap 이 아이콘보다 커진 만큼(가로 10dp, 세로 10dp)
        // 덜어낸 값이다. 아이콘이 놓이는 자리는 전과 같다.
        modifier = modifier.fillMaxWidth()
            // 상태 표시줄에 가리지 않게. 전체화면에서는 0 이 된다.
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 10.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconTap(AppIcons.Back, "뒤로", onClick = onBack, tint = Color.White, iconSize = 24.dp)
        // 남는 폭을 제목이 갖는다. 그러지 않으면 긴 제목이 오른쪽 버튼 밑으로 들어간다.
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/**
 * 영상 표면. Compose 가 직접 그리지 못하는 유일한 부분이라 뷰를 빌려온다.
 *
 * 컨트롤은 우리가 그리지만(`useController = false`) 표면 자체는 Media3 의
 * PlayerView 를 쓴다. 화면비 처리와 표면 생명주기를 직접 다루면 실수하기 쉽다.
 * 자막도 우리가 그리므로 내장 자막 뷰는 숨긴다.
 */
@OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(player: ExoPlayer, resizeMode: Int, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        // 레이아웃에서 부풀린다. surface_type 은 XML 에서만 정할 수 있는데,
        // 기본값인 SurfaceView 는 '채움' 에서 상자 밖으로 넘쳐 아래 버튼 줄을
        // 덮는다. 자세한 사정은 pika_player_view.xml 에 적어 두었다.
        factory = { context ->
            (LayoutInflater.from(context).inflate(R.layout.pika_player_view, null) as PlayerView)
                .apply { subtitleView?.visibility = View.GONE }
        },
        update = { view ->
            view.player = player
            view.resizeMode = RESIZE_MODES[resizeMode.coerceIn(RESIZE_MODES.indices)]
        },
        onRelease = { it.player = null },
    )
}

/** 맞춤 / 채움 / 늘이기 */
private val RESIZE_MODES = intArrayOf(
    AspectRatioFrameLayout.RESIZE_MODE_FIT,
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
    AspectRatioFrameLayout.RESIZE_MODE_FILL,
)


@Composable
private fun TransportControls(
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSkip: (Long) -> Unit,
    /** 이전 영상이 없으면 null. 자리는 남기고 누를 수 없게 둔다. */
    onPrevious: (() -> Unit)?,
    /** 다음 영상이 없으면 null. */
    onNext: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 갈 곳이 없어도 자리는 남긴다. 없앴더니 목록의 처음과 끝에서 줄이
        // 좁아지면서 재생 버튼이 좌우로 움직였다 — 눈 감고도 닿아야 하는
        // 버튼이라 자리가 흔들리는 쪽이 흐린 버튼보다 나쁘다.
        TrackButton(AppIcons.PreviousTrack, "이전 영상", onPrevious)

        SkipButton(AppIcons.Replay10, "10초 뒤로") { onSkip(-10_000) }

        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(27.dp))
                // 반투명이라 뒤 영상이 비친다. 아이콘은 흰색 불투명으로 또렷하게.
                .background(colors.onMediaKey.copy(alpha = 0.45f))
                .clickable(onClick = onTogglePlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) AppIcons.Pause else AppIcons.Play,
                contentDescription = if (isPlaying) "일시정지" else "재생",
                tint = Color.White,
                modifier = Modifier.size(34.dp),
            )
        }

        SkipButton(AppIcons.Forward10, "10초 앞으로") { onSkip(10_000) }

        TrackButton(AppIcons.NextTrack, "다음 영상", onNext)
    }
}

/** 이전·다음 영상. 10초 이동보다 한 단계 작게 둔다 — 자주 쓰는 쪽은 가운데다. */
@Composable
private fun TrackButton(icon: ImageVector, label: String, onClick: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
        contentAlignment = Alignment.Center,
    ) {
        // 갈 곳이 없으면 글자를 낮춰 알린다. 눌러도 아무 일이 없다는 것을
        // 눌러 보기 전에 알 수 있어야 한다.
        Icon(
            icon,
            label,
            tint = Color.White.copy(alpha = if (onClick == null) 0.25f else 1f),
            modifier = Modifier.size(21.dp),
        )
    }
}

/** 원 안에 '10' 을 겹쳐 그린다. 벡터에 숫자를 넣으면 폰트를 따라가지 못한다. */
@Composable
private fun SkipButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = Color.White,
            modifier = Modifier.size(30.dp))
        Text("10", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun SeekBar(
    state: PlayerUiState,
    onSeek: (Long) -> Unit,
    onMarkAb: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp) // 터치 영역. 보이는 바는 3dp 지만 그대로 두면 잡기 어렵다
                // 끌기와 따로 건다. 한 블록에서 둘을 다루면 먼저 등록한 쪽이
                // 몸짓을 삼켜서, 짧게 누른 것이 아무 일도 아닌 것이 된다.
                .pointerInput(state.durationMs) {
                    detectTapGestures { offset ->
                        if (size.width > 0 && state.durationMs > 0) {
                            val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeek((state.durationMs * ratio).toLong())
                        }
                    }
                }
                .pointerInput(state.durationMs) {
                    detectHorizontalDragGestures { change, _ ->
                        if (size.width > 0 && state.durationMs > 0) {
                            val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                            onSeek((state.durationMs * ratio).toLong())
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(state.progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.onMediaKey)
                )
            }

            // 구간 표시. 3dp 막대 안에 그리면 보이지 않아 위아래로 넘치게 긋는다.
            val marks = listOfNotNull(state.abStartMs, state.abEndMs)
            if (marks.isNotEmpty() && state.durationMs > 0) {
                val markColor = colors.onMediaText
                Canvas(modifier = Modifier.fillMaxWidth().height(11.dp)) {
                    val thickness = 2.dp.toPx()
                    marks.forEach { ms ->
                        val x = size.width * (ms.toFloat() / state.durationMs).coerceIn(0f, 1f)
                        drawRect(
                            color = markColor,
                            // 막대보다 눈금이 두꺼운 극단적인 경우에 coerceIn 이
                            // 터지지 않도록 위쪽 한계를 먼저 깎는다.
                            topLeft = Offset(
                                (x - thickness / 2).coerceIn(0f, (size.width - thickness).coerceAtLeast(0f)),
                                0f,
                            ),
                            size = Size(thickness, size.height),
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatDuration(state.positionMs), fontSize = 11.sp,
                fontWeight = FontWeight.Light, color = colors.onMediaTextMuted)
            AbButton(state = state, onClick = onMarkAb)
            Text("-" + formatDuration(state.durationMs - state.positionMs), fontSize = 11.sp,
                fontWeight = FontWeight.Light, color = colors.onMediaTextMuted)
        }
    }
}

/**
 * 구간 반복 버튼. 누를 때마다 A → B → 해제.
 *
 * 아래 아이콘 줄이 아니라 시간 줄에 두었다. 다루는 것이 '재생 위치' 라서
 * 시간·시크바 옆이 제자리이고, 아이콘 줄은 이미 일곱 개다.
 *
 * 찍은 지점을 글자로 보여 준다. 시크바의 눈금만으로는 어디였는지 읽기 어렵고,
 * 반복 연습은 몇 초 구간인지가 곧 목적이기 때문이다.
 *
 * 라벨은 지금 상태와 **다음에 할 일**을 함께 말한다. 'A-B' 는 그 용어를 아는
 * 사람에게만 읽히는 말이다 — VLC 가 메뉴에 "시작 지점 설정" 이라고 풀어 쓰는
 * 것과 같은 이유로, 처음 보는 사람도 무엇을 하는 버튼인지 알 수 있어야 한다.
 */
@Composable
private fun AbButton(state: PlayerUiState, onClick: () -> Unit) {
    val colors = PikaTheme.colors
    val start = state.abStartMs
    val end = state.abEndMs
    val label = when {
        start == null -> "구간 반복"
        end == null -> "A " + formatDuration(start) + " · 끝 지정"
        else -> formatDuration(start) + " ~ " + formatDuration(end)
    }
    Text(
        label,
        fontSize = 11.sp,
        fontWeight = if (start == null) FontWeight.Light else FontWeight.Medium,
        color = if (start == null) colors.onMediaTextFaint else colors.onMediaKey,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

@Composable
private fun SecondaryControls(
    modifier: Modifier = Modifier,
    state: PlayerUiState,
    isFullscreen: Boolean,
    onOpenSubtitleSheet: () -> Unit,
    onCycleResize: () -> Unit,
    onOpenSpeedSheet: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleLock: () -> Unit,
    onOpenPlaybackSheet: () -> Unit,
    onEnterPip: (() -> Unit)?,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        // 폭을 고정하면 버튼이 하나 늘 때마다 좁은 화면에서 넘친다. 남는 폭을
        // 균등하게 나눠 갖게 해서 개수·화면 크기와 무관하게 들어맞게 한다.
        SpeedItem(state.speed, onOpenSpeedSheet, Modifier.weight(1f))
        IconItem(AppIcons.Subtitle, "자막", state.subtitleEnabled, onOpenSubtitleSheet, Modifier.weight(1f))
        IconItem(
            AppIcons.AspectRatio,
            PlayerViewModel.RESIZE_MODE_LABELS[state.resizeMode.coerceIn(PlayerViewModel.RESIZE_MODE_LABELS.indices)],
            active = state.resizeMode != 0,
            onClick = onCycleResize,
            modifier = Modifier.weight(1f),
        )
        // 켜짐은 한 편 반복만이 아니다. 목록 반복이나 랜덤이 켜져 있는데 이
        // 버튼이 꺼진 것으로 보이면, 순서가 왜 이런지 찾을 곳이 없어진다.
        IconItem(
            icon = AppIcons.Repeat,
            label = "반복",
            active = state.repeatEnabled || state.loopQueueEnabled || state.shuffleEnabled,
            onClick = onOpenPlaybackSheet,
            modifier = Modifier.weight(1f),
        )
        // 이름은 그대로 두고 아이콘과 색이 상태를 말한다. 이 줄의 다른 버튼도
        // 모두 기능 이름을 달고 켜짐만 색으로 알린다 — 여기만 "나가기" 같은
        // 동사가 되면 결이 깨지고, 무엇을 나가는지도 애매해진다.
        IconItem(
            icon = if (isFullscreen) AppIcons.FullscreenExit else AppIcons.Fullscreen,
            label = "전체화면",
            active = isFullscreen,
            onClick = onToggleFullscreen,
            modifier = Modifier.weight(1f),
        )
        // 지원하지 않는 기기에서는 자리를 만들지 않는다. 눌러도 아무 일이 없는
        // 버튼을 두면 고장으로 읽힌다.
        onEnterPip?.let { IconItem(AppIcons.Pip, "작은 창", false, it, Modifier.weight(1f)) }
        IconItem(AppIcons.Lock, "잠금", state.locked, onToggleLock, Modifier.weight(1f))
    }
}

@Composable
private fun SpeedItem(speed: Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PikaTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 아이콘(19dp)과 같은 줄 높이를 준다. 기본 줄 높이는 24sp 라 이 글자만
        // 칸이 커져서 아래 라벨이 다른 버튼보다 내려가 있었다.
        Text("${speed}×", fontSize = 13.sp, lineHeight = 19.sp, color = colors.onMediaKey)
        Spacer(Modifier.height(7.dp))
        Text("속도", fontSize = 10.sp, fontWeight = FontWeight.Light, color = colors.onMediaTextFaint)
    }
}

@Composable
private fun IconItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors
    val tint = if (active) colors.onMediaKey else colors.onMediaTextMuted
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(19.dp))
        Spacer(Modifier.height(7.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Light,
            color = if (active) colors.onMediaKey else colors.onMediaTextFaint)
    }
}

/** 제스처 중에만 뜨는 표시. 값이 얼마나 바뀌는지 보여주지 않으면 감으로 조작하게 된다. */
@Composable
private fun GestureIndicator(feedback: GestureFeedback, positionMs: Long, modifier: Modifier = Modifier) {
    val colors = PikaTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        when (feedback) {
            is GestureFeedback.Seek -> {
                val sign = if (feedback.deltaMs >= 0) "+" else "−"
                Text(
                    "${formatDuration(feedback.targetMs)}   $sign${formatDuration(abs(feedback.deltaMs))}",
                    fontSize = 15.sp, color = Color.White,
                )
            }
            is GestureFeedback.Brightness -> Bar(AppIcons.Brightness, "밝기", feedback.value, colors.onMediaKey)
            is GestureFeedback.Volume -> Bar(AppIcons.Volume, "볼륨", feedback.value, colors.onMediaKey)
        }
    }
}

@Composable
private fun Bar(icon: ImageVector, label: String, value: Float, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
        Box(
            modifier = Modifier
                .width(96.dp).height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.24f)),
        ) {
            Box(Modifier.fillMaxWidth(value).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(accent))
        }
        Text("${(value * 100).toInt()}%", fontSize = 12.sp, color = Color.White)
    }
}
