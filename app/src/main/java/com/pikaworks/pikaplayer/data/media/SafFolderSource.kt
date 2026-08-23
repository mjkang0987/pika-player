package com.pikaworks.pikaplayer.data.media

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import com.pikaworks.pikaplayer.data.db.SafMetadata
import com.pikaworks.pikaplayer.data.db.SafMetadataDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 사용자가 직접 고른 폴더를 읽는다 (Storage Access Framework).
 *
 * 미디어 권한을 거부한 사용자를 위한 경로다. 이게 없으면 권한 거부 =
 * 앱을 쓸 수 없음이 되고, 저장소 권한은 거절률이 높다 — 기획서 7.2.
 * 폴더 탐색 화면(S2)도 같은 소스를 쓴다.
 */
class SafFolderSource(
    private val context: Context,
    private val metadataDao: SafMetadataDao,
) {

    data class Entry(
        val documentUri: Uri,
        /** 하위로 내려갈 때 부모 id 로 넘기는 값. */
        val documentId: String,
        val name: String,
        val isDirectory: Boolean,
        val sizeBytes: Long,
        /** 캐시한 메타데이터가 아직 유효한지 판단하는 값. */
        val lastModifiedMs: Long,
    )

    /** 고른 폴더 자체의 이름. 탐색 화면의 최상단 표기로 쓴다. */
    suspend fun treeName(treeUri: Uri): String? = withContext(Dispatchers.IO) {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        runCatching {
            context.contentResolver.query(docUri, projection, null, null, null)
        }.getOrNull()?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }

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

            // 크기와 수정 시각은 같은 커서에서 공짜로 나온다. 따로 조회할 이유가 없다.
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
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
                        documentId = id,
                        name = name,
                        isDirectory = isDir,
                        sizeBytes = if (c.isNull(3)) 0L else c.getLong(3),
                        lastModifiedMs = if (c.isNull(4)) 0L else c.getLong(4),
                    )
                }
            }
            entries.sortedWith(compareByDescending<Entry> { it.isDirectory }.thenBy { it.name })
        }

    /**
     * 폴더 안의 동영상을 [VideoItem] 으로.
     *
     * MediaStore 와 달리 SAF 는 재생시간·해상도를 주지 않아 파일마다 코덱을 열어
     * 직접 읽어야 한다. 파일 하나에 수십 ms 라 폴더를 열 때마다 다시 하면 눈에 띈다.
     * 그래서 뽑아낸 값을 Room 에 캐시하고, 파일의 수정 시각이 그대로면 건너뛴다.
     */
    suspend fun readVideos(entries: List<Entry>): List<VideoItem> = withContext(Dispatchers.IO) {
        val files = entries.filter { !it.isDirectory }
        if (files.isEmpty()) return@withContext emptyList()

        // SQLite 는 한 쿼리의 인자 개수가 제한돼 있다. 폴더에 파일이 많으면 나눠 묻는다.
        val cached = runCatching {
            files.map { it.documentUri.toString() }
                .chunked(500)
                .flatMap { metadataDao.findAll(it) }
        }.getOrDefault(emptyList()).associateBy { it.uri }

        val fresh = mutableListOf<SafMetadata>()
        val videos = files.map { entry ->
            val key = entry.documentUri.toString()
            val hit = cached[key]?.takeIf { it.lastModifiedMs == entry.lastModifiedMs }
            val meta = hit ?: readMetadata(entry).also { fresh += it }
            VideoItem(
                // SAF 문서에는 MediaStore id 가 없다. 목록 키로만 쓰는 값이다.
                id = entry.documentUri.hashCode().toLong(),
                uri = entry.documentUri,
                displayName = entry.name,
                durationMs = meta.durationMs,
                sizeBytes = entry.sizeBytes,
                width = meta.width,
                height = meta.height,
                dateModifiedSec = entry.lastModifiedMs / 1000,
                folderName = null,
            )
        }
        if (fresh.isNotEmpty()) runCatching { metadataDao.upsertAll(fresh) }
        videos
    }

    private fun readMetadata(entry: Entry): SafMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, entry.documentUri)
            SafMetadata(
                uri = entry.documentUri.toString(),
                lastModifiedMs = entry.lastModifiedMs,
                durationMs = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L,
                width = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 0,
                height = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 0,
            )
        } catch (e: Exception) {
            // 손상된 파일이나 지원하지 않는 컨테이너. 목록에서 빼지 않고 값만 비운다.
            // 캐시에도 남긴다 — 다음에 또 열어보고 또 실패할 이유가 없다.
            SafMetadata(entry.documentUri.toString(), entry.lastModifiedMs, 0L, 0, 0)
        } finally {
            runCatching { retriever.release() }
        }
    }
}
