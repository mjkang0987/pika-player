package com.pikaworks.pikaplayer.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.data.db.PlaylistSummary
import com.pikaworks.pikaplayer.ui.AppIcons
import com.pikaworks.pikaplayer.ui.IconTap
import com.pikaworks.pikaplayer.ui.theme.KoreanWrap
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/**
 * 재생목록 탭.
 *
 * 폴더와 다른 점: 폴더는 파일이 실제로 어디 있는지를 보여 주고, 이쪽은 사용자가
 * 직접 묶은 것이다. 그래서 여러 폴더에 흩어진 영상을 한 줄로 세울 수 있다.
 */
@Composable
fun PlaylistScreen(
    playlists: List<PlaylistSummary>,
    onOpen: (Long) -> Unit,
    onCreate: () -> Unit,
    /** 목록 삭제. 목록 안이 아니라 여기서 한다 — 손보러 들어간 자리에서 통째로 사라지면 안 된다. */
    onDelete: (PlaylistSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 20.dp, end = 9.dp, top = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("재생목록", fontSize = 26.sp, fontWeight = FontWeight.Light, color = colors.textPrimary)
            IconTap(
                icon = AppIcons.Plus,
                contentDescription = "재생목록 만들기",
                onClick = onCreate,
                tint = colors.key,
                iconSize = 22.dp,
            )
        }

        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "재생목록이 없습니다",
                        fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "여러 폴더에 흩어진 영상을 한 줄로 묶어 이어서 볼 수 있습니다. " +
                            "목록을 만든 뒤 영상을 길게 눌러 담으세요.",
                        fontSize = 12.sp, fontWeight = FontWeight.Light, lineHeight = 18.sp,
                        color = colors.textFaint, textAlign = TextAlign.Center, style = KoreanWrap,
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(playlists, key = { it.id }) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(playlist.id) }
                        .heightIn(min = 58.dp)
                        // 지우기 버튼이 제 여백을 갖고 있어 오른쪽은 덜어낸다.
                        .padding(start = 20.dp, end = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(AppIcons.NavPlaylist, null, tint = colors.key, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(playlist.name, fontSize = 14.sp, color = colors.textPrimary)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "${playlist.itemCount}개",
                            fontSize = 11.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
                        )
                    }
                    // 글자를 낮춰 둔다. 목록을 여는 것이 이 줄에서 할 일이고,
                    // 지우는 것은 가끔 하는 일이다.
                    IconTap(
                        icon = AppIcons.Trash,
                        contentDescription = "재생목록 삭제",
                        onClick = { onDelete(playlist) },
                        tint = colors.textFaint,
                        iconSize = 17.dp,
                        tapSize = 40.dp,
                    )
                    }
                }
            }
        }
    }
}
