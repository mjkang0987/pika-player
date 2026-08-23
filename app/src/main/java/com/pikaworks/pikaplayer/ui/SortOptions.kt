package com.pikaworks.pikaplayer.ui

import com.pikaworks.pikaplayer.data.prefs.SortOrder

/** 정렬 시트의 선택지. 보관함과 폴더가 같은 목록을 쓴다. */
val SORT_OPTIONS = listOf(
    SortOrder.DATE_DESC to "최근 수정순",
    SortOrder.NAME to "이름순",
    SortOrder.SIZE_DESC to "크기순",
    SortOrder.DURATION_DESC to "재생시간순",
)

fun sortLabel(order: String): String =
    SORT_OPTIONS.firstOrNull { it.first == order }?.second ?: SORT_OPTIONS.first().second
