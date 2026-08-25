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
import com.pikaworks.pikaplayer.data.media.MediaDeleter
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
        private set
    lateinit var mediaRescanner: MediaRescanner
        private set
    lateinit var mediaDeleter: MediaDeleter
        private set

    /**
     * 화면이 사라진 뒤에도 끝나야 하는 짧은 쓰기용.
     *
     * ViewModel 의 `onCleared()` 시점에는 `viewModelScope` 가 이미 취소돼 있다.
     * 거기서 코루틴을 띄우면 즉시 취소되고 마지막 재생 위치가 저장되지 않는다.
     * 앱과 수명이 같은 스코프가 따로 필요하다.
     */
    val persistScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
            // 1·2 는 출시 전 개발 빌드에만 있던 버전이다. 그 기기의 DB 는 버려도
            // 되고, 실제로 어떤 모양이었는지 남은 기록도 없어서 옮겨 적을 수가 없다.
            //
            // 여기에 3 을 넣지 않는 것이 요점이다. 전에는 버전을 가리지 않고 지우게
            // 두어서, 스키마를 한 줄만 고쳐도 사용자의 이어보기 기록·재생목록·
            // 비공개 폴더 목록이 조용히 사라졌다. 이제 3 부터는 맞는 마이그레이션이
            // 없으면 앱이 열리다 죽는다 — 데이터를 지우고 계속 도는 것보다, 내보내기
            // 전에 내가 알아차리는 편이 낫다.
            // 첫 인자는 dropAllTables — 지울 때 Room 이 아는 테이블만이 아니라
            // 전부 지운다. 여기 테이블은 모두 Room 이 만든 것이라 이 값이 맞다.
            .fallbackToDestructiveMigrationFrom(true, 1, 2)
            .build()
        mediaStore = MediaStoreSource(this)
        settings = SettingsStore(this)
        subtitleMatcher = SubtitleMatcher(this)
        safFolders = SafFolderSource(this, database.safMetadataDao())
        deviceStorage = DeviceStorage(this)
        mediaRescanner = MediaRescanner(this)
        mediaDeleter = MediaDeleter(this)
        vault = VaultStore(this)
    }
}
