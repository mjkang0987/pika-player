package com.pikaworks.pikaplayer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pikaworks.pikaplayer.data.db.PlaybackPosition
import com.pikaworks.pikaplayer.data.db.PlaybackPositionDao
import com.pikaworks.pikaplayer.data.media.FolderOption
import com.pikaworks.pikaplayer.data.media.LibraryRow
import android.net.Uri
import com.pikaworks.pikaplayer.data.media.DeviceStorage
import com.pikaworks.pikaplayer.data.media.MediaRescanner
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
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
    /**
     * 비공개 폴더에서 고를 수 있는 폴더 전체.
     *
     * 감춘 폴더도 들어 있어야 한다 — 목록에서 사라진 폴더를 다시 꺼낼 방법이
     * 없으면 한 번 감춘 것을 영영 되돌리지 못한다.
     */
    val allFolders: List<FolderOption> = emptyList(),
    /** 사용자가 부른 다시 검색이 도는 중. 빈 화면의 버튼을 잠근다. */
    val scanning: Boolean = false,
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

@OptIn(FlowPreview::class)
class LibraryViewModel(
    private val mediaStore: MediaStoreSource,
    private val safFolders: SafFolderSource,
    private val positionDao: PlaybackPositionDao,
    private val subtitleMatcher: SubtitleMatcher,
    private val deviceStorage: DeviceStorage,
    private val rescanner: MediaRescanner,
) : ViewModel() {

    private val videos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val subtitles = MutableStateFlow(SubtitleIndex.EMPTY)
    private val storage = MutableStateFlow<StorageUsage?>(null)
    private val sort = MutableStateFlow(SortOrder.DATE_DESC)
    /** 비공개 폴더로 감춘 folderKey. 잠금을 푼 동안에는 비어 있다. */
    private val hiddenFolders = MutableStateFlow<Set<String>>(emptySet())
    private val loading = MutableStateFlow(true)
    private val scanning = MutableStateFlow(false)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    /**
     * MediaStore 를 보고 있으면 참, 사용자가 고른 SAF 폴더를 보고 있으면 거짓.
     * SAF 목록은 MediaStore 와 무관하므로 그때는 변경 신호를 흘려보낸다.
     */
    private var watchingMediaStore = false

    init {
        // combine 은 한 번에 다섯 개까지다. 목록과 직접 관계없는 값들을 먼저 묶는다.
        val extras = combine(subtitles, storage, sort, hiddenFolders, scanning) { subs, space, order, hidden, isScanning ->
            Extras(subs, space, order, hidden, isScanning)
        }
        // 파일 하나가 들어와도 MediaStore 는 신호를 여러 번 보낸다. 잠깐 모아
        // 마지막 것만 처리한다. 화면에 없을 때도 도는 대신, 다시 들어왔을 때
        // 목록이 이미 맞아 있다.
        viewModelScope.launch {
            mediaStore.changes()
                .debounce(500)
                .collect { if (watchingMediaStore) reload(showLoading = false) }
        }

        viewModelScope.launch {
            combine(videos, positionDao.observeAll(), extras, loading) { list, positions, extra, isLoading ->
                val byUri: Map<String, PlaybackPosition> = positions.associateBy { it.uri }

                val rows = list
                    // 감춘 폴더는 목록에 아예 올리지 않는다. 이어보기·최근도 같은
                    // 목록에서 파생되므로 여기서 한 번만 걸러도 전부 반영된다.
                    .filter { it.folderKey !in extra.hiddenFolders }
                    .sortedFor(extra.sort)
                    .map { video ->
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

                // 거르기 전 목록에서 만든다. 감춘 폴더가 후보에서 빠지면 안 된다.
                val allFolders = list
                    .mapNotNull { video -> video.folderKey?.let { it to video } }
                    .groupBy({ it.first }, { it.second })
                    .map { (key, items) ->
                        FolderOption(key, items.first().folderName ?: key, items.size)
                    }
                    .sortedBy { it.name.lowercase() }

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
                    allFolders = allFolders,
                    scanning = extra.scanning,
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

    fun setHiddenFolders(keys: Set<String>) {
        hiddenFolders.value = keys
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
        watchingMediaStore = true
        reload(showLoading = true)
    }

    /**
     * @param showLoading 사용자가 부른 조회에서만 참. 파일이 추가돼서 저절로
     *   다시 읽는 경우에는 목록이 이미 떠 있으므로 자리 표시자로 덮지 않는다.
     */
    private fun reload(showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) loading.value = true
            videos.value = mediaStore.queryVideos()
            loading.value = false
        }
        // 목록을 막지 않는다. 배지와 용량은 준비되는 대로 나중에 붙어도 된다.
        viewModelScope.launch { subtitles.value = subtitleMatcher.indexAll() }
        viewModelScope.launch { storage.value = deviceStorage.read() }
    }

    /**
     * 파일은 있는데 목록에 안 나올 때의 탈출구.
     *
     * MediaStore 를 다시 훑게 한 뒤 그 결과로 목록을 새로 만든다. 훑는 동안
     * 변경 신호도 오지만 그건 마지막 것만 처리하므로, 끝난 뒤 한 번 더 읽어
     * 결과가 확실히 반영되게 한다.
     */
    fun rescan() {
        if (scanning.value) return
        viewModelScope.launch {
            scanning.value = true
            runCatching { rescanner.rescan() }
            scanning.value = false
            if (watchingMediaStore) reload(showLoading = false)
        }
    }

    /**
     * 권한 대신 사용자가 고른 폴더에서 읽는다.
     *
     * SAF 는 재생시간·해상도를 주지 않아 파일마다 직접 읽어야 하므로
     * MediaStore 경로보다 느리다. 폴더 하나 분량이라 감수한다.
     */
    fun loadFolder(treeUri: Uri) {
        watchingMediaStore = false
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
        val hiddenFolders: Set<String>,
        val scanning: Boolean,
    )

    class Factory(
        private val mediaStore: MediaStoreSource,
        private val safFolders: SafFolderSource,
        private val positionDao: PlaybackPositionDao,
        private val subtitleMatcher: SubtitleMatcher,
        private val deviceStorage: DeviceStorage,
        private val rescanner: MediaRescanner,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LibraryViewModel(mediaStore, safFolders, positionDao, subtitleMatcher, deviceStorage, rescanner) as T
    }
}
