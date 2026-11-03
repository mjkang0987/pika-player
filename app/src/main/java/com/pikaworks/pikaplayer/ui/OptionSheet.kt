package com.pikaworks.pikaplayer.ui

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.semantics.Role
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
        containerColor = colors.elevated,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp).selectableGroup()) {
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
                        // 화면 읽기 프로그램이 "여럿 중 하나 고르기" 로 읽게 한다.
                        .selectable(selected = isSelected, role = Role.RadioButton) {
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
                        // 고른 상태는 위 selectable 이 알린다. 아이콘까지 읽으면 두 번 듣는다.
                        Icon(AppIcons.Check, null, tint = colors.key, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
