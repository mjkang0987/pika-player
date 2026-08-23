package com.pikaworks.pikaplayer.data.media

import android.net.Uri

/** 라이브러리 한 줄에 필요한 값만 담는다. 재생 위치는 별도 DB에서 합친다. */
data class VideoItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val dateModifiedSec: Long,
    val folderName: String?,
    /** API 29+ 에서만 채워진다. 같은 폴더의 자막 파일을 찾을 때 쓴다. */
    val relativePath: String? = null,
    /** API 28 이하 폴백. 전체 파일 경로. */
    val filePath: String? = null,
) {
    /** 확장자를 뗀 이름. 자막 파일 자동 매칭의 기준. */
    val baseName: String get() = displayName.substringBeforeLast('.')

    /**
     * 같은 폴더인지 비교하는 키.
     *
     * API 29+ 는 RELATIVE_PATH, 그 이하는 전체 경로의 부모다. MediaStore 가
     * 버전마다 다른 열을 주기 때문에 자막 파일 쪽도 같은 규칙으로 만들어야 한다.
     */
    val folderKey: String?
        get() = relativePath ?: filePath?.substringBeforeLast('/', "")?.takeIf { it.isNotEmpty() }

    val resolutionLabel: String?
        get() = if (width > 0 && height > 0) "${width}×${height}" else null
}

/** 목록 행에 필요한 표시 상태를 한 덩어리로 묶은 것 */
data class LibraryRow(
    val video: VideoItem,
    val positionMs: Long,
    val subtitleFormat: String?,
) {
    /** 0f..1f. 재생 이력이 없으면 null */
    val progress: Float?
        get() = if (positionMs > 0 && video.durationMs > 0) {
            (positionMs.toFloat() / video.durationMs).coerceIn(0f, 1f)
        } else null
}
