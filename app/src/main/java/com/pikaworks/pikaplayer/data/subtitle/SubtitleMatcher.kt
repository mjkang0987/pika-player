package com.pikaworks.pikaplayer.data.subtitle

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.pikaworks.pikaplayer.data.media.VideoItem
import com.pikaworks.pikaplayer.subtitle.SubtitleFormat
import com.pikaworks.pikaplayer.subtitle.SubtitleLoader
import com.pikaworks.pikaplayer.subtitle.SubtitleTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 영상과 같은 폴더에서 같은 이름의 자막 파일을 찾는다.
 *
 * `제주도_여행_2일차.mp4` 옆의 `제주도_여행_2일차.srt` / `.smi` 를 집는 것.
 * 카테고리에서 사실상 표준인 동작이라 이게 안 되면 "자막이 안 붙는다"가 된다.
 */
class SubtitleMatcher(private val context: Context) {

    data class Match(
        val uri: Uri,
        val displayName: String,
        val format: SubtitleFormat,
    )

    /** 영상 옆에 놓인 자막 파일 후보들. 없으면 빈 목록. */
    suspend fun findFor(video: VideoItem): List<Match> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
        )

        // 같은 폴더 + 같은 기본 이름으로 좁힌다. 확장자 판별은 결과에서 한다.
        val (selection, args) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val path = video.relativePath ?: return@withContext emptyList()
            "${MediaStore.Files.FileColumns.RELATIVE_PATH} = ? AND " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?" to
                arrayOf(path, "${video.baseName}.%")
        } else {
            @Suppress("DEPRECATION")
            val dir = video.filePath?.let { File(it).parent } ?: return@withContext emptyList()
            @Suppress("DEPRECATION")
            "${MediaStore.Files.FileColumns.DATA} LIKE ?" to arrayOf("$dir/${video.baseName}.%")
        }

        val matches = mutableListOf<Match>()
        context.contentResolver.query(collection, projection, selection, args, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            while (c.moveToNext()) {
                val name = c.getString(nameCol) ?: continue
                val format = SubtitleFormat.fromFileName(name) ?: continue
                matches += Match(
                    uri = ContentUris.withAppendedId(collection, c.getLong(idCol)),
                    displayName = name,
                    format = format,
                )
            }
        }
        matches
    }

    /**
     * 자막 파일을 읽어 트랙으로 만든다.
     *
     * 인코딩 판별은 [SubtitleLoader] 가 한다 — 경로만 넘기면 CP949 파일이 깨진다.
     * 그래서 여기서 바이트를 통째로 읽어 넘긴다. 자막 파일은 커봐야 수백 KB다.
     */
    suspend fun load(match: Match, forcedCharsetName: String? = null): SubtitleTrack? =
        withContext(Dispatchers.IO) {
            val bytes = runCatching {
                context.contentResolver.openInputStream(match.uri)?.use { it.readBytes() }
            }.getOrNull() ?: return@withContext null

            val forced = forcedCharsetName?.let {
                runCatching { charset(it) }.getOrNull()
            }
            val loaded = SubtitleLoader.load(match.displayName, bytes, forced)
                ?: return@withContext null
            SubtitleTrack(loaded.cues)
        }
}
