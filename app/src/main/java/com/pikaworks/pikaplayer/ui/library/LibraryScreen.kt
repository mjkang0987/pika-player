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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pikaworks.pikaplayer.data.media.LibraryRow
import com.pikaworks.pikaplayer.data.media.StorageUsage
import com.pikaworks.pikaplayer.ui.OptionSheet
import com.pikaworks.pikaplayer.ui.SORT_OPTIONS
import com.pikaworks.pikaplayer.ui.SearchHeader
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onVideoClick: (LibraryRow) -> Unit,
    /** 길게 누르면 재생목록에 담는다. */
    onVideoLongClick: (LibraryRow) -> Unit,
    /** 줄 오른쪽 더보기(⋯). */
    onVideoMenu: (LibraryRow) -> Unit,
    onContinueClick: (ContinueItem) -> Unit,
    onSortChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onRescan: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors
    var sortSheetVisible by remember { mutableStateOf(false) }
    // LazyColumn 항목이 폐기돼도 검색 상태가 살아 있도록 여기서 들고 있는다.
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

    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize().background(colors.background),
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {

            item {
                SearchHeader(
                    title = "보관함",
                    query = state.query,
                    onQueryChange = onQueryChange,
                    searching = searching,
                    onSearchingChange = { searching = it },
                    placeholder = "영상 · 폴더 이름",
                )
            }

            if (state.continueWatching.isNotEmpty()) {
                item { SectionTitle("이어보기", state.continueWatching.size) }
                item { ContinueRow(state.continueWatching, onContinueClick) }
            }

            item { LibraryTabs(state = state, onSelect = onFilterChange) }
            item { SortBar(sort = state.sort, storage = state.storage, onSortClick = { sortSheetVisible = true }) }

            if (state.visibleRows.isEmpty() && !state.loading) {
                // 왜 비었는지에 따라 할 말이 다르다. 검색으로 걸러진 거라면 다시
                // 훑어봐야 소용없고, 정말 아무것도 없을 때만 탈출구를 보여준다.
                item {
                    EmptyLibrary(
                        filtered = state.query.isNotBlank() || state.filter != LibraryFilter.ALL,
                        scanning = state.scanning,
                        onRescan = onRescan,
                    )
                }
            }

            items(state.visibleRows, key = { it.video.id }) { row ->
                VideoListRow(
                    video = row.video,
                    onClick = { onVideoClick(row) },
                    onLongClick = { onVideoLongClick(row) },
                    onMenu = { onVideoMenu(row) },
                    progress = row.progress,
                    subtitleFormat = row.subtitleFormat,
                )
            }
        }
    }
}

/**
 * 목록이 비었을 때.
 *
 * 전에는 아무것도 안 그렸다. 사용자는 앱이 고장 난 건지 영상이 없는 건지
 * 구분할 수 없었고, 앱 안에서 할 수 있는 일도 없었다.
 */
@Composable
private fun EmptyLibrary(
    filtered: Boolean,
    scanning: Boolean,
    onRescan: () -> Unit,
) {
    val colors = PikaTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (filtered) "조건에 맞는 영상이 없습니다" else "영상이 없습니다",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary,
        )
        if (!filtered) {
            Spacer(Modifier.height(8.dp))
            Text(
                "기기에 영상은 있는데 여기 안 보인다면 다시 검색해 보세요.",
                fontSize = 12.sp,
                color = colors.textFaint,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                if (scanning) "검색 중…" else "다시 검색",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (scanning) colors.textFaint else colors.key,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(enabled = !scanning, onClick = onRescan)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
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
private fun ContinueRow(items: List<ContinueItem>, onClick: (ContinueItem) -> Unit) {
    val colors = PikaTheme.colors
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items, key = { it.video.id }) { item ->
            Column(
                modifier = Modifier
                    .width(152.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onClick(item) },
            ) {
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
            // 탭 사이 간격은 Arrangement 가 아니라 탭이 각자 갖는다. 간격에는
            // 누름 영역이 없어서, 탭 사이 빈틈을 누르면 아무 일도 안 일어났다.
            .padding(horizontal = 9.dp),
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
                    // 가로 여백은 밑줄보다 바깥에 둔다. 밑줄은 글자 너비만큼만
                    // 그어야 하는데, 안쪽에 두면 여백까지 함께 그어진다.
                    .padding(horizontal = 11.dp)
                    .drawBehind {
                        if (!selected) return@drawBehind
                        val thickness = 2.dp.toPx()
                        drawRect(
                            color = underline,
                            topLeft = Offset(0f, size.height - thickness),
                            size = Size(size.width, thickness),
                        )
                    }
                    // 위 여백도 탭 안쪽에 둔다. 스트립이 갖고 있으면 눌린 배경이
                    // 글자에만 붙어 잘려 보인다. 아래 여백은 고르지 않았을 때도
                    // 밑줄 자리를 비워 두는 몫이다 — 줄 높이가 흔들리지 않는다.
                    .padding(top = 14.dp, bottom = 10.dp),
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
            .padding(start = 12.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onSortClick)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(sortLabel(sort), fontSize = 12.sp, color = colors.textPrimary)
            Text("▾", fontSize = 11.sp, color = colors.textFaint)
        }
        Row(
            // 누를 수 없는 쪽이라 여백을 옮길 이유는 없지만, 정렬 버튼이 여백을
            // 가져간 만큼 여기서도 같은 값을 줘야 줄 높이가 맞는다.
            modifier = Modifier.padding(vertical = 10.dp),
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

