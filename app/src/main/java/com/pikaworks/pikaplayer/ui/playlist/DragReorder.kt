package com.pikaworks.pikaplayer.ui.playlist

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 목록에서 끌어 옮기기.
 *
 * 라이브러리를 들이지 않고 최소한으로 만든다. 필요한 것은 세 가지뿐이다 —
 * 지금 끌고 있는 줄이 무엇인지, 손가락이 얼마나 움직였는지, 그 위치가 어느
 * 줄 위인지.
 *
 * 자리를 **끌면서 즉시** 바꾼다. 손을 뗄 때 한 번에 옮기면 끄는 동안 무엇이
 * 어디로 갈지 보이지 않는다. 대신 한 칸 넘어갈 때마다 바꾸므로, 바꾸는 쪽
 * ([onSwap]) 은 여러 번 불려도 괜찮아야 한다.
 */
class DragReorderState internal constructor(
    private val listState: LazyListState,
    private val onSwap: (from: Int, to: Int) -> Unit,
    private val onCommit: () -> Unit,
) {
    /** 끌고 있는 줄의 목록 위치. 없으면 null. */
    var draggingIndex by mutableStateOf<Int?>(null)
        private set

    /** 끌기 시작한 자리에서 얼마나 벗어났는지(px). 그림을 띄우는 데 쓴다. */
    var offsetY by mutableFloatStateOf(0f)
        private set

    internal fun start(index: Int) {
        draggingIndex = index
        offsetY = 0f
    }

    internal fun stop() {
        // 끄는 동안에는 화면 안에서만 자리를 바꾸고, 손을 뗄 때 한 번 저장한다.
        // 한 칸 넘어갈 때마다 저장하면 목록이 다시 흘러들어오면서 끌던 줄이
        // 손가락 밑에서 튄다.
        if (draggingIndex != null) onCommit()
        draggingIndex = null
        offsetY = 0f
    }

    internal fun drag(deltaY: Float) {
        val from = draggingIndex ?: return
        offsetY += deltaY

        // 지금 끌고 있는 줄이 화면 어디에 있는지 알아야 이웃과 견줄 수 있다.
        val items = listState.layoutInfo.visibleItemsInfo
        val current = items.firstOrNull { it.index == from } ?: return
        val center = current.offset + current.size / 2f + offsetY

        // 가운데가 이웃의 가운데를 넘어서면 그때 자리를 바꾼다. 겹치기 시작할
        // 때 바로 바꾸면 두 줄 사이에서 덜덜 떨린다.
        val target = items.firstOrNull { other ->
            other.index != from && center in other.offset.toFloat()..(other.offset + other.size).toFloat()
        } ?: return

        onSwap(from, target.index)
        draggingIndex = target.index
        // 자리를 바꿨으므로 벗어난 거리도 그만큼 줄여 준다. 안 그러면 그림이
        // 손가락에서 한 칸씩 멀어진다.
        offsetY -= (target.offset - current.offset)
    }
}

@Composable
fun rememberDragReorder(
    listState: LazyListState,
    onSwap: (from: Int, to: Int) -> Unit,
    onCommit: () -> Unit,
): DragReorderState = remember(listState) { DragReorderState(listState, onSwap, onCommit) }

/**
 * 이 줄을 끌 수 있게 한다.
 *
 * 길게 눌러야 시작한다. 바로 반응하면 목록을 위아래로 넘기려던 손짓까지
 * 끌기로 잡아채 스크롤이 안 된다.
 */
fun Modifier.dragReorderHandle(state: DragReorderState, index: Int): Modifier =
    this.pointerInput(index) {
        detectDragGesturesAfterLongPress(
            onDragStart = { state.start(index) },
            onDragEnd = { state.stop() },
            onDragCancel = { state.stop() },
            onDrag = { change, amount ->
                change.consume()
                state.drag(amount.y)
            },
        )
    }
