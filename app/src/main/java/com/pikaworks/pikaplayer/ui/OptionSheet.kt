package com.pikaworks.pikaplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.ui.theme.PikaDarkColors
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/** 값 하나를 고르는 시트. 설정 화면의 선택형 항목이 모두 이걸 쓴다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> OptionSheet(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    /**
     * 재생 화면 위에 뜨는가.
     *
     * 재생 화면은 테마와 무관하게 늘 검은 바탕이다. 거기 뜨는 시트만 테마를
     * 따라가면 라이트에서 흰 상자가 튀어나온 것처럼 보인다.
     */
    onMedia: Boolean = false,
) {
    val colors = if (onMedia) PikaDarkColors else PikaTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            Text(
                title,
                fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
            )
            options.forEach { (value, label) ->
                val isSelected = value == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(value)
                            onDismiss()
                        }
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        label,
                        fontSize = 14.sp,
                        color = if (isSelected) colors.textPrimary else colors.textSecondary,
                    )
                    if (isSelected) {
                        Icon(AppIcons.Check, "선택됨", tint = colors.key, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/** [ToggleSheet] 의 한 줄. 설명은 없으면 생략한다. */
data class SheetToggle(
    val label: String,
    val on: Boolean,
    val description: String? = null,
    val onToggle: () -> Unit,
)

/**
 * 켜고 끄는 항목이 여러 개인 시트.
 *
 * [OptionSheet] 와 달리 고르면 닫히지 않는다. 여기 있는 값들은 서로 배타적이
 * 아니라서, 두 개를 켜려고 시트를 두 번 여는 일이 없어야 한다.
 *
 * 체크 표시로 켜짐을 알린다. 스위치를 쓰면 재생 화면 위에서만 쓰는 색을 따로
 * 만들어야 하고, 바로 위의 [OptionSheet] 와 생김새도 어긋난다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToggleSheet(
    title: String,
    toggles: List<SheetToggle>,
    onDismiss: () -> Unit,
    onMedia: Boolean = false,
) {
    val colors = if (onMedia) PikaDarkColors else PikaTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            Text(
                title,
                fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
            )
            toggles.forEach { toggle ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = toggle.onToggle)
                        .heightIn(min = 52.dp)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            toggle.label,
                            fontSize = 14.sp,
                            color = if (toggle.on) colors.textPrimary else colors.textSecondary,
                        )
                        toggle.description?.let {
                            Text(
                                it,
                                fontSize = 11.sp, fontWeight = FontWeight.Light, lineHeight = 15.sp,
                                color = colors.textMeta,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                    if (toggle.on) {
                        Icon(AppIcons.Check, "켜짐", tint = colors.key, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
