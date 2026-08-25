package com.pikaworks.pikaplayer.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * 이어보기용 재생 위치.
 *
 * uri 를 키로 쓴다 — MediaStore id 는 재스캔 시 바뀔 수 있고,
 * SAF 로 연 파일은 애초에 MediaStore id 가 없다.
 */
@Entity(tableName = "playback_position")
data class PlaybackPosition(
    @PrimaryKey val uri: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAtMs: Long,
)

@Dao
interface PlaybackPositionDao {

    @Upsert
    suspend fun upsert(position: PlaybackPosition)

    @Query("SELECT * FROM playback_position WHERE uri = :uri")
    suspend fun find(uri: String): PlaybackPosition?

    /** 이어보기 캐러셀용. 끝까지 본 것은 제외하고 최근 순으로 최대 10개 */
    @Query(
        """
        SELECT * FROM playback_position
        WHERE durationMs > 0 AND positionMs * 100 / durationMs BETWEEN 2 AND 97
        ORDER BY updatedAtMs DESC
        LIMIT 10
        """
    )
    fun observeContinueWatching(): Flow<List<PlaybackPosition>>

    @Query("SELECT * FROM playback_position")
    fun observeAll(): Flow<List<PlaybackPosition>>

    /** 이어보기에서 지운다. 다시 틀면 처음부터. */
    @Query("DELETE FROM playback_position WHERE uri = :uri")
    suspend fun delete(uri: String)
}

@Database(
    entities = [PlaybackPosition::class, SafMetadata::class, Playlist::class, PlaylistItem::class],
    version = 3,
    // 스키마를 app/schemas/3.json 으로 남긴다. 다음 버전의 마이그레이션은
    // 이 파일을 기준으로 쓴다 — 코드를 보고 손으로 옮겨 적으면 컬럼 하나를
    // 빠뜨려도 알아챌 방법이 없다.
    exportSchema = true,
)
abstract class PikaDatabase : RoomDatabase() {
    abstract fun playbackPositionDao(): PlaybackPositionDao
    abstract fun safMetadataDao(): SafMetadataDao
    abstract fun playlistDao(): PlaylistDao
}
