package com.pikaworks.pikaplayer.data.media

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 사용자가 직접 고른 폴더를 읽는다 (Storage Access Framework).
 *
 * 미디어 권한을 거부한 사용자를 위한 경로다. 이게 없으면 권한 거부 =
 * 앱을 쓸 수 없음이 되고, 저장소 권한은 거절률이 높다 — 기획서 7.2.
 * 폴더 탐색 화면(S2)도 같은 소스를 쓴다.
 */
class SafFolderSource(private val context: Context) {

    data class Entry(
        val documentUri: Uri,
        val name: String,
        val isDirectory: Boolean,
    )

    /** 고른 폴더의 접근 권한을 앱 재시작 후에도 유지한다. */
    fun persistPermission(treeUri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    fun hasPermission(treeUri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isReadPermission
        }

    /** 폴더 한 단계의 내용. 하위 폴더까지 파고들지 않는다 — 화면이 한 단계씩 이동한다. */
    suspend fun listChildren(treeUri: Uri, parentDocumentId: String? = null): List<Entry> =
        withContext(Dispatchers.IO) {
            val docId = parentDocumentId ?: DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            )

            val entries = mutableListOf<Entry>()
            runCatching {
                context.contentResolver.query(childrenUri, projection, null, null, null)
            }.getOrNull()?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val name = c.getString(1) ?: continue
                    val mime = c.getString(2) ?: ""
                    val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                    if (!isDir && !mime.startsWith("video/")) continue
                    entries += Entry(
                        documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id),
                        name = name,
                        isDirectory = isDir,
                    )
                }
            }
            entries.sortedWith(compareByDescending<Entry> { it.isDirectory }.thenBy { it.name })
        }

    /**
     * 폴더 안의 동영상을 [VideoItem] 으로.
     *
     * MediaStore 와 달리 SAF 는 재생시간·해상도를 주지 않아 파일마다 직접 읽어야 한다.
     * 파일 수가 많으면 느리다.
     * TODO: 추출한 메타데이터를 Room 에 캐시해 두 번째부터는 건너뛸 것.
     */
    suspend fun readVideos(entries: List<Entry>): List<VideoItem> = withContext(Dispatchers.IO) {
        entries.filter { !it.isDirectory }.mapIndexedNotNull { index, entry ->
            val meta = readMetadata(entry.documentUri)
            VideoItem(
                // SAF 문서에는 MediaStore id 가 없다. 목록 키로만 쓰는 값이다.
                id = entry.documentUri.hashCode().toLong(),
                uri = entry.documentUri,
                displayName = entry.name,
                durationMs = meta.durationMs,
                sizeBytes = meta.sizeBytes,
                width = meta.width,
                height = meta.height,
                dateModifiedSec = 0L,
                folderName = null,
            )
        }
    }

    private data class Meta(
        val durationMs: Long,
        val sizeBytes: Long,
        val width: Int,
        val height: Int,
    )

    private fun readMetadata(uri: Uri): Meta {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            Meta(
                durationMs = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L,
                sizeBytes = 0L, // SAF 에서 크기는 별도 조회가 필요하다. 목록 표시에는 없어도 된다.
                width = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 0,
                height = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 0,
            )
        } catch (e: Exception) {
            // 손상된 파일이나 지원하지 않는 컨테이너. 목록에서 빼지 않고 값만 비운다.
            Meta(0L, 0L, 0, 0)
        } finally {
            runCatching { retriever.release() }
        }
    }
}
