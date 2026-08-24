package com.pikaworks.pikaplayer.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.pikaworks.pikaplayer.entitlement.StoreResult
import com.pikaworks.pikaplayer.entitlement.Tier
import com.pikaworks.pikaplayer.entitlement.TierResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

/**
 * Play 결제.
 *
 * **Play Billing SDK 를 만지는 곳은 이 파일 하나뿐이다.** 바깥은 [Tier] 만 안다.
 * SDK 버전이 올라가 API 가 바뀌어도 고칠 곳이 여기로 한정되고, 결제를 붙이기
 * 전까지 나머지 코드가 컴파일되지 않는 일도 없다.
 *
 * 등급 판단 규칙 자체는 순수 모듈([TierResolver])에 있고 테스트로 못박혀 있다.
 * 여기서는 스토어에 묻고 답을 그 규칙에 넘기는 일만 한다.
 */
class BillingRepository(
    context: Context,
    private val scope: CoroutineScope,
    private val cache: EntitlementStore,
) {

    /** 스토어에서 받은 결과. 아직 못 물어봤으면 Unknown. */
    private val storeResult = MutableStateFlow<StoreResult>(StoreResult.Unknown)

    /** launchBillingFlow 에 넘길 원본. 바깥으로 내보내지 않는다. */
    private var details: Map<String, ProductDetails> = emptyMap()

    /** 구매 화면에 채울 값. SDK 타입이 화면까지 새지 않도록 우리 형태로 바꿔 둔다. */
    private val _products = MutableStateFlow<Map<String, ProductInfo>>(emptyMap())
    val products: StateFlow<Map<String, ProductInfo>> = _products.asStateFlow()

    /** 지금 등급. 캐시로 시작해 스토어 답이 오면 그것으로 바뀐다. */
    val tier: StateFlow<Tier> = combine(cache.cachedTier, storeResult) { cached, store ->
        TierResolver.resolve(cached, store)
    }.stateIn(scope, SharingStarted.Eagerly, Tier.FREE)

    private val purchasesListener = { result: BillingResult, purchases: List<Purchase>? ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        }
        // 사용자가 취소했거나(USER_CANCELED) 실패한 경우는 등급을 건드리지 않는다.
        // 여기서 Free 로 내리면 이미 산 사람이 구매를 한 번 취소했다는 이유로 잃는다.
        Unit
    }

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(purchasesListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    /** 앱 시작 시 한 번. 연결이 끊기면 다음 조회 때 다시 잇는다. */
    fun start() {
        connect { refresh() }
    }

    private fun connect(onReady: () -> Unit) {
        if (client.isReady) {
            onReady()
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) onReady()
                // 실패는 Unknown 으로 남긴다 — 캐시 등급이 유지된다.
            }

            override fun onBillingServiceDisconnected() {
                // 다시 잇는 것은 다음 호출에 맡긴다. 여기서 재시도 루프를 돌리면
                // Play 가 없는 기기에서 끝없이 재시도한다.
            }
        })
    }

    /** 보유 상품을 다시 확인한다. 화면으로 돌아올 때마다 부르면 환불·해지가 반영된다. */
    fun refresh() {
        connect {
            val owned = mutableSetOf<String>()
            var pending = 2

            fun done() {
                pending -= 1
                if (pending == 0) {
                    storeResult.value = StoreResult.Owned(
                        TierResolver.tierOf(owned, ProductIds.PRO, ProductIds.PRO_PLUS)
                    )
                    scope.launch { cache.save(tier.value) }
                }
            }

            listOf(BillingClient.ProductType.INAPP, BillingClient.ProductType.SUBS).forEach { type ->
                client.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(type).build()
                ) { result, purchases ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        owned += purchases
                            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                            .flatMap { it.products }
                        acknowledgeIfNeeded(purchases)
                    }
                    done()
                }
            }
        }
    }

    /** 구매 화면에 이름·가격을 채우기 위해 상품 정보를 받아 둔다. */
    fun loadProducts() {
        connect {
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(ProductIds.PRO)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build(),
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(ProductIds.PRO_PLUS)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build(),
                    )
                )
                .build()
            client.queryProductDetailsAsync(params) { result, list ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    details = list.associateBy { it.productId }
                    _products.value = details.mapValues { (_, d) -> d.toInfo() }
                }
            }
        }
    }

    /** 구매 화면을 띄운다. 결과는 [purchasesListener] 로 돌아온다. */
    fun purchase(activity: Activity, productId: String) {
        val product = details[productId] ?: return
        val params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .apply {
                // 구독은 어느 요금제인지까지 지정해야 한다. 일회성 상품에는 없는 값이다.
                product.subscriptionOfferDetails?.firstOrNull()?.let { setOfferToken(it.offerToken) }
            }
            .build()
        client.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build(),
        )
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        acknowledgeIfNeeded(purchases)
        // 구매 직후에는 이 목록이 방금 산 것만 담고 있다. 전체를 다시 물어야
        // 이미 가진 다른 상품이 사라지지 않는다.
        refresh()
    }

    /**
     * 확인하지 않은 구매는 3일 뒤 자동 환불된다. 결제가 됐는데 앱이 조용히
     * 되돌려지는 가장 흔한 사고 지점이라 조회할 때마다 확인한다.
     */
    private fun acknowledgeIfNeeded(purchases: List<Purchase>) {
        purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach { purchase ->
                client.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                ) { /* 실패하면 다음 refresh 에서 다시 시도한다 */ }
            }
    }
}

/** 화면이 쓰는 상품 정보. Play SDK 타입을 UI 로 끌고 가지 않기 위한 것. */
data class ProductInfo(
    val id: String,
    val title: String,
    /** 통화 기호까지 붙은 표시용 가격. 못 받았으면 null. */
    val formattedPrice: String?,
)

/**
 * 가격이 붙어 있는 자리가 상품 종류마다 다르다. 일회성은 하나뿐이고,
 * 구독은 요금제(offer) 안의 단계(phase)에 들어 있다.
 */
private fun ProductDetails.toInfo(): ProductInfo = ProductInfo(
    id = productId,
    title = name.ifBlank { title },
    formattedPrice = oneTimePurchaseOfferDetails?.formattedPrice
        ?: subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice,
)
