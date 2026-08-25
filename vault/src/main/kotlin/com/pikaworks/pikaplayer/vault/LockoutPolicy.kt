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

    /**
     * 초과분 1회마다 늘어나는 대기 시간. 마지막 값에서 멈춘다.
     *
     * 값이 하나뿐이라 늘어나지 않는다 — 몇 번을 틀려도 30초다.
     *
     * 전에는 30분까지 늘어났다. 이 자물쇠를 두드리는 사람은 거의 언제나 자기
     * PIN 을 헷갈린 본인이고, 자기 영상을 보려다 30분을 갇히는 것은 막아서 얻는
     * 것보다 잃는 것이 크다.
     *
     * 30초로도 기계로 찍어 보는 쪽은 막힌다. 네 자리 만 가지를 30초 간격으로
     * 손으로 훑으면 여든 시간이 넘게 걸린다. 애초에 이 잠금은 파일을 암호화하지
     * 않고 목록에서 감출 뿐이라(개인정보 처리방침에 그렇게 적었다), 작정한
     * 사람은 파일 관리자로 바로 간다. 여기서 막는 것은 남의 폰을 집어 든 손이다.
     */
    val BACKOFF_MS = listOf(30_000L)

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
