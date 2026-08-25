package com.pikaworks.pikaplayer.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    /** 이웃과 자리를 맞바꾼 뒤 전체 순서를 저장한다. */
    fun swap(from: Int, to: Int) {
        if (from !in rows.indices || to !in rows.indices) return
        val next = rows.toMutableList().also { it.add(to, it.removeAt(from)) }
        onReorder(next.map { it.item.uri })
    }

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        ScreenHeader(name, onBack)

        // 켜고 끄는 것(편집·랜덤·반복)과 한 번 하고 마는 것(추가·이름·삭제)을
        // 줄로 나눈다. 같은 줄에 섞여 있으면 무엇이 상태이고 무엇이 동작인지
        // 눌러 보기 전에는 알 수 없다.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Toggle("편집", editing) { editing = !editing }
            Toggle("랜덤", playMode.shuffle) {
                onPlayModeChange(playMode.copy(shuffle = !playMode.shuffle))
            }
            Toggle("목록 반복", playMode.loop) {
                onPlayModeChange(playMode.copy(loop = !playMode.loop))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Action("영상 추가", colors.key, onAdd)
            Spacer(Modifier.weight(1f))
            Action("이름", colors.textSecondary, onRename)
            Action("삭제", colors.textSecondary, onDelete)
        }

        if (rows.isEmpty()) {
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
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(rows, key = { _, row -> row.item.uri }) { index, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { if (!editing && row.available) onPlay(index) },
                                onLongClick = { editing = true },
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
                            // 끝에 닿은 쪽은 글자를 낮춰 더 갈 곳이 없음을 알린다.
                            IconTap(
                                icon = AppIcons.ChevronUp,
                                contentDescription = "위로",
                                onClick = { swap(index, index - 1) },
                                tint = if (index == 0) colors.divider else colors.textSecondary,
                                iconSize = 16.dp,
                                tapSize = 36.dp,
                            )
                            IconTap(
                                icon = AppIcons.ChevronDown,
                                contentDescription = "아래로",
                                onClick = { swap(index, index + 1) },
                                tint = if (index == rows.lastIndex) colors.divider else colors.textSecondary,
                                iconSize = 16.dp,
                                tapSize = 36.dp,
                            )
                            IconTap(
                                icon = AppIcons.Close,
                                contentDescription = "빼기",
                                onClick = { onRemove(row.item.uri) },
                                tint = colors.textFaint,
                                iconSize = 15.dp,
                                tapSize = 36.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}


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

/**
 * 켜고 끄는 버튼.
 *
 * 켜지면 채우고 꺼지면 테두리만 남긴다. 색만 바꾸면 "지금 켜져 있다" 와
 * "이걸 누르면 켜진다" 가 구분되지 않는다 — 실제로 그래서 눌러 보기 전에는
 * 무엇이 켜졌는지 알 수 없었다.
 */
@Composable
private fun Toggle(label: String, on: Boolean, onClick: () -> Unit) {
    val colors = PikaTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Text(
        label,
        fontSize = 12.sp,
        fontWeight = if (on) FontWeight.Medium else FontWeight.Light,
        color = if (on) colors.background else colors.textSecondary,
        modifier = Modifier
            .clip(shape)
            .background(if (on) colors.key else Color.Transparent)
            .border(1.dp, if (on) colors.key else colors.chipBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

/**
 * 한 번 하고 마는 버튼.
 *
 * 켜고 끄는 쪽과 달리 채워지지 않는다. 테두리 없이 글자만 두면 눌리는 것인지
 * 안내문인지 알 수 없어서 테두리는 남긴다.
 */
@Composable
private fun Action(label: String, color: Color, onClick: () -> Unit) {
    val colors = PikaTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Text(
        label,
        fontSize = 12.sp,
        color = color,
        modifier = Modifier
            .clip(shape)
            .border(1.dp, colors.chipBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}
