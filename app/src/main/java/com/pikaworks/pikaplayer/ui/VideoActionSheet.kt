package com.pikaworks.pikaplayer.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.data.media.VideoItem
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/** 목록에서 더보기(⋯)를 눌렀을 때 나오는 동작 모음. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoActionSheet(
    video: VideoItem,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
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
                video.displayName,
                fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
            )
            ActionRow(AppIcons.Plus, "재생목록에 담기", colors.textPrimary, colors.key) {
                onAddToPlaylist()
                onDismiss()
            }
            // 지우는 것만 색을 달리한다. 되돌릴 수 없는 동작은 손이 미끄러지기
            // 전에 눈에 먼저 걸려야 한다.
            ActionRow(AppIcons.Trash, "기기에서 삭제", colors.textPrimary, colors.textMeta) {
                onDelete()
                onDismiss()
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    labelColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 50.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(17.dp))
        Text(label, fontSize = 14.sp, color = labelColor)
    }
}
