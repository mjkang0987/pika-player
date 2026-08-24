package com.pikaworks.pikaplayer.entitlement

import kotlin.test.Test
import kotlin.test.assertEquals

class TierResolverTest {

    private val PRO = "pika_pro"

    @Test
    fun `스토어에 못 물어봤으면 캐시를 그대로 쓴다`() {
        assertEquals(Tier.PRO, TierResolver.resolve(Tier.PRO, StoreResult.Unknown))
        assertEquals(Tier.FREE, TierResolver.resolve(Tier.FREE, StoreResult.Unknown))
    }

    /** 오프라인에서 Pro 기능이 사라지면 안 된다. */
    @Test
    fun `오프라인에서 산 사람의 기능을 뺏지 않는다`() {
        val gate = TierGate { TierResolver.resolve(Tier.PRO, StoreResult.Unknown) }
        assertEquals(true, gate.isAllowed(Feature.PICTURE_IN_PICTURE))
    }

    /** 환불·해지가 반영되지 않으면 캐시가 영구 Pro 가 된다. */
    @Test
    fun `스토어가 없다고 답하면 캐시를 무시하고 내린다`() {
        assertEquals(Tier.FREE, TierResolver.resolve(Tier.PRO, StoreResult.Owned(Tier.FREE)))
    }

    @Test
    fun `스토어 응답이 캐시보다 높으면 올린다`() {
        assertEquals(Tier.PRO, TierResolver.resolve(Tier.FREE, StoreResult.Owned(Tier.PRO)))
    }

    @Test
    fun `보유 상품에서 등급을 계산한다`() {
        assertEquals(Tier.FREE, TierResolver.tierOf(emptyList(), PRO))
        assertEquals(Tier.PRO, TierResolver.tierOf(listOf(PRO), PRO))
    }

    @Test
    fun `모르는 상품 id 는 등급을 올리지 않는다`() {
        assertEquals(Tier.FREE, TierResolver.tierOf(listOf("something_else"), PRO))
        assertEquals(Tier.PRO, TierResolver.tierOf(listOf("something_else", PRO), PRO))
    }
}
