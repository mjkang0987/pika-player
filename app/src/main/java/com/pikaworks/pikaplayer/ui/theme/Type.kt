package com.pikaworks.pikaplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// TODO: 시안은 IBM Plex Sans KR 기준으로 그려졌다.
//  res/font/ 에 폰트를 넣고 아래 FontFamily 를 교체할 것.
//  (오프라인 동작이 전제이므로 다운로더블 폰트가 아니라 번들로 넣는다)
private val PikaFontFamily = FontFamily.Default

val PikaTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = PikaFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 26.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = PikaFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        letterSpacing = (-0.2).sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = PikaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = (-0.2).sp,
    ),
    bodySmall = TextStyle(
        fontFamily = PikaFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = 10.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = PikaFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 9.sp,
    ),
)

/**
 * 문장으로 읽는 한글 글에 쓰는 줄바꿈.
 *
 * 기본값은 글자 단위로 끊어서 "지워야" 가 "지워 / 야" 로 갈라진다. Paragraph 는
 * 낱말 경계를 보고 끊는다. 계산이 무거우므로 안내문처럼 문장인 글에만 쓰고,
 * 목록의 제목이나 한두 낱말짜리 라벨에는 걸지 않는다.
 */
val KoreanWrap = TextStyle(lineBreak = LineBreak.Paragraph)
