package com.pikaworks.pikaplayer.ui.player

import android.view.SurfaceView
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.pikaworks.pikaplayer.ui.formatDuration
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/**
 * 플레이어(S3, 세로).
 *
 * 시안 구조: 상단바 → 영상(컨트롤 오버레이) → 시크바 → 보조 버튼 → 다음 영상 목록
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors

    Column(modifier = modifier.fillMaxSize().background(Color.Black)) {

        TopBar(title = state.title, onBack = onBack)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clickable(onClick = onToggleControls),
        ) {
            VideoSurface(player = player, modifier = Modifier.fillMaxSize())

            if (state.controlsVisible) {
                // 밝은 장면에서 흰 아이콘이 묻히지 않도록 스크림을 깐다.
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.30f)))
                TransportControls(
                    isPlaying = state.isPlaying,
                    onTogglePlay = onTogglePlay,
                    onSkip = onSkip,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            state.cue?.let { cue ->
                // 자막은 영상 프레임 안쪽 하단에 고정. 컨트롤이 보이면 시크바를 피해 올라간다.
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
        }

        Spacer(Modifier.height(20.dp))
        SeekBar(state = state, onSeek = onSeek)
        Spacer(Modifier.height(20.dp))
        SecondaryControls(state = state)
    }
}

/**
 * 영상 표면. Compose 가 직접 그리지 못하는 유일한 부분이라 뷰를 빌려온다.
 */
@OptIn(UnstableApi::class)
@Composable
private fun VideoSurface(player: ExoPlayer, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context -> SurfaceView(context) },
        update = { view -> player.setVideoSurfaceView(view) },
        onRelease = { player.clearVideoSurface() },
    )
}

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
        Text("‹", fontSize = 26.sp, color = colors.textPrimary, modifier = Modifier.clickable(onClick = onBack))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Light, color = colors.textSecondary, maxLines = 1)
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
        // TODO: 아이콘을 시안대로 벡터로 교체한다. 지금은 동작 확인용 텍스트.
        Text("−10", fontSize = 14.sp, color = colors.textPrimary,
            modifier = Modifier.size(44.dp).clickable { onSkip(-10_000) }.padding(10.dp))

        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(colors.key.copy(alpha = 0.45f))
                .clickable(onClick = onTogglePlay),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (isPlaying) "❚❚" else "▶", fontSize = 18.sp, color = Color.White)
        }

        Text("+10", fontSize = 14.sp, color = colors.textPrimary,
            modifier = Modifier.size(44.dp).clickable { onSkip(10_000) }.padding(10.dp))
    }
}

@Composable
private fun SeekBar(state: PlayerUiState, onSeek: (Long) -> Unit) {
    val colors = PikaTheme.colors
    val density = LocalDensity.current
    var widthPx = 0f

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.16f))
                .pointerInput(state.durationMs) {
                    widthPx = size.width.toFloat()
                    detectHorizontalDragGestures { change, _ ->
                        if (widthPx > 0 && state.durationMs > 0) {
                            val ratio = (change.position.x / widthPx).coerceIn(0f, 1f)
                            onSeek((state.durationMs * ratio).toLong())
                        }
                    }
                },
        ) {
            Box(
                Modifier
                    .fillMaxWidth(state.progress)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.key)
            )
        }
        Spacer(Modifier.height(9.dp))
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
private fun SecondaryControls(state: PlayerUiState) {
    val colors = PikaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LabeledItem("${state.speed}×", "속도", active = false)
        LabeledItem("CC", state.subtitleLabel.substringBefore(" ·"), active = state.subtitleEnabled)
        LabeledItem("⤢", "화면비", active = false)
        LabeledItem("↻", "회전", active = false)
        LabeledItem("🔒", "잠금", active = false)
    }
}

@Composable
private fun LabeledItem(value: String, label: String, active: Boolean) {
    val colors = PikaTheme.colors
    val tint = if (active) colors.key else colors.textSecondary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 13.sp, color = tint)
        Spacer(Modifier.height(7.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Light,
            color = if (active) colors.key else colors.textFaint)
    }
}
