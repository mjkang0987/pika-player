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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.pikaworks.pikaplayer.data.media.VideoItem
import com.pikaworks.pikaplayer.data.prefs.SubtitlePosition
import com.pikaworks.pikaplayer.R
import com.pikaworks.pikaplayer.ui.AppIcons
import com.pikaworks.pikaplayer.ui.IconTap
import com.pikaworks.pikaplayer.ui.OptionSheet
import com.pikaworks.pikaplayer.ui.SPEED_OPTIONS
import com.pikaworks.pikaplayer.ui.formatDuration
import com.pikaworks.pikaplayer.ui.theme.PikaTheme
import kotlin.math.abs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * 플레이어(S3, 세로).
 *
 * 시안 구조: 상단바 → 영상(컨트롤 오버레이) → 시크바 → 보조 버튼
 * 컨트롤은 영상 위에 얹는다. 영상 아래 따로 두면 세로 화면에서 빈 공간이 크게 남는다.
 */
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
    /** SubtitlePosition 값. 레터박스면 영상 프레임 아래 검은 띠에 그린다. */
    subtitlePosition: String = SubtitlePosition.IN_VIDEO,
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

    if (speedSheetVisible) {
        OptionSheet(
            title = "재생속도",
            options = SPEED_OPTIONS,
            selected = state.speed,
            onSelect = onSelectSpeed,
            onDismiss = { speedSheetVisible = false },
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
            .background(Color.Black)
            // 이 화면은 시스템 바 아래까지 그려진다. 이 인셋이 없으면 맨 아래
            // 내용이 3버튼 내비게이션에 가린다. 배경은 인셋보다 먼저 칠하므로
            // 검은 바탕은 그대로 화면 끝까지 간다.
            //
            // 전체화면에서는 시스템 바를 감춘 상태라 이 값이 0 이 되어 영향이 없다.
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {

        if (!isFullscreen) TopBar(title = state.title, onBack = onBack)

        Box(
            modifier = (if (isFullscreen) Modifier.weight(1f).fillMaxWidth()
                        else Modifier.fillMaxWidth().aspectRatio(16f / 9f))
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
            } else if (state.controlsVisible) {
                // 밝은 장면에서 흰 아이콘이 묻히지 않도록 스크림을 깐다.
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.30f)))
                TransportControls(
                    isPlaying = state.isPlaying,
                    onTogglePlay = onTogglePlay,
                    onSkip = onSkip,
                    modifier = Modifier.align(Alignment.Center),
                )

                if (isFullscreen) {
                    // 가로에서는 컨트롤이 전부 영상 위에 얹힌다. 아래에 둘 자리가 없다.
                    FullscreenTopBar(
                        title = state.title,
                        onBack = onBack,
                        modifier = Modifier.align(Alignment.TopStart),
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
                    ) {
                        SeekBar(state = state, onSeek = onSeek, onMarkAb = onMarkAb)
                        Spacer(Modifier.height(2.dp))
                        SecondaryControls(
                            state = state,
                            onOpenSubtitleSheet = { subtitleSheetVisible = true },
                            onCycleResize = onCycleResize,
                            onOpenSpeedSheet = { speedSheetVisible = true },
                            onToggleFullscreen = onToggleFullscreen,
                            onToggleLock = onToggleLock,
                            onToggleRepeat = onToggleRepeat,
                            onEnterPip = onEnterPip,
                        )
                    }
                }
            }

            // 전체화면에는 레터박스 영역이 없으므로 항상 영상 안에 그린다.
            val insideVideo = isFullscreen || subtitlePosition == SubtitlePosition.IN_VIDEO
            if (insideVideo) {
                state.cue?.let { cue ->
                    SubtitleText(
                        text = cue.text,
                        scale = subtitleScale,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 26.dp, end = 26.dp, bottom = 10.dp),
                    )
                }
            }

            feedback?.let { GestureIndicator(it, state.positionMs, Modifier.align(Alignment.Center)) }
        }

        if (!isFullscreen && subtitlePosition == SubtitlePosition.LETTERBOX) {
            // 영상 아래 띠에 그린다. 자막이 영상을 가리지 않는다.
            Box(modifier = Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
                state.cue?.let { cue ->
                    SubtitleText(
                        text = cue.text,
                        scale = subtitleScale,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp),
                    )
                }
            }
        }

        if (!isFullscreen) {
            Spacer(Modifier.height(20.dp))
            SeekBar(
                state = state,
                onSeek = onSeek,
                onMarkAb = onMarkAb,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(12.dp))
            SecondaryControls(
                modifier = Modifier.padding(horizontal = 24.dp),
                state = state,
                onOpenSubtitleSheet = { subtitleSheetVisible = true },
                onCycleResize = onCycleResize,
                onOpenSpeedSheet = { speedSheetVisible = true },
                onToggleFullscreen = onToggleFullscreen,
                onToggleLock = onToggleLock,
                onToggleRepeat = onToggleRepeat,
                onEnterPip = onEnterPip,
            )
            // 남는 세로 공간만 쓴다. 좁은 화면에서는 높이가 0이 되어 조용히 사라진다.
            UpNext(
                videos = state.upNext,
                onClick = onPlayVideo,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }
}

