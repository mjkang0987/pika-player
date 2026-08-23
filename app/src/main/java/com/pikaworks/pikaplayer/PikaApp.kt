package com.pikaworks.pikaplayer

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import androidx.room.Room
import com.pikaworks.pikaplayer.core.AlwaysAllow
import com.pikaworks.pikaplayer.core.FeatureGate
import com.pikaworks.pikaplayer.data.db.PikaDatabase
import com.pikaworks.pikaplayer.data.media.DeviceStorage
import com.pikaworks.pikaplayer.data.media.MediaStoreSource
import com.pikaworks.pikaplayer.data.media.SafFolderSource
import com.pikaworks.pikaplayer.data.prefs.SettingsStore
import com.pikaworks.pikaplayer.data.subtitle.SubtitleMatcher

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

    /** Phase 1 은 모두 허용. Phase 2 에서 결제 상태를 읽는 구현으로 교체한다. */
    val featureGate: FeatureGate = AlwaysAllow

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
            .fallbackToDestructiveMigration()
            .build()
        mediaStore = MediaStoreSource(this)
        settings = SettingsStore(this)
        subtitleMatcher = SubtitleMatcher(this)
        safFolders = SafFolderSource(this, database.safMetadataDao())
        deviceStorage = DeviceStorage(this)
    }
}
