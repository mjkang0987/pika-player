package com.pikaworks.pikaplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pikaworks.pikaplayer.ui.library.LibraryScreen
import com.pikaworks.pikaplayer.ui.library.LibraryViewModel
import com.pikaworks.pikaplayer.ui.theme.PikaTheme

class MainActivity : ComponentActivity() {

    private val mediaPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
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
                val vm: LibraryViewModel = viewModel(
                    factory = LibraryViewModel.Factory(
                        mediaStore = app.mediaStore,
                        positionDao = app.database.playbackPositionDao(),
                    )
                )
                val state by vm.uiState.collectAsStateWithLifecycle()

                // TODO: 권한 온보딩 화면(S5)으로 교체한다.
                //  지금은 진입 시 바로 요청하고, 거부하면 빈 목록이 뜬다.
                //  기획서 7.2 — 거부 시 SAF 로 폴더를 직접 고르는 경로가 반드시 필요하다.
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    if (hasMediaPermission()) {
                        vm.refresh()
                    } else {
                        onPermissionResult = { granted -> if (granted) vm.refresh() }
                        requestPermission.launch(mediaPermission)
                    }
                }

                LibraryScreen(
                    state = state,
                    onVideoClick = { /* TODO: 플레이어 화면(S3)으로 이동 */ },
                )
            }
        }
    }

    private fun hasMediaPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, mediaPermission) == PackageManager.PERMISSION_GRANTED
}
