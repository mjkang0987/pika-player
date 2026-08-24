package com.pikaworks.pikaplayer.vault

/** 틀린 횟수와 언제까지 잠겼는지. 기기에 저장해 앱을 껐다 켜도 유지한다. */
data class LockoutState(
    val failedAttempts: Int = 0,
    /** 이 시각까지 입력을 막는다. 0 이면 잠기지 않음. */
    val lockedUntilMs: Long = 0L,
)

/**
 * 반복 시도 차단.
 *
 * 숫자 PIN 은 경우의 수가 적어서, 앱 안에서 마음껏 넣어볼 수 있으면 그냥 뚫린다.
 * 실질적인 방어는 해시 강도가 아니라 여기다.
 *
 * 상태를 기기에 저장하는 이유: 앱을 껐다 켜서 초기화된다면 막는 의미가 없다.
 */
object LockoutPolicy {

    /** 이 횟수까지는 그냥 틀릴 수 있다. 오타를 잠금으로 갚게 하면 안 된다. */
    const val FREE_ATTEMPTS = 4

    /** 초과분 1회마다 늘어나는 대기 시간. 마지막 값에서 멈춘다. */
    val BACKOFF_MS = listOf(30_000L, 60_000L, 300_000L, 900_000L, 1_800_000L)

    fun onFailure(state: LockoutState, nowMs: Long): LockoutState {
        val attempts = state.failedAttempts + 1
        val over = attempts - FREE_ATTEMPTS
        if (over <= 0) return LockoutState(attempts, 0L)
        val wait = BACKOFF_MS[(over - 1).coerceAtMost(BACKOFF_MS.lastIndex)]
        return LockoutState(attempts, nowMs + wait)
    }

    /** 맞으면 깨끗이 지운다. 다음에 오타를 내도 바로 잠기지 않아야 한다. */
    fun onSuccess(): LockoutState = LockoutState()

    fun isLocked(state: LockoutState, nowMs: Long): Boolean = remainingMs(state, nowMs) > 0

    fun remainingMs(state: LockoutState, nowMs: Long): Long =
        (state.lockedUntilMs - nowMs).coerceAtLeast(0L)
}
