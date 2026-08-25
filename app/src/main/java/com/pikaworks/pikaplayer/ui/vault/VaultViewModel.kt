package com.pikaworks.pikaplayer.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pikaworks.pikaplayer.data.vault.VaultSettings
import com.pikaworks.pikaplayer.data.vault.VaultStore
import com.pikaworks.pikaplayer.vault.LockoutPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** PIN 화면이 지금 무엇을 하는 중인가. */
enum class PinMode {
    NONE,
    SET,
    CONFIRM,

    /** 감춘 폴더를 보려고 푸는 중. */
    UNLOCK,

    /**
     * 재생 화면의 어린이 잠금을 푸는 중.
     *
     * 확인 절차는 [UNLOCK] 과 같지만 결과가 다르다 — 감춘 폴더까지 같이 열리면
     * 아이에게 영상을 보여 주려고 잠금을 푼 것만으로 숨겨 둔 폴더가 드러난다.
     */
    CHILD_UNLOCK,
}

data class VaultUiState(
    val settings: VaultSettings = VaultSettings(),
    val mode: PinMode = PinMode.NONE,
    val entered: String = "",
    val error: String? = null,
    /** 남은 잠금 시간(ms). 화면이 초 단위로 다시 그리지 않도록 값만 준다. */
    val lockedForMs: Long = 0L,
    /**
     * 이번 실행에서 잠금을 풀었는가.
     *
     * 기기에 저장하지 않는다. 앱을 껐다 켜면 다시 잠기는 것이 이 기능의 요점이다.
     */
    val unlocked: Boolean = false,
    /**
     * 어린이 잠금을 방금 풀었다는 신호. 한 번 쓰고 [VaultViewModel.consumeChildUnlock]
     * 으로 되돌린다. 남겨 두면 다음에 잠글 때 곧바로 다시 풀린다.
     */
    val childUnlocked: Boolean = false,
) {
    val hiddenFolders: Set<String> get() = settings.hiddenFolders

    /** 목록에서 감출 폴더. 잠금을 푼 동안에는 아무것도 감추지 않는다. */
    val foldersToHide: Set<String>
        get() = if (unlocked || !settings.enabled) emptySet() else settings.hiddenFolders
}

/**
 * 비공개 폴더(Pro).
 *
 * PIN 확인은 일부러 무겁고(해시 12만 회) 실패 시 잠금 상태를 기기에 남긴다.
 * 그래서 화면이 아니라 여기서 순서를 지킨다 — 잠겼는지 먼저 보고, 확인하고,
 * 결과를 저장한다.
 */
class VaultViewModel(private val store: VaultStore) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    /** 확인 단계에서 첫 입력을 들고 있는다. 화면 상태에 두면 화면 회전에 노출된다. */
    private var firstEntry: String? = null

    init {
        viewModelScope.launch {
            store.settings.collect { s -> _uiState.value = _uiState.value.copy(settings = s) }
        }
    }

    fun startSet() {
        firstEntry = null
        _uiState.value = _uiState.value.copy(mode = PinMode.SET, entered = "", error = null)
    }

    fun startUnlock(nowMs: Long) {
        _uiState.value = _uiState.value.copy(
            mode = PinMode.UNLOCK,
            entered = "",
            error = null,
            lockedForMs = LockoutPolicy.remainingMs(_uiState.value.settings.lockout, nowMs),
        )
    }

    /** 재생 화면의 잠금을 풀려고 PIN 을 묻는다. */
    fun startChildUnlock(nowMs: Long) {
        _uiState.value = _uiState.value.copy(
            mode = PinMode.CHILD_UNLOCK,
            entered = "",
            error = null,
            lockedForMs = LockoutPolicy.remainingMs(_uiState.value.settings.lockout, nowMs),
        )
    }

    fun consumeChildUnlock() {
        _uiState.value = _uiState.value.copy(childUnlocked = false)
    }

    fun cancel() {
        firstEntry = null
        _uiState.value = _uiState.value.copy(mode = PinMode.NONE, entered = "", error = null)
    }

    fun onDigit(digit: Char, nowMs: Long) {
        val state = _uiState.value
        if (state.entered.length >= PIN_LENGTH) return
        val next = state.entered + digit
        _uiState.value = state.copy(entered = next, error = null)
        if (next.length == PIN_LENGTH) submit(next, nowMs)
    }

    fun onBackspace() {
        val state = _uiState.value
        if (state.entered.isEmpty()) return
        _uiState.value = state.copy(entered = state.entered.dropLast(1), error = null)
    }

    private fun submit(pin: String, nowMs: Long) {
        when (_uiState.value.mode) {
            PinMode.SET -> {
                firstEntry = pin
                _uiState.value = _uiState.value.copy(mode = PinMode.CONFIRM, entered = "")
            }

            PinMode.CONFIRM -> {
                if (pin == firstEntry) {
                    viewModelScope.launch {
                        store.setPin(pin)
                        firstEntry = null
                        _uiState.value = _uiState.value.copy(mode = PinMode.NONE, entered = "", unlocked = true)
                    }
                } else {
                    firstEntry = null
                    _uiState.value = _uiState.value.copy(
                        mode = PinMode.SET,
                        entered = "",
                        error = "두 번 입력한 값이 다릅니다",
                    )
                }
            }

            PinMode.UNLOCK, PinMode.CHILD_UNLOCK -> unlock(pin, nowMs)
            PinMode.NONE -> Unit
        }
    }

    private fun unlock(pin: String, nowMs: Long) {
        val lockout = _uiState.value.settings.lockout
        if (LockoutPolicy.isLocked(lockout, nowMs)) {
            _uiState.value = _uiState.value.copy(
                entered = "",
                lockedForMs = LockoutPolicy.remainingMs(lockout, nowMs),
            )
            return
        }
        viewModelScope.launch {
            if (store.verifyPin(pin)) {
                store.saveLockout(LockoutPolicy.onSuccess())
                val forChildLock = _uiState.value.mode == PinMode.CHILD_UNLOCK
                _uiState.value = _uiState.value.copy(
                    mode = PinMode.NONE,
                    entered = "",
                    error = null,
                    lockedForMs = 0L,
                    // 어린이 잠금을 푼 것으로 감춘 폴더까지 열지는 않는다.
                    unlocked = _uiState.value.unlocked || !forChildLock,
                    childUnlocked = forChildLock,
                )
            } else {
                val next = LockoutPolicy.onFailure(lockout, nowMs)
                store.saveLockout(next)
                _uiState.value = _uiState.value.copy(
                    entered = "",
                    error = "PIN 이 맞지 않습니다",
                    lockedForMs = LockoutPolicy.remainingMs(next, nowMs),
                )
            }
        }
    }

    /** 목록으로 돌아갈 때 다시 잠근다. 켜 둔 채로 폰을 넘기면 의미가 없다. */
    fun lock() {
        _uiState.value = _uiState.value.copy(unlocked = false)
    }

    fun setHidden(folderKey: String, hidden: Boolean) {
        viewModelScope.launch { store.setHidden(folderKey, hidden) }
    }

    fun disable() {
        viewModelScope.launch {
            store.disable()
            _uiState.value = _uiState.value.copy(unlocked = false, mode = PinMode.NONE, entered = "")
        }
    }

    class Factory(private val store: VaultStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = VaultViewModel(store) as T
    }
}
