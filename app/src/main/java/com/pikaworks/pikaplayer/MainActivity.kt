package com.pikaworks.pikaplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pikaworks.pikaplayer.data.media.VideoItem
import com.pikaworks.pikaplayer.data.prefs.SubtitleEncoding
import com.pikaworks.pikaplayer.data.prefs.ThemeMode
import androidx.activity.result.IntentSenderRequest
import com.pikaworks.pikaplayer.data.media.MediaDeleter
import com.pikaworks.pikaplayer.ui.ConfirmDialog
import com.pikaworks.pikaplayer.ui.MessageBar
import com.pikaworks.pikaplayer.ui.VideoActionSheet
import com.pikaworks.pikaplayer.ui.BottomNav
import com.pikaworks.pikaplayer.ui.Tab
import com.pikaworks.pikaplayer.ui.folder.FolderScreen
import com.pikaworks.pikaplayer.ui.folder.FolderViewModel
import com.pikaworks.pikaplayer.ui.library.LibraryScreen
import com.pikaworks.pikaplayer.ui.library.LibraryViewModel
import com.pikaworks.pikaplayer.ui.permission.PermissionScreen
import com.pikaworks.pikaplayer.ui.playlist.AddToPlaylistSheet
import com.pikaworks.pikaplayer.ui.playlist.NameDialog
import com.pikaworks.pikaplayer.ui.playlist.PickVideosSheet
import com.pikaworks.pikaplayer.ui.playlist.PlaylistDetailScreen
import com.pikaworks.pikaplayer.ui.playlist.PlaylistRow
import com.pikaworks.pikaplayer.ui.playlist.PlaylistScreen
import com.pikaworks.pikaplayer.ui.playlist.PlaylistViewModel
import com.pikaworks.pikaplayer.ui.player.PipController
import com.pikaworks.pikaplayer.ui.player.PlayerOrientation
import com.pikaworks.pikaplayer.ui.player.PlayerScreen
import com.pikaworks.pikaplayer.ui.player.PlayerViewModel
import com.pikaworks.pikaplayer.ui.player.SystemControls
import com.pikaworks.pikaplayer.ui.recent.RecentScreen
import com.pikaworks.pikaplayer.ui.settings.LicenseScreen
import com.pikaworks.pikaplayer.ui.settings.SettingsScreen
import com.pikaworks.pikaplayer.ui.vault.PIN_LENGTH
import com.pikaworks.pikaplayer.ui.vault.PinMode
import com.pikaworks.pikaplayer.ui.vault.PinScreen
import com.pikaworks.pikaplayer.ui.vault.VaultScreen
import com.pikaworks.pikaplayer.ui.vault.VaultViewModel
import com.pikaworks.pikaplayer.ui.theme.PikaTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** 이름을 받는 창이 무엇을 하려는 참인지. 만들기와 이름 바꾸기가 같은 창을 쓴다. */
private sealed interface Naming {
    /** [andAdd] 가 있으면 만들자마자 그 영상을 담는다. 담기 시트에서 온 경우다. */
    data class Create(val andAdd: VideoItem? = null) : Naming
    data class Rename(val id: Long, val current: String) : Naming
}

/**
 * 되돌릴 수 없는 동작을 한 번 더 묻기 위해 들고 있는 것.
 *
 * 동작마다 상태 변수를 따로 두면 새 삭제 버튼이 생길 때마다 묻는 것을 잊는다.
 * 한 곳으로 모아 두면 "여기에 넣지 않으면 창이 안 뜬다" 가 규칙이 된다.
 */
private data class Confirm(
    val title: String,
    val body: String,
    val confirmLabel: String,
    val onConfirm: () -> Unit,
)

class MainActivity : ComponentActivity() {

    private val mediaPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            @Suppress("DEPRECATION")
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private var onPermissionResult: ((Boolean) -> Unit)? = null

    /**
     * 화면으로 돌아올 때마다 다시 확인한다. 사용자가 시스템 설정에서 권한을 켜고
     * 돌아오는 경로가 있는데, 한 번만 읽으면 계속 막힌 채로 남는다.
     */
    private val permissionGranted = mutableStateOf(false)

    /**
     * 화면으로 돌아올 때마다 올라간다.
     *
     * 목록 갱신을 권한 상태에만 걸면, 값이 그대로라 `LaunchedEffect` 가 다시 돌지
     * 않는다. 다른 앱에서 영상을 받아 오고 돌아와도 목록이 그대로였다.
     */
    private val resumeTick = mutableStateOf(0)

    /** PiP 창 안인가. 여기 있는 동안에는 영상만 그린다. */
    private val inPip = mutableStateOf(false)

    /**
     * 앱을 벗어날 때 자동으로 작은 창으로 넘어갈지. 재생 중이고 Pro 이며 설정이
     * 켜져 있을 때만 채워진다. 플레이어 화면이 사라지면 비운다.
     *
     * `onUserLeaveHint()` 는 Activity 콜백이라 Compose 상태를 읽을 수 없다.
     * 그래서 지금 할 수 있는 동작을 여기에 올려두는 방식으로 잇는다.
     */
    private var autoPipAction: (() -> Unit)? = null

