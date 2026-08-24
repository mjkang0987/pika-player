package com.pikaworks.pikaplayer.ui.settings

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pikaworks.pikaplayer.data.prefs.Settings
import com.pikaworks.pikaplayer.data.prefs.SubtitlePosition
import com.pikaworks.pikaplayer.data.prefs.SubtitleScale
import com.pikaworks.pikaplayer.data.prefs.ThemeMode
import com.pikaworks.pikaplayer.ui.ENCODING_OPTIONS
import com.pikaworks.pikaplayer.ui.OptionSheet
import com.pikaworks.pikaplayer.ui.pro.ProFeatures
import com.pikaworks.pikaplayer.ui.AppIcons
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
    onPlaybackSpeedChange: (Float) -> Unit,
    onSubtitleEncodingChange: (String) -> Unit,
    onSubtitleScaleChange: (Float) -> Unit,
    onSubtitlePositionChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onOpenLicenses: () -> Unit,
    /** Pro 기능을 쓸 수 있는가. 잠긴 항목을 그릴지 실제 설정으로 열지 가른다. */
    proUnlocked: Boolean,
    onOpenPro: () -> Unit,
    onAutoPipChange: (Boolean) -> Unit,
    /** 비공개 폴더가 켜져 있는가. 켜져 있으면 폴더 고르기로, 아니면 PIN 설정으로 간다. */
    vaultEnabled: Boolean,
    onOpenVault: () -> Unit,
    onBack: () -> Unit,
    versionName: String,
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors
    var openPicker by remember { mutableStateOf<Picker?>(null) }

    when (openPicker) {
        Picker.SPEED -> OptionSheet(
            title = "기본 재생속도",
            options = SPEEDS,
            selected = settings.playbackSpeed,
            onSelect = onPlaybackSpeedChange,
            onDismiss = { openPicker = null },
        )
        Picker.ENCODING -> OptionSheet(
            title = "기본 인코딩",
            options = ENCODING_OPTIONS,
            selected = settings.subtitleEncoding,
            onSelect = onSubtitleEncodingChange,
            onDismiss = { openPicker = null },
        )
        Picker.SUBTITLE_SCALE -> OptionSheet(
            title = "자막 글자 크기",
            options = SCALES,
            selected = settings.subtitleScale,
            onSelect = onSubtitleScaleChange,
            onDismiss = { openPicker = null },
        )
        Picker.SUBTITLE_POSITION -> OptionSheet(
            title = "자막 표시 위치",
            options = POSITIONS,
            selected = settings.subtitlePosition,
            onSelect = onSubtitlePositionChange,
            onDismiss = { openPicker = null },
        )
        Picker.THEME -> OptionSheet(
            title = "테마",
            options = THEMES,
            selected = settings.theme,
            onSelect = onThemeChange,
            onDismiss = { openPicker = null },
        )
        null -> Unit
    }

    LazyColumn(modifier = modifier.fillMaxSize().background(colors.background)) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 60.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(AppIcons.Back, "뒤로", tint = colors.textPrimary,
                    modifier = Modifier.size(23.dp).clickable(onClick = onBack))
                Text("설정", fontSize = 20.sp, color = colors.textPrimary)
            }
        }

        item { ProCard(unlocked = proUnlocked, onClick = onOpenPro) }

        // 잠긴 항목을 별도 화면으로 빼지 않는다. 같은 자리에 두어야 "여기 있는데
        // 잠겨 있다 → 사면 여기가 열린다" 가 한 번에 읽힌다. 별도 화면이면 Free
        // 사용자는 들어가기 전까지 무엇이 있는지 모르고, 산 사람은 어디서 켜는지
        // 다시 찾아야 한다.
        item { SectionHeader("Pro 기능") }
        item {
            if (proUnlocked) {
                SwitchRow("자동 작은 창", settings.autoPip, onAutoPipChange)
            } else {
                LockedRow("자동 작은 창", onOpenPro)
            }
        }
        item {
            if (proUnlocked) {
                ValueRow("비공개 폴더", if (vaultEnabled) "켜짐" else "꺼짐", onOpenVault)
            } else {
                LockedRow("비공개 폴더", onOpenPro)
            }
        }

        item { SectionHeader("재생") }
        item { ValueRow("기본 재생속도", label(SPEEDS, settings.playbackSpeed)) { openPicker = Picker.SPEED } }
        item { SwitchRow("이어보기", settings.resumePlayback, onResumeChange) }
        item { SwitchRow("다음 영상 자동 재생", settings.autoPlayNext, onAutoPlayNextChange) }

        item { SectionHeader("자막") }
        item { ValueRow("기본 인코딩", label(ENCODING_OPTIONS, settings.subtitleEncoding)) { openPicker = Picker.ENCODING } }
        item { ValueRow("글자 크기", label(SCALES, settings.subtitleScale)) { openPicker = Picker.SUBTITLE_SCALE } }
        item { ValueRow("표시 위치", label(POSITIONS, settings.subtitlePosition)) { openPicker = Picker.SUBTITLE_POSITION } }

        item { SectionHeader("제스처") }
        item { SwitchRow("밝기 · 볼륨 스와이프", settings.gesturesEnabled, onGesturesChange) }
        item { SwitchRow("더블탭 10초 이동", settings.doubleTapSeekEnabled, onDoubleTapSeekChange) }

        item { SectionHeader("화면") }
        item { ValueRow("테마", label(THEMES, settings.theme)) { openPicker = Picker.THEME } }
        item { SwitchRow("자동회전 연동", settings.followAutoRotate, onFollowAutoRotateChange) }

        item { SectionHeader("정보") }
        item { InfoRow("버전", versionName) }
        item { ValueRow("오픈소스 라이선스", "", onOpenLicenses) }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

