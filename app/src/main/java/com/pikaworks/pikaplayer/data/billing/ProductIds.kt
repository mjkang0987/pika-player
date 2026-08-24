package com.pikaworks.pikaplayer.data.billing

/**
 * Play Console 에 등록할 상품 id.
 *
 * 한 번 등록하면 바꿀 수 없다. 여기 값과 콘솔 값이 다르면 구매가 조회되지 않고,
 * 산 사람이 Free 로 보인다.
 */
object ProductIds {
    /** 일회성 인앱결제 — Pro 기능 전부. 지금 파는 상품은 이것 하나다. */
    const val PRO = "pika_pro"
}
