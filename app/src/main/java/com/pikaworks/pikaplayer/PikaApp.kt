package com.pikaworks.pikaplayer

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import androidx.room.Room
import com.pikaworks.pikaplayer.data.db.PikaDatabase
import com.pikaworks.pikaplayer.data.media.DeviceStorage
import com.pikaworks.pikaplayer.data.media.MediaRescanner
import com.pikaworks.pikaplayer.data.media.MediaStoreSource
import com.pikaworks.pikaplayer.data.media.SafFolderSource
import com.pikaworks.pikaplayer.data.prefs.SettingsStore
import com.pikaworks.pikaplayer.data.subtitle.SubtitleMatcher
import com.pikaworks.pikaplayer.data.vault.VaultStore

/**
 * 의존성을 손으로 엮는다. 1인 개발 규모에서 DI 프레임워크는 아직 값을 하지 않는다.
 * 그래프가 복잡해지면 그때 도입한다.
 */
class PikaApp : Application(), ImageLoaderFactory {

    lateinit var database: PikaDatabase
        private set
    lateinit var mediaStore: MediaStoreSource
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var subtitleMatcher: SubtitleMatcher
        private set
    lateinit var safFolders: SafFolderSource
        private set
    lateinit var deviceStorage: DeviceStorage
    lateinit var mediaRescanner: MediaRescanner
        private set

    /**
     * 화면이 사라진 뒤에도 끝나야 하는 짧은 쓰기용.
     *
     * ViewModel 의 `onCleared()` 시점에는 `viewModelScope` 가 이미 취소돼 있다.
     * 거기서 코루틴을 띄우면 즉시 취소되고 마지막 재생 위치가 저장되지 않는다.
     * 앱과 수명이 같은 스코프가 따로 필요하다.
     */
    val persistScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private set
    lateinit var vault: VaultStore
        private set

    /**
     * Coil 기본 ImageLoader 는 이미지 파일만 다룬다.
     * 동영상 첫 프레임을 썸네일로 쓰려면 VideoFrameDecoder 를 붙여야 한다.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .crossfade(true)
            .build()

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, PikaDatabase::class.java, "pika.db")
            // 아직 출시 전이라 기기에 남은 DB 는 개발용뿐이다. 스키마가 계속 바뀌는
            // 동안 마이그레이션을 손으로 쓰는 것보다 다시 만드는 편이 안전하다.
            // 출시 시점에는 반드시 실제 마이그레이션으로 바꿔야 한다.
            //
            // dropAllTables = true: 스키마가 안 맞으면 테이블을 전부 지우고 새로 만든다.
            // false 면 Room 이 아는 테이블만 지워서, 손으로 만든 테이블이 남는 경우를
            // 위한 값이다 — 여기는 전부 Room 이 만든 것이라 true 가 맞다.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
        mediaStore = MediaStoreSource(this)
        settings = SettingsStore(this)
        subtitleMatcher = SubtitleMatcher(this)
        safFolders = SafFolderSource(this, database.safMetadataDao())
        deviceStorage = DeviceStorage(this)
        mediaRescanner = MediaRescanner(this)
        vault = VaultStore(this)
    }
}
