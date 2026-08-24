package com.pikaworks.pikaplayer.entitlement

/** 스토어에 물어본 결과. */
sealed interface StoreResult {
    /** 아직 답을 못 받았다 — 연결 전, 오프라인, 조회 실패. */
    data object Unknown : StoreResult

    /** 스토어가 답했다. 보유한 상품에서 계산한 등급. */
    data class Owned(val tier: Tier) : StoreResult
}

/**
 * 캐시된 등급과 스토어 응답을 합쳐 지금 등급을 정한다.
 *
 * 두 가지를 동시에 지켜야 한다.
 *
 * 1. **오프라인에서 산 사람의 기능을 뺏지 않는다.** 스토어에 못 물어봤다는 이유로
 *    Free 로 떨어뜨리면, 비행기 안에서 Pro 기능이 사라진다.
 * 2. **환불·해지는 실제로 반영된다.** 스토어가 "가진 것 없음" 이라고 답했으면
 *    캐시가 뭐라고 하든 그 답을 따른다. 안 그러면 캐시가 영구 Pro 가 된다.
 *
 * 이 두 줄이 서로 반대 방향이라, 어느 한쪽만 보고 짜면 반드시 다른 쪽이 깨진다.
 */
object TierResolver {

    fun resolve(cached: Tier, store: StoreResult): Tier = when (store) {
        StoreResult.Unknown -> cached
        is StoreResult.Owned -> store.tier
    }

    /** 보유 상품 id 목록에서 등급을 계산한다. */
    fun tierOf(ownedProductIds: Collection<String>, proId: String): Tier =
        if (ownedProductIds.contains(proId)) Tier.PRO else Tier.FREE
}
