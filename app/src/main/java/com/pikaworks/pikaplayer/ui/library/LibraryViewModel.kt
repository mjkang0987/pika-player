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

data class LibraryUiState(
    val loading: Boolean = true,
    val continueWatching: List<ContinueItem> = emptyList(),
    val rows: List<LibraryRow> = emptyList(),
    /** 최근 탭. 재생 이력이 있는 것만 최근 순으로. */
    val recent: List<LibraryRow> = emptyList(),
    /** 아직 못 읽었으면 null */
    val storage: StorageUsage? = null,
) {
    val videoCount: Int get() = rows.size
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
    private val loading = MutableStateFlow(true)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(videos, positionDao.observeAll(), subtitles, storage, loading) { list, positions, subs, space, isLoading ->
                val byUri: Map<String, PlaybackPosition> = positions.associateBy { it.uri }

                val rows = list.map { video ->
                    LibraryRow(
                        video = video,
                        positionMs = byUri[video.uri.toString()]?.positionMs ?: 0L,
                        subtitleFormat = subs.formatOf(video),
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
                    storage = space,
                )
            }.collect { _uiState.value = it }
        }
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
