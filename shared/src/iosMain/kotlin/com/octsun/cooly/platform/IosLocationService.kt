package com.octsun.cooly.platform

import com.octsun.cooly.domain.model.GeoPoint
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

/** CLLocationManager-backed location + permission. */
@OptIn(ExperimentalForeignApi::class)
class IosLocationService : LocationService {

    private val manager = CLLocationManager()
    private val mutex = Mutex()  // serialize so concurrent requests can't clobber the delegate

    private var authDelegate: NSObject? = null
    private var locationDelegate: NSObject? = null

    override val hasPermission: Boolean
        get() = manager.authorizationStatus.let {
            it == kCLAuthorizationStatusAuthorizedWhenInUse ||
                it == kCLAuthorizationStatusAuthorizedAlways
        }

    override suspend fun ensurePermission(): Boolean = mutex.withLock {
        if (hasPermission) return true
        suspendCancellableCoroutine { cont ->
            val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
                    val status = manager.authorizationStatus
                    if (status != kCLAuthorizationStatusNotDetermined) {
                        authDelegate = null
                        if (cont.isActive) {
                            cont.resume(
                                status == kCLAuthorizationStatusAuthorizedWhenInUse ||
                                    status == kCLAuthorizationStatusAuthorizedAlways,
                            )
                        }
                    }
                }
            }
            authDelegate = delegate
            manager.delegate = delegate
            cont.invokeOnCancellation { authDelegate = null }
            manager.requestWhenInUseAuthorization()
        }
    }

    override suspend fun currentLocation(): GeoPoint? {
        if (!hasPermission) return null
        return mutex.withLock {
            withTimeoutOrNull(10_000) {
                suspendCancellableCoroutine { cont ->
                    val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                        override fun locationManager(
                            manager: CLLocationManager,
                            didUpdateLocations: List<*>,
                        ) {
                            val loc = didUpdateLocations.lastOrNull() as? CLLocation
                            locationDelegate = null
                            if (cont.isActive) {
                                cont.resume(loc?.coordinate?.useContents { GeoPoint(latitude, longitude) })
                            }
                        }

                        override fun locationManager(
                            manager: CLLocationManager,
                            didFailWithError: NSError,
                        ) {
                            locationDelegate = null
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                    locationDelegate = delegate
                    manager.delegate = delegate
                    cont.invokeOnCancellation { locationDelegate = null }
                    manager.requestLocation()
                }
            }
        }
    }
}
