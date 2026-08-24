package com.pikaworks.pikaplayer.data.media

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * 기기에 있는데 MediaStore 가 모르는 파일을 다시 훑게 한다.
 *
 * 보통은 필요 없다. 브라우저 내려받기·카메라·메신저·USB 전송은 모두 파일을
 * 만들면서 MediaStore 에 알린다. 문제가 되는 건 그 경로를 거치지 않고 파일이
 * 생기는 경우다 — adb push, 루팅된 기기의 파일 관리자, 색인이 깨진 기기.
 * 그런 사용자에게 "파일은 분명히 있는데 앱이 못 본다"는 막다른 길이 생긴다.
 * 이 클래스는 그 탈출구다.
 *
 * 훑는 일은 MediaProvider 가 자기 권한으로 한다. 우리가 폴더 안을 직접 볼 수
 * 없어도(범위 지정 저장소) 경로만 넘기면 되는 이유다.
 */
class MediaRescanner(private val context: Context) {

    /** 훑고 나서 돌아온다. 사용자가 직접 누른 동작이므로 끝날 때까지 기다린다. */
    suspend fun rescan() = withContext(Dispatchers.IO) {
        for (path in targets()) {
            runCatching { scan(path) }
        }
    }

    /**
     * 공유 저장소 최상단 하나만 넘긴다. MediaProvider 가 그 아래를 재귀로 돈다.
     *
     * 표준 폴더(Movies·Download·DCIM)만 고르면 빠르지만, 사용자가 직접 만든
     * 폴더에 둔 영상을 놓친다. 그걸 놓치면 이 기능이 존재할 이유가 없다.
     */
    private fun targets(): List<String> {
        @Suppress("DEPRECATION")
        val root = runCatching { Environment.getExternalStorageDirectory() }.getOrNull()
        if (root != null) return listOf(root.absolutePath)

        // 최상단을 못 얻는 기기를 위한 대비책. 표준 폴더라도 훑는다.
        return listOf(
            Environment.DIRECTORY_MOVIES,
            Environment.DIRECTORY_DOWNLOADS,
            Environment.DIRECTORY_DCIM,
            Environment.DIRECTORY_PICTURES,
        ).mapNotNull { dir ->
            @Suppress("DEPRECATION")
            runCatching { Environment.getExternalStoragePublicDirectory(dir)?.absolutePath }.getOrNull()
        }
    }

    private suspend fun scan(path: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 이 호출은 훑기가 끝나야 돌아온다. 이미 IO 문맥이다.
            MediaStore.scanFile(context.contentResolver, File(path))
            return
        }
        // API 28 이하에는 위 API 가 없다. 서비스에 붙어서 콜백을 기다린다.
        suspendCancellableCoroutine<Unit> { cont ->
            MediaScannerConnection.scanFile(context, arrayOf(path), null) { _, _ ->
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }
}
