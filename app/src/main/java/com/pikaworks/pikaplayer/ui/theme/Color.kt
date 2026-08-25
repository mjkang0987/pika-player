package com.pikaworks.pikaplayer.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 기획서 7.4 디자인 토큰.
 *
 * 키컬러는 초록 쪽으로 튼 시안(hue 약 170°)이고, 중성색도 같은 방향으로
 * 미세하게 틀어 톤을 맞춘다. 라이트 키컬러는 흰 배경 대비 확보를 위해
 * 더 어두운 값을 쓴다(5.6:1).
 *
 * 라이트 표면은 기획서의 #f5fbf9(채도 43%, 색상 160°)에서 #eafbf8(채도 68%,
 * 색상 169°)로 올렸다. 흰 배경 위에서 층이 거의 안 보였다. 색상도 함께 옮겨
 * 키컬러의 170° 에 맞췄다 — 전 값은 10° 어긋나 있었다.
 */
@Immutable
data class PikaColors(
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMeta: Color,
    val textFaint: Color,
    val key: Color,
    val divider: Color,
    val chipBorder: Color,
    val progressChipBorder: Color,
    val isDark: Boolean,
) {
    /**
     * 썸네일·영상 위에 얹히는 요소는 앱 테마와 무관하게 항상 '어두운 영상 위' 값을 쓴다.
     * 라이트 테마 값을 쓰면 썸네일에 묻혀 보이지 않는다.
     */
    val onMediaKey: Color get() = Color(0xFF5CC7B4)
    val onMediaText: Color get() = Color.White
    /** 영상 위 보조 아이콘·시간. 흰색을 그대로 쓰면 제목과 위계가 안 갈린다. */
    val onMediaTextMuted: Color get() = Color.White.copy(alpha = 0.78f)
    /** 영상 위 작은 라벨. 위 둘보다 한 단계 더 물러난다. */
    val onMediaTextFaint: Color get() = Color.White.copy(alpha = 0.60f)
    val onMediaTrack: Color get() = Color.White.copy(alpha = 0.20f)
}

val PikaDarkColors = PikaColors(
    background = Color(0xFF0C0C0E),
    surface = Color(0xFF101614),
    textPrimary = Color(0xFFE8F3EF),
    textSecondary = Color(0xFF8FA6A1),
    textMeta = Color(0xFF7D9691),
    textFaint = Color(0xFF647A75),
    key = Color(0xFF5CC7B4),
    divider = Color(0xFF1E2826),
    chipBorder = Color(0xFF2A3734),
    progressChipBorder = Color(0xFF2A4F47),
    isDark = true,
)

val PikaLightColors = PikaColors(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFEAFBF8),
    textPrimary = Color(0xFF0F1614),
    textSecondary = Color(0xFF5A6E69),
    textMeta = Color(0xFF647A75),
    textFaint = Color(0xFF7D908B),
    key = Color(0xFF267365),
    divider = Color(0xFFE2EEEA),
    chipBorder = Color(0xFFD5E4DF),
    progressChipBorder = Color(0xFFB3D5CC),
    isDark = false,
)
