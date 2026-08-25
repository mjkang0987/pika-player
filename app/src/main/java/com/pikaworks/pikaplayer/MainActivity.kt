package com.pikaworks.pikaplayer

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.util.Log
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
                LaunchedEffect(granted, folderUri, resumed) {
                    // 목록이 비었을 때 어느 경로를 탔는지 밖에서 알 수 있어야 한다.
                    Log.i("PikaMedia", "미디어 권한=$granted, 선택한 폴더=${folderUri != null}")
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
                        autoPlayNext = settings?.autoPlayNext ?: true,
                        defaultSpeed = settings?.playbackSpeed ?: 1f,
                        defaultCharset = settings?.subtitleEncoding ?: SubtitleEncoding.AUTO,
                        subtitleScale = settings?.subtitleScale ?: 1f,
                        subtitlePosition = settings?.subtitlePosition ?: SubtitlePosition.IN_VIDEO,
                        pipMode = pip,
                        autoPip = settings?.autoPip ?: false,
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
                                onContinueClick = { item ->
                                    play(item.video, libraryState.continueWatching.map { it.video })
                                },
                                onSortChange = { scope.launch { app.settings.setLibrarySort(it) } },
                                onFilterChange = libraryVm::setFilter,
                                onQueryChange = libraryVm::setQuery,
                                onRescan = libraryVm::rescan,
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

                            Tab.SETTINGS -> if (vaultState.mode != PinMode.NONE) {
                                BackHandler { vaultVm.cancel() }
                                PinScreen(
                                    modifier = Modifier.weight(1f),
                                    title = if (vaultState.mode == PinMode.UNLOCK) "잠금 해제" else "PIN 설정",
                                    subtitle = when (vaultState.mode) {
                                        PinMode.SET -> "숫자 ${PIN_LENGTH}자리를 정하세요"
                                        PinMode.CONFIRM -> "확인을 위해 한 번 더 입력하세요"
                                        else -> "감춘 폴더를 보려면 PIN 을 입력하세요"
                                    },
                                    entered = vaultState.entered,
                                    lockedForMs = vaultState.lockedForMs,
                                    error = vaultState.error,
                                    onDigit = { vaultVm.onDigit(it, System.currentTimeMillis()) },
                                    onBackspace = vaultVm::onBackspace,
                                    onBack = { vaultVm.cancel() },
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
                                        vaultVm.disable()
                                        showVault = false
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
                                    onSubtitlePositionChange = { scope.launch { app.settings.setSubtitlePosition(it) } },
                                    onThemeChange = { scope.launch { app.settings.setTheme(it) } },
                                    onOpenLicenses = { showLicenses = true },
                                    onAutoPipChange = { scope.launch { app.settings.setAutoPip(it) } },
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
        autoPlayNext: Boolean,
        defaultSpeed: Float,
        defaultCharset: String,
        subtitleScale: Float,
        subtitlePosition: String,
        pipMode: Boolean,
        autoPip: Boolean,
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

        LaunchedEffect(resumePlayback) { playerVm.setResumePlayback(resumePlayback) }
        LaunchedEffect(video.uri) {
            playerVm.open(
                video = video,
                resume = resumePlayback,
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
                // ViewModel 은 Activity 에 매여 있어 여기서 멈추지 않으면 소리가 계속 난다.
                playerVm.close()
                systemControls.keepScreenOn(false)
                systemControls.resetBrightness()
                PlayerOrientation.setImmersive(this@MainActivity, false)
                PlayerOrientation.apply(this@MainActivity, locked = false, followAutoRotate = true, forcedLandscape = null)
            }
        }
        // PiP 중에는 뒤로가기가 오지 않는다. 창을 닫는 것은 시스템이 처리한다.
        BackHandler(enabled = !pipMode, onBack = onExit)

        // 재생 중인지는 보지 않는다. 플레이어 화면에 있다는 것 자체가 보고 있다는
        // 뜻이고, 일시정지했다고 작은 창을 안 띄우면 오히려 예상과 어긋난다.
        val canAutoPip = autoPip && pipController.isSupported && !pipMode
        DisposableEffect(canAutoPip) {
            autoPipAction = if (!canAutoPip) null else {
                {
                    val size = playerVm.player.videoSize
                    pipController.enter(size.width, size.height)
                }
            }
            onDispose { autoPipAction = null }
        }

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
            onToggleRepeat = playerVm::toggleRepeat,
            onCycleResize = playerVm::cycleResizeMode,
            onCycleSpeed = playerVm::cycleSpeed,
            onToggleFullscreen = { forcedLandscape = !isLandscape },
            onBrightnessDelta = systemControls::adjustBrightness,
            onVolumeDelta = systemControls::adjustVolume,
            onPlayVideo = playerVm::playNext,
            // 지원하지 않는 기기에서는 버튼 자체를 만들지 않는다. 눌러도 아무
            // 일이 없는 버튼을 두면 고장으로 읽힌다.
            onEnterPip = if (!pipController.isSupported) null else {
                {
                    val video = playerVm.player.videoSize
                    pipController.enter(video.width, video.height)
                }
            },
            onBack = onExit,
            isFullscreen = isLandscape,
            brightnessVolumeGestures = brightnessVolumeGestures,
            doubleTapSeek = doubleTapSeek,
            subtitleScale = subtitleScale,
            subtitlePosition = subtitlePosition,
            pipMode = pipMode,
        )
    }

    private fun hasMediaPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, mediaPermission) == PackageManager.PERMISSION_GRANTED
}
