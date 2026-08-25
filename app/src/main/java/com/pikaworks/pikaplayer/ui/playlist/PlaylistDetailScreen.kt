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
import com.pikaworks.pikaplayer.ui.ThumbHeight
import com.pikaworks.pikaplayer.ui.ThumbWidth

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
    onPlay: (Int) -> Unit,
    /** 편집에서 '완료'를 눌렀을 때의 최종 순서. 여기 없는 것은 빠진 것이다. */
    onApplyEdit: (List<String>) -> Unit,
    onAdd: () -> Unit,
    onRename: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors

    /**
     * 편집하는 동안 화면이 임시로 들고 있는 순서. null 이면 편집 중이 아니다.
     *
     * 한 칸 옮길 때마다 저장하지 않는다. 그러면 '취소'가 되돌릴 것이 남지 않아
     * 이름만 취소인 버튼이 된다. 완료를 눌러야 한 번에 적는다.
     */
    var draft by remember { mutableStateOf<List<PlaylistRow>?>(null) }
    val editing = draft != null
    val shown = draft ?: rows

    fun startEdit() { draft = rows }

    /** 이웃과 자리를 맞바꾼다. 저장은 완료를 누를 때. */
    fun swap(from: Int, to: Int) {
        val current = draft ?: return
        if (from !in current.indices || to !in current.indices) return
        draft = current.toMutableList().also { it.add(to, it.removeAt(from)) }
    }

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        ScreenHeader(name, onBack)

        // 담는 쪽은 왼쪽, 손보는 쪽은 오른쪽. 이 목록에 무엇을 더할지가 이 화면에
        // 와서 가장 자주 하는 일이라 읽기 시작하는 자리에 둔다.
        //
        // 편집에 들어가면 나머지가 사라지고 취소·완료만 남는다.
        //
        // 랜덤과 목록 반복도 여기 있었는데 재생 화면으로 옮겼다. 틀기 전에
        // 미리 정해 두는 값이라 정작 보는 중에는 바꿀 수 없었고, 지금 그렇게
        // 돌고 있다는 표시도 재생 화면에는 없었다.
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val current = draft
            if (current == null) {
                Action("영상 추가", colors.key, onAdd)
                Spacer(Modifier.weight(1f))
                // 목록 자체를 지우는 것은 밖(재생목록 화면)에서 한다. 안에 두면
                // 이 목록을 손보러 들어온 자리에서 목록이 통째로 사라진다.
                Action("이름 변경", colors.textSecondary, onRename)
                Action("편집", colors.textSecondary) { startEdit() }
            } else {
                // 편집 중에는 나머지를 치운다. 특히 '영상 추가'가 남으면 임시로
                // 들고 있던 순서에 없는 줄이 생기고, 완료를 누르는 순간 방금
                // 담은 것이 도로 빠진다.
                Spacer(Modifier.weight(1f))
                Action("취소", colors.textMeta) { draft = null }
                PrimaryAction("완료") {
                    onApplyEdit(current.map { it.item.uri })
                    draft = null
                }
            }
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
                itemsIndexed(shown, key = { _, row -> row.item.uri }) { index, row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { if (!editing && row.available) onPlay(index) },
                                onLongClick = { startEdit() },
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
                                tint = if (index == shown.lastIndex) colors.divider else colors.textSecondary,
                                iconSize = 16.dp,
                                tapSize = 36.dp,
                            )
                            IconTap(
                                icon = AppIcons.Close,
                                contentDescription = "빼기",
                                // 아직 저장하지 않는다. 잘못 눌러도 취소로 되돌린다.
                                onClick = { draft = shown.filterNot { it.item.uri == row.item.uri } },
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
            .width(ThumbWidth).height(ThumbHeight)
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
 * 이 줄에서 눌러야 할 것 하나.
 *
 * 편집을 끝내는 길은 완료와 취소 둘인데, 둘 다 테두리만 남기면 어느 쪽이
 * 하려던 일인지 갈리지 않는다. 채워서 눈이 먼저 닿게 한다.
 */
@Composable
private fun PrimaryAction(label: String, onClick: () -> Unit) {
    val colors = PikaTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Text(
        label,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = colors.background,
        modifier = Modifier
            .clip(shape)
            .background(colors.key)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
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
