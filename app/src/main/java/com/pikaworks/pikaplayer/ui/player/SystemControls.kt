package com.pikaworks.pikaplayer.ui.player

import android.app.Activity
import android.media.AudioManager
import android.view.WindowManager

/**
 * 밝기와 볼륨은 플레이어가 아니라 시스템이 들고 있다.
 *
 * 밝기는 이 창에만 적용한다(`WindowManager.LayoutParams.screenBrightness`).
 * 기기 전체 밝기를 바꾸면 앱을 나간 뒤에도 어두운 채로 남아 사용자가 당황한다.
 */
class SystemControls(private val activity: Activity) {

    private val audioManager: AudioManager? =
        activity.getSystemService(AudioManager::class.java)

    private val maxVolume: Int
        get() = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1

    /** 0f..1f. 아직 지정한 적 없으면 시스템 값을 따르므로 중간값에서 시작한다. */
    var brightness: Float = activity.window.attributes.screenBrightness
        .takeIf { it >= 0f } ?: 0.5f
        private set

    var volume: Float = (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0)
        .toFloat() / maxVolume.coerceAtLeast(1)
        private set

    fun adjustBrightness(delta: Float): Float {
        brightness = (brightness + delta).coerceIn(0.01f, 1f)
        activity.window.attributes = activity.window.attributes.apply {
            screenBrightness = brightness
        }
        return brightness
    }

    fun adjustVolume(delta: Float): Float {
        volume = (volume + delta).coerceIn(0f, 1f)
        audioManager?.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            (volume * maxVolume).toInt(),
            0, // 시스템 볼륨 UI 를 띄우지 않는다 — 우리 인디케이터와 겹친다
        )
        return volume
    }

    /** 재생 중에는 화면이 꺼지지 않아야 한다. */
    fun keepScreenOn(on: Boolean) {
        if (on) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
