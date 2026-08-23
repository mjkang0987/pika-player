package com.pikaworks.pikaplayer.ui.player

import android.view.View
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.pikaworks.pikaplayer.ui.AppIcons
import com.pikaworks.pikaplayer.ui.formatDuration
import com.pikaworks.pikaplayer.ui.theme.PikaTheme
import kotlin.math.abs

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
    onToggleSubtitle: () -> Unit,
    onToggleLock: () -> Unit,
    onCycleResize: () -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onBrightnessDelta: (Float) -> Float,
    onVolumeDelta: (Float) -> Float,
    onBack: () -> Unit,
    isFullscreen: Boolean = false,
    gesturesEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors
    var feedback by remember { mutableStateOf<GestureFeedback?>(null) }

    Column(modifier = modifier.fillMaxSize().background(Color.Black)) {

        if (!isFullscreen) TopBar(title = state.title, onBack = onBack)

        Box(
            modifier = (if (isFullscreen) Modifier.weight(1f).fillMaxWidth()
                        else Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                .playerGestures(
                    enabled = gesturesEnabled && !state.locked,
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
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                    ) {
                        SeekBar(state = state, onSeek = onSeek)
                        Spacer(Modifier.height(10.dp))
                        SecondaryControls(
                            state = state,
                            onToggleSubtitle = onToggleSubtitle,
                            onCycleResize = onCycleResize,
                            onCycleSpeed = onCycleSpeed,
                            onToggleFullscreen = onToggleFullscreen,
                            onToggleLock = onToggleLock,
                        )
                    }
                }
            }

            state.cue?.let { cue ->
                // 자막은 영상 프레임 안쪽 하단에 고정한다.
                Text(
                    text = cue.text,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 26.dp, bottom = 10.dp),
                )
            }

            feedback?.let { GestureIndicator(it, state.positionMs, Modifier.align(Alignment.Center)) }
        }

        if (!isFullscreen) {
            Spacer(Modifier.height(20.dp))
            SeekBar(state = state, onSeek = onSeek, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(20.dp))
            SecondaryControls(
                modifier = Modifier.padding(horizontal = 24.dp),
                state = state,
                onToggleSubtitle = onToggleSubtitle,
                onCycleResize = onCycleResize,
                onCycleSpeed = onCycleSpeed,
                onToggleFullscreen = onToggleFullscreen,
                onToggleLock = onToggleLock,
            )
        }
    }
}

@Composable
private fun FullscreenTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(AppIcons.Back, "뒤로", tint = Color.White,
            modifier = Modifier.size(24.dp).clickable(onClick = onBack))
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
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                subtitleView?.visibility = View.GONE
            }
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
            .padding(start = 22.dp, end = 22.dp, top = 62.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = AppIcons.Back,
            contentDescription = "뒤로",
            tint = colors.textPrimary,
            modifier = Modifier.size(24.dp).clickable(onClick = onBack),
        )
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            color = colors.textSecondary,
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
                .background(colors.key.copy(alpha = 0.45f))
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
private fun SeekBar(state: PlayerUiState, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
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
                        .background(colors.key)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatDuration(state.positionMs), fontSize = 11.sp,
                fontWeight = FontWeight.Light, color = colors.textSecondary)
            Text("-" + formatDuration(state.durationMs - state.positionMs), fontSize = 11.sp,
                fontWeight = FontWeight.Light, color = colors.textSecondary)
        }
    }
}

@Composable
private fun SecondaryControls(
    modifier: Modifier = Modifier,
    state: PlayerUiState,
    onToggleSubtitle: () -> Unit,
    onCycleResize: () -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleLock: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SpeedItem(state.speed, onCycleSpeed)
        IconItem(AppIcons.Subtitle, "자막", active = state.subtitleEnabled, onClick = onToggleSubtitle)
        IconItem(
            AppIcons.AspectRatio,
            PlayerViewModel.RESIZE_MODE_LABELS[state.resizeMode.coerceIn(PlayerViewModel.RESIZE_MODE_LABELS.indices)],
            active = state.resizeMode != 0,
            onClick = onCycleResize,
        )
        IconItem(AppIcons.Fullscreen, "전체화면", active = false, onClick = onToggleFullscreen)
        IconItem(AppIcons.Lock, "잠금", active = state.locked, onClick = onToggleLock)
    }
}

@Composable
private fun SpeedItem(speed: Float, onClick: () -> Unit) {
    val colors = PikaTheme.colors
    Column(
        modifier = Modifier.width(56.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("${speed}×", fontSize = 13.sp, color = colors.key)
        Spacer(Modifier.height(7.dp))
        Text("속도", fontSize = 10.sp, fontWeight = FontWeight.Light, color = colors.textFaint)
    }
}

@Composable
private fun IconItem(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val colors = PikaTheme.colors
    val tint = if (active) colors.key else colors.textSecondary
    Column(
        modifier = Modifier.width(56.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(19.dp))
        Spacer(Modifier.height(7.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Light,
            color = if (active) colors.key else colors.textFaint)
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
            is GestureFeedback.Brightness -> Bar(AppIcons.Brightness, "밝기", feedback.value, colors.key)
            is GestureFeedback.Volume -> Bar(AppIcons.Volume, "볼륨", feedback.value, colors.key)
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
