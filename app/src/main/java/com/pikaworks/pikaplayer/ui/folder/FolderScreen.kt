package com.pikaworks.pikaplayer.ui.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    onOpenFolder: (String) -> Unit,
    onUp: () -> Unit,
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

        Breadcrumb(openedFolder = state.openedFolder, onUp = onUp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("이름순", fontSize = 12.sp, color = colors.textPrimary)
            Text(
                if (state.openedFolder == null) "폴더 ${state.folders.size} · 영상 ${state.totalVideoCount}"
                else "영상 ${state.videos.size}",
                fontSize = 11.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
            )
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (state.openedFolder == null) {
                items(state.folders, key = { it.name }) { folder ->
                    FolderRow(folder = folder, onClick = { onOpenFolder(folder.name) })
                }
            } else {
                items(state.videos, key = { it.id }) { video ->
                    VideoListRow(video = video, onClick = { onVideoClick(video) })
                }
            }
        }
    }
}

@Composable
private fun Breadcrumb(openedFolder: String?, onUp: () -> Unit) {
    val colors = PikaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            "내부 저장소",
            fontSize = 12.sp,
            color = if (openedFolder == null) colors.textSecondary else colors.key,
            modifier = if (openedFolder == null) Modifier else Modifier.clickable(onClick = onUp),
        )
        if (openedFolder != null) {
            Icon(AppIcons.ChevronRight, null, tint = colors.textFaint, modifier = Modifier.size(12.dp))
            Text(openedFolder, fontSize = 12.sp, fontWeight = FontWeight.Light,
                color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                "영상 ${folder.videoCount} · ${formatSize(folder.totalBytes)}",
                fontSize = 10.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
            )
        }
        Icon(AppIcons.ChevronRight, null, tint = colors.textFaint, modifier = Modifier.size(15.dp))
    }
}

