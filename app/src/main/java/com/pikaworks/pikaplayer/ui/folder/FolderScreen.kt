package com.pikaworks.pikaplayer.ui.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.data.media.VideoItem
import com.pikaworks.pikaplayer.ui.AppIcons
import com.pikaworks.pikaplayer.ui.VideoListRow
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
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 60.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("폴더", fontSize = 26.sp, fontWeight = FontWeight.Light, color = colors.textPrimary)
        }

        Breadcrumb(rootLabel = state.rootLabel, crumbs = state.crumbs, onNavigateTo = onNavigateTo)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("이름순", fontSize = 12.sp, color = colors.textPrimary)
            Text(
                summaryLabel(state),
                fontSize = 11.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
            )
        }

        // SAF 로 연 폴더에는 하위 폴더와 영상이 같은 자리에 함께 있다.
        // MediaStore 는 둘 중 한쪽만 채워지므로 같은 코드로 처리된다.
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.folders, key = { "d:" + it.id }) { folder ->
                FolderRow(folder = folder, onClick = { onOpenFolder(folder) })
            }
            items(state.videos, key = { "f:" + it.uri }) { video ->
                VideoListRow(video = video, onClick = { onVideoClick(video) })
            }
        }
    }
}

/** SAF 는 폴더별 영상 수를 세지 않는다. 합계를 보여주면 0 으로 읽힌다. */
private fun summaryLabel(state: FolderUiState): String = when {
    state.crumbs.isNotEmpty() -> "영상 ${state.videos.size}"
    state.showsFolderCounts -> "폴더 ${state.folders.size} · 영상 ${state.totalVideoCount}"
    else -> "폴더 ${state.folders.size}"
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
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        val atRoot = crumbs.isEmpty()
        Text(
            rootLabel,
            fontSize = 12.sp,
            maxLines = 1,
            color = if (atRoot) colors.textSecondary else colors.key,
            modifier = if (atRoot) Modifier else Modifier.clickable { onNavigateTo(0) },
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
                modifier = if (isLast) Modifier else Modifier.clickable { onNavigateTo(index + 1) },
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

