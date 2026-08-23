package com.pikaworks.pikaplayer

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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pikaworks.pikaplayer.data.media.VideoItem
import com.pikaworks.pikaplayer.data.prefs.SubtitleEncoding
import com.pikaworks.pikaplayer.data.prefs.SubtitlePosition
import com.pikaworks.pikaplayer.data.prefs.ThemeMode
import com.pikaworks.pikaplayer.ui.BottomNav
import com.pikaworks.pikaplayer.ui.Tab
import com.pikaworks.pikaplayer.ui.folder.FolderScreen
import com.pikaworks.pikaplayer.ui.folder.FolderViewModel
import com.pikaworks.pikaplayer.ui.library.LibraryScreen
import com.pikaworks.pikaplayer.ui.library.LibraryViewModel
import com.pikaworks.pikaplayer.ui.permission.PermissionScreen
import com.pikaworks.pikaplayer.ui.player.PlayerOrientation
import com.pikaworks.pikaplayer.ui.player.PlayerScreen
import com.pikaworks.pikaplayer.ui.player.PlayerViewModel
import com.pikaworks.pikaplayer.ui.player.SystemControls
import com.pikaworks.pikaplayer.ui.recent.RecentScreen
import com.pikaworks.pikaplayer.ui.settings.LicenseScreen
import com.pikaworks.pikaplayer.ui.settings.SettingsScreen
import com.pikaworks.pikaplayer.ui.theme.PikaTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionGranted.value = granted
            onPermissionResult?.invoke(granted)
        }

    override fun onResume() {
        super.onResume()
        permissionGranted.value = hasMediaPermission()
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

                var denied by remember { mutableStateOf(false) }
                var tab by remember { mutableStateOf(Tab.LIBRARY) }
                var showLicenses by remember { mutableStateOf(false) }
                var playing by remember { mutableStateOf<VideoItem?>(null) }
                // 재생을 시작한 목록. 플레이어 하단의 '다음 영상'과 자동 재생이 여기서 나온다.
                var queue by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
                val play = { video: VideoItem, from: List<VideoItem> ->
                    queue = from
                    playing = video
                }

                val libraryVm: LibraryViewModel = viewModel(
                    factory = LibraryViewModel.Factory(
                        mediaStore = app.mediaStore,
                        safFolders = app.safFolders,
                        positionDao = app.database.playbackPositionDao(),
                        subtitleMatcher = app.subtitleMatcher,
                        deviceStorage = app.deviceStorage,
                    )
                )
                val folderVm: FolderViewModel = viewModel(
                    factory = FolderViewModel.Factory(
                        mediaStore = app.mediaStore,
                        safFolders = app.safFolders,
                    )
                )
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

                // 정렬은 설정에 저장한다. 두 화면이 같은 값을 본다.
                val librarySort = settings?.librarySort
                LaunchedEffect(librarySort) {
                    librarySort?.let {
                        libraryVm.setSort(it)
                        folderVm.setSort(it)
                    }
                }

                val folderUri = settings?.folderTreeUri
                LaunchedEffect(granted, folderUri) {
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

                val video = playing
                when {
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
                        settingsGesturesEnabled = settings?.gesturesEnabled ?: true,
                        followAutoRotate = settings?.followAutoRotate ?: true,
                        queue = queue,
                        autoPlayNext = settings?.autoPlayNext ?: true,
                        defaultSpeed = settings?.playbackSpeed ?: 1f,
                        defaultCharset = settings?.subtitleEncoding ?: SubtitleEncoding.AUTO,
                        subtitleScale = settings?.subtitleScale ?: 1f,
                        subtitlePosition = settings?.subtitlePosition ?: SubtitlePosition.IN_VIDEO,
                        onExit = { playing = null },
                    )

                    else -> Column(modifier = Modifier.fillMaxSize()) {
                        when (tab) {
                            Tab.LIBRARY -> LibraryScreen(
                                modifier = Modifier.weight(1f),
                                state = libraryState,
                                onVideoClick = { row ->
                                    play(row.video, libraryState.visibleRows.map { it.video })
                                },
                                onSortChange = { scope.launch { app.settings.setLibrarySort(it) } },
                                onFilterChange = libraryVm::setFilter,
                                onQueryChange = libraryVm::setQuery,
                            )

                            Tab.FOLDER -> {
                                BackHandler(enabled = folderState.crumbs.isNotEmpty()) { folderVm.goUp() }
                                FolderScreen(
                                    modifier = Modifier.weight(1f),
                                    state = folderState,
                                    onOpenFolder = folderVm::open,
                                    onNavigateTo = folderVm::navigateTo,
                                    onVideoClick = { play(it, folderState.videos) },
                                    onSortChange = { scope.launch { app.settings.setLibrarySort(it) } },
                                )
                            }

                            Tab.RECENT -> RecentScreen(
                                modifier = Modifier.weight(1f),
                                rows = libraryState.recent,
                                // 목록은 최근 순이지만 '다음 영상'은 폴더 안 순서를 따른다.
                                onVideoClick = { row -> play(row.video, libraryState.rows.map { it.video }) },
                            )

                            Tab.SETTINGS -> if (showLicenses) {
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
                                    onSubtitlePositionChange = { scope.launch { app.settings.setSubtitlePosition(it) } },
                                    onThemeChange = { scope.launch { app.settings.setTheme(it) } },
                                    onOpenLicenses = { showLicenses = true },
                                    onBack = { tab = Tab.LIBRARY },
                                    versionName = BuildConfig.VERSION_NAME,
                                )
                            }
                        }
                        BottomNav(current = tab, onSelect = { tab = it })
                    }
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
        settingsGesturesEnabled: Boolean,
        followAutoRotate: Boolean,
        queue: List<VideoItem>,
        autoPlayNext: Boolean,
        defaultSpeed: Float,
        defaultCharset: String,
        subtitleScale: Float,
        subtitlePosition: String,
        onExit: () -> Unit,
    ) {
        val playerVm: PlayerViewModel = viewModel(
            factory = PlayerViewModel.Factory(
                context = applicationContext,
                positionDao = app.database.playbackPositionDao(),
                subtitleMatcher = app.subtitleMatcher,
            )
        )
        val playerState by playerVm.uiState.collectAsStateWithLifecycle()
        val systemControls = remember { SystemControls(this) }

        val isLandscape =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        var forcedLandscape by remember { mutableStateOf<Boolean?>(null) }

        LaunchedEffect(video.uri) {
            playerVm.open(
                video = video,
                resume = true,
                speed = defaultSpeed,
                charset = defaultCharset,
                queue = queue,
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
        LaunchedEffect(isLandscape) {
            PlayerOrientation.setImmersive(this@MainActivity, isLandscape)
        }
        LaunchedEffect(playerState.isPlaying) {
            systemControls.keepScreenOn(playerState.isPlaying)
        }
        DisposableEffect(Unit) {
            onDispose {
                systemControls.keepScreenOn(false)
                PlayerOrientation.setImmersive(this@MainActivity, false)
                PlayerOrientation.apply(this@MainActivity, locked = false, followAutoRotate = true, forcedLandscape = null)
            }
        }
        BackHandler(onBack = onExit)

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
            onToggleLock = playerVm::toggleLock,
            onCycleResize = playerVm::cycleResizeMode,
            onCycleSpeed = playerVm::cycleSpeed,
            onToggleFullscreen = { forcedLandscape = !isLandscape },
            onBrightnessDelta = systemControls::adjustBrightness,
            onVolumeDelta = systemControls::adjustVolume,
            onPlayVideo = playerVm::playNext,
            onBack = onExit,
            isFullscreen = isLandscape,
            gesturesEnabled = settingsGesturesEnabled,
            subtitleScale = subtitleScale,
            subtitlePosition = subtitlePosition,
        )
    }

    private fun hasMediaPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, mediaPermission) == PackageManager.PERMISSION_GRANTED
}
