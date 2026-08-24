package com.pikaworks.pikaplayer.entitlement

/**
 * 결제 등급.
 *
 * 기획서 3장: Pro 기능은 전부 온디바이스라 서버 비용이 0 이다. 그래서 일회성
 * 구매 하나로 간다. 서버 연산이 드는 기능(AI 업스케일링 등)을 위한 구독 등급은
 * 그 기능을 실제로 만들 때 다시 넣는다 — 팔 것이 없는데 등급만 두면 화면에도
 * 코드에도 빈칸이 남는다.
 *
 * 등급은 겹쳐 쌓이는 구조로 둔다. 나중에 등급이 늘어도 [atLeast] 는 그대로다.
 */
enum class Tier {
    FREE,
    PRO;

    /** [other] 이상인가. */
    fun atLeast(other: Tier): Boolean = ordinal >= other.ordinal
}

/**
 * 기능과 그 기능에 필요한 등급.
 *
 * 기획서 3장 "기능별 티어 분류" 를 코드로 옮긴 것이다. 표와 코드가 어긋나면
 * 사용자에게는 코드가 진실이므로, 분류를 바꿀 때는 기획서도 같이 고쳐야 한다.
 */
enum class Feature(val required: Tier) {
    // Free — 재생 자체는 절대 막지 않는다(기획서 3장 분류 원칙 2).
    LOCAL_PLAYBACK(Tier.FREE),
    SUBTITLE(Tier.FREE),
    GESTURES(Tier.FREE),
    RESUME(Tier.FREE),
    FOLDER_BROWSE(Tier.FREE),
    SEARCH(Tier.FREE),
    SLEEP_TIMER(Tier.FREE),

    // Pro — 온디바이스. 일회성 구매.
    PICTURE_IN_PICTURE(Tier.PRO),
    NETWORK_SOURCE(Tier.PRO),
    PRIVATE_FOLDER(Tier.PRO),
    CLOUD_SOURCE(Tier.PRO),
    WIFI_TRANSFER(Tier.PRO),
    CHROMECAST(Tier.PRO),
    PARENTAL_LOCK(Tier.PRO),
    MULTI_PLAYLIST(Tier.PRO),
}

/** "이 기능을 쓸 수 있는가" 를 묻는 단 하나의 자리. */
fun interface FeatureGate {
    fun isAllowed(feature: Feature): Boolean
}

/** 등급으로 판단하는 기본 구현. */
class TierGate(private val tier: () -> Tier) : FeatureGate {
    override fun isAllowed(feature: Feature): Boolean = tier().atLeast(feature.required)
}

/** 개발·스크린샷용. 전부 허용한다. 출시 빌드에 들어가면 안 된다. */
val AllowAll = FeatureGate { true }
