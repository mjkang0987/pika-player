package com.pikaworks.pikaplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

enum class Tab(val label: String, val icon: ImageVector) {
    LIBRARY("보관함", AppIcons.NavLibrary),
    FOLDER("폴더", AppIcons.NavFolder),
    PLAYLIST("재생목록", AppIcons.NavPlaylist),
    RECENT("최근", AppIcons.NavRecent),
    SETTINGS("설정", AppIcons.NavSettings),
}

/**
 * 하단 네비게이션.
 *
 * 앱이 edge-to-edge 라 시스템 내비게이션 바 아래까지 그려진다. 아래 여백을
 * 고정값으로 두면 3버튼 내비게이션 기기에서 버튼에 가린다 — 시스템이 알려주는
 * 값을 받아 쓰고, 우리 여백은 그 위에 얹는다.
 */
@Composable
fun BottomNav(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val colors = PikaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .windowInsetsPadding(WindowInsets.navigationBars)
            // 탭이 다섯이면 고정 폭으로는 좁은 화면을 넘친다. 남는 폭을 고르게
            // 나눠 갖게 해서 개수와 화면 크기에 관계없이 들어맞게 한다.
            .padding(horizontal = 12.dp),
    ) {
        Tab.entries.forEach { tab ->
            val active = tab == current
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clickable { onSelect(tab) }
                    // 여백은 버튼 안쪽에 둔다. 바깥에 두면 눌렀을 때 칠해지는
                    // 배경이 여백을 뺀 안쪽만 덮어 잘려 보인다.
                    .padding(top = 10.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (active) colors.key else colors.textFaint,
                    modifier = Modifier.size(21.dp),
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    tab.label,
                    fontSize = 10.sp,
                    fontWeight = if (active) FontWeight.Normal else FontWeight.Light,
                    color = if (active) colors.key else colors.textFaint,
                )
            }
        }
    }
}