/**
 * 플레이어 하단의 다음 영상 목록.
 *
 * 플레이어는 라이트 테마에서도 검은 배경이라 목록 행을 그대로 쓸 수 없다.
 * 여기서는 영상 위에 얹는 색(onMedia*)만 쓴다.
 */
@Composable
private fun UpNext(videos: List<VideoItem>, onClick: (VideoItem) -> Unit, modifier: Modifier = Modifier) {
    val colors = PikaTheme.colors
    Column(modifier = modifier) {
        Text(
            "다음 영상",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
            color = colors.onMediaKey,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 6.dp),
        )
        // 비었다고 통째로 빼면 아래가 검게 남아 화면이 잘린 것처럼 보인다.
        // 자리는 그대로 두고 왜 비었는지만 말한다.
        if (videos.isEmpty()) {
            Text(
                "이 폴더의 마지막 영상입니다",
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                color = colors.onMediaTextFaint,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp),
            )
        } else {
            LazyColumn {
                items(videos, key = { it.uri.toString() }) { video ->
                    UpNextRow(video = video, onClick = { onClick(video) })
                }
            }
        }
    }
}

@Composable
private fun UpNextRow(video: VideoItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier
                .width(68.dp).height(38.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.10f)),
        ) {
            AsyncImage(
                model = video.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            video.displayName,
            fontSize = 12.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            formatDuration(video.durationMs),
            fontSize = 10.sp,
            fontWeight = FontWeight.Light,
            color = Color.White.copy(alpha = 0.55f),
        )
    }
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
private fun FullscreenTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        // 왼쪽·위아래 여백은 IconTap 이 아이콘보다 커진 만큼(가로 10dp, 세로 10dp)
        // 덜어낸 값이다. 아이콘이 놓이는 자리는 전과 같다.
        modifier = modifier.fillMaxWidth()
            .padding(start = 10.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconTap(AppIcons.Back, "뒤로", onClick = onBack, tint = Color.White, iconSize = 24.dp)
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1)
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
private fun TopBar(title: String, onBack: () -> Unit) {
    val colors = PikaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            // 가로 10dp·세로 10dp 는 IconTap 이 아이콘보다 커진 몫을 덜어낸 값이다.
            .padding(start = 12.dp, end = 22.dp, top = 6.dp, bottom = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        IconTap(AppIcons.Back, "뒤로", onClick = onBack, tint = colors.onMediaText, iconSize = 24.dp)
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            color = colors.onMediaTextMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSkip: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
    onOpenSubtitleSheet: () -> Unit,
    onCycleResize: () -> Unit,
    onOpenSpeedSheet: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleRepeat: () -> Unit,
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
        IconItem(AppIcons.Repeat, "반복", state.repeatEnabled, onToggleRepeat, Modifier.weight(1f))
        IconItem(AppIcons.Fullscreen, "전체화면", false, onToggleFullscreen, Modifier.weight(1f))
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
