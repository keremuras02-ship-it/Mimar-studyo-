package com.example.core

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val BILLING_TAG = "ProPlanManager"
private val Context.planDataStore: DataStore<Preferences> by preferencesDataStore(name = "mimarstudyo_plan_prefs")

/**
 * Product ID registered in Google Play Console for the one-time MimarStüdyo Pro unlock (99,99 TL).
 */
const val PRO_PRODUCT_ID = "mimarstudyo_pro_lifetime"
const val PRO_DEFAULT_PRICE_DISPLAY = "99,99 ₺"

data class ProEntitlementState(
    val isPro: Boolean = false,
    val formattedPrice: String = PRO_DEFAULT_PRICE_DISPLAY,
    val isBillingConnected: Boolean = false,
    val isStoreProductAvailable: Boolean = false,
    val isProcessingPurchase: Boolean = false,
    val statusMessage: String? = null,
    val dailyRendersUsed: Int = 0,
    val dailyRenderLimit: Int = FREE_DAILY_RENDER_LIMIT,
    val dailyChatsUsed: Int = 0,
    val dailyChatLimit: Int = FREE_DAILY_CHAT_LIMIT,
    val maxFreeProjects: Int = FREE_MAX_CLOUD_PROJECTS,
    val isDebugPreviewPro: Boolean = false
) {
    val remainingFreeRenders: Int
        get() = (dailyRenderLimit - dailyRendersUsed).coerceAtLeast(0)

    val remainingFreeChats: Int
        get() = (dailyChatLimit - dailyChatsUsed).coerceAtLeast(0)

    fun canRunRender(): Boolean = isPro || dailyRendersUsed < dailyRenderLimit

    fun canSendChat(): Boolean = isPro || dailyChatsUsed < dailyChatLimit

    fun canCreateProject(existingCount: Int): Boolean = isPro || existingCount < maxFreeProjects

    companion object {
        const val FREE_DAILY_RENDER_LIMIT = 3
        const val FREE_DAILY_CHAT_LIMIT = 10
        const val FREE_MAX_CLOUD_PROJECTS = 2
    }
}

/**
 * Manages MimarStüdyo Pro one-time in-app purchase (99,99 TL) via Google Play Billing Library,
 * plus per-user daily free-tier quota tracking persisted in DataStore Preferences.
 */
