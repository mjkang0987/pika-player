package com.pikaworks.pikaplayer.core

/**
 * Pro 기능 접근 판단 지점.
 *
 * 기획서 6장 결정: 기능은 먼저 만들되 결제 분기는 나중에 붙인다.
 * 다만 "이 기능을 쓸 수 있는가"를 묻는 자리는 처음부터 한 곳으로 모아둔다.
 * 이게 없으면 Phase 2 에서 여러 화면에 흩어진 분기를 일일이 심어야 한다.
 *
 * Phase 1 은 [AlwaysAllow] 를 쓴다. Phase 2 에서 결제 상태를 읽는 구현체로 교체한다.
 */
interface FeatureGate {
    fun isAllowed(feature: Feature): Boolean
}

enum class Feature {
    /** Phase 1 범위 (Free) */
    LOCAL_PLAYBACK,
    SUBTITLE,
    GESTURES,

    /** Phase 2 범위 (Pro) — 아직 화면에 노출하지 않는다 */
    PICTURE_IN_PICTURE,
    NETWORK_SOURCE,
    PRIVATE_FOLDER,
}

object AlwaysAllow : FeatureGate {
    override fun isAllowed(feature: Feature): Boolean = true
}
