package com.pikaworks.pikaplayer.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pikaworks.pikaplayer.ui.AppIcons
import com.pikaworks.pikaplayer.ui.ScreenHeader
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

/**
 * 오픈소스 라이선스 고지.
 *
 * 자동 생성 플러그인을 붙이지 않고 목록을 직접 관리한다. 의존성이 열 몇 개뿐이고
 * 전부 Apache 2.0 이라 플러그인이 만들어낼 결과와 다르지 않다. 대신
 * **의존성을 추가하면 여기에도 한 줄 추가해야 한다** — build.gradle.kts 와
 * 이 목록이 어긋나면 고지 누락이 된다.
 */
private data class LicensedLibrary(
    val name: String,
    val owner: String,
    val license: String,
)

private val LIBRARIES = listOf(
    LicensedLibrary("Kotlin 표준 라이브러리", "JetBrains", "Apache License 2.0"),
    LicensedLibrary("Kotlin Coroutines", "JetBrains", "Apache License 2.0"),
    LicensedLibrary("AndroidX Core KTX", "The Android Open Source Project", "Apache License 2.0"),
    LicensedLibrary("AndroidX Lifecycle", "The Android Open Source Project", "Apache License 2.0"),
    LicensedLibrary("AndroidX Activity Compose", "The Android Open Source Project", "Apache License 2.0"),
    LicensedLibrary("Jetpack Compose", "The Android Open Source Project", "Apache License 2.0"),
    LicensedLibrary("Material Components for Android", "Google", "Apache License 2.0"),
    LicensedLibrary("AndroidX Media3 (ExoPlayer)", "The Android Open Source Project", "Apache License 2.0"),
    LicensedLibrary("AndroidX Room", "The Android Open Source Project", "Apache License 2.0"),
    LicensedLibrary("AndroidX DataStore", "The Android Open Source Project", "Apache License 2.0"),
    LicensedLibrary("Coil", "Coil Contributors", "Apache License 2.0"),
)

private const val APACHE_2_0 = """Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License."""

@Composable
fun LicenseScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = PikaTheme.colors

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        // 제목은 목록과 함께 밀려 올라가지 않는다. 어느 화면에 있는지와
        // 뒤로 갈 길은 스크롤 위치와 상관없이 늘 보여야 한다.
        ScreenHeader("오픈소스 라이선스", onBack)

        LazyColumn(modifier = Modifier.weight(1f)) {

            items(LIBRARIES, key = { it.name }) { library ->
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 9.dp)) {
                    Text(library.name, fontSize = 14.sp, color = colors.textPrimary)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${library.owner} · ${library.license}",
                        fontSize = 10.sp, fontWeight = FontWeight.Light, color = colors.textMeta,
                    )
                }
            }

            item {
                Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 40.dp)) {
                    Text(
                        "Apache License 2.0",
                        fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp, color = colors.key,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        APACHE_2_0,
                        fontSize = 11.sp, fontWeight = FontWeight.Light,
                        lineHeight = 17.sp, color = colors.textSecondary,
                    )
                }
            }
        }
    }
}
