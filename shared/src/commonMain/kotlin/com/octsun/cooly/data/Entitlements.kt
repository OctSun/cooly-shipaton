package com.octsun.cooly.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Premium entitlement source. The production implementation is backed by RevenueCat
 * (entitlement "plus"); until the SDK is wired, [StubEntitlements] keeps everyone free.
 */
interface Entitlements {
    /** True while the user has an active Cooly Plus entitlement. */
    val isPlus: StateFlow<Boolean>

    /** Re-check the entitlement (e.g. after a purchase or restore). */
    suspend fun refresh()
}

class StubEntitlements(initial: Boolean = false) : Entitlements {
    private val _isPlus = MutableStateFlow(initial)
    override val isPlus: StateFlow<Boolean> = _isPlus.asStateFlow()
    override suspend fun refresh() = Unit
}
