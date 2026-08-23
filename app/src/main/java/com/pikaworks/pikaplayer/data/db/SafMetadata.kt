package com.pikaworks.pikaplayer.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

/**
 * SAF 로 연 파일에서 뽑아낸 메타데이터 캐시.
 *
 * MediaStore 와 달리 SAF 는 재생시간·해상도를 주지 않아 파일마다
 * `MediaMetadataRetriever` 로 직접 읽어야 한다. 코덱을 열었다 닫는 작업이라
 * 파일 하나에 수십 ms 씩 걸리고, 폴더를 열 때마다 처음부터 다시 한다.
 *
 * 파일이 바뀌었는지는 [lastModifiedMs] 로 판단한다. 값이 다르면 다시 읽는다.
 */
@Entity(tableName = "saf_metadata")
data class SafMetadata(
    @PrimaryKey val uri: String,
    val lastModifiedMs: Long,
    val durationMs: Long,
    val width: Int,
    val height: Int,
)

@Dao
interface SafMetadataDao {

    @Query("SELECT * FROM saf_metadata WHERE uri IN (:uris)")
    suspend fun findAll(uris: List<String>): List<SafMetadata>

    @Upsert
    suspend fun upsertAll(items: List<SafMetadata>)
}
