package com.pikaworks.pikaplayer.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pikaworks.pikaplayer.R
import com.pikaworks.pikaplayer.data.prefs.SortOrder
import com.pikaworks.pikaplayer.data.prefs.SubtitleEncoding
import com.pikaworks.pikaplayer.data.prefs.SubtitleScale

/** 정렬 시트의 선택지. 보관함과 폴더가 같은 목록을 쓴다. */
val SORT_OPTIONS: List<Pair<String, Int>> = listOf(
    SortOrder.DATE_DESC to R.string.sort_date_desc,
    SortOrder.NAME to R.string.sort_name,
    SortOrder.SIZE_DESC to R.string.sort_size_desc,
    SortOrder.DURATION_DESC to R.string.sort_duration_desc,
)

@StringRes
fun sortLabel(order: String): Int =
    SORT_OPTIONS.firstOrNull { it.first == order }?.second ?: SORT_OPTIONS.first().second

/** 재생속도 선택지. 설정 화면과 재생 화면의 속도 시트가 같은 목록을 쓴다. */
val SPEED_OPTIONS = listOf(
    0.5f to "0.5×", 0.75f to "0.75×", 1.0f to "1.0×",
    1.25f to "1.25×", 1.5f to "1.5×", 2.0f to "2.0×",
)

/** 자막 인코딩 선택지. 설정 화면과 플레이어의 자막 시트가 같은 목록을 쓴다. */
val ENCODING_OPTIONS
    @Composable get() = listOf(
    SubtitleEncoding.AUTO to stringResource(R.string.sort_encoding_auto),
    SubtitleEncoding.UTF_8 to "UTF-8",
    SubtitleEncoding.CP949 to "CP949",
    SubtitleEncoding.EUC_KR to "EUC-KR",
    SubtitleEncoding.SHIFT_JIS to "Shift-JIS",
)

/** 자막 글자 크기 선택지. 설정 화면과 플레이어의 자막 시트가 같은 목록을 쓴다. */
val SUBTITLE_SCALE_OPTIONS
    @Composable get() = listOf(
    SubtitleScale.SMALL to stringResource(R.string.sort_subtitle_scale_small),
    SubtitleScale.NORMAL to stringResource(R.string.sort_subtitle_scale_normal),
    SubtitleScale.LARGE to stringResource(R.string.sort_subtitle_scale_large),
    SubtitleScale.EXTRA_LARGE to stringResource(R.string.sort_subtitle_scale_extra_large),
    SubtitleScale.HUGE to stringResource(R.string.sort_subtitle_scale_huge),
)
