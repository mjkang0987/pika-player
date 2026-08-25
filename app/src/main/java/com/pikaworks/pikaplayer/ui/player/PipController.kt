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
     * 홈 제스처 도중에 시스템이 알아서 작은 창으로 넘겨 주는가.
     *
     * 이게 되면 우리가 onUserLeaveHint 에서 부를 필요가 없다. 그 콜백은 제스처가
     * **끝난 뒤** 오기 때문에 화면이 한 번 끊겼다가 창이 뜬다.
     */
    val supportsAutoEnter: Boolean =
        isSupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

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
            activity.enterPictureInPictureMode(params(videoWidth, videoHeight, isPlaying, autoEnter = false))
        }.getOrDefault(false)
    }

    /**
     * 창을 띄우지 않고 설정만 미리 올려 둔다.
     *
     * 두 가지를 한다.
     * - **버튼 모양**: 재생 상태가 바뀌면 아이콘도 따라가야 한다. 안 그러면 영상이
     *   멈춘 뒤에도 일시정지 아이콘이 남아, 눌러도 안 바뀌는 것처럼 보인다.
     * - **자동 전환**: [autoEnter] 는 홈 제스처가 *시작되기 전에* 켜져 있어야 한다.
     *   제스처가 시작된 뒤에 켜 봐야 이번 전환에는 반영되지 않는다.
     */
    fun sync(videoWidth: Int, videoHeight: Int, isPlaying: Boolean, autoEnter: Boolean) {
        if (!isSupported || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            activity.setPictureInPictureParams(params(videoWidth, videoHeight, isPlaying, autoEnter))
        }
    }

    /**
     * 자동 전환을 끈다.
     *
     * 재생 화면을 벗어난 뒤에도 켜져 있으면 보관함에서 홈을 눌러도 작은 창이 뜬다.
     * 설정은 Activity 에 남으므로 떠날 때 반드시 되돌려야 한다.
     */
    fun clearAutoEnter() {
        if (!supportsAutoEnter || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        runCatching {
            activity.setPictureInPictureParams(
                PictureInPictureParams.Builder().setAutoEnterEnabled(false).build()
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun params(
        videoWidth: Int,
        videoHeight: Int,
        isPlaying: Boolean,
        autoEnter: Boolean,
    ): PictureInPictureParams {
        val ratio = if (videoWidth > 0 && videoHeight > 0) {
            (videoWidth.toFloat() / videoHeight).coerceIn(MIN_RATIO, MAX_RATIO)
        } else {
            16f / 9f
        }
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational((ratio * 1000).toInt(), 1000))
            .setActions(listOf(togglePlayAction(isPlaying)))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter)
        }
        return builder.build()
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
