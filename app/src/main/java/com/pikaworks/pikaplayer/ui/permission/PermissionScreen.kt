package com.pikaworks.pikaplayer.ui.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.pikaworks.pikaplayer.ui.player.PlayerIcons
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/**
 * 권한 온보딩(S5).
 *
 * 무엇을 하지 않는지를 먼저 보여준다. 저장소 권한은 거절률이 높아
 * 시스템 대화상자를 띄우기 전에 설득이 필요하다.
 *
 * [onPickFolder] 는 거부한 사용자를 위한 우회로다. 이 버튼이 없으면
 * 권한 거부 = 앱을 쓸 수 없음이 되어 그 자리에서 이탈한다.
 */
@Composable
fun PermissionScreen(
    onAllow: () -> Unit,
    onPickFolder: () -> Unit,
    denied: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 36.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.divider, RoundedCornerShape(30.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(PlayerIcons.Subtitle, null, tint = colors.key, modifier = Modifier.size(46.dp))
            }

            Spacer(Modifier.height(30.dp))

            Text(
                if (denied) "폴더를 직접 골라주세요" else "기기에 있는 동영상을\n불러올게요",
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 31.sp,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                if (denied) {
                    "권한 없이도 쓸 수 있습니다. 동영상이 들어있는 폴더를 고르면 그 안만 읽습니다."
                } else {
                    "보관함을 만들려면 동영상 파일에 접근할 수 있어야 합니다. 파일을 읽기만 하고, 어디에도 보내지 않습니다."
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 23.sp,
            )

            Spacer(Modifier.height(34.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Promise("네트워크 없이 완전히 오프라인으로 동작")
                Promise("파일을 수정하거나 삭제하지 않음")
                Promise("계정 가입이나 로그인 없음")
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PrimaryButton(
                label = if (denied) "폴더 선택" else "동영상 접근 허용",
                onClick = if (denied) onPickFolder else onAllow,
            )
            if (!denied) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onPickFolder),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("폴더를 직접 선택할게요", fontSize = 14.sp,
                        fontWeight = FontWeight.Light, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun Promise(text: String) {
    val colors = PikaTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Text("✓", fontSize = 15.sp, color = colors.key)
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Light, color = colors.textPrimary)
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    val colors = PikaTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.key)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium,
            color = if (colors.isDark) colors.background else androidx.compose.ui.graphics.Color.White)
    }
}
