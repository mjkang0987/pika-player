package com.pikaworks.pikaplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 아이콘 하나로 된 버튼.
 *
 * 아이콘 크기(20~24dp)를 그대로 누름 영역으로 쓰면 두 가지가 어긋난다.
 * 손가락으로 겨누기에 너무 작고, 눌렀을 때 칠해지는 배경이 아이콘에 딱 붙어
 * 잘려 보인다. 여백은 반드시 이 안에서 만든다 — 바깥 컨테이너가 만든 여백은
 * 누름 영역 밖이라 같은 문제가 그대로 남는다.
 *
 * [tapSize] 가 [iconSize] 보다 큰 만큼 자리를 더 차지한다. 쓰는 쪽에서 바깥
 * 여백을 그만큼 덜어내면 아이콘이 놓이는 자리는 전과 같다.
 */
@Composable
fun IconTap(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 23.dp,
    tapSize: Dp = 44.dp,
) {
    Box(
        modifier = modifier
            .size(tapSize)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}
