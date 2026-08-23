package com.pikaworks.pikaplayer.ui.folder

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pikaworks.pikaplayer.data.media.MediaStoreSource
import com.pikaworks.pikaplayer.data.media.SafFolderSource
import com.pikaworks.pikaplayer.data.media.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FolderSummary(
    /** MediaStore 는 폴더 이름, SAF 는 document id. 내려갈 때 이 값을 쓴다. */
    val id: String,
    val name: String,
    /** SAF 는 하위를 열기 전에는 알 수 없다. 그때는 null. */
    val videoCount: Int?,
    val totalBytes: Long?,
)

/** 지금 어느 폴더 안에 있는지. 비어 있으면 최상단. */
data class Crumb(val id: String, val name: String)

data class FolderUiState(
    val loading: Boolean = true,
    val rootLabel: String = "내부 저장소",
    val crumbs: List<Crumb> = emptyList(),
    val folders: List<FolderSummary> = emptyList(),
    val videos: List<VideoItem> = emptyList(),
) {
    val openedFolder: Crumb? get() = crumbs.lastOrNull()
    val totalVideoCount: Int get() = folders.sumOf { it.videoCount ?: 0 }
    /** SAF 는 폴더별 영상 수를 모른다. 합계를 보여주면 0 으로 보인다. */
    val showsFolderCounts: Boolean get() = folders.all { it.videoCount != null }
}

/**
 * 폴더 탐색(S2). 두 가지 출처를 같은 화면으로 보여준다.
 *
 * - **MediaStore**: 영상마다 폴더 이름을 이미 주므로 그걸로 묶는다. 한 단계뿐이다.
 * - **SAF**: 사용자가 고른 폴더를 실제로 파고든다. 여러 단계가 될 수 있다.
 *
 * 미디어 권한을 거부한 사용자는 SAF 경로로만 앱을 쓴다. 전에는 이 화면이
 * MediaStore 만 알아서 그런 사용자에게 폴더 탭이 늘 비어 있었다.
 */
class FolderViewModel(
    private val mediaStore: MediaStoreSource,
    private val safFolders: SafFolderSource,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderUiState())
    val uiState: StateFlow<FolderUiState> = _uiState.asStateFlow()

    private var allVideos: List<VideoItem> = emptyList()
    private var mediaFolders: List<FolderSummary> = emptyList()
    /** SAF 모드일 때만 값이 있다. */
    private var treeUri: Uri? = null

    /** 미디어 권한이 있을 때. 화면에 돌아올 때마다 호출한다. */
    fun refresh() {
        treeUri = null
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            allVideos = mediaStore.queryVideos()
            mediaFolders = allVideos
                .groupBy { it.folderName ?: "기타" }
                .map { (name, items) ->
                    FolderSummary(name, name, items.size, items.sumOf { it.sizeBytes })
                }
                .sortedBy { it.name }
            _uiState.value = FolderUiState(loading = false, folders = mediaFolders)
        }
    }

    /** 권한 대신 사용자가 고른 폴더를 쓸 때. */
    fun loadTree(uri: Uri) {
        treeUri = uri
        allVideos = emptyList()
        viewModelScope.launch {
            _uiState.value = FolderUiState(loading = true, rootLabel = safFolders.treeName(uri) ?: "선택한 폴더")
            listSaf(parentDocumentId = null, crumbs = emptyList())
        }
    }

    fun open(folder: FolderSummary) {
        val crumb = Crumb(folder.id, folder.name)
        if (treeUri == null) {
            // MediaStore 는 폴더 안이 곧 영상 목록이다. 더 내려갈 곳이 없다.
            _uiState.value = _uiState.value.copy(
                crumbs = listOf(crumb),
                folders = emptyList(),
                videos = allVideos.filter { it.folderName == folder.name },
            )
        } else {
            viewModelScope.launch { listSaf(folder.id, _uiState.value.crumbs + crumb) }
        }
    }

    /** 상위로. 이미 최상단이면 false 를 돌려줘 뒤로가기를 화면 밖으로 넘긴다. */
    fun goUp(): Boolean {
        if (_uiState.value.crumbs.isEmpty()) return false
        navigateTo(_uiState.value.crumbs.size - 1)
        return true
    }

    /** [depth] 단계까지만 남긴다. 0 이면 최상단. */
    fun navigateTo(depth: Int) {
        val crumbs = _uiState.value.crumbs.take(depth)
        if (treeUri == null) {
            val opened = crumbs.lastOrNull()
            _uiState.value = _uiState.value.copy(
                crumbs = crumbs,
                folders = if (opened == null) mediaFolders else emptyList(),
                videos = if (opened == null) emptyList() else allVideos.filter { it.folderName == opened.name },
            )
        } else {
            viewModelScope.launch { listSaf(crumbs.lastOrNull()?.id, crumbs) }
        }
    }

    private suspend fun listSaf(parentDocumentId: String?, crumbs: List<Crumb>) {
        val tree = treeUri ?: return
        _uiState.value = _uiState.value.copy(loading = true)
        val entries = safFolders.listChildren(tree, parentDocumentId)
        val (dirs, files) = entries.partition { it.isDirectory }
        _uiState.value = _uiState.value.copy(
            loading = false,
            crumbs = crumbs,
            folders = dirs.map { FolderSummary(it.documentId, it.name, videoCount = null, totalBytes = null) },
            videos = safFolders.readVideos(files),
        )
    }

    class Factory(
        private val mediaStore: MediaStoreSource,
        private val safFolders: SafFolderSource,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FolderViewModel(mediaStore, safFolders) as T
    }
}
