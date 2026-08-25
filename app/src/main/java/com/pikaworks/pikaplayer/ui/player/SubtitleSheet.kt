package com.pikaworks.pikaplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.ui.AppIcons
import com.pikaworks.pikaplayer.ui.ENCODING_OPTIONS
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/**
 * 자막 설정 시트(S4).
 *
 * 인코딩 칩을 위쪽에 둔다. 국내 사용자가 가장 자주 건드리는 항목이라
 * 설정 깊숙이 숨기면 "자막이 깨졌다"로 바로 이탈한다 — 기획서 7.2.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubtitleSheet(
    state: PlayerUiState,
    onSelectSubtitle: (Int) -> Unit,
    onSelectCharset: (String) -> Unit,
    onAdjustOffset: (Long) -> Unit,
    onResetOffset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = PikaTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {

            Text(
                "자막",
                fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
            )

            SectionHeader("트랙")
            if (state.subtitleOptions.isEmpty()) {
                Text(
                    "이 영상 옆에 자막 파일이 없습니다",
                    fontSize = 13.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            } else {
                state.subtitleOptions.forEachIndexed { index, option ->
                    TrackRow(
                        name = option.name,
                        subtitle = "외부 파일 · ${option.formatLabel}",
                        selected = state.selectedSubtitle == index,
                        onClick = { onSelectSubtitle(index) },
                    )
                }
                TrackRow(
                    name = "자막 끄기",
                    subtitle = null,
                    selected = state.selectedSubtitle == -1,
                    onClick = { onSelectSubtitle(-1) },
                )
            }

            SectionHeader("인코딩")
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                ENCODING_OPTIONS.forEach { (value, label) ->
                    Chip(
                        label = label,
                        selected = state.subtitleCharset == value,
                        onClick = { onSelectCharset(value) },
                    )
                }
            }
            Text(
                "글자가 깨져 보이면 CP949를 먼저 시도해 보세요.",
                fontSize = 10.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
            )

            SectionHeader("싱크")
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                OffsetButton("−0.5") { onAdjustOffset(-500) }
                Text(
                    formatOffset(state.subtitleOffsetMs),
                    fontSize = 20.sp, fontWeight = FontWeight.Light, color = colors.textPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onResetOffset)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
                OffsetButton("+0.5") { onAdjustOffset(500) }
            }
        }
    }
}

private fun formatOffset(ms: Long): String {
    val sec = ms / 1000.0
    return if (sec >= 0) "+%.1f초".format(sec) else "%.1f초".format(sec)
}

@Composable
private fun SectionHeader(title: String) {
    val colors = PikaTheme.colors
    Text(
        title,
        fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp, color = colors.key,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun TrackRow(name: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    val colors = PikaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 46.dp)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                fontSize = 14.sp,
                color = if (selected) colors.textPrimary else colors.textSecondary,
            )
            subtitle?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, fontSize = 10.sp, fontWeight = FontWeight.Light, color = colors.textMeta)
            }
        }
        if (selected) {
            Icon(AppIcons.Check, "선택됨", tint = colors.key, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = PikaTheme.colors
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (selected) Modifier.background(colors.progressChipBorder)
                    .border(1.dp, colors.key, RoundedCornerShape(8.dp))
                else Modifier.border(1.dp, colors.chipBorder, RoundedCornerShape(8.dp))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Light,
            color = if (selected) colors.key else colors.textSecondary,
        )
    }
}

@Composable
private fun OffsetButton(label: String, onClick: () -> Unit) {
    val colors = PikaTheme.colors
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, colors.chipBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 13.sp, color = colors.textPrimary)
    }
}
