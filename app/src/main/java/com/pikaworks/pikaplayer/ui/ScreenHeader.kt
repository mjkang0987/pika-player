package com.pikaworks.pikaplayer.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/**
 * 뒤로 + 제목으로 된 화면 머리말. 설정·라이선스·비공개 폴더·Pro·PIN 이 같이 쓴다.
 *
 * 다섯 화면에 같은 Row 가 복사돼 있었다. 누름 영역을 고치려면 다섯 군데를
 * 똑같이 손봐야 했고, 그래서 한곳으로 모은다.
 *
 * 위 여백은 상태바 높이를 받아서 쓴다. 전에는 60dp 를 박아 뒀는데, 그 값은
 * 상태바가 24dp 인 기기를 가정한 것이었다. 상태바가 24dp 면 그 아래로 36dp 가
 * 더 붙어 허전했고, 노치가 있어 48dp 인 기기에서는 반대로 좁았다.
 *
 * 왼쪽 여백은 [IconTap] 이 아이콘보다 커진 만큼(10dp) 덜어낸 값이다.
 * 아이콘과 제목이 놓이는 가로 자리는 전과 같다.
 */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PikaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 10.dp, end = 20.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        IconTap(AppIcons.Back, "뒤로", onClick = onBack, tint = colors.textPrimary)
        Text(title, fontSize = 20.sp, color = colors.textPrimary)
    }
}
