package com.pikaworks.pikaplayer.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pikaworks.pikaplayer.data.db.Playlist
import com.pikaworks.pikaplayer.data.db.PlaylistDao
import com.pikaworks.pikaplayer.data.db.PlaylistItem
import com.pikaworks.pikaplayer.data.db.PlaylistSummary
import com.pikaworks.pikaplayer.data.media.VideoItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 재생목록에 담긴 한 줄.
 *
 * 담을 때 저장해 둔 이름과, 지금 기기에서 찾아낸 영상을 함께 들고 있다.
 * [video] 가 null 이면 파일이 사라졌거나 아직 목록을 못 읽은 것이다 — 그래도
 * 이름은 보여 줘야 지울 수 있다.
 */
data class PlaylistRow(
    val item: PlaylistItem,
    val video: VideoItem?,
) {
    val available: Boolean get() = video != null
}

class PlaylistViewModel(private val dao: PlaylistDao) : ViewModel() {

    val playlists: StateFlow<List<PlaylistSummary>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 지금 열어 본 목록. null 이면 목록 화면. */
    private val _openId = MutableStateFlow<Long?>(null)
    val openId: StateFlow<Long?> = _openId

    @OptIn(ExperimentalCoroutinesApi::class)
    val openItems: StateFlow<List<PlaylistItem>> = _openId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else dao.observeItems(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun open(id: Long) { _openId.value = id }
    fun close() { _openId.value = null }

    /**
     * @param andAdd 담기 시트에서 만든 경우 그 영상. 만들고 나서 다시 길게 눌러
     *   담게 하면 두 번 일하는 셈이라, 만든 자리에서 바로 넣는다.
     */
    fun create(name: String, andAdd: VideoItem? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            // 시각은 이름이 같아도 순서를 가르려고 남긴다. 최근 만든 것이 위로 온다.
            val id = dao.create(Playlist(name = trimmed, createdAtMs = System.currentTimeMillis()))
            if (andAdd != null) dao.addItem(id, andAdd.uri.toString(), andAdd.displayName)
        }
    }

    fun rename(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { dao.rename(id, trimmed) }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            dao.delete(id)
            if (_openId.value == id) _openId.value = null
        }
    }

    fun add(playlistId: Long, video: VideoItem) {
        viewModelScope.launch { dao.addItem(playlistId, video.uri.toString(), video.displayName) }
    }

    fun remove(playlistId: Long, uri: String) {
        viewModelScope.launch { dao.removeItem(playlistId, uri) }
    }

    /** 화면이 정한 최종 순서를 그대로 저장한다. 끌기가 끝났을 때 한 번 부른다. */
    fun reorder(playlistId: Long, urisInOrder: List<String>) {
        viewModelScope.launch { dao.renumber(playlistId, urisInOrder) }
    }

    class Factory(private val dao: PlaylistDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PlaylistViewModel(dao) as T
    }
}
