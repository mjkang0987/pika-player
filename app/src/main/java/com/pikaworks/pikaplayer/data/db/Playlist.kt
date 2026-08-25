package com.pikaworks.pikaplayer.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** 사용자가 직접 묶은 목록. 폴더와 무관하게 아무 영상이나 담을 수 있다. */
@Entity(tableName = "playlist")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtMs: Long,
)

/**
 * 재생목록에 담긴 영상 하나.
 *
 * uri 를 키로 쓰는 이유는 [PlaybackPosition] 과 같다 — MediaStore id 는 재스캔에
 * 바뀔 수 있고 SAF 파일에는 아예 없다.
 *
 * 이름을 같이 저장한다. 파일이 사라지거나 아직 목록을 못 읽었을 때도 무엇이
 * 담겨 있었는지는 보여 줘야 한다. 빈 줄만 남으면 지울 수도 없다.
 *
 * 목록이 지워지면 담긴 것도 함께 지운다(CASCADE). 남겨 두면 아무도 참조하지
 * 않는 줄이 쌓인다.
 */
@Entity(
    tableName = "playlist_item",
    primaryKeys = ["playlistId", "uri"],
    indices = [Index("playlistId")],
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PlaylistItem(
    val playlistId: Long,
    val uri: String,
    val displayName: String,
    /** 목록 안 순서. 빈틈이 생겨도 되므로 다시 매기지 않는다. */
    val position: Int,
)

/** 목록 화면에 쓰는 요약. 안에 몇 개 있는지까지 한 번에 읽는다. */
data class PlaylistSummary(
    val id: Long,
    val name: String,
    val itemCount: Int,
)

@Dao
interface PlaylistDao {

    @Query(
        """
        SELECT p.id AS id, p.name AS name,
               (SELECT COUNT(*) FROM playlist_item i WHERE i.playlistId = p.id) AS itemCount
        FROM playlist p
        ORDER BY p.createdAtMs DESC
        """
    )
    fun observeAll(): Flow<List<PlaylistSummary>>

    @Query("SELECT * FROM playlist_item WHERE playlistId = :playlistId ORDER BY position")
    fun observeItems(playlistId: Long): Flow<List<PlaylistItem>>

    @Insert
    suspend fun create(playlist: Playlist): Long

    @Query("UPDATE playlist SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM playlist WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM playlist_item WHERE playlistId = :playlistId AND uri = :uri")
    suspend fun removeItem(playlistId: Long, uri: String)

    /** 이미 담긴 영상인지. 담기 시트에서 체크 표시에 쓴다. */
    @Query("SELECT playlistId FROM playlist_item WHERE uri = :uri")
    fun observePlaylistsContaining(uri: String): Flow<List<Long>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_item WHERE playlistId = :playlistId")
    suspend fun lastPosition(playlistId: Long): Int

    @Insert
    suspend fun insertItem(item: PlaylistItem)

    /**
     * 맨 뒤에 담는다. 이미 있으면 아무 일도 하지 않는다.
     *
     * 같은 영상을 한 목록에 두 번 담는 것은 실수일 가능성이 높다. 조용히
     * 넘기는 편이 중복 줄을 만드는 것보다 낫다.
     */
    @Transaction
    suspend fun addItem(playlistId: Long, uri: String, displayName: String) {
        if (contains(playlistId, uri)) return
        insertItem(PlaylistItem(playlistId, uri, displayName, lastPosition(playlistId) + 1))
    }

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_item WHERE playlistId = :playlistId AND uri = :uri)")
    suspend fun contains(playlistId: Long, uri: String): Boolean

    @Query("UPDATE playlist_item SET position = :position WHERE playlistId = :playlistId AND uri = :uri")
    suspend fun setPosition(playlistId: Long, uri: String, position: Int)

    /**
     * 이웃한 둘의 자리를 맞바꾼다.
     *
     * 끌어서 옮기는 대신 위·아래 버튼을 쓴다. 목록에서 끌어 옮기는 것은
     * 스크롤과 부딪혀 다루기 까다롭고, 만들 것도 훨씬 많다.
     */
    @Transaction
    suspend fun swap(playlistId: Long, a: PlaylistItem, b: PlaylistItem) {
        setPosition(playlistId, a.uri, b.position)
        setPosition(playlistId, b.uri, a.position)
    }
}
