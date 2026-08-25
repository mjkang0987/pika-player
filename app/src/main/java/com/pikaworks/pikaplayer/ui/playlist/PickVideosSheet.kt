package com.pikaworks.pikaplayer.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pikaworks.pikaplayer.data.media.VideoItem
import com.pikaworks.pikaplayer.ui.AppIcons
import com.pikaworks.pikaplayer.ui.formatDuration
import com.pikaworks.pikaplayer.ui.theme.KoreanWrap
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/**
 * 재생목록 안에서 영상을 고르는 시트.
 *
 * 시트를 닫지 않고 여러 개를 이어서 담을 수 있다. 하나 담을 때마다 닫히면
 * 열 개를 담는 데 열 번을 열어야 한다. 이미 담긴 것은 체크로 알린다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickVideosSheet(
    videos: List<VideoItem>,
    alreadyIn: Set<String>,
    onPick: (VideoItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = PikaTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).padding(bottom = 20.dp)) {
            Text(
                "영상 추가",
                fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
            )

            if (videos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "담을 영상이 없습니다. 기기에 영상이 있는지 확인해 보세요.",
                        fontSize = 12.sp, fontWeight = FontWeight.Light, color = colors.textFaint,
                        textAlign = TextAlign.Center, style = KoreanWrap,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(videos, key = { it.id }) { video ->
                        val added = video.uri.toString() in alreadyIn
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !added) { onPick(video) }
                                .heightIn(min = 58.dp)
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(70.dp).height(40.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.divider),
                            ) {
                                AsyncImage(
                                    model = video.uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    video.displayName.substringBeforeLast('.'),
                                    fontSize = 13.sp,
                                    color = if (added) colors.textFaint else colors.textPrimary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    formatDuration(video.durationMs),
                                    fontSize = 11.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
                                )
                            }
                            if (added) {
                                Icon(AppIcons.Check, "담김", tint = colors.key, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
