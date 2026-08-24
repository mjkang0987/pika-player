package com.pikaworks.pikaplayer.ui.folder

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pikaworks.pikaplayer.data.media.MediaStoreSource
import com.pikaworks.pikaplayer.data.media.SafFolderSource
import com.pikaworks.pikaplayer.data.media.VideoItem
import com.pikaworks.pikaplayer.data.media.sortedFor
import com.pikaworks.pikaplayer.data.prefs.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FolderSummary(
    /** MediaStore 는 폴더 이름, SAF 는 document id. 내려갈 때 이 값을 쓴다. */
    val id: String,
    val name: String,
    /** SAF 는 하위를 세기 전까지 모른다. 그때는 null. */
    val videoCount: Int?,
    val totalBytes: Long?,
    /** 정렬용. MediaStore 는 폴더 안 영상 중 가장 최근, SAF 는 폴더 자체의 수정 시각. */
    val latestModifiedSec: Long?,
)

/**
 * 폴더 목록 정렬.
 *
 * 재생시간처럼 폴더에 없는 기준은 이름순으로 둔다. 값이 없는 항목끼리는 정렬이
 * 안정적이라 이름 순서가 그대로 유지된다 — 그래서 기준선을 이름순으로 잡는다.
 */
fun List<FolderSummary>.sortedFor(order: String): List<FolderSummary> {
    val byName = sortedBy { it.name.lowercase() }
    return when (order) {
        SortOrder.SIZE_DESC -> byName.sortedByDescending { it.totalBytes ?: -1L }
        SortOrder.DATE_DESC -> byName.sortedByDescending { it.latestModifiedSec ?: -1L }
        else -> byName
    }
}

/** 지금 어느 폴더 안에 있는지. 비어 있으면 최상단. */
data class Crumb(val id: String, val name: String)

