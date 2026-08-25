package com.pikaworks.pikaplayer.ui.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.data.db.PlaylistSummary
import com.pikaworks.pikaplayer.ui.AppIcons
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/**
 * 영상을 길게 눌렀을 때 나오는 담기 시트.
 *
 * 목록이 하나도 없으면 만들기부터 안내한다. 빈 시트를 보여 주고 사용자가
 * "어디서 만드나" 를 찾아 나서게 두지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    videoName: String,
    playlists: List<PlaylistSummary>,
    /** 이 영상이 이미 들어 있는 목록. */
    containing: Set<Long>,
    onSelect: (Long) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = PikaTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            Text(
                "재생목록에 담기",
                fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 2.dp),
            )
            Text(
                videoName,
                fontSize = 12.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
            )

            playlists.forEach { playlist ->
                val already = playlist.id in containing
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 이미 들어 있는 목록은 누를 수 없다. 눌리게 두면 담긴
                        // 것처럼 알림이 뜨는데 실제로는 아무 일도 일어나지 않는다.
                        .clickable(enabled = !already) {
                            onSelect(playlist.id)
                            onDismiss()
                        }
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        playlist.name,
                        fontSize = 14.sp,
                        color = if (already) colors.textFaint else colors.textPrimary,
                    )
                    if (already) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(AppIcons.Check, null, tint = colors.key, modifier = Modifier.size(14.dp))
                            Text("담김", fontSize = 11.sp, fontWeight = FontWeight.Light, color = colors.textMeta)
                        }
                    } else {
                        Text(
                            "${playlist.itemCount}개",
                            fontSize = 11.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onCreate()
                        onDismiss()
                    }
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(AppIcons.Plus, null, tint = colors.key, modifier = Modifier.size(16.dp))
                Text("새 재생목록", fontSize = 14.sp, color = colors.key)
            }
        }
    }
}