    /**
     * 홈 버튼이나 앱 전환으로 화면을 벗어날 때 불린다. 뒤로가기로 나갈 때는
     * 불리지 않는다 — 그때는 사용자가 재생을 끝내려는 것이므로 맞는 동작이다.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        autoPipAction?.invoke()
    }

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionGranted.value = granted
            onPermissionResult?.invoke(granted)
        }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip.value = isInPictureInPictureMode
    }

    override fun onResume() {
        super.onResume()
        permissionGranted.value = hasMediaPermission()
        resumeTick.value += 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        permissionGranted.value = hasMediaPermission()

        val app = application as PikaApp

        setContent {
            // 테마 설정이 PikaTheme 의 입력이므로 테마 바깥에서 읽어야 한다.
            val settings by app.settings.settings.collectAsStateWithLifecycle(initialValue = null)

            PikaTheme(themeMode = settings?.theme ?: ThemeMode.SYSTEM) {
                val scope = rememberCoroutineScope()
                val granted by permissionGranted
                val resumed by resumeTick

                var denied by remember { mutableStateOf(false) }
                var tab by remember { mutableStateOf(Tab.LIBRARY) }
                var showLicenses by remember { mutableStateOf(false) }
                var showVault by remember { mutableStateOf(false) }
                val pip by inPip
                var playing by remember { mutableStateOf<VideoItem?>(null) }
                // 재생을 시작한 목록. 플레이어 하단의 '다음 영상'과 자동 재생이 여기서 나온다.
                var queue by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
                // 목록을 통째로 튼 것인지(재생목록·폴더) 함께 넘긴다. 목록 반복과
                // 랜덤은 그럴 때만 뜻이 있고, 켜고 끄는 것은 재생 화면에서 한다.
                // 람다로는 기본값을 줄 수 없어 지역 함수로 둔다.
                var queueIsExplicit by remember { mutableStateOf(false) }
                fun play(
                    video: VideoItem,
                    from: List<VideoItem>,
                    explicitQueue: Boolean = false,
                ) {
                    queue = from
                    queueIsExplicit = explicitQueue
                    playing = video
                }

                val libraryVm: LibraryViewModel = viewModel(
                    factory = LibraryViewModel.Factory(
                        mediaStore = app.mediaStore,
                        safFolders = app.safFolders,
                        positionDao = app.database.playbackPositionDao(),
                        subtitleMatcher = app.subtitleMatcher,
                        deviceStorage = app.deviceStorage,
                        rescanner = app.mediaRescanner,
                    )
                )
                val folderVm: FolderViewModel = viewModel(
                    factory = FolderViewModel.Factory(
                        mediaStore = app.mediaStore,
                        safFolders = app.safFolders,
                    )
                )
                val vaultVm: VaultViewModel = viewModel(factory = VaultViewModel.Factory(app.vault))
                val vaultState by vaultVm.uiState.collectAsStateWithLifecycle()

                val playlistVm: PlaylistViewModel = viewModel(
                    factory = PlaylistViewModel.Factory(app.database.playlistDao())
                )
                val playlists by playlistVm.playlists.collectAsStateWithLifecycle()
                val openPlaylistId by playlistVm.openId.collectAsStateWithLifecycle()
                val openPlaylistItems by playlistVm.openItems.collectAsStateWithLifecycle()
                val containingPlaylists by playlistVm.containing.collectAsStateWithLifecycle()
                // 길게 눌러 담을 영상. null 이면 시트를 띄우지 않는다.
                var pendingAdd by remember { mutableStateOf<VideoItem?>(null) }
                // 더보기(⋯)를 누른 영상. null 이면 시트를 띄우지 않는다.
                var pendingMenu by remember { mutableStateOf<VideoItem?>(null) }
                // 지울지 물어보는 중인 영상.
                var pendingDelete by remember { mutableStateOf<VideoItem?>(null) }
                // 되돌릴 수 없는 동작은 모두 이걸 거쳐 간다. Confirm 의 주석 참고.
                var confirming by remember { mutableStateOf<Confirm?>(null) }
                // 방금 무슨 일이 있었는지 알리는 한 줄. 스스로 사라진다.
                var message by remember { mutableStateOf<String?>(null) }

                // 안드로이드 11+ 는 시스템이 대신 물어본다. 그 창의 결과가 여기로 온다.
                val deleteLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartIntentSenderForResult()
                ) { result ->
                    message = if (MediaDeleter.confirmed(result.resultCode)) {
                        // 목록은 MediaStore 감시가 알아서 따라온다.
                        "삭제했습니다"
                    } else {
                        "삭제하지 않았습니다"
                    }
                }

                // 이름을 받는 창. 만들기와 이름 바꾸기가 같은 창을 쓴다.
                var naming by remember { mutableStateOf<Naming?>(null) }
                // 재생목록에 영상을 고르는 시트를 열었는가.
                var picking by remember { mutableStateOf(false) }
                // 이번 재생에 쓸 방식. 목록마다 기억하지는 않는다.

                // 탭을 벗어나면 접는다. 안 그러면 설정으로 돌아왔을 때 라이선스가 떠 있다.
                LaunchedEffect(tab) {
                    if (tab != Tab.SETTINGS) {
                        showLicenses = false
                        showVault = false
                        // 설정을 벗어나면 다시 잠근다. 풀어둔 채로 폰을 넘기면
                        // 감춘 폴더가 그대로 보인다.
                        vaultVm.lock()
                    }
                }

                val libraryState by libraryVm.uiState.collectAsStateWithLifecycle()
                val folderState by folderVm.uiState.collectAsStateWithLifecycle()

                // 보관함 검색어만 ViewModel 에 있다(거르기 탭의 개수를 같이 세야 해서).
                // 탭을 옮기면 지운다 — 보이지 않는 검색어가 목록을 계속 좁히면 안 된다.
                LaunchedEffect(tab) { if (tab != Tab.LIBRARY) libraryVm.setQuery("") }

                val pickFolder = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocumentTree()
                ) { treeUri ->
                    if (treeUri != null) {
                        app.safFolders.persistPermission(treeUri)
                        scope.launch { app.settings.setFolderTreeUri(treeUri.toString()) }
                        libraryVm.loadFolder(treeUri)
                        folderVm.loadTree(treeUri)
                    }
                }

                // PIN 을 맞게 넣거나 처음 정하고 나면 폴더 고르는 화면으로 이어진다.
                // 여기서 잇지 않으면 PIN 만 넣고 아무 일도 안 일어난 것처럼 보인다.
                LaunchedEffect(vaultState.unlocked) { if (vaultState.unlocked) showVault = true }

                // 앱을 벗어났다 돌아오면 다시 잠근다. 탭 전환만 막으면 홈 버튼으로
                // 나갔다 온 사람에게는 감춘 폴더가 그대로 열려 있다.
                LaunchedEffect(resumed) {
                    vaultVm.lock()
                    showVault = false
                }

                val hiddenFolders = vaultState.foldersToHide
                LaunchedEffect(hiddenFolders) {
                    libraryVm.setHiddenFolders(hiddenFolders)
                    folderVm.setHiddenFolders(hiddenFolders)
                }

                // 정렬은 설정에 저장한다. 두 화면이 같은 값을 본다.
                val librarySort = settings?.librarySort
                LaunchedEffect(librarySort) {
                    librarySort?.let {
                        libraryVm.setSort(it)
                        folderVm.setSort(it)
                    }
                }

                val folderUri = settings?.folderTreeUri
                // 화면에 돌아올 때와 사용자가 당겨서 새로고침할 때가 같은 일을
                // 한다. 두 벌로 두면 한쪽만 고치는 실수가 난다.
                val refreshAll = {
                    when {
                        granted -> {
                            libraryVm.refresh()
                            folderVm.refresh()
                        }
                        folderUri != null -> {
                            val tree = Uri.parse(folderUri)
                            libraryVm.loadFolder(tree)
                            // 폴더 탭도 같은 트리를 쓴다. 없으면 이 사용자에게는 늘 빈 탭이다.
                            folderVm.loadTree(tree)
                        }
                    }
                }
                LaunchedEffect(granted, folderUri, resumed) { refreshAll() }

                pendingMenu?.let { target ->
                    VideoActionSheet(
                        video = target,
                        onAddToPlaylist = { pendingAdd = target },
                        onDelete = { pendingDelete = target },
                        onDismiss = { pendingMenu = null },
                    )
                }

                pendingDelete?.let { target ->
                    ConfirmDialog(
                        title = "기기에서 삭제",
                        // 무엇이 사라지는지 이름으로 말한다. "이 영상" 이라고만
                        // 하면 잘못 고른 것을 알아챌 방법이 없다.
                        body = "${target.displayName}\n\n파일이 기기에서 지워집니다. 되돌릴 수 없습니다.",
                        confirmLabel = "삭제",
                        onConfirm = {
                            scope.launch {
                                when (val outcome = app.mediaDeleter.delete(listOf(target.uri))) {
                                    is MediaDeleter.Result.NeedsConfirm ->
                                        deleteLauncher.launch(IntentSenderRequest.Builder(outcome.request).build())
                                    MediaDeleter.Result.Deleted -> message = "삭제했습니다"
                                    MediaDeleter.Result.Denied ->
                                        message = "이 기기에서는 앱에서 지울 수 없습니다"
                                }
                            }
                        },
                        onDismiss = { pendingDelete = null },
                    )
                }

                confirming?.let { ask ->
                    ConfirmDialog(
                        title = ask.title,
                        body = ask.body,
                        confirmLabel = ask.confirmLabel,
                        onConfirm = ask.onConfirm,
                        onDismiss = { confirming = null },
                    )
                }

                // 시트가 열릴 때 어느 목록에 이미 들어 있는지 물어 둔다.
                LaunchedEffect(pendingAdd) { playlistVm.setAddTarget(pendingAdd?.uri?.toString()) }

                pendingAdd?.let { target ->
                    AddToPlaylistSheet(
                        videoName = target.displayName,
                        playlists = playlists,
                        containing = containingPlaylists,
                        onSelect = { id ->
                            playlistVm.add(id, target)
                            message = "${playlists.firstOrNull { it.id == id }?.name.orEmpty()} 에 담았습니다"
                        },
                        onCreate = { naming = Naming.Create(target) },
                        onDismiss = { pendingAdd = null },
                    )
                }

                when (val request = naming) {
                    null -> Unit
                    is Naming.Create -> NameDialog(
                        title = "새 재생목록",
                        initial = "",
                        confirmLabel = "만들기",
                        onConfirm = { playlistVm.create(it, request.andAdd) },
                        onDismiss = { naming = null },
                    )
                    is Naming.Rename -> NameDialog(
                        title = "이름 바꾸기",
                        initial = request.current,
                        confirmLabel = "저장",
                        onConfirm = { playlistVm.rename(request.id, it) },
                        onDismiss = { naming = null },
                    )
                }

                val video = playing
                // 알림 띠는 무엇 위에든 떠야 한다. 화면 전체를 한 겹으로 감싸고
                // 그 위에 얹는다.
                Box(modifier = Modifier.fillMaxSize()) {
                when {
                    // 설정을 아직 못 읽었으면 고른 폴더가 있는지도 모른다. 그 상태로
                    // 권한 화면을 띄우면 SAF 사용자에게 한 프레임 깜빡인다.
                    settings == null -> Box(Modifier.fillMaxSize().background(PikaTheme.colors.background))

                    // 권한도 없고 고른 폴더도 없으면 보여줄 것이 없다.
                    !granted && folderUri == null -> PermissionScreen(
                        denied = denied,
                        onAllow = {
                            onPermissionResult = { ok -> denied = !ok }
                            requestPermission.launch(mediaPermission)
                        },
                        onPickFolder = { pickFolder.launch(null) },
                    )

                    video != null -> PlayerRoute(
                        app = app,
                        video = video,
                        brightnessVolumeGestures = settings?.gesturesEnabled ?: true,
                        doubleTapSeek = settings?.doubleTapSeekEnabled ?: true,
                        resumePlayback = settings?.resumePlayback ?: true,
                        followAutoRotate = settings?.followAutoRotate ?: true,
                        queue = queue,
                        queueIsExplicit = queueIsExplicit,
                        autoPlayNext = settings?.autoPlayNext ?: true,
                        onAutoPlayNextChange = { scope.launch { app.settings.setAutoPlayNext(it) } },
                        defaultSpeed = settings?.playbackSpeed ?: 1f,
                        defaultCharset = settings?.subtitleEncoding ?: SubtitleEncoding.AUTO,
                        subtitleScale = settings?.subtitleScale ?: 1f,
                        pipMode = pip,
                        autoPip = settings?.autoPip ?: false,
                        // PIN 이 없으면 물을 것이 없다. 설정만 켜 두고 PIN 을 지운
                        // 경우에도 잠긴 채로 갇히지 않는다.
                        childLock = (settings?.childLock ?: false) && vaultState.settings.enabled,
                        vaultVm = vaultVm,
                        onExit = { playing = null },
                    )

                    else -> Column(modifier = Modifier.fillMaxSize()) {
                        // 다른 탭에서 뒤로가기는 보관함으로. 바로 앱이 꺼지면 당황한다.
                        BackHandler(
                            enabled = tab != Tab.LIBRARY && !showLicenses && !showVault &&
                                vaultState.mode == PinMode.NONE
                        ) {
                            tab = Tab.LIBRARY
                        }
                        when (tab) {
                            Tab.LIBRARY -> LibraryScreen(
                                modifier = Modifier.weight(1f),
                                state = libraryState,
                                onVideoClick = { row ->
                                    play(row.video, libraryState.visibleRows.map { it.video })
                                },
                                // 이어보기는 그 줄에 보이는 것들을 대기열로 삼는다.
                                // 거르기 탭에 걸려 목록에서 빠진 영상도 여기서는 눌린다.
                                //
                                // 여러 폴더에서 모인 줄이라 explicitQueue 로 넘긴다.
                                // 그러지 않으면 폴더로 한 번 더 걸러져, 화면에 보이는
                                // 줄과 '다음 영상'이 서로 다른 목록이 된다.
                                onContinueClick = { item ->
                                    play(
                                        item.video,
                                        libraryState.continueWatching.map { it.video },
                                        explicitQueue = true,
                                    )
                                },
                                onContinueRemove = { item ->
                                    confirming = Confirm(
                                        title = "이어보기에서 빼기",
                                        // 무엇이 사라지고 무엇이 남는지 말한다.
                                        body = "${item.video.displayName}\n\n" +
                                            "이 줄에서만 빠집니다. 보던 위치는 그대로 남아, " +
                                            "보관함에서 다시 열면 이어서 재생합니다.",
                                        confirmLabel = "빼기",
                                        onConfirm = {
                                            libraryVm.dismissFromContinue(item.video.uri.toString())
                                        },
                                    )
                                },
                                onVideoLongClick = { row -> pendingAdd = row.video },
                                onVideoMenu = { row -> pendingMenu = row.video },
                                onSortChange = { scope.launch { app.settings.setLibrarySort(it) } },
                                onFilterChange = libraryVm::setFilter,
                                onQueryChange = libraryVm::setQuery,
                                onRescan = libraryVm::rescan,
                                onRefresh = refreshAll,
                            )

                            Tab.FOLDER -> {
                                BackHandler(enabled = folderState.crumbs.isNotEmpty()) { folderVm.goUp() }
                                FolderScreen(
                                    modifier = Modifier.weight(1f),
                                    state = folderState,
                                    onOpenFolder = folderVm::open,
                                    onNavigateTo = folderVm::navigateTo,
                                    onRefresh = refreshAll,
                                    // 이 화면의 대기열은 지금 보고 있는 폴더 그 자체다.
                                    // 보관함처럼 "기기의 모든 영상" 이 아니므로
                                    // 목록 반복과 랜덤을 열어 준다.
                                    onVideoClick = { play(it, folderState.videos, explicitQueue = true) },
                                    onPlayAll = { videos ->
                                        videos.firstOrNull()?.let { play(it, videos, explicitQueue = true) }
                                    },
                                    onVideoLongClick = { pendingAdd = it },
                                    onVideoMenu = { pendingMenu = it },
                                    onSortChange = { scope.launch { app.settings.setLibrarySort(it) } },
                                )
                            }

                            Tab.PLAYLIST -> {
                                val open = playlists.firstOrNull { it.id == openPlaylistId }
                                if (open == null) {
                                    PlaylistScreen(
                                        modifier = Modifier.weight(1f),
                                        playlists = playlists,
                                        onOpen = playlistVm::open,
                                        onCreate = { naming = Naming.Create() },
                                        onDelete = { target ->
                                            confirming = Confirm(
                                                title = "재생목록 삭제",
                                                body = "${target.name}\n\n목록만 지워집니다. " +
                                                    "담아 둔 영상 파일은 그대로 남습니다.",
                                                confirmLabel = "삭제",
                                                onConfirm = { playlistVm.delete(target.id) },
                                            )
                                        },
                                    )
                                } else {
                                    BackHandler { playlistVm.close() }
                                    // 담을 때 저장해 둔 이름과 지금 기기에 있는 영상을 잇는다.
                                    val byUri = libraryState.rows.associateBy { it.video.uri.toString() }
                                    val rows = openPlaylistItems.map { item ->
                                        PlaylistRow(item, byUri[item.uri]?.video)
                                    }
                                    PlaylistDetailScreen(
                                        modifier = Modifier.weight(1f),
                                        name = open.name,
                                        rows = rows,
                                        onPlay = { index ->
                                            // 대기열은 목록 순서 그대로. 찾을 수 없는 것은 뺀다.
                                            // 반복이나 랜덤은 재생 화면에서 켠다.
                                            val found = rows.mapNotNull { it.video }
                                            rows[index].video?.let { picked ->
                                                play(picked, found, explicitQueue = true)
                                            }
                                        },
                                        // 편집에서 완료를 누를 때 한 번만 저장한다.
                                        // 잘못 뺀 것은 취소로 되돌아가므로 따로 묻지 않는다.
                                        onApplyEdit = { uris -> playlistVm.applyEdit(open.id, uris) },
                                        onAdd = { picking = true },
                                        onRename = { naming = Naming.Rename(open.id, open.name) },
                                        onBack = { playlistVm.close() },
                                    )

                                    if (picking) {
                                        PickVideosSheet(
                                            videos = libraryState.rows.map { it.video },
                                            alreadyIn = openPlaylistItems.map { it.uri }.toSet(),
                                            onPick = { video ->
                                                playlistVm.add(open.id, video)
                                                message = "${video.displayName} 을 담았습니다"
                                            },
                                            onDismiss = { picking = false },
                                        )
                                    }
                                }
                            }

                            Tab.RECENT -> RecentScreen(
                                modifier = Modifier.weight(1f),
                                rows = libraryState.recent,
                                // 대기열은 이 화면에 보이는 최근 목록 그대로다.
                                //
                                // 전에는 기기의 모든 영상을 넘기고 폴더로 걸렀다. 그래서
                                // 최근에서 한 편을 틀면 화면에 없던 같은 폴더의 영상들이
                                // '다음 영상'으로 붙었다 — 어디서 온 목록인지 알 방법이
                                // 없었다. 보고 있던 것이 곧 대기열이어야 한다.
                                onVideoClick = { row ->
                                    play(row.video, libraryState.recent.map { it.video }, explicitQueue = true)
                                },
                                onVideoLongClick = { row -> pendingAdd = row.video },
                                onVideoMenu = { row -> pendingMenu = row.video },
                                onRefresh = refreshAll,
                            )

                            Tab.SETTINGS -> if (vaultState.mode != PinMode.NONE) {
                                BackHandler { vaultVm.cancel() }
                                PinScreen(
                                    modifier = Modifier.weight(1f),
                                    title = if (vaultState.mode == PinMode.UNLOCK) "잠금 해제" else "PIN 설정",
                                    subtitle = when (vaultState.mode) {
                                        // 잊으면 못 되돌린다는 말은 정하는 자리에서 한다.
                                        // 잠긴 뒤의 탈출구는 PinScreen 이 알아서 띄운다.
                                        PinMode.SET -> "숫자 ${PIN_LENGTH}자리를 정하세요\n잊으면 되돌릴 수 없습니다"
                                        PinMode.CONFIRM -> "확인을 위해 한 번 더 입력하세요"
                                        else -> "감춘 폴더를 보려면 PIN 을 입력하세요"
                                    },
                                    entered = vaultState.entered,
                                    lockedForMs = vaultState.lockedForMs,
                                    error = vaultState.error,
                                    onDigit = { vaultVm.onDigit(it, System.currentTimeMillis()) },
                                    onBackspace = vaultVm::onBackspace,
                                    onBack = { vaultVm.cancel() },
                                    // 새로 정하는 중에는 되돌릴 것이 없다.
                                    recoverable = vaultState.mode == PinMode.UNLOCK,
                                )
                            } else if (showVault) {
                                BackHandler { showVault = false }
                                VaultScreen(
                                    modifier = Modifier.weight(1f),
                                    // 폴더 화면의 목록이 아니라 보관함이 만든 것을 쓴다.
                                    // 폴더 화면의 id 는 표시 이름이나 SAF 문서 id 라
                                    // 거르기에 쓰는 folderKey 와 값이 다르다.
                                    folders = libraryState.allFolders,
                                    hidden = vaultState.hiddenFolders,
                                    onToggle = vaultVm::setHidden,
                                    onChangePin = { vaultVm.startSet() },
                                    onDisable = {
                                        confirming = Confirm(
                                            title = "비공개 폴더 끄기",
                                            body = "PIN 과 감춰 둔 폴더 목록이 지워집니다. " +
                                                "감춰 뒀던 폴더는 다시 보관함에 나타납니다.",
                                            confirmLabel = "끄기",
                                            onConfirm = {
                                                vaultVm.disable()
                                                showVault = false
                                            },
                                        )
                                    },
                                    onBack = { showVault = false },
                                )
                            } else if (showLicenses) {
                                BackHandler { showLicenses = false }
                                LicenseScreen(
                                    modifier = Modifier.weight(1f),
                                    onBack = { showLicenses = false },
                                )
                            } else settings?.let { s ->
                                SettingsScreen(
                                    modifier = Modifier.weight(1f),
                                    settings = s,
                                    onResumeChange = { scope.launch { app.settings.setResumePlayback(it) } },
                                    onAutoPlayNextChange = { scope.launch { app.settings.setAutoPlayNext(it) } },
                                    onGesturesChange = { scope.launch { app.settings.setGesturesEnabled(it) } },
                                    onDoubleTapSeekChange = { scope.launch { app.settings.setDoubleTapSeekEnabled(it) } },
                                    onFollowAutoRotateChange = { scope.launch { app.settings.setFollowAutoRotate(it) } },
                                    onPlaybackSpeedChange = { scope.launch { app.settings.setPlaybackSpeed(it) } },
                                    onSubtitleEncodingChange = { scope.launch { app.settings.setSubtitleEncoding(it) } },
                                    onSubtitleScaleChange = { scope.launch { app.settings.setSubtitleScale(it) } },
                                    onThemeChange = { scope.launch { app.settings.setTheme(it) } },
                                    onOpenLicenses = { showLicenses = true },
                                    onAutoPipChange = { scope.launch { app.settings.setAutoPip(it) } },
                                    onChildLockChange = { scope.launch { app.settings.setChildLock(it) } },
                                    vaultEnabled = vaultState.settings.enabled,
                                    onOpenVault = {
                                        // 아직 PIN 이 없으면 정하는 것부터. 있으면 확인 먼저.
                                        when {
                                            !vaultState.settings.enabled -> vaultVm.startSet()
                                            vaultState.unlocked -> showVault = true
                                            else -> vaultVm.startUnlock(System.currentTimeMillis())
                                        }
                                    },
                                    onBack = { tab = Tab.LIBRARY },
                                    versionName = BuildConfig.VERSION_NAME,
                                )
                            }
                        }
                        BottomNav(current = tab, onSelect = { tab = it })
                    }
                }

                MessageBar(
                    message = message,
                    onDone = { message = null },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                )
                }
            }
        }
    }

    /**
     * 플레이어는 방향·몰입 모드 같은 창 상태를 건드리므로 화면을 벗어날 때
     * 되돌려야 한다. 별도 함수로 묶어 그 짝을 한눈에 보이게 한다.
     */
    @androidx.compose.runtime.Composable
    private fun PlayerRoute(
        app: PikaApp,
        video: VideoItem,
        brightnessVolumeGestures: Boolean,
        doubleTapSeek: Boolean,
        resumePlayback: Boolean,
        followAutoRotate: Boolean,
        queue: List<VideoItem>,
        /** 목록 하나를 통째로 튼 것인가(재생목록·폴더). 폴더로 거르지 않는다. */
        queueIsExplicit: Boolean,
        autoPlayNext: Boolean,
        /** 재생 화면에서 자동 재생을 껐다 켜면 설정에도 남긴다. */
        onAutoPlayNextChange: (Boolean) -> Unit,
        defaultSpeed: Float,
        defaultCharset: String,
        subtitleScale: Float,
        pipMode: Boolean,
        autoPip: Boolean,
        /** 어린이 잠금. 잠금을 풀 때 PIN 을 묻고, 잠긴 동안에는 나갈 수 없다. */
        childLock: Boolean,
        vaultVm: VaultViewModel,
        onExit: () -> Unit,
    ) {
        val playerVm: PlayerViewModel = viewModel(
            factory = PlayerViewModel.Factory(
                context = applicationContext,
                positionDao = app.database.playbackPositionDao(),
                subtitleMatcher = app.subtitleMatcher,
                persistScope = app.persistScope,
            )
        )
        val playerState by playerVm.uiState.collectAsStateWithLifecycle()
        val systemControls = remember { SystemControls(this) }
        val pipController = remember { PipController(this) }

        val isLandscape =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        var forcedLandscape by remember { mutableStateOf<Boolean?>(null) }

        /**
         * 전체화면 = 가로. 영상 비율과 상관없이 같다.
         *
         * 한동안 영상 비율로 갈랐다. 세로 영상은 눕히면 높이가 폰의 짧은 쪽에
         * 묶여서 실제로 작아지기 때문이다(1080×1920 영상이 1080×2400 화면에서
         * 607×1080 이 된다). 그래서 세로 영상은 눕히지 않고 시스템 바만 감췄다.
         *
         * 그런데 그러면 같은 버튼이 영상마다 다른 일을 한다. 무엇이 일어날지
         * 누르기 전에 알 수 없는 버튼은 크기 몇 백 픽셀보다 비싸다. 눕히는 쪽으로
         * 통일하고, 세로 영상은 좌우에 검은 자리를 두고 본다 — 눕혀서 보고 싶은
         * 사람이 스스로 고른 결과다.
         */
        val isFullscreen = isLandscape

        LaunchedEffect(resumePlayback) { playerVm.setResumePlayback(resumePlayback) }
        LaunchedEffect(video.uri) {
            playerVm.open(
                video = video,
                resume = resumePlayback,
                speed = defaultSpeed,
                charset = defaultCharset,
                queue = queue,
                explicitQueue = queueIsExplicit,
            )
        }
        LaunchedEffect(autoPlayNext) { playerVm.setAutoPlayNext(autoPlayNext) }

        LaunchedEffect(playerState.locked, followAutoRotate, forcedLandscape) {
            PlayerOrientation.apply(
                activity = this@MainActivity,
                locked = playerState.locked,
                followAutoRotate = followAutoRotate,
                forcedLandscape = forcedLandscape,
            )
        }
        LaunchedEffect(isFullscreen) {
            PlayerOrientation.setImmersive(this@MainActivity, isFullscreen)
        }
        LaunchedEffect(playerState.isPlaying) {
            systemControls.keepScreenOn(playerState.isPlaying)
        }
        DisposableEffect(Unit) {
            onDispose {
                // ViewModel 은 Activity 에 매여 있어 여기서 멈추지 않으면 소리가 계속 난다.
                playerVm.close()
                systemControls.keepScreenOn(false)
                systemControls.resetBrightness()
                PlayerOrientation.setImmersive(this@MainActivity, false)
                PlayerOrientation.apply(this@MainActivity, locked = false, followAutoRotate = true, forcedLandscape = null)
            }
        }
        val vaultState by vaultVm.uiState.collectAsStateWithLifecycle()
        val askingPin = vaultState.mode == PinMode.CHILD_UNLOCK

        // PIN 이 맞으면 그때 잠금을 푼다. 신호는 한 번 쓰고 되돌린다.
        LaunchedEffect(vaultState.childUnlocked) {
            if (vaultState.childUnlocked) {
                playerVm.toggleLock()
                vaultVm.consumeChildUnlock()
            }
        }

        // PiP 중에는 뒤로가기가 오지 않는다. 창을 닫는 것은 시스템이 처리한다.
        //
        // 어린이 잠금이 걸린 동안에는 뒤로가기를 삼킨다. 잠금 해제에 PIN 을 물어도
        // 뒤로가기로 목록에 나갈 수 있으면 막는 의미가 없다.
        BackHandler(enabled = !pipMode) {
            when {
                askingPin -> vaultVm.cancel()
                childLock && playerState.locked -> Unit
                // 전체보기에서는 먼저 거기서 빠져나온다. 한 번에 목록까지 나가면
                // 화면을 채우려고 눌렀던 것이 앱을 벗어나는 일이 된다.
                //
                // 손으로 눕힌 경우(forcedLandscape 가 null)는 세우지 않는다. 우리가
                // 돌린 것이 아니면 되돌릴 것도 없다.
                forcedLandscape == true -> forcedLandscape = false
                else -> onExit()
            }
        }

        // 재생 중인지는 보지 않는다. 플레이어 화면에 있다는 것 자체가 보고 있다는
        // 뜻이고, 일시정지했다고 작은 창을 안 띄우면 오히려 예상과 어긋난다.
        val canAutoPip = autoPip && pipController.isSupported && !pipMode

        // API 31+ 는 시스템이 홈 제스처 도중에 알아서 넘겨 준다. onUserLeaveHint 는
        // 제스처가 끝난 뒤에 오므로 화면이 한 번 끊겼다가 창이 뜬다. 시스템이
        // 해 줄 수 있으면 우리는 손을 뗀다.
        val needsManualAutoPip = canAutoPip && !pipController.supportsAutoEnter
        DisposableEffect(needsManualAutoPip) {
            autoPipAction = if (!needsManualAutoPip) null else {
                {
                    val size = playerVm.player.videoSize
                    pipController.enter(size.width, size.height, playerVm.player.isPlaying)
                }
            }
            onDispose { autoPipAction = null }
        }

        // PiP 창의 재생/일시정지. 창 안에는 우리 화면이 없어서 시스템이 그려 주는
        // 버튼뿐이고, 눌리면 방송으로 돌아온다.
        //
        // 앱 안에서만 도는 방송이라 NOT_EXPORTED 로 등록한다. API 33 이하에서는
        // 이 값이 무시되지만, 보내는 쪽이 패키지를 박아 두어 밖에서 닿지 않는다.
        DisposableEffect(pipController) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == PipController.ACTION_TOGGLE_PLAY) playerVm.togglePlay()
                }
            }
            ContextCompat.registerReceiver(
                this@MainActivity,
                receiver,
                IntentFilter(PipController.ACTION_TOGGLE_PLAY),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            onDispose { unregisterReceiver(receiver) }
        }

        // 창에 들어가기 전에도 올려 둔다. 자동 전환은 홈 제스처가 시작되기 전에
        // 켜져 있어야 하고, 버튼 모양은 재생 상태를 따라가야 한다.
        //
        // 영상 크기는 준비된 뒤에야 알 수 있다. isPlaying 이 처음 참이 되는 시점은
        // 준비가 끝난 뒤라 그때 함께 올라간다.
        LaunchedEffect(autoPip, pipMode, playerState.isPlaying) {
            val size = playerVm.player.videoSize
            pipController.sync(
                videoWidth = size.width,
                videoHeight = size.height,
                isPlaying = playerState.isPlaying,
                autoEnter = autoPip && pipController.isSupported,
            )
        }

        // 재생 화면을 벗어나면 자동 전환을 되돌린다. 설정이 Activity 에 남기
        // 때문에, 켜 둔 채로 나가면 보관함에서 홈을 눌러도 작은 창이 뜬다.
        DisposableEffect(pipController) {
            onDispose { pipController.clearAutoEnter() }
        }

        // PIN 은 재생 화면을 **덮는다**. 대신 그리면 PlayerRoute 가 사라지면서
        // onDispose 가 재생을 멈춘다 — 잠금을 푸는 동안 소리가 끊기면 안 된다.
        Box(modifier = Modifier.fillMaxSize()) {

            PlayerScreen(
                player = playerVm.player,
                state = playerState,
                onTogglePlay = playerVm::togglePlay,
                onSkip = playerVm::skip,
                onSeek = playerVm::seekTo,
                onToggleControls = playerVm::toggleControls,
                onSelectSubtitle = playerVm::selectSubtitle,
                onSelectCharset = playerVm::setSubtitleCharset,
                onAdjustSubtitleOffset = playerVm::adjustSubtitleOffset,
                onResetSubtitleOffset = playerVm::resetSubtitleOffset,
                // 잠긴 것을 풀 때만 PIN 을 묻는다. 잠그는 것은 아무나 해도 된다.
                onToggleLock = {
                    if (childLock && playerState.locked) {
                        vaultVm.startChildUnlock(System.currentTimeMillis())
                    } else {
                        playerVm.toggleLock()
                    }
                },
                onToggleRepeat = playerVm::toggleRepeat,
                onToggleShuffle = playerVm::toggleShuffle,
                onToggleLoopQueue = playerVm::toggleLoopQueue,
                // 설정의 '다음 영상 자동 재생' 과 같은 값이다. 여기서 바꾼 것이
                // 설정에 남지 않으면 두 곳이 서로 다른 말을 하게 된다.
                onSetAutoPlayNext = onAutoPlayNextChange,
                onMarkAb = playerVm::markAb,
                onCycleResize = playerVm::cycleResizeMode,
                onSelectSpeed = playerVm::setSpeed,
                // 눕히거나 세우거나. 영상 비율은 보지 않는다 — 위 isFullscreen 참고.
                onToggleFullscreen = {
                    forcedLandscape = if (isFullscreen) false else true
                },
                onBrightnessDelta = systemControls::adjustBrightness,
                onVolumeDelta = systemControls::adjustVolume,
                onPlayVideo = playerVm::playNext,
                // 지원하지 않는 기기에서는 버튼 자체를 만들지 않는다. 눌러도 아무
                // 일이 없는 버튼을 두면 고장으로 읽힌다.
                onEnterPip = if (!pipController.isSupported) null else {
                    {
                        val video = playerVm.player.videoSize
                        pipController.enter(video.width, video.height, playerState.isPlaying)
                    }
                },
                onBack = onExit,
                isFullscreen = isFullscreen,
                brightnessVolumeGestures = brightnessVolumeGestures,
                doubleTapSeek = doubleTapSeek,
                subtitleScale = subtitleScale,
                pipMode = pipMode,
            )

            if (askingPin) {
                PinScreen(
                    // 이 화면만 시스템 바 아래까지 홀로 놓인다. 설정 안에서는
                    // 아래에 하단 네비게이션이 있어 그쪽이 인셋을 먹지만,
                    // 여기서는 받아 주는 것이 없어 숫자판이 시스템 버튼에 가린다.
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    title = "잠금 해제",
                    subtitle = "어린이 잠금이 켜져 있습니다",
                    entered = vaultState.entered,
                    lockedForMs = vaultState.lockedForMs,
                    error = vaultState.error,
                    onDigit = { vaultVm.onDigit(it, System.currentTimeMillis()) },
                    onBackspace = vaultVm::onBackspace,
                    onBack = { vaultVm.cancel() },
                    recoverable = true,
                )
            }
        }
    }

    private fun hasMediaPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, mediaPermission) == PackageManager.PERMISSION_GRANTED
}
