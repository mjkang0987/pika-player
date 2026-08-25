package com.pikaworks.pikaplayer.data.media

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 기기에서 영상 파일을 지운다.
 *
 * 우리가 만들지 않은 파일은 앱이 마음대로 지울 수 없다. 안드로이드 11(API 30)
 * 부터는 시스템이 대신 물어보는 창을 띄워 주고, 사용자가 거기서 허락해야 지워진다.
 * 그래서 이 클래스는 "지웠다/안 지웠다" 가 아니라 **무엇을 해야 하는지** 를 돌려준다.
 *
 * 그 아래 버전에는 그런 창이 없다. 우리가 만든 파일이 아니면 그냥 실패하는데,
 * 실패를 조용히 삼키면 사용자는 지워진 줄 안다. 실패도 결과로 돌려준다.
 */
class MediaDeleter(private val context: Context) {

    sealed interface Result {
        /** 바로 지워졌다. */
        data object Deleted : Result

        /** 시스템 창에서 사용자가 허락해야 한다. */
        data class NeedsConfirm(val request: IntentSender) : Result

        /** 이 기기에서는 앱이 지울 수 없다. */
        data object Denied : Result
    }

    suspend fun delete(uris: List<Uri>): Result = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext Result.Deleted

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 창을 띄우는 것은 화면 쪽 일이다. 여기서는 띄울 거리만 만든다.
            return@withContext Result.NeedsConfirm(
                MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
            )
        }

        val removed = uris.count { uri -> runCatching { deleteDirect(context.contentResolver, uri) }.getOrDefault(0) > 0 }
        if (removed == uris.size) Result.Deleted else Result.Denied
    }

    private fun deleteDirect(resolver: ContentResolver, uri: Uri): Int =
        resolver.delete(uri, null, null)

    companion object {
        /** 시스템 창에서 허락했는지. [Activity.RESULT_OK] 면 지워졌다. */
        fun confirmed(resultCode: Int): Boolean = resultCode == Activity.RESULT_OK
    }
}
