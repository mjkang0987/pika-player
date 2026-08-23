package com.pikaworks.pikaplayer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pikaworks.pikaplayer.data.db.PlaybackPosition
import com.pikaworks.pikaplayer.data.db.PlaybackPositionDao
import com.pikaworks.pikaplayer.data.media.LibraryRow
import android.net.Uri
import com.pikaworks.pikaplayer.data.media.DeviceStorage
import com.pikaworks.pikaplayer.data.media.MediaStoreSource
import com.pikaworks.pikaplayer.data.media.SafFolderSource
import com.pikaworks.pikaplayer.data.media.StorageUsage
import com.pikaworks.pikaplayer.data.media.sortedFor
import com.pikaworks.pikaplayer.data.prefs.SortOrder
import com.pikaworks.pikaplayer.data.media.VideoItem
import com.pikaworks.pikaplayer.data.subtitle.SubtitleIndex
import com.pikaworks.pikaplayer.data.subtitle.SubtitleMatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ContinueItem(
    val video: VideoItem,
    val positionMs: Long,
) {
    val progress: Float
        get() = if (video.durationMs > 0) {
            (positionMs.toFloat() / video.durationMs).coerceIn(0f, 1f)
        } else 0f
}

/**
 * 보관함 상단 띠의 역할.
 *
 * 시안에는 여기에 영상 / 폴더 / 최근 / 자막 이 있었는데, 폴더와 최근은 하단
 * 네비게이션과 정확히 같은 곳으로 간다. 같은 화면에 두 개의 길을 두면 어느
 * 쪽이 지금 위치인지 흐려진다. 그래서 이 띠는 이동이 아니라 **목록 거르기**로
 * 정리했다. 자막은 시안에 있었고 다른 데서 갈 수 없는 유일한 항목이라 남겼다.
 */
object LibraryFilter {
    const val ALL = "all"
    const val SUBTITLE = "subtitle"
    const val WATCHING = "watching"
    const val UNWATCHED = "unwatched"

    val ORDER = listOf(ALL, SUBTITLE, WATCHING, UNWATCHED)

    fun label(filter: String): String = when (filter) {
        SUBTITLE -> "자막"
        WATCHING -> "보던 중"
        UNWATCHED -> "안 봄"
        else -> "영상"
    }

    fun matches(filter: String, row: LibraryRow): Boolean = when (filter) {
        SUBTITLE -> row.subtitleFormat != null
        WATCHING -> row.progress != null
        UNWATCHED -> row.positionMs == 0L
        else -> true
    }
}

data class LibraryUiState(
    val loading: Boolean = true,
    val continueWatching: List<ContinueItem> = emptyList(),
    val rows: List<LibraryRow> = emptyList(),
    /** 최근 탭. 재생 이력이 있는 것만 최근 순으로. */
    val recent: List<LibraryRow> = emptyList(),
    /** 아직 못 읽었으면 null */
    val storage: StorageUsage? = null,
    val sort: String = SortOrder.DATE_DESC,
    val filter: String = LibraryFilter.ALL,
    /** 검색어(S7). 비어 있으면 검색하지 않는다. */
    val query: String = "",
) {
    /** 검색어에 걸린 것들. 거르기 탭의 개수도 이 범위에서 센다. */
    private val searched: List<LibraryRow>
        get() = if (query.isBlank()) rows else rows.filter { row ->
            row.video.displayName.contains(query, ignoreCase = true) ||
                row.video.folderName?.contains(query, ignoreCase = true) == true
        }

    /** 실제로 목록에 그릴 것들. 재생 대기열도 여기서 나온다. */
    val visibleRows: List<LibraryRow> get() = searched.filter { LibraryFilter.matches(filter, it) }

    fun countOf(filter: String): Int = searched.count { LibraryFilter.matches(filter, it) }
}

