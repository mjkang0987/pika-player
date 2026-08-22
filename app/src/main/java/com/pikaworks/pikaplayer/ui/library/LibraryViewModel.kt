package com.pikaworks.pikaplayer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pikaworks.pikaplayer.data.db.PlaybackPosition
import com.pikaworks.pikaplayer.data.db.PlaybackPositionDao
import com.pikaworks.pikaplayer.data.media.LibraryRow
import com.pikaworks.pikaplayer.data.media.MediaStoreSource
import com.pikaworks.pikaplayer.data.media.VideoItem
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
) {
    val videoCount: Int get() = rows.size
}

class LibraryViewModel(
    private val mediaStore: MediaStoreSource,
    private val positionDao: PlaybackPositionDao,
) : ViewModel() {

    private val videos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val loading = MutableStateFlow(true)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(videos, positionDao.observeAll(), loading) { list, positions, isLoading ->
                val byUri: Map<String, PlaybackPosition> = positions.associateBy { it.uri }

                val rows = list.map { video ->
                    LibraryRow(
                        video = video,
                        positionMs = byUri[video.uri.toString()]?.positionMs ?: 0L,
                        // TODO: 같은 이름의 자막 파일을 찾아 형식을 채운다 (SubtitleMatcher)
                        subtitleFormat = null,
                    )
                }

                // 이어보기: 시작만 했거나 거의 끝난 것은 뺀다. 최근 순 최대 10개.
                val continueItems = rows
                    .filter { it.progress?.let { p -> p in 0.02f..0.97f } == true }
                    .sortedByDescending { byUri[it.video.uri.toString()]?.updatedAtMs ?: 0L }
                    .take(10)
                    .map { ContinueItem(it.video, it.positionMs) }

                LibraryUiState(loading = isLoading, continueWatching = continueItems, rows = rows)
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
    }

    class Factory(
        private val mediaStore: MediaStoreSource,
        private val positionDao: PlaybackPositionDao,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LibraryViewModel(mediaStore, positionDao) as T
    }
}
