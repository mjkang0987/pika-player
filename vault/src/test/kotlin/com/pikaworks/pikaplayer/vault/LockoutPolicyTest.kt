package com.pikaworks.pikaplayer.vault

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LockoutPolicyTest {

    private val now = 1_000_000L

    /** 오타 몇 번을 잠금으로 갚게 하면 안 된다. */
    @Test
    fun `처음 네 번은 잠기지 않는다`() {
        var s = LockoutState()
        repeat(LockoutPolicy.FREE_ATTEMPTS) {
            s = LockoutPolicy.onFailure(s, now)
            assertFalse(LockoutPolicy.isLocked(s, now), "${s.failedAttempts}번째에 잠겼다")
        }
        assertEquals(LockoutPolicy.FREE_ATTEMPTS, s.failedAttempts)
    }

    @Test
    fun `다섯 번째부터 잠긴다`() {
        var s = LockoutState()
        repeat(LockoutPolicy.FREE_ATTEMPTS + 1) { s = LockoutPolicy.onFailure(s, now) }
        assertTrue(LockoutPolicy.isLocked(s, now))
        assertEquals(LockoutPolicy.BACKOFF_MS.first(), LockoutPolicy.remainingMs(s, now))
    }

    @Test
    fun `틀릴수록 대기가 길어지고 마지막 값에서 멈춘다`() {
        var s = LockoutState()
        repeat(LockoutPolicy.FREE_ATTEMPTS) { s = LockoutPolicy.onFailure(s, now) }
        LockoutPolicy.BACKOFF_MS.forEach { expected ->
            s = LockoutPolicy.onFailure(s, now)
            assertEquals(expected, LockoutPolicy.remainingMs(s, now))
        }
        // 더 틀려도 마지막 값 그대로
        repeat(3) {
            s = LockoutPolicy.onFailure(s, now)
            assertEquals(LockoutPolicy.BACKOFF_MS.last(), LockoutPolicy.remainingMs(s, now))
        }
    }

    @Test
    fun `시간이 지나면 풀린다`() {
        var s = LockoutState()
        repeat(LockoutPolicy.FREE_ATTEMPTS + 1) { s = LockoutPolicy.onFailure(s, now) }
        val wait = LockoutPolicy.BACKOFF_MS.first()
        assertTrue(LockoutPolicy.isLocked(s, now + wait - 1))
        assertFalse(LockoutPolicy.isLocked(s, now + wait))
        assertEquals(0L, LockoutPolicy.remainingMs(s, now + wait + 5_000))
    }

    /** 앱을 껐다 켜서 초기화되면 막는 의미가 없다 — 저장하는 값이 그대로 유지되는지. */
    @Test
    fun `잠금 해제 시각은 절대 시각이라 다시 계산해도 같다`() {
        var s = LockoutState()
        repeat(LockoutPolicy.FREE_ATTEMPTS + 1) { s = LockoutPolicy.onFailure(s, now) }
        val restored = LockoutState(s.failedAttempts, s.lockedUntilMs)
        assertEquals(LockoutPolicy.remainingMs(s, now), LockoutPolicy.remainingMs(restored, now))
    }

    @Test
    fun `맞으면 깨끗이 지워진다`() {
        var s = LockoutState()
        repeat(10) { s = LockoutPolicy.onFailure(s, now) }
        val cleared = LockoutPolicy.onSuccess()
        assertEquals(0, cleared.failedAttempts)
        assertFalse(LockoutPolicy.isLocked(cleared, now))
    }
}