class ProPlanManager(
    context: Context,
    private val userId: String
) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(ProEntitlementState())
    val state: StateFlow<ProEntitlementState> = _state.asStateFlow()

    private var cachedProductDetails: ProductDetails? = null
    private var playStoreOwnsPro: Boolean = false
    private var debugOverridePro: Boolean = false

    private val keyPlayProCached = booleanPreferencesKey("pro_play_cached_$userId")
    private val keyDebugPro = booleanPreferencesKey("pro_debug_override_$userId")
    private val keyQuotaDate = stringPreferencesKey("quota_date_$userId")
    private val keyRendersToday = intPreferencesKey("renders_today_$userId")
    private val keyChatsToday = intPreferencesKey("chats_today_$userId")

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        scope.launch {
            loadLocalPreferences()
            connectBilling()
        }
    }

    private fun todayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private suspend fun loadLocalPreferences() {
        try {
            val prefs = appContext.planDataStore.data.first()
            val today = todayKey()
            val storedDate = prefs[keyQuotaDate]
            val renders = if (storedDate == today) prefs[keyRendersToday] ?: 0 else 0
            val chats = if (storedDate == today) prefs[keyChatsToday] ?: 0 else 0
            playStoreOwnsPro = prefs[keyPlayProCached] ?: false
            debugOverridePro = if (BuildConfig.DEBUG) (prefs[keyDebugPro] ?: false) else false

            if (storedDate != today) {
                appContext.planDataStore.edit { editPrefs ->
                    editPrefs[keyQuotaDate] = today
                    editPrefs[keyRendersToday] = 0
                    editPrefs[keyChatsToday] = 0
                }
            }

            _state.update {
                it.copy(
                    isPro = playStoreOwnsPro || debugOverridePro,
                    isDebugPreviewPro = debugOverridePro && !playStoreOwnsPro,
                    dailyRendersUsed = renders,
                    dailyChatsUsed = chats
                )
            }
        } catch (e: Exception) {
            Log.w(BILLING_TAG, "Failed to load local plan preferences", e)
        }
    }

    fun connectBilling() {
        if (billingClient.isReady) {
            queryProductDetails()
            queryExistingPurchases(notifyIfEmpty = false)
            return
        }
        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _state.update { it.copy(isBillingConnected = true) }
                        queryProductDetails()
                        queryExistingPurchases(notifyIfEmpty = false)
                    } else {
                        Log.w(BILLING_TAG, "Billing setup returned ${billingResult.responseCode}: ${billingResult.debugMessage}")
                        _state.update { it.copy(isBillingConnected = false) }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    _state.update { it.copy(isBillingConnected = false) }
                }
            })
        } catch (e: Exception) {
            Log.w(BILLING_TAG, "Billing startConnection failed", e)
            _state.update { it.copy(isBillingConnected = false) }
        }
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRO_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = productDetailsList.firstOrNull { it.productId == PRO_PRODUCT_ID }
                cachedProductDetails = details
                val priceText = details?.oneTimePurchaseOfferDetails?.formattedPrice
                    ?.takeIf { it.isNotBlank() }
                    ?: PRO_DEFAULT_PRICE_DISPLAY
                _state.update {
                    it.copy(
                        formattedPrice = priceText,
                        isStoreProductAvailable = details != null
                    )
                }
            } else {
                Log.w(BILLING_TAG, "queryProductDetails failed: ${billingResult.responseCode} ${billingResult.debugMessage}")
            }
        }
    }

    fun restorePurchases() {
        if (!billingClient.isReady) {
            _state.update { it.copy(statusMessage = "Google Play Faturalandırma hizmetine bağlanılıyor...") }
            connectBilling()
            return
        }
        queryExistingPurchases(notifyIfEmpty = true)
    }

    fun refreshOnResume() {
        if (billingClient.isReady) {
            queryExistingPurchases(notifyIfEmpty = false)
        } else {
            connectBilling()
        }
    }

    private fun queryExistingPurchases(notifyIfEmpty: Boolean) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                if (notifyIfEmpty) {
                    _state.update { it.copy(statusMessage = "Satın alımlar kontrol edilemedi. Lütfen tekrar deneyin.") }
                }
                return@queryPurchasesAsync
            }

            val validProPurchase = purchases.firstOrNull { purchase ->
                purchase.products.contains(PRO_PRODUCT_ID) &&
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
            }

            if (validProPurchase != null) {
                handleVerifiedPurchase(validProPurchase, announce = notifyIfEmpty)
            } else {
                playStoreOwnsPro = false
                scope.launch {
                    appContext.planDataStore.edit { it[keyPlayProCached] = false }
                }
                _state.update {
                    it.copy(
                        isPro = debugOverridePro,
                        isDebugPreviewPro = debugOverridePro,
                        statusMessage = if (notifyIfEmpty) {
                            "Bu Google hesabında aktif bir MimarStüdyo Pro satın alımı bulunamadı."
                        } else {
                            it.statusMessage
                        }
                    )
                }
            }
        }
    }

    fun launchPurchase(activity: Activity) {
        if (_state.value.isPro) {
            _state.update { it.copy(statusMessage = "MimarStüdyo Pro zaten hesabınızda aktif.") }
            return
        }

        val details = cachedProductDetails
        if (billingClient.isReady && details != null) {
            _state.update { it.copy(isProcessingPurchase = true, statusMessage = null) }
            val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))
                .build()

            val result = billingClient.launchBillingFlow(activity, billingFlowParams)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _state.update {
                    it.copy(
                        isProcessingPurchase = false,
                        statusMessage = "Google Play ödeme penceresi açılamadı (Kod: ${result.responseCode})."
                    )
                }
            }
        } else {
            // Reconnect and explain clearly that Play Console product SKU must be published for real billing
            connectBilling()
            _state.update {
                it.copy(
                    isProcessingPurchase = false,
                    statusMessage = "Google Play Store ürünü ($PRO_PRODUCT_ID • $PRO_DEFAULT_PRICE_DISPLAY) henüz Play Console'da yayınlanmadı. Yayınlandığında ödeme ekranı otomatik açılır."
                )
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val list = purchases.orEmpty()
                val proPurchase = list.firstOrNull { it.products.contains(PRO_PRODUCT_ID) }
                if (proPurchase != null) {
                    when (proPurchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> {
                            handleVerifiedPurchase(proPurchase, announce = true)
                        }
                        Purchase.PurchaseState.PENDING -> {
                            _state.update {
                                it.copy(
                                    isProcessingPurchase = false,
                                    statusMessage = "Ödemeniz beklemede. Google Play onayladığında Pro mod otomatik açılacaktır."
                                )
                            }
                        }
                        else -> {
                            _state.update { it.copy(isProcessingPurchase = false) }
                        }
                    }
                } else {
                    _state.update { it.copy(isProcessingPurchase = false) }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _state.update {
                    it.copy(
                        isProcessingPurchase = false,
                        statusMessage = "Satın alma işlemi iptal edildi."
                    )
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                queryExistingPurchases(notifyIfEmpty = true)
            }
            else -> {
                _state.update {
                    it.copy(
                        isProcessingPurchase = false,
                        statusMessage = "Google Play ödeme hatası (Kod: ${billingResult.responseCode})."
                    )
                }
            }
        }
    }

    private fun handleVerifiedPurchase(purchase: Purchase, announce: Boolean) {
        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(ackParams) { ackResult ->
                if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    grantProFromPlayStore(announce)
                } else {
                    Log.w(BILLING_TAG, "Acknowledge failed: ${ackResult.responseCode}")
                    _state.update {
                        it.copy(
                            isProcessingPurchase = false,
                            statusMessage = "Satın alma onaylanırken hata oluştu. Lütfen 'Geri Yükle' ile tekrar deneyin."
                        )
                    }
                }
            }
        } else {
            grantProFromPlayStore(announce)
        }
    }

    private fun grantProFromPlayStore(announce: Boolean) {
        playStoreOwnsPro = true
        scope.launch {
            appContext.planDataStore.edit { it[keyPlayProCached] = true }
        }
        _state.update {
            it.copy(
                isPro = true,
                isDebugPreviewPro = false,
                isProcessingPurchase = false,
                statusMessage = if (announce) {
                    "Tebrikler! MimarStüdyo Pro aktif edildi — artık tamamen reklamsız ve sınırsız."
                } else {
                    it.statusMessage
                }
            )
        }
    }

    /**
     * Debug-only helper so QA testers on local/emulator builds can verify both Normal (restricted + ads)
     * and Pro (unlimited + ad-free) experiences before Play Console SKU publication.
     */
    fun setDebugProPreview(enabled: Boolean) {
        if (!BuildConfig.DEBUG) return
        debugOverridePro = enabled
        scope.launch {
            appContext.planDataStore.edit { it[keyDebugPro] = enabled }
        }
        _state.update {
            it.copy(
                isPro = playStoreOwnsPro || enabled,
                isDebugPreviewPro = enabled && !playStoreOwnsPro,
                statusMessage = if (enabled) {
                    "Test Modu: Pro Mod (99,99 ₺) aktifleştirildi — reklamlar kapatıldı ve tüm kısıtlamalar açıldı."
                } else {
                    "Test Modu: Normal (Ücretsiz) moda dönüldü — reklamlar ve kısıtlamalar aktif."
                }
            )
        }
    }

    fun recordRenderUsed() {
        if (_state.value.isPro) return
        scope.launch {
            val today = todayKey()
            var newCount = _state.value.dailyRendersUsed + 1
            appContext.planDataStore.edit { prefs ->
                val storedDate = prefs[keyQuotaDate]
                if (storedDate != today) {
                    prefs[keyQuotaDate] = today
                    prefs[keyRendersToday] = 1
                    prefs[keyChatsToday] = 0
                    newCount = 1
                } else {
                    newCount = (prefs[keyRendersToday] ?: 0) + 1
                    prefs[keyRendersToday] = newCount
                }
            }
            _state.update { it.copy(dailyRendersUsed = newCount) }
        }
    }

    fun recordChatUsed() {
        if (_state.value.isPro) return
        scope.launch {
            val today = todayKey()
            var newCount = _state.value.dailyChatsUsed + 1
            appContext.planDataStore.edit { prefs ->
                val storedDate = prefs[keyQuotaDate]
                if (storedDate != today) {
                    prefs[keyQuotaDate] = today
                    prefs[keyRendersToday] = 0
                    prefs[keyChatsToday] = 1
                    newCount = 1
                } else {
                    newCount = (prefs[keyChatsToday] ?: 0) + 1
                    prefs[keyChatsToday] = newCount
                }
            }
            _state.update { it.copy(dailyChatsUsed = newCount) }
        }
    }

    fun clearStatusMessage() {
        _state.update { it.copy(statusMessage = null) }
    }

    fun dispose() {
        try {
            billingClient.endConnection()
        } catch (_: Exception) {
        }
        scope.cancel()
    }
}
