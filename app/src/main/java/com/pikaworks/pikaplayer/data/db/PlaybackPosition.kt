package com.pikaworks.pikaplayer.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

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
    /**
     * 이어보기 줄에서 뺀 표시.
     *
     * 위치는 그대로 두고 줄에서만 감춘다. 지워 버리면 줄을 정리한 대가로 기록을
     * 잃는다 — 나중에 그 영상을 다시 열었을 때 보던 데가 아니라 처음부터 시작한다.
     *
     * 다시 재생하면 위치를 새로 쓰면서 이 값이 false 로 돌아간다. 한 번 뺐다고
     * 영영 안 나오면, 어제 뺀 영상을 오늘 다시 보기 시작해도 이어볼 길이 없다.
     */
    val dismissedFromContinue: Boolean = false,
)

@Dao
interface PlaybackPositionDao {

    @Upsert
    suspend fun upsert(position: PlaybackPosition)

    @Query("SELECT * FROM playback_position WHERE uri = :uri")
    suspend fun find(uri: String): PlaybackPosition?

    @Query("SELECT * FROM playback_position")
    fun observeAll(): Flow<List<PlaybackPosition>>

    /** 이어보기 줄에서만 뺀다. 저장된 위치는 그대로 둔다. */
    @Query("UPDATE playback_position SET dismissedFromContinue = 1 WHERE uri = :uri")
    suspend fun dismissFromContinue(uri: String)
}

/**
 * 3 → 4: 이어보기 줄에서 뺀 표시.
 *
 * 기본값 0 이라 기존 줄은 전부 "안 뺐음" 으로 남는다. 값이 없는 컬럼을 새로
 * 붙이는 것뿐이라 데이터는 하나도 건드리지 않는다.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE playback_position " +
                "ADD COLUMN dismissedFromContinue INTEGER NOT NULL DEFAULT 0"
        )
    }
}

@Database(
    entities = [PlaybackPosition::class, SafMetadata::class, Playlist::class, PlaylistItem::class],
    version = 4,
    // 스키마를 app/schemas/<버전>.json 으로 남긴다. 다음 버전의 마이그레이션은
    // 이 파일을 기준으로 쓴다 — 코드를 보고 손으로 옮겨 적으면 컬럼 하나를
    // 빠뜨려도 알아챌 방법이 없다.
    exportSchema = true,
)
abstract class PikaDatabase : RoomDatabase() {
    abstract fun playbackPositionDao(): PlaybackPositionDao
    abstract fun safMetadataDao(): SafMetadataDao
    abstract fun playlistDao(): PlaylistDao
}
