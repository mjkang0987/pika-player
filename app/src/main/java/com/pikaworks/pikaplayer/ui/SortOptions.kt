package com.pikaworks.pikaplayer.ui

import com.pikaworks.pikaplayer.data.prefs.SortOrder
import com.pikaworks.pikaplayer.data.prefs.SubtitleEncoding
import com.pikaworks.pikaplayer.data.prefs.SubtitleScale

/** 정렬 시트의 선택지. 보관함과 폴더가 같은 목록을 쓴다. */
val SORT_OPTIONS = listOf(
    SortOrder.DATE_DESC to "최근 수정순",
    SortOrder.NAME to "이름순",
    SortOrder.SIZE_DESC to "크기순",
    SortOrder.DURATION_DESC to "재생시간순",
)

fun sortLabel(order: String): String =
    SORT_OPTIONS.firstOrNull { it.first == order }?.second ?: SORT_OPTIONS.first().second

/** 재생속도 선택지. 설정 화면과 재생 화면의 속도 시트가 같은 목록을 쓴다. */
val SPEED_OPTIONS = listOf(
    0.5f to "0.5×", 0.75f to "0.75×", 1.0f to "1.0×",
    1.25f to "1.25×", 1.5f to "1.5×", 2.0f to "2.0×",
)

/** 자막 인코딩 선택지. 설정 화면과 플레이어의 자막 시트가 같은 목록을 쓴다. */
val ENCODING_OPTIONS = listOf(
    SubtitleEncoding.AUTO to "자동 감지",
    SubtitleEncoding.UTF_8 to "UTF-8",
    SubtitleEncoding.CP949 to "CP949",
    SubtitleEncoding.EUC_KR to "EUC-KR",
    SubtitleEncoding.SHIFT_JIS to "Shift-JIS",
)

/** 자막 글자 크기 선택지. 설정 화면과 플레이어의 자막 시트가 같은 목록을 쓴다. */
val SUBTITLE_SCALE_OPTIONS = listOf(
    SubtitleScale.SMALL to "작게",
    SubtitleScale.NORMAL to "보통",
    SubtitleScale.LARGE to "크게",
    SubtitleScale.EXTRA_LARGE to "아주 크게",
    SubtitleScale.HUGE to "가장 크게",
)
