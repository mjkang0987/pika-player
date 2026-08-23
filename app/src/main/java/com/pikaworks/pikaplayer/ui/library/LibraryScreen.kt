package com.pikaworks.pikaplayer.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pikaworks.pikaplayer.data.media.LibraryRow
import com.pikaworks.pikaplayer.data.media.StorageUsage
import com.pikaworks.pikaplayer.ui.AppIcons
import com.pikaworks.pikaplayer.ui.OptionSheet
import com.pikaworks.pikaplayer.ui.SORT_OPTIONS
import com.pikaworks.pikaplayer.ui.VideoListRow
import com.pikaworks.pikaplayer.ui.sortLabel
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
    onSortChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors
    var sortSheetVisible by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }

    if (sortSheetVisible) {
        OptionSheet(
            title = "정렬",
            options = SORT_OPTIONS,
            selected = state.sort,
            onSelect = onSortChange,
            onDismiss = { sortSheetVisible = false },
        )
    }

    LazyColumn(modifier = modifier.fillMaxSize().background(colors.background)) {

        item {
            Header(
                searching = searching,
                query = state.query,
                onQueryChange = onQueryChange,
                onToggleSearch = {
                    searching = !searching
                    if (!searching) onQueryChange("")
                },
            )
        }

        if (state.continueWatching.isNotEmpty()) {
            item { SectionTitle("이어보기", state.continueWatching.size) }
            item { ContinueRow(state.continueWatching) }
        }

        item { LibraryTabs(state = state, onSelect = onFilterChange) }
        item { SortBar(sort = state.sort, storage = state.storage, onSortClick = { sortSheetVisible = true }) }

        items(state.visibleRows, key = { it.video.id }) { row ->
            VideoListRow(
                video = row.video,
                onClick = { onVideoClick(row) },
                progress = row.progress,
                subtitleFormat = row.subtitleFormat,
            )
        }
    }
}

/**
 * 제목과 검색(S7).
 *
 * 검색을 켜면 제목 자리를 입력칸이 대신한다. 검색은 목록을 좁히는 동작이라
 * 별도 화면으로 넘기지 않는다 — 결과를 보면서 바로 지우고 다시 칠 수 있어야 한다.
 */
@Composable
private fun Header(
    searching: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
) {
    val colors = PikaTheme.colors
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 60.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (searching) {
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("영상 · 폴더 이름", fontSize = 18.sp,
                        fontWeight = FontWeight.Light, color = colors.textFaint)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        color = colors.textPrimary,
                    ),
                    cursorBrush = SolidColor(colors.key),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
        } else {
            Text("보관함", fontSize = 26.sp, fontWeight = FontWeight.Light, color = colors.textPrimary)
        }
        Icon(
            if (searching) AppIcons.Close else AppIcons.Search,
            if (searching) "검색 닫기" else "검색",
            tint = colors.textSecondary,
            modifier = Modifier.padding(start = 12.dp).size(22.dp).clickable(onClick = onToggleSearch),
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

/** 목록 거르기. 이동이 아니라 지금 목록을 좁히는 띠다 — [LibraryFilter] 참고. */
@Composable
private fun LibraryTabs(state: LibraryUiState, onSelect: (String) -> Unit) {
    val colors = PikaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        LibraryFilter.ORDER.forEach { filter ->
            val selected = filter == state.filter
            // 밑줄은 글자 너비만큼만 긋는다. 가로 스크롤 안에서는 폭 제약이 없어
            // fillMaxWidth 가 먹지 않으므로 글자 뒤에 직접 그린다.
            val underline = colors.key
            Text(
                "${LibraryFilter.label(filter)} ${state.countOf(filter)}",
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Light,
                color = if (selected) colors.key else colors.textSecondary,
                modifier = Modifier
                    .clickable { onSelect(filter) }
                    .drawBehind {
                        if (!selected) return@drawBehind
                        val thickness = 2.dp.toPx()
                        drawRect(
                            color = underline,
                            topLeft = Offset(0f, size.height - thickness),
                            size = Size(size.width, thickness),
                        )
                    }
                    // 고르지 않았을 때도 밑줄 자리를 비워 둔다. 줄 높이가 흔들리지 않는다.
                    .padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun SortBar(sort: String, storage: StorageUsage?, onSortClick: () -> Unit) {
    val colors = PikaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onSortClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(sortLabel(sort), fontSize = 12.sp, color = colors.textPrimary)
            Text("▾", fontSize = 11.sp, color = colors.textFaint)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 아직 못 읽었으면 막대만 비워 둔다. 자리가 흔들리지 않는다.
            Box(
                modifier = Modifier
                    .width(38.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.divider),
            ) {
                if (storage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(storage.usedRatio)
                            .height(4.dp)
                            .background(colors.key),
                    )
                }
            }
            Text(
                if (storage == null) "내부 저장소"
                else "내부 저장소 ${formatSize(storage.freeBytes)} 남음",
                fontSize = 11.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
            )
        }
    }
}

