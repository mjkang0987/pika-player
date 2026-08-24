package com.pikaworks.pikaplayer.data.vault

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pikaworks.pikaplayer.vault.LockoutState
import com.pikaworks.pikaplayer.vault.PinHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.vaultDataStore by preferencesDataStore(name = "vault")

/** 비공개 폴더 설정. PIN 자체는 저장하지 않고 해시와 소금만 남는다. */
data class VaultSettings(
    val enabled: Boolean = false,
    /** 숨긴 폴더의 [com.pikaworks.pikaplayer.data.media.VideoItem.folderKey] 모음 */
    val hiddenFolders: Set<String> = emptySet(),
    val lockout: LockoutState = LockoutState(),
)

/**
 * 비공개 폴더(Pro).
 *
 * "잠긴 폴더" 이지 "암호화된 폴더" 가 아니다 — 파일 자체는 그대로 있고 목록에서
 * 감출 뿐이다. 다른 앱이나 PC 에 연결하면 보인다. [PinHasher] 주석 참고.
 */
class VaultStore(private val context: Context) {

    private object Keys {
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val HIDDEN = stringSetPreferencesKey("hidden_folders")
        val FAILED = intPreferencesKey("failed_attempts")
        val LOCKED_UNTIL = longPreferencesKey("locked_until")
    }

    val settings: Flow<VaultSettings> = context.vaultDataStore.data.map { p ->
        VaultSettings(
            enabled = p[Keys.PIN_HASH] != null,
            hiddenFolders = p[Keys.HIDDEN] ?: emptySet(),
            lockout = LockoutState(
                failedAttempts = p[Keys.FAILED] ?: 0,
                lockedUntilMs = p[Keys.LOCKED_UNTIL] ?: 0L,
            ),
        )
    }

    /** PIN 을 새로 정한다. 소금도 새로 만든다 — 같은 PIN 이어도 저장값이 달라진다. */
    suspend fun setPin(pin: String) = withContext(Dispatchers.Default) {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash(pin, salt)
        context.vaultDataStore.edit {
            it[Keys.PIN_HASH] = encode(hash)
            it[Keys.PIN_SALT] = encode(salt)
            it[Keys.FAILED] = 0
            it[Keys.LOCKED_UNTIL] = 0L
        }
    }

    /**
     * 해시 계산은 일부러 무겁다(12만 회). 메인 스레드에서 돌리면 화면이 멈춘다.
     *
     * 반환값이 true 라고 화면을 열어주면 안 된다. 잠금 여부는 호출하는 쪽에서
     * [VaultSettings.lockout] 으로 먼저 확인한다 — 여기서 같이 처리하면 저장소가
     * 시간을 알아야 해서 테스트가 어려워진다.
     */
    suspend fun verifyPin(pin: String): Boolean = withContext(Dispatchers.Default) {
        val prefs = context.vaultDataStore.data.first()
        val hash = prefs[Keys.PIN_HASH]?.let(::decode) ?: return@withContext false
        val salt = prefs[Keys.PIN_SALT]?.let(::decode) ?: return@withContext false
        PinHasher.verify(pin, salt, hash)
    }

    suspend fun saveLockout(state: LockoutState) {
        context.vaultDataStore.edit {
            it[Keys.FAILED] = state.failedAttempts
            it[Keys.LOCKED_UNTIL] = state.lockedUntilMs
        }
    }

    suspend fun setHidden(folderKey: String, hidden: Boolean) {
        context.vaultDataStore.edit { prefs ->
            val current = prefs[Keys.HIDDEN] ?: emptySet()
            prefs[Keys.HIDDEN] = if (hidden) current + folderKey else current - folderKey
        }
    }

    /** 비공개 폴더를 끈다. 숨긴 목록도 같이 지운다 — 끄고 켰더니 그대로면 놀란다. */
    suspend fun disable() {
        context.vaultDataStore.edit { it.clear() }
    }

    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(text: String) = runCatching { Base64.decode(text, Base64.NO_WRAP) }.getOrNull()
}
