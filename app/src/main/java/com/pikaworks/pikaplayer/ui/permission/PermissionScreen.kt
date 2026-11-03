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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.R
import com.pikaworks.pikaplayer.ui.AppIcons
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
                Icon(AppIcons.VideoLibrary, null, tint = colors.key, modifier = Modifier.size(46.dp))
            }

            Spacer(Modifier.height(30.dp))

            Text(
                if (denied) stringResource(R.string.permission_title_denied) else stringResource(R.string.permission_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 31.sp,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                if (denied) {
                    stringResource(R.string.permission_body_denied)
                } else {
                    stringResource(R.string.permission_body)
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
                Promise(stringResource(R.string.permission_promise_offline))
                Promise(stringResource(R.string.permission_promise_no_modify))
                Promise(stringResource(R.string.permission_promise_no_account))
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PrimaryButton(
                label = if (denied) stringResource(R.string.permission_pick_folder) else stringResource(R.string.permission_allow),
                onClick = if (denied) onPickFolder else onAllow,
            )
            if (!denied) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onPickFolder),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.permission_pick_folder_self), fontSize = 14.sp,
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
