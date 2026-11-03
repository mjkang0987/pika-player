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
import com.pikaworks.pikaplayer.ui.SUBTITLE_SCALE_OPTIONS
import com.pikaworks.pikaplayer.ui.theme.PikaDarkColors
import androidx.compose.ui.res.stringResource
import com.pikaworks.pikaplayer.R

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
    /** 설정에 저장된 자막 배율. 시트에서 고른 값이 설정에도 그대로 남는다. */
    subtitleScale: Float,
    onSelectScale: (Float) -> Unit,
    onAdjustOffset: (Long) -> Unit,
    onResetOffset: () -> Unit,
    onDismiss: () -> Unit,
) {
    // 재생 화면은 테마와 무관하게 늘 검은 바탕이다. 이 시트도 거기 맞춘다.
    val colors = PikaDarkColors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.elevated,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {

            Text(
                stringResource(R.string.player_subtitles),
                fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
            )

            SectionHeader(stringResource(R.string.player_subtitle_tracks))
            if (state.subtitleOptions.isEmpty()) {
                Text(
                    stringResource(R.string.player_subtitle_no_files),
                    fontSize = 13.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            } else {
                state.subtitleOptions.forEachIndexed { index, option ->
                    TrackRow(
                        name = option.name,
                        subtitle = stringResource(R.string.player_subtitle_external_file, option.formatLabel),
                        selected = state.selectedSubtitle == index,
                        onClick = { onSelectSubtitle(index) },
                    )
                }
                TrackRow(
                    name = stringResource(R.string.player_subtitle_turn_off),
                    subtitle = null,
                    selected = state.selectedSubtitle == -1,
                    onClick = { onSelectSubtitle(-1) },
                )
            }

            SectionHeader(stringResource(R.string.player_subtitle_encoding))
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
                stringResource(R.string.player_subtitle_encoding_hint),
                fontSize = 10.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
            )

            // 글자 크기도 여기 둔다. 자막이 작다는 것은 영상을 보는 중에
            // 알게 되는데, 그 순간 설정은 재생 화면을 나갔다 와야 하는 자리에
            // 있다. 여기서 고르면 시트를 닫는 즉시 바뀐 크기가 보인다.
            // 고른 값은 설정에 그대로 남는다 — 두 곳이 다른 값을 들고 있으면
            // 어느 쪽이 진짜인지 알 수 없다.
            SectionHeader(stringResource(R.string.player_subtitle_text_size))
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                SUBTITLE_SCALE_OPTIONS.forEach { (value, label) ->
                    Chip(
                        label = label,
                        selected = subtitleScale == value,
                        onClick = { onSelectScale(value) },
                    )
                }
            }

            SectionHeader(stringResource(R.string.player_subtitle_sync))
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

@Composable
private fun formatOffset(ms: Long): String {
    val sec = ms / 1000.0
    return stringResource(R.string.player_subtitle_offset_seconds, sec)
}

@Composable
private fun SectionHeader(title: String) {
    val colors = PikaDarkColors
    Text(
        title,
        fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp, color = colors.key,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
    )
}

@Composable
private fun TrackRow(name: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    val colors = PikaDarkColors
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
            Icon(AppIcons.Check, stringResource(R.string.player_selected), tint = colors.key, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = PikaDarkColors
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
    val colors = PikaDarkColors
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
