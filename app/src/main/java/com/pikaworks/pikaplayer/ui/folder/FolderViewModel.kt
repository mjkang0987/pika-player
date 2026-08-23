package com.pikaworks.pikaplayer.ui.folder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pikaworks.pikaplayer.data.media.MediaStoreSource
import com.pikaworks.pikaplayer.data.media.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FolderSummary(
    val name: String,
    val videoCount: Int,
    val totalBytes: Long,
)

data class FolderUiState(
    val loading: Boolean = true,
    /** null 이면 폴더 목록, 값이 있으면 그 폴더 안 */
    val openedFolder: String? = null,
    val folders: List<FolderSummary> = emptyList(),
    val videos: List<VideoItem> = emptyList(),
) {
    val totalVideoCount: Int get() = folders.sumOf { it.videoCount }
}

/**
 * 폴더 탐색(S2).
 *
 * MediaStore 는 이미 영상마다 폴더 이름을 준다. 파일시스템을 다시 훑을 필요 없이
 * 그걸로 묶으면 된다. SAF 로 고른 폴더를 쓰는 경우는 [SafFolderSource] 쪽 경로다.
 */
class FolderViewModel(
    private val mediaStore: MediaStoreSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderUiState())
    val uiState: StateFlow<FolderUiState> = _uiState.asStateFlow()

    private var allVideos: List<VideoItem> = emptyList()

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            allVideos = mediaStore.queryVideos()
            rebuild()
        }
    }

    fun open(folder: String) {
        _uiState.value = _uiState.value.copy(
            openedFolder = folder,
            videos = allVideos.filter { it.folderName == folder },
        )
    }

    /** 상위로. 이미 최상단이면 false 를 돌려줘 뒤로가기를 화면 밖으로 넘긴다. */
    fun goUp(): Boolean {
        if (_uiState.value.openedFolder == null) return false
        _uiState.value = _uiState.value.copy(openedFolder = null, videos = emptyList())
        return true
    }

    private fun rebuild() {
        val folders = allVideos
            .groupBy { it.folderName ?: "기타" }
            .map { (name, items) ->
                FolderSummary(name, items.size, items.sumOf { it.sizeBytes })
            }
            .sortedBy { it.name }

        _uiState.value = FolderUiState(loading = false, folders = folders)
    }

    class Factory(private val mediaStore: MediaStoreSource) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FolderViewModel(mediaStore) as T
    }
}
