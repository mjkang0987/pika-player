package com.pikaworks.pikaplayer.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.ui.theme.PikaDarkColors
import kotlinx.coroutines.delay

/**
 * 방금 무슨 일이 일어났는지 알리는 짧은 띠.
 *
 * 목록에 담거나 파일을 지우는 것은 화면이 눈에 띄게 바뀌지 않는 동작이다.
 * 아무 반응이 없으면 눌렸는지조차 알 수 없어서, 한 줄로 알리고 스스로 사라진다.
 *
 * 색은 테마를 따르지 않고 늘 어둡다. 밝은 화면에서도 배경 위에 떠 있는 것으로
 * 읽혀야 하고, 재생 화면(검은 바탕) 위에도 같은 모습으로 떠야 한다.
 */
@Composable
fun MessageBar(
    message: String?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 같은 문구가 다시 와도 시간을 새로 센다. 연달아 담을 때 첫 번째 것에
    // 맞춰 사라져 버리면 두 번째는 잠깐 스치고 만다.
    LaunchedEffect(message) {
        if (message != null) {
            delay(SHOW_MS)
            onDone()
        }
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            Text(
                message.orEmpty(),
                fontSize = 13.sp,
                color = PikaDarkColors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PikaDarkColors.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

private const val SHOW_MS = 2200L
