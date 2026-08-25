package com.pikaworks.pikaplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pikaworks.pikaplayer.data.media.VideoItem
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/**
 * 목록 한 줄. 보관함 · 폴더 · 최근이 모두 같은 모양을 쓴다.
 *
 * 썸네일 위 진행 바는 높이를 모서리 반경과 같게(4dp) 유지해야 한다.
 * 바가 반경보다 얇으면 클리핑 곡선이 바를 대각선으로 잘라 끝이 쐐기가 된다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoListRow(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** 길게 눌렀을 때. 없으면 길게 눌러도 아무 일이 없다. */
    onLongClick: (() -> Unit)? = null,
    /** 0f..1f. 재생 이력이 없으면 null */
    progress: Float? = null,
    subtitleFormat: String? = null,
) {
    val colors = PikaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            // 짧게·길게를 한 곳에서 다뤄야 한다. clickable 과 따로 걸면 둘이
            // 서로의 몸짓을 가로챈다.
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(92.dp).height(52.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.divider),
        ) {
            AsyncImage(
                model = video.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                formatDuration(video.durationMs),
                fontSize = 9.sp,
                color = colors.onMediaText,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
            progress?.let { p ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth().height(4.dp)
                        .background(colors.onMediaTrack),
                ) {
                    Box(Modifier.fillMaxWidth(p).fillMaxHeight().background(colors.onMediaKey))
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                video.displayName,
                fontSize = 13.sp, color = colors.textPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                listOfNotNull(video.resolutionLabel, formatSize(video.sizeBytes).takeIf { video.sizeBytes > 0 })
                    .joinToString(" · "),
                fontSize = 10.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
            )
            if (progress != null || subtitleFormat != null) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    progress?.let {
                        Chip("${(it * 100).toInt()}%", colors.key, colors.progressChipBorder)
                    }
                    subtitleFormat?.let { Chip(it, colors.textMeta, colors.chipBorder) }
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, textColor: Color, borderColor: Color) {
    Text(
        text,
        fontSize = 9.sp,
        color = textColor,
        modifier = Modifier
            .border(1.dp, borderColor, RoundedCornerShape(3.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}
