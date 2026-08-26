package com.pikaworks.pikaplayer.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.provider.Settings
import android.view.WindowManager
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

    /** 상태바·내비게이션 바를 감춘다. 스와이프하면 잠깐 다시 나온다. */
    fun setImmersive(activity: Activity, immersive: Boolean) {
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        WindowCompat.setDecorFitsSystemWindows(activity.window, !immersive)

        // 낡은 전체화면 플래그도 함께 건다.
        //
        // targetSdk 30 이상에서는 무시된다고 문서에 적혀 있고, 실제로 인셋 계산에는
        // 영향이 없다. 그래도 다는 이유는 제조사 코드가 이 플래그를 따로 읽는
        // 경우가 있어서다 — 삼성 기기에서 상태바를 감춰도 배터리 잔량만 화면에
        // 남는데, 같은 조건에서 삼성 갤러리는 그것까지 지운다. 갤러리처럼 오래된
        // 앱이 아직 쓰는 값이 이것뿐이다.
        //
        // 효과가 없는 것으로 확인되면 지울 것. 짐작으로 넣은 줄이다.
        if (immersive) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        if (immersive) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
