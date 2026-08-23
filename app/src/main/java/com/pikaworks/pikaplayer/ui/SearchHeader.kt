package com.pikaworks.pikaplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/**
 * 화면 제목과 검색(S7). 보관함 · 폴더 · 최근이 같은 것을 쓴다.
 *
 * 검색을 켜면 제목 자리를 입력칸이 대신한다. 별도 화면으로 넘기지 않는다 —
 * 결과를 보면서 바로 지우고 다시 칠 수 있어야 한다.
 *
 * 검색어는 화면마다 따로 둔다. 탭을 옮겼는데 보이지 않는 검색어가 목록을 계속
 * 좁히고 있으면 "왜 이것밖에 안 나오지" 가 된다.
 *
 * [searching] 을 밖에서 들고 있는 이유: 이 헤더는 LazyColumn 의 한 항목으로 들어간다.
 * 목록을 아래로 많이 내리면 항목이 폐기되는데, 상태를 안에 두면 그때 검색이 꺼진다.
 */
@Composable
fun SearchHeader(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    searching: Boolean,
    onSearchingChange: (Boolean) -> Unit,
    placeholder: String = "영상 이름",
    modifier: Modifier = Modifier,
) {
    val colors = PikaTheme.colors
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 60.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (searching) {
            // 입력칸이 붙은 뒤에 포커스를 줘야 한다. 화면 쪽에서 미리 요청하면
            // FocusRequester 가 아직 연결되지 않아 터진다.
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        placeholder,
                        fontSize = 18.sp, fontWeight = FontWeight.Light, color = colors.textFaint,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        color = colors.textPrimary,
                    ),
                    cursorBrush = SolidColor(colors.key),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
        } else {
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.Light, color = colors.textPrimary)
        }
        Icon(
            if (searching) AppIcons.Close else AppIcons.Search,
            if (searching) "검색 닫기" else "검색",
            tint = colors.textSecondary,
            modifier = Modifier
                .padding(start = 12.dp)
                .size(22.dp)
                .clickable {
                    onSearchingChange(!searching)
                    if (searching) onQueryChange("") // 닫을 때는 걸러둔 것도 푼다
                },
        )
    }
}

/** 검색어에 걸리는지. 대소문자를 무시한다. */
fun matchesQuery(query: String, vararg fields: String?): Boolean =
    query.isBlank() || fields.any { it?.contains(query, ignoreCase = true) == true }
