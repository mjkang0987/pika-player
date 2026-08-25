package com.pikaworks.pikaplayer.ui.player

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import com.pikaworks.pikaplayer.R

/**
 * 화면 속 화면(PiP).
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
    fun enter(videoWidth: Int, videoHeight: Int, isPlaying: Boolean): Boolean {
        // isSupported 안에 이미 들어 있는 조건이지만, 컴파일러는 val 을 통해서는
        // API 레벨을 좁히지 못한다. 여기서 다시 물어야 한다.
        if (!isSupported || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return runCatching {
            activity.enterPictureInPictureMode(params(videoWidth, videoHeight, isPlaying))
        }.getOrDefault(false)
    }

    /**
     * 창은 그대로 두고 버튼만 갈아끼운다.
     *
     * 재생 상태가 바뀌면 버튼 모양도 따라가야 한다. 이걸 부르지 않으면 영상이
     * 멈춘 뒤에도 일시정지 아이콘이 그대로 남아, 누르면 다시 멈추는 것처럼 보인다.
     */
    fun update(videoWidth: Int, videoHeight: Int, isPlaying: Boolean) {
        if (!isSupported || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching { activity.setPictureInPictureParams(params(videoWidth, videoHeight, isPlaying)) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun params(videoWidth: Int, videoHeight: Int, isPlaying: Boolean): PictureInPictureParams {
        val ratio = if (videoWidth > 0 && videoHeight > 0) {
            (videoWidth.toFloat() / videoHeight).coerceIn(MIN_RATIO, MAX_RATIO)
        } else {
            16f / 9f
        }
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational((ratio * 1000).toInt(), 1000))
            .setActions(listOf(togglePlayAction(isPlaying)))
            .build()
    }

    /**
     * PiP 창 안에는 우리 화면이 없다. 조작은 시스템이 그려 주는 이 버튼 하나뿐이고,
     * 눌리면 방송으로 돌아온다 — 받는 쪽은 MainActivity 다.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun togglePlayAction(isPlaying: Boolean): RemoteAction {
        val label = if (isPlaying) "일시정지" else "재생"
        val icon = if (isPlaying) R.drawable.ic_pip_pause else R.drawable.ic_pip_play
        // 앱 밖으로 나가면 안 되는 방송이다. 패키지를 박아 우리에게만 간다.
        val intent = Intent(ACTION_TOGGLE_PLAY).setPackage(activity.packageName)
        val pending = PendingIntent.getBroadcast(
            activity,
            REQUEST_TOGGLE_PLAY,
            intent,
            // 재생 상태가 바뀔 때마다 다시 만들므로 UPDATE_CURRENT.
            // IMMUTABLE 은 API 31+ 에서 필수다.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return RemoteAction(Icon.createWithResource(activity, icon), label, label, pending)
    }

    companion object {
        /** PiP 창의 재생/일시정지 버튼이 보내는 방송. */
        const val ACTION_TOGGLE_PLAY = "com.pikaworks.pikaplayer.PIP_TOGGLE_PLAY"

        private const val REQUEST_TOGGLE_PLAY = 1
        private const val MIN_RATIO = 0.42f
        private const val MAX_RATIO = 2.39f
    }
}