class LibraryViewModel(
    private val mediaStore: MediaStoreSource,
    private val safFolders: SafFolderSource,
    private val positionDao: PlaybackPositionDao,
    private val subtitleMatcher: SubtitleMatcher,
    private val deviceStorage: DeviceStorage,
) : ViewModel() {

    private val videos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val subtitles = MutableStateFlow(SubtitleIndex.EMPTY)
    private val storage = MutableStateFlow<StorageUsage?>(null)
    private val sort = MutableStateFlow(SortOrder.DATE_DESC)
    private val loading = MutableStateFlow(true)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        // combine 은 한 번에 다섯 개까지다. 목록과 직접 관계없는 값들을 먼저 묶는다.
        val extras = combine(subtitles, storage, sort) { subs, space, order ->
            Extras(subs, space, order)
        }
        viewModelScope.launch {
            combine(videos, positionDao.observeAll(), extras, loading) { list, positions, extra, isLoading ->
                val byUri: Map<String, PlaybackPosition> = positions.associateBy { it.uri }

                val rows = list.sortedFor(extra.sort).map { video ->
                    LibraryRow(
                        video = video,
                        positionMs = byUri[video.uri.toString()]?.positionMs ?: 0L,
                        subtitleFormat = extra.subtitles.formatOf(video),
                    )
                }

                // 이어보기: 시작만 했거나 거의 끝난 것은 뺀다. 최근 순 최대 10개.
                val continueItems = rows
                    .filter { it.progress?.let { p -> p in 0.02f..0.97f } == true }
                    .sortedByDescending { byUri[it.video.uri.toString()]?.updatedAtMs ?: 0L }
                    .take(10)
                    .map { ContinueItem(it.video, it.positionMs) }

                val recent = rows
                    .filter { byUri.containsKey(it.video.uri.toString()) }
                    .sortedByDescending { byUri[it.video.uri.toString()]?.updatedAtMs ?: 0L }

                LibraryUiState(
                    loading = isLoading,
                    continueWatching = continueItems,
                    rows = rows,
                    recent = recent,
                    storage = extra.storage,
                    sort = extra.sort,
                )
            }.collect { next ->
                // 위 combine 은 상태를 통째로 새로 만든다. 거르기와 검색어는 화면에서
                // 온 값이라, 여기서 넘겨받지 않으면 목록이 갱신될 때마다 초기화된다.
                val current = _uiState.value
                _uiState.value = next.copy(filter = current.filter, query = current.query)
            }
        }
    }

    fun setSort(order: String) {
        sort.value = order
    }

    /** 거르기와 검색어는 화면을 벗어나면 잊는다. 설정처럼 오래 남을 값이 아니다. */
    fun setFilter(value: String) {
        _uiState.value = _uiState.value.copy(filter = value)
    }

    fun setQuery(value: String) {
        _uiState.value = _uiState.value.copy(query = value)
    }

    /** 권한을 받은 뒤, 그리고 화면에 돌아올 때마다 호출한다. */
    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            videos.value = mediaStore.queryVideos()
            loading.value = false
        }
        // 목록을 막지 않는다. 배지와 용량은 준비되는 대로 나중에 붙어도 된다.
        viewModelScope.launch { subtitles.value = subtitleMatcher.indexAll() }
        viewModelScope.launch { storage.value = deviceStorage.read() }
    }

    /**
     * 권한 대신 사용자가 고른 폴더에서 읽는다.
     *
     * SAF 는 재생시간·해상도를 주지 않아 파일마다 직접 읽어야 하므로
     * MediaStore 경로보다 느리다. 폴더 하나 분량이라 감수한다.
     */
    fun loadFolder(treeUri: Uri) {
        viewModelScope.launch { storage.value = deviceStorage.read() }
        viewModelScope.launch {
            loading.value = true
            val entries = safFolders.listChildren(treeUri)
            videos.value = safFolders.readVideos(entries)
            loading.value = false
        }
    }

    /** combine 인자 수를 줄이려고 묶은 값. 화면에는 풀어서 내보낸다. */
    private data class Extras(
        val subtitles: SubtitleIndex,
        val storage: StorageUsage?,
        val sort: String,
    )

    class Factory(
        private val mediaStore: MediaStoreSource,
        private val safFolders: SafFolderSource,
        private val positionDao: PlaybackPositionDao,
        private val subtitleMatcher: SubtitleMatcher,
        private val deviceStorage: DeviceStorage,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LibraryViewModel(mediaStore, safFolders, positionDao, subtitleMatcher, deviceStorage) as T
    }
}
