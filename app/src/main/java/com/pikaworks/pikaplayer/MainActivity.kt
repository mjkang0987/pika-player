package com.pikaworks.pikaplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pikaworks.pikaplayer.data.media.VideoItem
import com.pikaworks.pikaplayer.ui.library.LibraryScreen
import com.pikaworks.pikaplayer.ui.library.LibraryViewModel
import com.pikaworks.pikaplayer.ui.player.PlayerScreen
import com.pikaworks.pikaplayer.ui.player.PlayerViewModel
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
                        positionDao = app.database.playbackPositionDao(),
                    )
                )
                val libraryState by libraryVm.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    // TODO: 권한 온보딩 화면(S5)으로 교체.
                    //  기획서 7.2 — 거부 시 SAF 로 폴더를 직접 고르는 경로가 반드시 필요하다.
                    if (hasMediaPermission()) {
                        libraryVm.refresh()
                    } else {
                        onPermissionResult = { granted -> if (granted) libraryVm.refresh() }
                        requestPermission.launch(mediaPermission)
                    }
                }

                val video = playing
                if (video == null) {
                    LibraryScreen(
                        state = libraryState,
                        onVideoClick = { row -> playing = row.video },
                    )
                } else {
                    val playerVm: PlayerViewModel = viewModel(
                        key = video.uri.toString(),
                        factory = PlayerViewModel.Factory(
                            context = applicationContext,
                            positionDao = app.database.playbackPositionDao(),
                            subtitleMatcher = app.subtitleMatcher,
                        )
                    )
                    val playerState by playerVm.uiState.collectAsStateWithLifecycle()

                    val systemControls = remember { SystemControls(this@MainActivity) }

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
                        onBrightnessDelta = systemControls::adjustBrightness,
                        onVolumeDelta = systemControls::adjustVolume,
                        onBack = { playing = null },
                    )
                }
            }
        }
    }

    private fun hasMediaPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, mediaPermission) == PackageManager.PERMISSION_GRANTED
}
