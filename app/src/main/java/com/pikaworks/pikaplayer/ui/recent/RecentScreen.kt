package com.pikaworks.pikaplayer.ui.recent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.data.media.LibraryRow
import com.pikaworks.pikaplayer.ui.VideoListRow
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/** 최근 탭. 한 번이라도 재생한 영상을 최근 순으로. */
@Composable
fun RecentScreen(
    rows: List<LibraryRow>,
    onVideoClick: (LibraryRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors

    if (rows.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().background(colors.background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "아직 재생한 영상이 없습니다",
                fontSize = 14.sp, fontWeight = FontWeight.Light,
                color = colors.textMeta, textAlign = TextAlign.Center,
            )
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize().background(colors.background)) {
        item {
            Text(
                "최근",
                fontSize = 26.sp, fontWeight = FontWeight.Light, color = colors.textPrimary,
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 60.dp, bottom = 14.dp),
            )
        }
        items(rows, key = { it.video.id }) { row ->
            VideoListRow(
                video = row.video,
                onClick = { onVideoClick(row) },
                progress = row.progress,
                subtitleFormat = row.subtitleFormat,
            )
        }
    }
}
