package com.pikaworks.pikaplayer.data.billing

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pikaworks.pikaplayer.entitlement.Tier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.entitlementDataStore by preferencesDataStore(name = "entitlement")

/**
 * 마지막으로 확인한 결제 등급 캐시.
 *
 * 진실은 Play 스토어에 있다. 이건 스토어에 물어보기 전과 오프라인 구간을 메우는
 * 용도다 — 앱을 켜자마자 산 기능이 잠깐 사라졌다 돌아오는 것을 막는다.
 * 스토어가 답하면 그 답으로 덮어쓴다([com.pikaworks.pikaplayer.entitlement.TierResolver]).
 *
 * 이 값은 기기 안에 평문으로 있다. 루팅한 기기에서 고칠 수 있다는 뜻이고,
 * 서버 없이 막을 방법은 없다 — 기획서 3장이 감수하기로 한 수준이다.
 */
class EntitlementStore(private val context: Context) {

    private val key = stringPreferencesKey("tier")

    val cachedTier: Flow<Tier> = context.entitlementDataStore.data.map { prefs ->
        prefs[key]?.let { name -> runCatching { Tier.valueOf(name) }.getOrNull() } ?: Tier.FREE
    }

    suspend fun save(tier: Tier) {
        context.entitlementDataStore.edit { it[key] = tier.name }
    }
}
