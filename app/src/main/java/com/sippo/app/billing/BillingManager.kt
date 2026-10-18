package com.sippo.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google Play Billing for the single one-time product that unlocks Sippo+.
 *
 * [owned] is null until Play has answered at least once; the app keeps its cached flag meanwhile,
 * so the unlock survives offline launches. Create the in-app product [PRODUCT_ID] in Play Console.
 */
class BillingManager(context: Context) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_ID = "sippo_plus_lifetime"
        private const val TAG = "SippoBilling"
    }

    sealed interface Event {
        data object Purchased : Event
        data object Pending : Event
        data class Failed(val message: String) : Event
    }

    private val _owned = MutableStateFlow<Boolean?>(null)
    val owned: StateFlow<Boolean?> = _owned.asStateFlow()

    private val _price = MutableStateFlow<String?>(null)
    val price: StateFlow<String?> = _price.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 4)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private var details: ProductDetails? = null

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    /** True once Play returned product details, i.e. a real purchase flow can be launched. */
    val canPurchase: Boolean get() = details != null

    fun connect() {
        if (client.isReady) {
            refresh()
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) refresh()
                else Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
            }

            override fun onBillingServiceDisconnected() = Unit // reconnected on next connect()/purchase
        })
    }

    /** Re-reads ownership and price from Play. Also serves as "Restore purchase". */
    fun refresh() {
        if (!client.isReady) return connect()
        val productParams = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
        ).build()
        client.queryProductDetailsAsync(productParams) { result, list ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                details = list.firstOrNull { it.productId == PRODUCT_ID }
                _price.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
            }
        }
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) handle(purchases, fromFlow = false)
        }
    }

    /** Returns false when no purchase flow could be started (e.g. product not configured yet). */
    fun launchPurchase(activity: Activity): Boolean {
        val pd = details ?: run { connect(); return false }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd).build()))
            .build()
        return client.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> handle(purchases.orEmpty(), fromFlow = true)
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> _events.tryEmit(Event.Failed(result.debugMessage.ifEmpty { "Purchase failed" }))
        }
    }

    private fun handle(purchases: List<Purchase>, fromFlow: Boolean) {
        val ours = purchases.filter { PRODUCT_ID in it.products }
        val bought = ours.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        if (bought != null) {
            if (!bought.isAcknowledged) {
                client.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(bought.purchaseToken).build()
                ) { r -> if (r.responseCode != BillingClient.BillingResponseCode.OK) Log.w(TAG, "Ack failed: ${r.debugMessage}") }
            }
            _owned.value = true
            if (fromFlow) _events.tryEmit(Event.Purchased)
        } else if (ours.any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
            if (fromFlow) _events.tryEmit(Event.Pending)
        } else if (!fromFlow) {
            _owned.value = false
        }
    }
}
