package com.pikaworks.pikaplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.res.Configuration
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.pikaworks.pikaplayer.data.media.VideoItem
import com.pikaworks.pikaplayer.ui.library.LibraryScreen
import com.pikaworks.pikaplayer.ui.library.LibraryViewModel
import com.pikaworks.pikaplayer.ui.permission.PermissionScreen
import com.pikaworks.pikaplayer.ui.settings.SettingsScreen
import com.pikaworks.pikaplayer.ui.player.PlayerScreen
import com.pikaworks.pikaplayer.ui.player.PlayerViewModel
import com.pikaworks.pikaplayer.ui.player.PlayerOrientation
import com.pikaworks.pikaplayer.ui.player.SystemControls
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

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
     * 화면으로 돌아올 때마다 다시 확인한다. 사용자가 시스템 설정에서
     * 권한을 켜고 돌아오는 경로가 있는데, 한 번만 읽으면 계속 막힌 채로 남는다.
     */
    private val permissionGranted = mutableStateOf(false)

    override fun onResume() {
        super.onResume()
        permissionGranted.value = hasMediaPermission()
    }

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            onPermissionResult?.invoke(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as PikaApp

        setContent {
            PikaTheme {
                // 화면이 둘뿐이라 내비게이션 라이브러리를 쓰지 않는다.
                // 폴더·설정까지 늘어나면 그때 도입한다.
                var playing by remember { mutableStateOf<VideoItem?>(null) }

                val libraryVm: LibraryViewModel = viewModel(
                    factory = LibraryViewModel.Factory(
                        mediaStore = app.mediaStore,
                        safFolders = app.safFolders,
                        positionDao = app.database.playbackPositionDao(),
                    )
                )
                val libraryState by libraryVm.uiState.collectAsStateWithLifecycle()

                val settings by app.settings.settings
                    .collectAsStateWithLifecycle(initialValue = null)

                val granted by permissionGranted
                var denied by remember { mutableStateOf(false) }
                var showSettings by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                val pickFolder = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocumentTree()
                ) { treeUri ->
                    if (treeUri != null) {
                        app.safFolders.persistPermission(treeUri)
                        scope.launch { app.settings.setFolderTreeUri(treeUri.toString()) }
                        libraryVm.loadFolder(treeUri)
                    }
                }

                val folderUri = settings?.folderTreeUri
                LaunchedEffect(granted, folderUri) {
                    when {
                        granted -> libraryVm.refresh()
                        folderUri != null -> libraryVm.loadFolder(Uri.parse(folderUri))
                    }
                }

                val video = playing
                if (!granted && folderUri == null) {
                    // 권한도 없고 고른 폴더도 없으면 아무것도 보여줄 수 없다.
                    PermissionScreen(
                        denied = denied,
                        onAllow = {
                            onPermissionResult = { ok ->
                                permissionGranted.value = ok
                                denied = !ok
                            }
                            requestPermission.launch(mediaPermission)
                        },
                        onPickFolder = { pickFolder.launch(null) },
                    )
                } else if (showSettings) {
                    settings?.let { s ->
                        SettingsScreen(
                            settings = s,
                            onResumeChange = { scope.launch { app.settings.setResumePlayback(it) } },
                            onAutoPlayNextChange = { scope.launch { app.settings.setAutoPlayNext(it) } },
                            onGesturesChange = { scope.launch { app.settings.setGesturesEnabled(it) } },
                            onDoubleTapSeekChange = { scope.launch { app.settings.setDoubleTapSeekEnabled(it) } },
                            onFollowAutoRotateChange = { scope.launch { app.settings.setFollowAutoRotate(it) } },
                            onBack = { showSettings = false },
                            versionName = BuildConfig.VERSION_NAME,
                        )
                    }
                } else if (video == null) {
                    LibraryScreen(
                        state = libraryState,
                        onVideoClick = { row -> playing = row.video },
                        onSettingsClick = { showSettings = true },
                    )
                } else {
                    // key 를 주면 영상마다 ViewModel 이 새로 만들어지고 이전 것이
                    // 해제되지 않는다. 하나만 두고 open() 으로 갈아끼운다.
                    val playerVm: PlayerViewModel = viewModel(
                        factory = PlayerViewModel.Factory(
                            context = applicationContext,
                            positionDao = app.database.playbackPositionDao(),
                            subtitleMatcher = app.subtitleMatcher,
                        )
                    )
                    val playerState by playerVm.uiState.collectAsStateWithLifecycle()

                    val systemControls = remember { SystemControls(this@MainActivity) }

                    val isLandscape =
                        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                    // 전체화면은 곧 가로 방향이다. 별도 상태로 두면 기기를 돌렸을 때와
                    // 버튼을 눌렀을 때가 어긋난다.
                    var forcedLandscape by remember { mutableStateOf<Boolean?>(null) }

                    val followAutoRotate = settings?.followAutoRotate ?: true

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
                    DisposableEffect(Unit) {
                        onDispose {
                            PlayerOrientation.setImmersive(this@MainActivity, false)
                            PlayerOrientation.apply(this@MainActivity, false, true, null)
                        }
                    }

                    LaunchedEffect(video.uri) { playerVm.open(video, resume = true) }
                    // 재생 중에는 화면이 꺼지지 않게 한다. 화면을 벗어나면 되돌린다.
                    LaunchedEffect(playerState.isPlaying) {
                        systemControls.keepScreenOn(playerState.isPlaying)
                    }
                    DisposableEffect(Unit) {
                        onDispose { systemControls.keepScreenOn(false) }
                    }
                    BackHandler { playing = null }

                    PlayerScreen(
                        player = playerVm.player,
                        state = playerState,
                        onTogglePlay = playerVm::togglePlay,
                        onSkip = playerVm::skip,
                        onSeek = playerVm::seekTo,
                        onToggleControls = playerVm::toggleControls,
                        onToggleSubtitle = playerVm::toggleSubtitle,
                        onToggleLock = playerVm::toggleLock,
                        onCycleResize = playerVm::cycleResizeMode,
                        onCycleSpeed = playerVm::cycleSpeed,
                        onToggleFullscreen = { forcedLandscape = !isLandscape },
                        onBrightnessDelta = systemControls::adjustBrightness,
                        onVolumeDelta = systemControls::adjustVolume,
                        onBack = { playing = null },
                        isFullscreen = isLandscape,
                        gesturesEnabled = settings?.gesturesEnabled ?: true,
                    )
                }
            }
        }
    }

    private fun hasMediaPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, mediaPermission) == PackageManager.PERMISSION_GRANTED
}
