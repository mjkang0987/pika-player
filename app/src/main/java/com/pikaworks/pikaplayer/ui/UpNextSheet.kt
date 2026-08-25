package com.pikaworks.pikaplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.pikaworks.pikaplayer.ui.theme.PikaDarkColors

/**
 * 지금 틀고 있는 목록. 이 영상 뒤에 오는 것들을 차례대로 보여준다.
 *
 * 플레이어는 라이트 테마에서도 검은 배경이라 목록 행을 그대로 쓸 수 없다.
 * 여기서는 영상 위에 얹는 색(onMedia*)만 쓴다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpNextSheet(
    videos: List<VideoItem>,
    onClick: (VideoItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = PikaDarkColors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Text(
            "재생목록",
            fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
        )
        LazyColumn(modifier = Modifier.padding(bottom = 28.dp)) {
            items(videos, key = { it.uri.toString() }) { video ->
                UpNextRow(video = video, onClick = { onClick(video) })
            }
        }
    }
}

@Composable
private fun UpNextRow(video: VideoItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier
                .width(ThumbWidth).height(ThumbHeight)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.10f)),
        ) {
            AsyncImage(
                model = video.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            video.displayName,
            fontSize = 12.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            formatDuration(video.durationMs),
            fontSize = 10.sp,
            fontWeight = FontWeight.Light,
            color = Color.White.copy(alpha = 0.55f),
        )
    }
}
