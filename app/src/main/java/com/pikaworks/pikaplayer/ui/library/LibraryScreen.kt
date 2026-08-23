package com.pikaworks.pikaplayer.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pikaworks.pikaplayer.data.media.LibraryRow
import com.pikaworks.pikaplayer.ui.formatDuration
import com.pikaworks.pikaplayer.ui.formatRemaining
import com.pikaworks.pikaplayer.ui.formatSize
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/**
 * 라이브러리(S1).
 *
 * 구성: 헤더 → 이어보기 가로 캐러셀(최대 10) → 상단 탭 → 정렬·용량 → 목록
 */
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onVideoClick: (LibraryRow) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors

    LazyColumn(modifier = modifier.fillMaxSize().background(colors.background)) {

        item { Header(onSettingsClick = onSettingsClick) }

        if (state.continueWatching.isNotEmpty()) {
            item { SectionTitle("이어보기", state.continueWatching.size) }
            item { ContinueRow(state.continueWatching) }
        }

        item { LibraryTabs(videoCount = state.videoCount) }
        item { SortBar() }

        items(state.rows, key = { it.video.id }) { row ->
            VideoRow(row = row, onClick = { onVideoClick(row) })
        }
    }
}

@Composable
private fun Header(onSettingsClick: () -> Unit) {
    val colors = PikaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 60.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("보관함", fontSize = 26.sp, fontWeight = FontWeight.Light, color = colors.textPrimary)
        // TODO: 하단 네비게이션(보관함/폴더/최근/설정)이 들어오면 그쪽으로 옮긴다.
        Text(
            "설정",
            fontSize = 13.sp,
            color = colors.textSecondary,
            modifier = Modifier.clickable(onClick = onSettingsClick).padding(12.dp),
        )
    }
}

@Composable
private fun SectionTitle(title: String, count: Int) {
    val colors = PikaTheme.colors
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
        Text("$count", fontSize = 11.sp, fontWeight = FontWeight.Light, color = colors.textFaint)
    }
}

@Composable
private fun ContinueRow(items: List<ContinueItem>) {
    val colors = PikaTheme.colors
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items, key = { it.video.id }) { item ->
            Column(modifier = Modifier.width(152.dp)) {
                Box(
                    modifier = Modifier
                        .width(152.dp).height(86.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.divider),
                ) {
                    AsyncImage(
                        model = item.video.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // 진행 바: 높이를 모서리 반경과 같게 맞춰야 끝이 사선으로 깎이지 않는다.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth().height(4.dp)
                            .background(colors.onMediaTrack),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(item.progress).height(4.dp)
                                .background(colors.onMediaKey),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    item.video.displayName.substringBeforeLast('.'),
                    fontSize = 12.sp, color = colors.textPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    formatRemaining(item.video.durationMs, item.positionMs),
                    fontSize = 10.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
                )
            }
        }
    }
}

@Composable
private fun LibraryTabs(videoCount: Int) {
    val colors = PikaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        // TODO: 폴더 / 최근 / 자막 탭 연결
        Column {
            Text("영상 $videoCount", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.key)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.width(56.dp).height(2.dp).background(colors.key))
        }
        Text("폴더", fontSize = 13.sp, fontWeight = FontWeight.Light, color = colors.textSecondary)
        Text("최근", fontSize = 13.sp, fontWeight = FontWeight.Light, color = colors.textSecondary)
        Text("자막", fontSize = 13.sp, fontWeight = FontWeight.Light, color = colors.textSecondary)
    }
}

@Composable
private fun SortBar() {
    val colors = PikaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("최근 수정순", fontSize = 12.sp, color = colors.textPrimary)
        // TODO: 저장소 사용량은 StatFs 로 읽어 채운다
        Text("내부 저장소", fontSize = 11.sp, fontWeight = FontWeight.Light, color = colors.textMeta)
    }
}

@Composable
private fun VideoRow(row: LibraryRow, onClick: () -> Unit) {
    val colors = PikaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                model = row.video.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                formatDuration(row.video.durationMs),
                fontSize = 9.sp,
                color = colors.onMediaText,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
            row.progress?.let { p ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth().height(4.dp)
                        .background(colors.onMediaTrack),
                ) {
                    Box(Modifier.fillMaxWidth(p).height(4.dp).background(colors.onMediaKey))
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                row.video.displayName,
                fontSize = 13.sp, color = colors.textPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                listOfNotNull(row.video.resolutionLabel, formatSize(row.video.sizeBytes))
                    .joinToString(" · "),
                fontSize = 10.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
            )
            row.progress?.let { p ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "${(p * 100).toInt()}%",
                    fontSize = 9.sp, color = colors.key,
                    modifier = Modifier
                        .border(1.dp, colors.progressChipBorder, RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
    }
}