private enum class Picker { SPEED, ENCODING, SUBTITLE_SCALE, SUBTITLE_POSITION, THEME }

private val SPEEDS = listOf(
    0.5f to "0.5×", 0.75f to "0.75×", 1.0f to "1.0×",
    1.25f to "1.25×", 1.5f to "1.5×", 2.0f to "2.0×",
)
private val SCALES = listOf(
    SubtitleScale.SMALL to "작게", SubtitleScale.NORMAL to "보통", SubtitleScale.LARGE to "크게",
)
private val POSITIONS = listOf(
    SubtitlePosition.IN_VIDEO to "영상 하단", SubtitlePosition.LETTERBOX to "레터박스",
)
private val THEMES = listOf(
    ThemeMode.SYSTEM to "시스템 설정", ThemeMode.DARK to "다크", ThemeMode.LIGHT to "라이트",
)

private fun <T> label(options: List<Pair<T, String>>, value: T): String =
    options.firstOrNull { it.first == value }?.second ?: value.toString()

/**
 * Pro 진입 카드.
 *
 * Free 에게는 무엇을 얻는지 보여주고, 산 사람에게는 조용한 확인 줄이 된다.
 * 이미 산 사람에게 계속 파는 화면을 보여줄 이유가 없다.
 */
@Composable
private fun ProCard(unlocked: Boolean, onClick: () -> Unit) {
    val colors = PikaTheme.colors
    if (unlocked) {
        Row(
            modifier = Modifier
                .padding(start = 20.dp, end = 20.dp, top = 14.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surface)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(AppIcons.Check, null, tint = colors.key, modifier = Modifier.size(15.dp))
                Text("Pika Pro", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                Text("사용 중", fontSize = 11.sp, fontWeight = FontWeight.Light, color = colors.textMeta)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("구매 내역", fontSize = 12.sp, fontWeight = FontWeight.Light, color = colors.textMeta)
                Icon(AppIcons.ChevronRight, null, tint = colors.textFaint, modifier = Modifier.size(15.dp))
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, top = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.chipBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Pika Pro", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.key)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("자세히 보기", fontSize = 12.sp, color = colors.key)
                Icon(AppIcons.ChevronRight, null, tint = colors.key, modifier = Modifier.size(13.dp))
            }
        }
        Text(
            "${ProFeatures.shortPitch}를 씁니다.\n한 번만 결제하고 계속 씁니다.",
            fontSize = 12.sp, fontWeight = FontWeight.Light,
            lineHeight = 18.sp, color = colors.textSecondary,
        )
    }
}

/** 잠긴 Pro 항목. 누르면 무엇을 사는지 보여주는 화면으로 보낸다. */
@Composable
private fun LockedRow(label: String, onClick: () -> Unit) {
    val colors = PikaTheme.colors
    SettingRow(
        label = label,
        dim = true,
        content = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(AppIcons.Lock, null, tint = colors.textFaint, modifier = Modifier.size(13.dp))
                Text(
                    "Pro",
                    fontSize = 10.sp, fontWeight = FontWeight.Medium, color = colors.textFaint,
                    modifier = Modifier
                        .border(1.dp, colors.chipBorder, RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        },
        onClick = onClick,
    )
}

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
private fun SettingRow(
    label: String,
    content: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
    /** 잠긴 항목은 글자를 한 단계 낮춰 쓸 수 없다는 것을 색으로도 알린다. */
    dim: Boolean = false,
) {
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
        Text(label, fontSize = 14.sp, color = if (dim) colors.textSecondary else colors.textPrimary)
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
