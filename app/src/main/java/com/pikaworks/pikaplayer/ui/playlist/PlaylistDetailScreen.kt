package com.pikaworks.pikaplayer.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.ui.AppIcons
import com.pikaworks.pikaplayer.ui.IconTap
import com.pikaworks.pikaplayer.ui.ScreenHeader
import com.pikaworks.pikaplayer.ui.formatDuration
import com.pikaworks.pikaplayer.ui.theme.KoreanWrap
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/**
 * 재생목록 하나의 내용.
 *
 * 순서를 끌어서 바꾸는 대신 위·아래 버튼을 둔다. 목록에서 끌어 옮기는 것은
 * 스크롤과 부딪혀 다루기 까다롭고, 만들 것도 훨씬 많다.
 */
@Composable
fun PlaylistDetailScreen(
    name: String,
    rows: List<PlaylistRow>,
    onPlay: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (String) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        ScreenHeader(name, onBack)

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ActionText("이름 바꾸기", colors.key, onRename)
            ActionText("목록 삭제", colors.textMeta, onDelete)
        }

        if (rows.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "아직 담긴 영상이 없습니다.\n보관함이나 폴더에서 영상을 길게 눌러 담으세요.",
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
                        .clickable(enabled = row.available) { onPlay(index) }
                        .heightIn(min = 56.dp)
                        .padding(start = 20.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "${index + 1}",
                        fontSize = 12.sp, fontWeight = FontWeight.Light, color = colors.textFaint,
                        modifier = Modifier.size(width = 20.dp, height = 20.dp),
                    )
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
                    IconTap(
                        icon = AppIcons.ChevronUp,
                        contentDescription = "위로",
                        onClick = { onMove(index, -1) },
                        tint = if (index == 0) colors.textFaint else colors.textSecondary,
                        iconSize = 16.dp,
                        tapSize = 36.dp,
                    )
                    IconTap(
                        icon = AppIcons.ChevronDown,
                        contentDescription = "아래로",
                        onClick = { onMove(index, 1) },
                        tint = if (index == rows.lastIndex) colors.textFaint else colors.textSecondary,
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

@Composable
private fun ActionText(label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 12.sp,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}
