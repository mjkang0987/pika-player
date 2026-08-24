package com.pikaworks.pikaplayer.entitlement

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntitlementTest {

    @Test
    fun `Pro 는 Pro 기능을 전부 쓴다`() {
        val gate = TierGate { Tier.PRO }
        Feature.entries.forEach { assertTrue(gate.isAllowed(it), "$it 을 Pro 가 못 쓴다") }
    }

    @Test
    fun `Free 는 Pro 기능을 못 쓴다`() {
        val gate = TierGate { Tier.FREE }
        assertFalse(gate.isAllowed(Feature.PICTURE_IN_PICTURE))
        assertFalse(gate.isAllowed(Feature.PRIVATE_FOLDER))
    }

    /**
     * 기획서 3장 분류 원칙 2 — 재생 자체는 절대 막지 않는다.
     * 여기가 깨지면 "돈 내야 영상이 나온다" 는 리뷰가 붙는다.
     */
    @Test
    fun `재생과 자막과 조작은 결제 없이 전부 된다`() {
        val gate = TierGate { Tier.FREE }
        listOf(
            Feature.LOCAL_PLAYBACK,
            Feature.SUBTITLE,
            Feature.GESTURES,
            Feature.RESUME,
            Feature.FOLDER_BROWSE,
            Feature.SEARCH,
            Feature.SLEEP_TIMER,
        ).forEach { assertTrue(gate.isAllowed(it), "$it 은 Free 여야 한다") }
    }

    @Test
    fun `등급이 바뀌면 즉시 반영된다`() {
        var tier = Tier.FREE
        val gate = TierGate { tier }
        assertFalse(gate.isAllowed(Feature.PICTURE_IN_PICTURE))
        tier = Tier.PRO
        assertTrue(gate.isAllowed(Feature.PICTURE_IN_PICTURE))
    }

    @Test
    fun `등급 비교는 자기 자신을 포함한다`() {
        assertTrue(Tier.FREE.atLeast(Tier.FREE))
        assertTrue(Tier.PRO.atLeast(Tier.PRO))
        assertFalse(Tier.FREE.atLeast(Tier.PRO))
    }

    /** 기능을 새로 넣을 때 등급을 빠뜨리지 않았는지. */
    @Test
    fun `모든 기능에 등급이 붙어 있다`() {
        assertEquals(Feature.entries.size, Feature.entries.distinctBy { it.name }.size)
        Feature.entries.forEach { feature ->
            assertTrue(feature.required in Tier.entries, "${feature.name} 등급 누락")
        }
    }
}
