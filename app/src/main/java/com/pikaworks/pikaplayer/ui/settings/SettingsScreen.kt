package com.pikaworks.pikaplayer.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.data.prefs.Settings
import com.pikaworks.pikaplayer.ui.player.PlayerIcons
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/**
 * 설정(S6). Phase 1 은 Free 항목만.
 *
 * 제스처와 자동 재생은 반드시 끌 수 있어야 한다 — 의도치 않은 탐색이나
 * 다음 영상 재생을 싫어하는 사용자가 일정 비율 존재한다(기획서 7.2).
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    onResumeChange: (Boolean) -> Unit,
    onAutoPlayNextChange: (Boolean) -> Unit,
    onGesturesChange: (Boolean) -> Unit,
    onDoubleTapSeekChange: (Boolean) -> Unit,
    onFollowAutoRotateChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    versionName: String,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors

    LazyColumn(modifier = modifier.fillMaxSize().background(colors.background)) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 60.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(PlayerIcons.Back, "뒤로", tint = colors.textPrimary,
                    modifier = Modifier.size(23.dp).clickable(onClick = onBack))
                Text("설정", fontSize = 20.sp, color = colors.textPrimary)
            }
        }

        item { SectionHeader("재생") }
        item { ValueRow("기본 재생속도", "${settings.playbackSpeed}×") { /* TODO: 선택 시트 */ } }
        item { SwitchRow("이어보기", settings.resumePlayback, onResumeChange) }
        item { SwitchRow("다음 영상 자동 재생", settings.autoPlayNext, onAutoPlayNextChange) }

        item { SectionHeader("자막") }
        item { ValueRow("기본 인코딩", encodingLabel(settings.subtitleEncoding)) { /* TODO */ } }
        item { ValueRow("글자 크기", "보통") { /* TODO */ } }
        item { ValueRow("표시 위치", "영상 하단") { /* TODO */ } }

        item { SectionHeader("제스처") }
        item { SwitchRow("밝기 · 볼륨 스와이프", settings.gesturesEnabled, onGesturesChange) }
        item { SwitchRow("더블탭 10초 이동", settings.doubleTapSeekEnabled, onDoubleTapSeekChange) }

        item { SectionHeader("화면") }
        item { ValueRow("테마", "시스템 설정") { /* TODO */ } }
        item { SwitchRow("자동회전 연동", settings.followAutoRotate, onFollowAutoRotateChange) }

        item { SectionHeader("정보") }
        item { InfoRow("버전", versionName) }
        item { ValueRow("오픈소스 라이선스", "") { /* TODO */ } }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

private fun encodingLabel(value: String): String = if (value == "auto") "자동 감지" else value

@Composable
private fun SectionHeader(title: String) {
    val colors = PikaTheme.colors
    Text(
        title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.8.sp,
        color = colors.key,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp),
    )
}

@Composable
private fun SettingRow(label: String, content: @Composable () -> Unit, onClick: (() -> Unit)? = null) {
    val colors = PikaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 50.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 14.sp, color = colors.textPrimary)
        content()
    }
}

@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    val colors = PikaTheme.colors
    SettingRow(label, content = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (value.isNotEmpty()) {
                Text(value, fontSize = 13.sp, fontWeight = FontWeight.Light, color = colors.textMeta)
            }
            Text("›", fontSize = 16.sp, color = colors.textFaint)
        }
    }, onClick = onClick)
}

/** 버전은 이동하는 항목이 아니므로 화살표를 붙이지 않는다. */
@Composable
private fun InfoRow(label: String, value: String) {
    val colors = PikaTheme.colors
    SettingRow(label, content = {
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Light, color = colors.textMeta)
    })
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingRow(label, content = { PikaSwitch(checked) { onChange(!checked) } }, onClick = { onChange(!checked) })
}

/**
 * 손잡이는 흰색이다. 키컬러 트랙 위에 어두운 손잡이를 두면 구멍처럼 읽힌다.
 */
@Composable
private fun PikaSwitch(checked: Boolean, onClick: () -> Unit) {
    val colors = PikaTheme.colors
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) colors.key else colors.chipBorder)
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (checked) androidx.compose.ui.graphics.Color.White else colors.textSecondary)
        )
    }
}
