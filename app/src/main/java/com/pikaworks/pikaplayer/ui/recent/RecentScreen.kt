package com.pikaworks.pikaplayer.ui.recent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.data.media.LibraryRow
import com.pikaworks.pikaplayer.ui.SearchHeader
import com.pikaworks.pikaplayer.ui.VideoListRow
import com.pikaworks.pikaplayer.ui.matchesQuery
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/** 최근 탭. 한 번이라도 재생한 영상을 최근 순으로. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentScreen(
    rows: List<LibraryRow>,
    onVideoClick: (LibraryRow) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }

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

    val visible = rows.filter { matchesQuery(query, it.video.displayName, it.video.folderName) }

    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize().background(colors.background),
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                SearchHeader(
                    title = "최근",
                    query = query,
                    onQueryChange = { query = it },
                    searching = searching,
                    onSearchingChange = { searching = it },
                    placeholder = "영상 · 폴더 이름",
                    modifier = Modifier.padding(bottom = 14.dp),
                )
            }
            items(visible, key = { it.video.id }) { row ->
                VideoListRow(
                    video = row.video,
                    onClick = { onVideoClick(row) },
                    progress = row.progress,
                    subtitleFormat = row.subtitleFormat,
                )
            }
        }
    }
}