data class FolderUiState(
    val loading: Boolean = true,
    val rootLabel: String = "내부 저장소",
    val crumbs: List<Crumb> = emptyList(),
    val folders: List<FolderSummary> = emptyList(),
    val videos: List<VideoItem> = emptyList(),
    val sort: String = SortOrder.DATE_DESC,
)

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
    private var sort: String = SortOrder.DATE_DESC
    /** 비공개 폴더로 감춘 folderKey. 잠금을 푼 동안에는 비어 있다. */
    private var hiddenFolders: Set<String> = emptySet()
    /** SAF 목록 조회. 폴더를 연달아 누르면 앞선 것을 버린다. */
    private var listJob: Job? = null
    /** SAF 하위 폴더의 영상 수를 뒤에서 세는 작업. 폴더를 옮기면 취소한다. */
    private var countJob: Job? = null
    /** SAF 모드일 때만 값이 있다. */
    private var treeUri: Uri? = null

    init {
        // 기기에 영상이 추가·삭제되면 폴더 목록도 그 자리에서 맞춘다.
        // 폴더 안에 들어가 있을 때는 건드리지 않는다 — 목록을 다시 만들면
        // 보고 있던 폴더에서 최상단으로 튕겨 나가기 때문이다.
        viewModelScope.launch {
            mediaStore.changes()
                .debounce(500)
                .collect {
                    if (treeUri == null && _uiState.value.crumbs.isEmpty()) {
                        loadMediaFolders(showLoading = false)
                    }
                }
        }
    }

    /** 보관함과 같은 정렬 기준을 쓴다. 화면마다 순서가 다르면 혼란스럽다. */
    fun setSort(order: String) {
        if (sort == order) return
        sort = order
        _uiState.value = _uiState.value.copy(
            sort = order,
            folders = _uiState.value.folders.sortedFor(order),
            videos = _uiState.value.videos.sortedFor(order),
        )
    }

    fun setHiddenFolders(keys: Set<String>) {
        if (hiddenFolders == keys) return
        hiddenFolders = keys
        // 이미 그려진 목록에서도 즉시 빠져야 한다. 다시 훑을 것 없이 다시 묶는다.
        if (treeUri == null) refresh()
    }

    /** 미디어 권한이 있을 때. 화면에 돌아올 때마다 호출한다. */
    fun refresh() {
        treeUri = null
        // SAF 로 보던 중에 권한을 켰을 수 있다. 남은 조회가 결과를 덮어쓰지 않게 끊는다.
        listJob?.cancel()
        countJob?.cancel()
        loadMediaFolders(showLoading = true)
    }

    /**
     * @param showLoading 사용자가 부른 조회에서만 참. 파일이 추가돼서 저절로
     *   다시 읽는 경우에는 목록이 이미 떠 있으므로 자리 표시자로 덮지 않는다.
     */
    private fun loadMediaFolders(showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) _uiState.value = _uiState.value.copy(loading = true)
            allVideos = mediaStore.queryVideos()
            mediaFolders = allVideos
                .filter { it.folderKey !in hiddenFolders }
                .groupBy { it.folderName ?: "기타" }
                .map { (name, items) ->
                    FolderSummary(
                        id = name,
                        name = name,
                        videoCount = items.size,
                        totalBytes = items.sumOf { it.sizeBytes },
                        latestModifiedSec = items.maxOf { it.dateModifiedSec },
                    )
                }
            _uiState.value = FolderUiState(
                loading = false,
                folders = mediaFolders.sortedFor(sort),
                sort = sort,
            )
        }
    }

    /** 권한 대신 사용자가 고른 폴더를 쓸 때. */
    fun loadTree(uri: Uri) {
        treeUri = uri
        allVideos = emptyList()
        _uiState.value = FolderUiState(loading = true, sort = sort)
        // 폴더 이름 조회는 목록보다 늦게 끝날 수 있다. 상태를 갈아끼우지 말고 이름만 덧붙인다.
        viewModelScope.launch {
            val name = safFolders.treeName(uri) ?: "선택한 폴더"
            _uiState.update { it.copy(rootLabel = name) }
        }
        openSaf(parentDocumentId = null, crumbs = emptyList())
    }

    /** 앞선 조회가 늦게 끝나 나중 화면을 덮어쓰지 않도록 하나만 돌린다. */
    private fun openSaf(parentDocumentId: String?, crumbs: List<Crumb>) {
        listJob?.cancel()
        countJob?.cancel()
        listJob = viewModelScope.launch { listSaf(parentDocumentId, crumbs) }
    }

    fun open(folder: FolderSummary) {
        val crumb = Crumb(folder.id, folder.name)
        if (treeUri == null) {
            // MediaStore 는 폴더 안이 곧 영상 목록이다. 더 내려갈 곳이 없다.
            _uiState.value = _uiState.value.copy(
                crumbs = listOf(crumb),
                folders = emptyList(),
                videos = allVideos
                    .filter { it.folderName == folder.name && it.folderKey !in hiddenFolders }
                    .sortedFor(sort),
            )
        } else {
            openSaf(folder.id, _uiState.value.crumbs + crumb)
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
                folders = if (opened == null) mediaFolders.sortedFor(sort) else emptyList(),
                videos = if (opened == null) emptyList()
                else allVideos.filter { it.folderName == opened.name }.sortedFor(sort),
            )
        } else {
            openSaf(crumbs.lastOrNull()?.id, crumbs)
        }
    }

    private suspend fun listSaf(parentDocumentId: String?, crumbs: List<Crumb>) {
        val tree = treeUri ?: return
        _uiState.value = _uiState.value.copy(loading = true)
        val entries = safFolders.listChildren(tree, parentDocumentId)
        val (dirs, files) = entries.partition { it.isDirectory }
        val folders = dirs.map {
            FolderSummary(
                id = it.documentId,
                name = it.name,
                videoCount = null,
                totalBytes = null,
                latestModifiedSec = it.lastModifiedMs / 1000,
            )
        }
        _uiState.value = _uiState.value.copy(
            loading = false,
            crumbs = crumbs,
            folders = folders.sortedFor(sort),
            videos = safFolders.readVideos(files).sortedFor(sort),
        )
        countFolders(tree, folders)
    }

    /**
     * SAF 하위 폴더의 영상 수와 용량을 뒤에서 채운다.
     *
     * 폴더 하나에 커서 한 번이고 크기는 그 커서에서 바로 나온다 — 코덱을 여는
     * [SafFolderSource.readVideos] 와 달리 싸다. 그래도 목록을 띄우는 것보다는
     * 느리므로 화면을 먼저 그리고 값은 뒤따라 붙인다.
     *
     * 세는 것은 바로 아래 단계뿐이다. MediaStore 쪽 폴더도 같은 기준이라 맞춘다.
     */
    private fun countFolders(tree: Uri, folders: List<FolderSummary>) {
        if (folders.isEmpty()) return
        countJob = viewModelScope.launch {
            folders.forEach { folder ->
                val files = safFolders.listChildren(tree, folder.id).filter { !it.isDirectory }
                _uiState.update { state ->
                    // 세는 사이에 다른 폴더로 옮겼으면 버린다.
                    if (state.folders.none { it.id == folder.id }) return@update state
                    state.copy(
                        folders = state.folders.map {
                            if (it.id == folder.id) {
                                it.copy(videoCount = files.size, totalBytes = files.sumOf { f -> f.sizeBytes })
                            } else it
                        }
                    )
                }
            }
            // 채우는 도중에 순서를 흔들면 읽기 어렵다. 다 찬 뒤 한 번만 다시 정렬한다.
            _uiState.update { it.copy(folders = it.folders.sortedFor(sort)) }
        }
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
