package com.pikaworks.pikaplayer.ui.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.data.media.VideoItem
import com.pikaworks.pikaplayer.ui.AppIcons
import com.pikaworks.pikaplayer.ui.OptionSheet
import com.pikaworks.pikaplayer.ui.SORT_OPTIONS
import com.pikaworks.pikaplayer.ui.SearchHeader
import com.pikaworks.pikaplayer.ui.VideoListRow
import com.pikaworks.pikaplayer.ui.matchesQuery
import com.pikaworks.pikaplayer.ui.sortLabel
import com.pikaworks.pikaplayer.ui.formatSize
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/** 폴더 탐색(S2). 폴더 목록 → 폴더 안 영상, 한 단계씩. */
@Composable
fun FolderScreen(
    state: FolderUiState,
    onOpenFolder: (FolderSummary) -> Unit,
    /** 0 이면 최상단, n 이면 앞의 n 단계까지 남긴다. */
    onNavigateTo: (Int) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onSortChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors
    var sortSheetVisible by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
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

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {

        SearchHeader(
            title = "폴더",
            query = query,
            onQueryChange = { query = it },
            searching = searching,
            onSearchingChange = { searching = it },
            placeholder = "폴더 · 영상 이름",
        )

        Breadcrumb(rootLabel = state.rootLabel, crumbs = state.crumbs, onNavigateTo = onNavigateTo)

        // 검색은 지금 보고 있는 단계 안에서만 찾는다. 하위 폴더까지 뒤지면
        // 한 단계씩 이동한다는 이 화면의 규칙과 어긋난다.
        val visibleFolders = state.folders.filter { matchesQuery(query, it.name) }
        val visibleVideos = state.videos.filter { matchesQuery(query, it.displayName) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(start = 12.dp, end = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                // 여백을 버튼 안으로. 바깥에 두면 눌린 배경이 글자에만 붙는다.
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { sortSheetVisible = true }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(sortLabel(state.sort), fontSize = 12.sp, color = colors.textPrimary)
                Text("▾", fontSize = 11.sp, color = colors.textFaint)
            }
            Text(
                summaryLabel(visibleFolders, visibleVideos.size),
                fontSize = 11.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        }

        // SAF 로 연 폴더에는 하위 폴더와 영상이 같은 자리에 함께 있다.
        // MediaStore 는 둘 중 한쪽만 채워지므로 같은 코드로 처리된다.
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(visibleFolders, key = { "d:" + it.id }) { folder ->
                FolderRow(folder = folder, onClick = { onOpenFolder(folder) })
            }
            items(visibleVideos, key = { "f:" + it.uri }) { video ->
                VideoListRow(video = video, onClick = { onVideoClick(video) })
            }
        }
    }
}

/** SAF 는 폴더별 영상 수를 뒤에서 채운다. 다 차기 전에는 합계를 내지 않는다. */
private fun summaryLabel(folders: List<FolderSummary>, videoCount: Int): String {
    if (folders.isEmpty()) return "영상 $videoCount"
    if (folders.any { it.videoCount == null }) return "폴더 ${folders.size}"
    val total = folders.sumOf { it.videoCount ?: 0 } + videoCount
    return "폴더 ${folders.size} · 영상 $total"
}

/**
 * 경로 표시. SAF 로 연 폴더는 여러 단계로 내려갈 수 있어서 한 칸씩 눌러 돌아간다.
 * 깊어지면 줄이 넘치므로 가로로 스크롤시킨다.
 */
@Composable
private fun Breadcrumb(rootLabel: String, crumbs: List<Crumb>, onNavigateTo: (Int) -> Unit) {
    val colors = PikaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            // 조각마다 가로 4dp·세로 7dp 를 안쪽에 갖는다. 그만큼 여기서 덜어내
            // 글자가 놓이는 자리는 전과 같다.
            .padding(start = 16.dp, end = 16.dp, top = 3.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        val atRoot = crumbs.isEmpty()
        Text(
            rootLabel,
            fontSize = 12.sp,
            maxLines = 1,
            color = if (atRoot) colors.textSecondary else colors.key,
            // 누를 수 없는 조각도 같은 여백을 가져야 줄이 흔들리지 않는다.
            modifier = (if (atRoot) Modifier else Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onNavigateTo(0) })
                .padding(horizontal = 4.dp, vertical = 7.dp),
        )
        crumbs.forEachIndexed { index, crumb ->
            val isLast = index == crumbs.lastIndex
            Icon(AppIcons.ChevronRight, null, tint = colors.textFaint, modifier = Modifier.size(12.dp))
            Text(
                crumb.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isLast) colors.textSecondary else colors.key,
                modifier = (if (isLast) Modifier else Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onNavigateTo(index + 1) })
                    .padding(horizontal = 4.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun FolderRow(folder: FolderSummary, onClick: () -> Unit) {
    val colors = PikaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 58.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(AppIcons.NavFolder, null, tint = colors.textSecondary, modifier = Modifier.size(26.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(folder.name, fontSize = 14.sp, color = colors.textPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(
                if (folder.videoCount != null && folder.totalBytes != null) {
                    "영상 ${folder.videoCount} · ${formatSize(folder.totalBytes)}"
                } else "폴더",
                fontSize = 10.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
            )
        }
        Icon(AppIcons.ChevronRight, null, tint = colors.textFaint, modifier = Modifier.size(15.dp))
    }
}

