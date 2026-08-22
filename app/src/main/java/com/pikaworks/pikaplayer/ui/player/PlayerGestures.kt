package com.pikaworks.pikaplayer.ui.player

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/** 제스처 중 화면에 띄울 피드백. 없으면 null. */
sealed interface GestureFeedback {
    /** 좌우 스와이프 탐색 중. [targetMs] 로 이동 예정, [deltaMs] 는 현재 위치 대비 증감 */
    data class Seek(val targetMs: Long, val deltaMs: Long) : GestureFeedback
    data class Brightness(val value: Float) : GestureFeedback
    data class Volume(val value: Float) : GestureFeedback
}

/** 화면 너비를 끝에서 끝까지 훑었을 때 이동하는 시간. 너무 크면 미세 조정이 안 된다. */
private const val SEEK_RANGE_MS = 120_000L

private enum class DragMode { NONE, SEEK, BRIGHTNESS, VOLUME }

/**
 * 플레이어 제스처.
 *
 * - 좌우 스와이프: 탐색 (놓을 때 반영, 끄는 동안은 미리보기만)
 * - 왼쪽 상하: 밝기 / 오른쪽 상하: 볼륨
 * - 더블탭: 좌우 10초 이동
 * - 한 번 탭: 컨트롤 표시 전환
 *
 * 방향은 드래그 시작 직후 한 번만 정한다. 매 프레임 다시 판단하면
 * 대각선으로 움직일 때 탐색과 볼륨이 번갈아 걸린다.
 */
fun Modifier.playerGestures(
    enabled: Boolean,
    durationMs: Long,
    currentPositionMs: () -> Long,
    onTap: () -> Unit,
    onDoubleTapSeek: (Long) -> Unit,
    onSeekCommit: (Long) -> Unit,
    onBrightnessDelta: (Float) -> Unit,
    onVolumeDelta: (Float) -> Unit,
    onFeedback: (GestureFeedback?) -> Unit,
): Modifier = composed {
    this
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onTap = { onTap() },
                onDoubleTap = { offset ->
                    val delta = if (offset.x < size.width / 2f) -10_000L else 10_000L
                    onDoubleTapSeek(delta)
                },
            )
        }
        .pointerInput(enabled, durationMs) {
            if (!enabled) return@pointerInput

            var mode = DragMode.NONE
            var accumulatedX = 0f
            var startPosition = 0L
            var pendingSeek: Long? = null

            detectDragGestures(
                onDragStart = { offset ->
                    mode = DragMode.NONE
                    accumulatedX = 0f
                    startPosition = currentPositionMs()
                    pendingSeek = null
                },
                onDragEnd = {
                    pendingSeek?.let(onSeekCommit)
                    mode = DragMode.NONE
                    pendingSeek = null
                    onFeedback(null)
                },
                onDragCancel = {
                    mode = DragMode.NONE
                    pendingSeek = null
                    onFeedback(null)
                },
                onDrag = { change, dragAmount ->
                    if (mode == DragMode.NONE) {
                        mode = if (abs(dragAmount.x) > abs(dragAmount.y)) {
                            DragMode.SEEK
                        } else if (change.position.x < size.width / 2f) {
                            DragMode.BRIGHTNESS
                        } else {
                            DragMode.VOLUME
                        }
                    }

                    when (mode) {
                        DragMode.SEEK -> {
                            if (durationMs > 0 && size.width > 0) {
                                accumulatedX += dragAmount.x
                                val deltaMs = (accumulatedX / size.width * SEEK_RANGE_MS).toLong()
                                val target = (startPosition + deltaMs).coerceIn(0L, durationMs)
                                pendingSeek = target
                                onFeedback(GestureFeedback.Seek(target, target - startPosition))
                            }
                        }
                        DragMode.BRIGHTNESS -> {
                            if (size.height > 0) {
                                onBrightnessDelta(-dragAmount.y / size.height)
                            }
                        }
                        DragMode.VOLUME -> {
                            if (size.height > 0) {
                                onVolumeDelta(-dragAmount.y / size.height)
                            }
                        }
                        DragMode.NONE -> Unit
                    }
                    change.consume()
                },
            )
        }
}
