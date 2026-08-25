package com.pikaworks.pikaplayer.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.pikaworks.pikaplayer.ui.AppIcons
import com.pikaworks.pikaplayer.ui.IconTap
import com.pikaworks.pikaplayer.ui.ScreenHeader
import com.pikaworks.pikaplayer.ui.formatDuration
import com.pikaworks.pikaplayer.ui.theme.KoreanWrap
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/** 재생목록을 어떻게 틀지. 목록마다 따로 기억하지 않고 이번 재생에만 쓴다. */
data class PlayMode(
    val shuffle: Boolean = false,
    /** 마지막까지 가면 처음으로 돌아간다. */
    val loop: Boolean = false,
)

/**
 * 재생목록 하나의 내용.
 *
 * 평소에는 눌러서 재생만 한다. 순서를 바꾸거나 빼는 것은 **편집** 으로 들어가야
 * 한다 — 목록을 훑는 동안 지우기 버튼이 손에 걸리면 안 된다. 줄을 길게 누르면
 * 바로 편집으로 들어간다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(
    name: String,
    rows: List<PlaylistRow>,
    playMode: PlayMode,
    onPlayModeChange: (PlayMode) -> Unit,
    onPlay: (Int) -> Unit,
    onReorder: (List<String>) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors
    var editing by remember { mutableStateOf(false) }

    // 끄는 동안에는 이 목록만 바꾸고, 손을 뗄 때 저장한다.
    var order by remember(rows) { mutableStateOf(rows) }
    LaunchedEffect(rows) { order = rows }

    val listState = rememberLazyListState()
    val drag = rememberDragReorder(
        listState = listState,
        onSwap = { from, to ->
            order = order.toMutableList().also { it.add(to, it.removeAt(from)) }
        },
        onCommit = { onReorder(order.map { it.item.uri }) },
    )

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        ScreenHeader(name, onBack)

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Pill("영상 추가", colors.key, onClick = onAdd)
            Pill(
                if (editing) "완료" else "편집",
                if (editing) colors.key else colors.textSecondary,
                onClick = { editing = !editing },
            )
            Spacer(Modifier.weight(1f))
            Pill("이름", colors.textMeta, onClick = onRename)
            Pill("삭제", colors.textMeta, onClick = onDelete)
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Pill(
                "랜덤",
                if (playMode.shuffle) colors.key else colors.textFaint,
                onClick = { onPlayModeChange(playMode.copy(shuffle = !playMode.shuffle)) },
            )
            Pill(
                "목록 반복",
                if (playMode.loop) colors.key else colors.textFaint,
                onClick = { onPlayModeChange(playMode.copy(loop = !playMode.loop)) },
            )
        }

        if (order.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "아직 담긴 영상이 없습니다.\n위의 영상 추가를 누르거나, 보관함에서 영상을 길게 눌러 담으세요.",
                    fontSize = 12.sp, fontWeight = FontWeight.Light, lineHeight = 18.sp,
                    color = colors.textFaint, textAlign = TextAlign.Center, style = KoreanWrap,
                )
            }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(order, key = { _, row -> row.item.uri }) { index, row ->
                    val dragging = drag.draggingIndex == index
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // 끌고 있는 줄은 위로 올려야 이웃에 가리지 않는다.
                            .zIndex(if (dragging) 1f else 0f)
                            .offsetForDrag(dragging, drag.offsetY)
                            .background(if (dragging) colors.surface else Color.Transparent)
                            .then(
                                if (editing) Modifier.dragReorderHandle(drag, index)
                                else Modifier.combinedClickable(
                                    onClick = { if (row.available) onPlay(index) },
                                    onLongClick = { editing = true },
                                )
                            )
                            .heightIn(min = 62.dp)
                            .padding(start = 20.dp, end = if (editing) 8.dp else 20.dp, top = 7.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Thumbnail(row, colors.divider)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                row.item.displayName.substringBeforeLast('.'),
                                fontSize = 13.sp,
                                // 파일이 사라졌으면 글자를 낮춰 눌러도 소용없다는 것을 알린다.
                                color = if (row.available) colors.textPrimary else colors.textFaint,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                row.video?.let { formatDuration(it.durationMs) } ?: "찾을 수 없음",
                                fontSize = 11.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
                            )
                        }
                        if (editing) {
                            IconTap(
                                icon = AppIcons.Close,
                                contentDescription = "빼기",
                                onClick = { onRemove(row.item.uri) },
                                tint = colors.textFaint,
                                iconSize = 15.dp,
                                tapSize = 40.dp,
                            )
                            Icon(
                                AppIcons.DragHandle, "끌어서 옮기기",
                                tint = colors.textFaint,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 끌고 있는 줄만 손가락을 따라 움직인다.
 *
 * 자리(레이아웃)는 그대로 두고 그리는 위치만 옮긴다. 자리까지 옮기면 목록
 * 전체가 매 프레임 다시 배치되어 무겁고, 이웃의 위치를 견주는 계산도 흔들린다.
 */
private fun Modifier.offsetForDrag(dragging: Boolean, offsetY: Float): Modifier =
    if (!dragging) this else this.graphicsLayer { translationY = offsetY }

@Composable
private fun Thumbnail(row: PlaylistRow, placeholder: Color) {
    Box(
        modifier = Modifier
            .width(78.dp).height(44.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(placeholder),
    ) {
        // 파일이 없으면 Coil 이 아무것도 못 그린다. 자리만 남는 것이 맞다.
        row.video?.let {
            AsyncImage(
                model = it.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** 화면 위쪽 작은 버튼들. 켜짐은 색으로만 알린다. */
@Composable
private fun Pill(label: String, color: Color, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 12.sp,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    )
}
