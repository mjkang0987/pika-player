package com.pikaworks.pikaplayer.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational

/**
 * 화면 속 화면(PiP) — Pro 기능.
 *
 * 기기가 지원하지 않는 경우가 실제로 있다(태블릿·저사양 기기, 제조사 제외).
 * 지원 여부를 먼저 물어보지 않으면 버튼을 눌러도 아무 일이 없는 것처럼 보인다.
 */
class PipController(private val activity: Activity) {

    val isSupported: Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    /**
     * [videoWidth] × [videoHeight] 비율로 들어간다.
     *
     * 시스템이 받는 비율에는 상·하한이 있다(대략 1:2.39 ~ 2.39:1). 벗어난 값을
     * 넘기면 예외가 난다 — 세로로 긴 영상이나 메타데이터가 이상한 파일에서 실제로
     * 벗어나므로 잘라서 넘긴다.
     */
    fun enter(videoWidth: Int, videoHeight: Int): Boolean {
        if (!isSupported) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false

        val ratio = if (videoWidth > 0 && videoHeight > 0) {
            (videoWidth.toFloat() / videoHeight).coerceIn(MIN_RATIO, MAX_RATIO)
        } else {
            16f / 9f
        }
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational((ratio * 1000).toInt(), 1000))
            .build()
        return runCatching { activity.enterPictureInPictureMode(params) }.getOrDefault(false)
    }

    private companion object {
        const val MIN_RATIO = 0.42f
        const val MAX_RATIO = 2.39f
    }
}
