package com.octsun.cooly.data

import com.octsun.cooly.config.RevenueCatConfig
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Result of a purchase/restore attempt, for user-facing feedback. */
sealed interface PurchaseOutcome {
    data object Success : PurchaseOutcome
    data object Cancelled : PurchaseOutcome
    data object NothingToRestore : PurchaseOutcome
    data class Failed(val message: String?) : PurchaseOutcome
}

/**
 * RevenueCat-backed entitlements. [configure] must be called once per process before use;
 * it is a no-op when the platform API key is blank (SDK stays un-initialized).
 */
class RevenueCatEntitlements : Entitlements {

    private val _isPlus = MutableStateFlow(false)
    override val isPlus: StateFlow<Boolean> = _isPlus.asStateFlow()

    override suspend fun refresh() {
        runCatching {
            update(Purchases.sharedInstance.awaitCustomerInfo())
        }
    }

    /** Buys the first available package of the current Offering (single-product setup). */
    suspend fun purchasePlus(): PurchaseOutcome = try {
        val offerings = Purchases.sharedInstance.awaitOfferings()
        val pkg = offerings.current?.availablePackages?.firstOrNull()
            ?: return PurchaseOutcome.Failed("No offering configured")
        val result = Purchases.sharedInstance.awaitPurchase(pkg)
        update(result.customerInfo)
        if (_isPlus.value) PurchaseOutcome.Success else PurchaseOutcome.Failed(null)
    } catch (e: PurchasesTransactionException) {
        if (e.userCancelled) PurchaseOutcome.Cancelled else PurchaseOutcome.Failed(e.message)
    } catch (e: Exception) {
        PurchaseOutcome.Failed(e.message)
    }

    /** Localized store price of the current offering's first package (paywall display). */
    suspend fun plusPriceLabel(): String? = runCatching {
        Purchases.sharedInstance.awaitOfferings()
            .current?.availablePackages?.firstOrNull()
            ?.storeProduct?.price?.formatted
    }.getOrNull()

    /** Restores previous purchases (App Store guideline 3.1.2 requires this affordance). */
    suspend fun restore(): PurchaseOutcome = try {
        update(Purchases.sharedInstance.awaitRestore())
        if (_isPlus.value) PurchaseOutcome.Success else PurchaseOutcome.NothingToRestore
    } catch (e: Exception) {
        PurchaseOutcome.Failed(e.message)
    }

    private fun update(info: CustomerInfo) {
        _isPlus.value = info.entitlements.active.containsKey(RevenueCatConfig.ENTITLEMENT_PLUS)
    }

    companion object {
        /** True once the SDK has been configured with a non-blank key. */
        val isAvailable: Boolean
            get() = platformRevenueCatApiKey().isNotBlank()

        /** Configure the SDK once per process. Safe to call repeatedly. */
        fun configure() {
            val key = platformRevenueCatApiKey()
            if (key.isBlank() || Purchases.isConfigured) return
            Purchases.configure(PurchasesConfiguration(apiKey = key) {})
        }
    }
}

/** Platform-specific RevenueCat public key ("goog_…" on Android, "appl_…" on iOS). */
expect fun platformRevenueCatApiKey(): String
