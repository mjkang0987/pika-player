package com.pikaworks.pikaplayer

import android.app.Application
import androidx.room.Room
import com.pikaworks.pikaplayer.core.AlwaysAllow
import com.pikaworks.pikaplayer.core.FeatureGate
import com.pikaworks.pikaplayer.data.db.PikaDatabase
import com.pikaworks.pikaplayer.data.media.MediaStoreSource
import com.pikaworks.pikaplayer.data.prefs.SettingsStore
import com.pikaworks.pikaplayer.data.subtitle.SubtitleMatcher

/**
 * 의존성을 손으로 엮는다. 1인 개발 규모에서 DI 프레임워크는 아직 값을 하지 않는다.
 * 그래프가 복잡해지면 그때 도입한다.
 */
class PikaApp : Application() {

    lateinit var database: PikaDatabase
        private set
    lateinit var mediaStore: MediaStoreSource
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var subtitleMatcher: SubtitleMatcher
        private set

    /** Phase 1 은 모두 허용. Phase 2 에서 결제 상태를 읽는 구현으로 교체한다. */
    val featureGate: FeatureGate = AlwaysAllow

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, PikaDatabase::class.java, "pika.db").build()
        mediaStore = MediaStoreSource(this)
        settings = SettingsStore(this)
        subtitleMatcher = SubtitleMatcher(this)
    }
}
