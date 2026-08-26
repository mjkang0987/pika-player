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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pikaworks.pikaplayer.data.prefs.Settings
import com.pikaworks.pikaplayer.data.prefs.ThemeMode
import com.pikaworks.pikaplayer.ui.ENCODING_OPTIONS
import com.pikaworks.pikaplayer.ui.OptionSheet
import com.pikaworks.pikaplayer.ui.SUBTITLE_SCALE_OPTIONS
import com.pikaworks.pikaplayer.ui.SPEED_OPTIONS
import com.pikaworks.pikaplayer.ui.ScreenHeader
import com.pikaworks.pikaplayer.ui.theme.KoreanWrap
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
    onThemeChange: (String) -> Unit,
    onOpenLicenses: () -> Unit,
    onAutoPipChange: (Boolean) -> Unit,
    onChildLockChange: (Boolean) -> Unit,
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
            options = SPEED_OPTIONS,
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
            options = SUBTITLE_SCALE_OPTIONS,
            selected = settings.subtitleScale,
            onSelect = onSubtitleScaleChange,
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

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        // 제목은 목록과 함께 밀려 올라가지 않는다. 어느 화면에 있는지와
        // 뒤로 갈 길은 스크롤 위치와 상관없이 늘 보여야 한다.
        ScreenHeader("설정", onBack)

        LazyColumn(modifier = Modifier.weight(1f)) {


            item { SectionHeader("라이브러리") }
            item {
                SwitchRow(
                    "자동 작은 창",
                    settings.autoPip,
                    onAutoPipChange,
                    description = "홈으로 나갈 때 영상이 작은 창으로 따라 나옵니다",
                )
            }
            item {
                ValueRow(
                    "비공개 폴더",
                    if (vaultEnabled) "켜짐" else "꺼짐",
                    description = "고른 폴더를 목록에서 감춥니다",
                    onClick = onOpenVault,
                )
            }
            // 어린이 잠금은 비공개 폴더와 같은 PIN 을 쓴다. PIN 이 없으면 켤 수 없으므로
            // 스위치 대신 PIN 을 정하러 가는 줄을 보여 준다 — 눌러도 안 켜지는 스위치는
            // 고장으로 읽힌다.
            item {
                if (vaultEnabled) {
                    SwitchRow(
                    "어린이 잠금",
                    settings.childLock,
                    onChildLockChange,
                    description = "잠금을 풀 때 PIN 을 묻고, 잠긴 동안 나갈 수 없습니다",
                )
                } else {
                    ValueRow(
                    "어린이 잠금",
                    "PIN 설정 필요",
                    description = "비공개 폴더와 같은 PIN 을 씁니다",
                    onClick = onOpenVault,
                )
                }
            }

            item { SectionHeader("재생") }
            item { ValueRow("기본 재생속도", label(SPEED_OPTIONS, settings.playbackSpeed)) { openPicker = Picker.SPEED } }
            item { SwitchRow("이어보기", settings.resumePlayback, onResumeChange) }
            item { SwitchRow("다음 영상 자동 재생", settings.autoPlayNext, onAutoPlayNextChange) }

            item { SectionHeader("자막") }
            item { ValueRow("기본 인코딩", label(ENCODING_OPTIONS, settings.subtitleEncoding)) { openPicker = Picker.ENCODING } }
            item { ValueRow("글자 크기", label(SUBTITLE_SCALE_OPTIONS, settings.subtitleScale)) { openPicker = Picker.SUBTITLE_SCALE } }

            item { SectionHeader("제스처") }
            item { SwitchRow("밝기 · 볼륨 스와이프", settings.gesturesEnabled, onGesturesChange) }
            item { SwitchRow("더블탭 10초 이동", settings.doubleTapSeekEnabled, onDoubleTapSeekChange) }

            item { SectionHeader("화면") }
            item { ValueRow("테마", label(THEMES, settings.theme)) { openPicker = Picker.THEME } }
            item { SwitchRow("자동회전 연동", settings.followAutoRotate, onFollowAutoRotateChange) }

            item { SectionHeader("정보") }
            item { InfoRow("버전", versionName) }
            item { ValueRow("오픈소스 라이선스", "", onClick = onOpenLicenses) }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

private enum class Picker { SPEED, ENCODING, SUBTITLE_SCALE, THEME }

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

/** 잠긴 Pro 항목. 누르면 무엇을 사는지 보여주는 화면으로 보낸다. */

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
    /**
     * 이름만으로 무엇인지 안 읽히는 항목에만 붙인다.
     *
     * 모든 줄에 달면 목록이 두 배로 길어져 훑기 어려워진다. "이어보기" 처럼
     * 이름이 곧 설명인 것에는 붙이지 않는다.
     */
    description: String? = null,
) {
    val colors = PikaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 50.dp)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // 설명이 길면 오른쪽 스위치·값을 밀어내지 않고 여기서 줄바꿈한다.
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, fontSize = 14.sp, color = colors.textPrimary)
            if (description != null) {
                Spacer(Modifier.height(3.dp))
                Text(
                    description,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light,
                    lineHeight = 15.sp,
                    color = colors.textMeta,
                    style = KoreanWrap,
                )
            }
        }
        content()
    }
}

@Composable
private fun ValueRow(
    label: String,
    value: String,
    description: String? = null,
    onClick: () -> Unit,
) {
    val colors = PikaTheme.colors
    SettingRow(label, description = description, content = {
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
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    description: String? = null,
) {
    SettingRow(
        label,
        description = description,
        content = { PikaSwitch(checked) { onChange(!checked) } },
        onClick = { onChange(!checked) },
    )
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
