package com.pikaworks.pikaplayer.data.media

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/**
 * MediaStore 에서 기기 내 동영상 목록을 읽는다.
 *
 * 이 경로는 READ_MEDIA_VIDEO(또는 그 이전 버전의 저장소 읽기) 권한을 전제로 한다.
 * 권한을 거부한 사용자는 SAF 로 폴더를 직접 고르는 경로로 간다 — 기획서 7.2 참고.
 */
class MediaStoreSource(private val context: Context) {

    /**
     * 기기의 동영상 목록이 바뀔 때마다 신호를 낸다.
     *
     * 전에는 화면에 다시 들어올 때만 다시 읽었다. 앱을 켜 둔 채로 파일을
     * 내려받거나 옮기면 목록이 그대로여서, 사용자는 앱이 못 찾는다고 느꼈다.
     * 하위 경로까지 함께 보므로 어느 폴더에 들어와도 신호가 온다.
     */
    fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        context.contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }

    suspend fun queryVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        ) + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(MediaStore.Video.Media.RELATIVE_PATH)
        } else {
            @Suppress("DEPRECATION")
            arrayOf(MediaStore.Video.Media.DATA)
        }

        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

        val items = mutableListOf<VideoItem>()
        // 권한이 도중에 취소되면 SecurityException 이 난다. 목록이 비는 편이
        // 앱이 죽는 것보다 낫다 — 화면은 이미 빈 상태를 다룬다.
        //
        // 다만 조용히 삼키기만 하면 "목록이 왜 비었나"를 밖에서 알 길이 없다.
        // 빈 목록은 권한 거부·조회 실패·정말 영상이 없음이 모두 같은 모습이다.
        // 남기는 곳은 여기 한 군데뿐이므로 로그로 구분한다.
        runCatching {
            context.contentResolver.query(collection, projection, null, null, sortOrder)
        }.onFailure {
            Log.w(TAG, "MediaStore 조회 실패", it)
        }.getOrNull()?.use { c ->
            Log.i(TAG, "MediaStore 조회: ${c.count}건")
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val wCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val hCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val bucketCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                c.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                c.getColumnIndex(MediaStore.Video.Media.DATA)
            }

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                items += VideoItem(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    displayName = c.getString(nameCol) ?: continue,
                    durationMs = c.getLong(durCol),
                    sizeBytes = c.getLong(sizeCol),
                    width = c.getInt(wCol),
                    height = c.getInt(hCol),
                    dateModifiedSec = c.getLong(dateCol),
                    folderName = c.getString(bucketCol),
                    relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && pathCol >= 0) {
                        c.getString(pathCol)
                    } else null,
                    filePath = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && pathCol >= 0) {
                        c.getString(pathCol)
                    } else null,
                )
            }
        }
        Log.i(TAG, "읽어들인 영상: ${items.size}건")
        items
    }

    private companion object {
        const val TAG = "PikaMedia"
    }
}
