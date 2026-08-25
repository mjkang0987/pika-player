package com.pikaworks.pikaplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.pikaworks.pikaplayer.ui.player.RepeatMode
import com.pikaworks.pikaplayer.ui.theme.PikaDarkColors

/**
 * 이 영상이 끝나면 무엇이 일어나는가.
 *
 * 네 가지가 한 시트에 있는 이유는 모두 그 한 가지 질문의 답이기 때문이다.
 * 흩어 놓으면 서로 어떻게 맞물리는지 알기 어렵다.
 *
 * 반복은 셋 중 하나를 고르는 것이라 동그라미로, 랜덤과 자동 재생은 따로 켜고
 * 끄는 것이라 체크로 그린다. 생김새가 다르면 "이걸 켜면 저건 꺼지나" 를 눌러
 * 보지 않고도 알 수 있다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSheet(
    repeatMode: String,
    onRepeatModeChange: (String) -> Unit,
    /**
     * 목록을 통째로 튼 경우인가(재생목록·폴더 등).
     *
     * 목록 반복과 랜덤은 그럴 때만 뜻이 있다. 보관함에서 한 편을 누른 대기열은
     * 기기의 모든 영상이라, 거기서 섞는 것은 순서를 흐트러뜨리는 것이 아니라
     * 그냥 아무 영상이나 트는 것이 된다.
     */
    queueControls: Boolean,
    shuffle: Boolean,
    onShuffleChange: () -> Unit,
    autoPlayNext: Boolean,
    onAutoPlayNextChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = PikaDarkColors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            Text(
                "반복과 순서",
                fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
            )

            GroupLabel("반복")
            Choice(
                label = "반복 안 함",
                selected = repeatMode == RepeatMode.OFF,
                onSelect = { onRepeatModeChange(RepeatMode.OFF) },
            )
            Choice(
                label = "한 편 반복",
                description = "지금 영상이 끝나면 처음부터 다시 틉니다.",
                selected = repeatMode == RepeatMode.ONE,
                onSelect = { onRepeatModeChange(RepeatMode.ONE) },
            )
            if (queueControls) {
                Choice(
                    label = "목록 반복",
                    description = "마지막 영상 다음에 목록의 처음으로 돌아갑니다.",
                    selected = repeatMode == RepeatMode.ALL,
                    onSelect = { onRepeatModeChange(RepeatMode.ALL) },
                )
            }

            GroupLabel("그 밖")
            if (queueControls) {
                Toggle(
                    label = "랜덤",
                    description = "지금 영상 뒤의 순서를 섞습니다.",
                    on = shuffle,
                    onToggle = onShuffleChange,
                )
            }
            Toggle(
                label = "자동 재생",
                description = "끝나면 다음 영상으로 넘어갑니다. 이미 끝까지 본 영상은 처음부터 틉니다.",
                on = autoPlayNext,
                onToggle = { onAutoPlayNextChange(!autoPlayNext) },
            )
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.8.sp,
        color = PikaDarkColors.textMeta,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 2.dp),
    )
}

/** 셋 중 하나. 고르면 나머지가 풀린다. */
@Composable
private fun Choice(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    description: String? = null,
) {
    val colors = PikaDarkColors
    SheetRow(label, description, selected, onSelect) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(1.5.dp, if (selected) colors.key else colors.chipBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // 가운데를 채워 고른 것을 알린다. 테두리 색만 바꾸면 두 상태가
            // 나란히 있지 않은 한 어느 쪽인지 알기 어렵다.
            if (selected) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(colors.key))
            }
        }
    }
}

/** 따로 켜고 끄는 것. 서로 영향이 없다. */
@Composable
private fun Toggle(
    label: String,
    on: Boolean,
    onToggle: () -> Unit,
    description: String? = null,
) {
    val colors = PikaDarkColors
    SheetRow(label, description, on, onToggle) {
        if (on) {
            Icon(AppIcons.Check, "켜짐", tint = colors.key, modifier = Modifier.size(18.dp))
        } else {
            Box(Modifier.size(18.dp))
        }
    }
}

/** 두 줄의 뼈대. 표시만 다르고 나머지는 같아야 한 시트로 읽힌다. */
@Composable
private fun SheetRow(
    label: String,
    description: String?,
    highlighted: Boolean,
    onClick: () -> Unit,
    indicator: @Composable () -> Unit,
) {
    val colors = PikaDarkColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                label,
                fontSize = 14.sp,
                color = if (highlighted) colors.textPrimary else colors.textSecondary,
            )
            description?.let {
                Text(
                    it,
                    fontSize = 11.sp, fontWeight = FontWeight.Light, lineHeight = 15.sp,
                    color = colors.textMeta,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        indicator()
    }
}
