package com.pikaworks.pikaplayer.ui

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
 * 바깥 여백은 [IconTap] 이 아이콘보다 커진 만큼(왼쪽 10dp, 위아래 각 8dp)
 * 덜어낸 값이다. 아이콘과 제목이 놓이는 자리는 전과 같다.
 */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PikaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 20.dp, top = 52.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        IconTap(AppIcons.Back, "뒤로", onClick = onBack, tint = colors.textPrimary)
        Text(title, fontSize = 20.sp, color = colors.textPrimary)
    }
}
