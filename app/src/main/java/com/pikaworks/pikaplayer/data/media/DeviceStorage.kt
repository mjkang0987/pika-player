package com.pikaworks.pikaplayer.data.media

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 남은 용량 / 전체 용량. 둘 다 바이트. */
data class StorageUsage(val freeBytes: Long, val totalBytes: Long) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)
    /** 0f..1f. 전체를 모르면 0f */
    val usedRatio: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}

/**
 * 영상이 놓이는 저장소의 잔량.
 *
 * `Environment.getExternalStorageDirectory()` 는 deprecated 라 앱 전용 디렉터리
 * 경로로 같은 볼륨을 잰다. 권한도 필요 없고 사용자가 보는 "내부 저장소" 와 같은 값이다.
 */
class DeviceStorage(private val context: Context) {

    suspend fun read(): StorageUsage? = withContext(Dispatchers.IO) {
        val path = context.getExternalFilesDir(null)?.path
            ?: context.filesDir?.path
            ?: return@withContext null
        runCatching {
            val stat = StatFs(path)
            StorageUsage(
                freeBytes = stat.availableBlocksLong * stat.blockSizeLong,
                totalBytes = stat.blockCountLong * stat.blockSizeLong,
            )
        }.getOrNull()
    }
}
