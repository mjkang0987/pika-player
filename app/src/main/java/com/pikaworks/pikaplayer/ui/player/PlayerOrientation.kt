package com.pikaworks.pikaplayer.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.provider.Settings
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * 전체화면은 곧 가로 방향이다. 별도 상태로 두지 않고 방향에서 읽는다.
 * 따로 관리하면 기기를 돌렸을 때와 버튼을 눌렀을 때가 어긋난다.
 */
object PlayerOrientation {

    /** 시스템 자동회전이 켜져 있는가. 꺼져 있으면 버튼으로만 전환한다. */
    fun isSystemAutoRotateOn(activity: Activity): Boolean = runCatching {
        Settings.System.getInt(
            activity.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
        ) == 1
    }.getOrDefault(false)

    /**
     * 방향 정책을 적용한다.
     *
     * 앱 내 잠금이 시스템 자동회전보다 **우선**이다. 누워서 보는 사용자는
     * 자동회전이 켜져 있어도 화면이 제멋대로 도는 것을 싫어한다 — 기획서 7.2.
     */
    fun apply(
        activity: Activity,
        locked: Boolean,
        followAutoRotate: Boolean,
        forcedLandscape: Boolean?,
    ) {
        activity.requestedOrientation = when {
            locked -> ActivityInfo.SCREEN_ORIENTATION_LOCKED
            forcedLandscape == true -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            forcedLandscape == false -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            followAutoRotate && isSystemAutoRotateOn(activity) -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    /** 가로에서는 상태바·내비게이션 바를 감춘다. 스와이프하면 잠깐 다시 나온다. */
    fun setImmersive(activity: Activity, immersive: Boolean) {
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        WindowCompat.setDecorFitsSystemWindows(activity.window, !immersive)
        if (immersive) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
