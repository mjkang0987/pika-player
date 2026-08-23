package com.pikaworks.pikaplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.pikaworks.pikaplayer.data.prefs.ThemeMode

private val LocalPikaColors = staticCompositionLocalOf { PikaDarkColors }

/** 앱 어디서나 `PikaTheme.colors.key` 로 토큰에 접근한다. */
object PikaTheme {
    val colors: PikaColors
        @Composable @ReadOnlyComposable get() = LocalPikaColors.current
}

@Composable
fun PikaTheme(
    /** ThemeMode 값. 시스템 설정을 따르면 기기 설정을 읽는다. */
    themeMode: String = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        else -> isSystemInDarkTheme()
    }
    val colors = if (darkTheme) PikaDarkColors else PikaLightColors

    // Material3 는 타이포그래피와 리플 기본값 때문에 쓴다.
    // 색은 우리 토큰이 진짜 출처이고, 아래 매핑은 Material 컴포넌트가 참조하는 용도.
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.key,
            background = colors.background,
            surface = colors.surface,
            onPrimary = colors.background,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
        )
    } else {
        lightColorScheme(
            primary = colors.key,
            background = colors.background,
            surface = colors.surface,
            onPrimary = androidx.compose.ui.graphics.Color.White,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary,
        )
    }

    CompositionLocalProvider(LocalPikaColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = PikaTypography, content = content)
    }
}
