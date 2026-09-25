package com.octsun.cooly.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.octsun.cooly.domain.model.GeoPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

/**
 * Bridges a suspend permission request to an [ActivityResultLauncher] owned by the Activity.
 * A single pending slot is safe because [AndroidLocationService.ensurePermission] serializes
 * requests with a Mutex, so a second request can never clobber the first.
 */
class PermissionBridge {
    var launcher: ActivityResultLauncher<Array<String>>? = null
    private var pending: ((Boolean) -> Unit)? = null

    suspend fun request(permissions: Array<String>): Boolean =
        suspendCancellableCoroutine { cont ->
            val l = launcher
            if (l == null) {
                cont.resume(false)
                return@suspendCancellableCoroutine
            }
            pending = { granted -> if (cont.isActive) cont.resume(granted) }
            cont.invokeOnCancellation { pending = null }
            l.launch(permissions)
        }

    fun onResult(granted: Boolean) {
        val cb = pending
        pending = null
        cb?.invoke(granted)
    }
}

class AndroidLocationService(
    context: Context,
    private val bridge: PermissionBridge,
) : LocationService {

    private val appContext = context.applicationContext
    private val fused = LocationServices.getFusedLocationProviderClient(appContext)
    private val permissionMutex = Mutex()

    override val hasPermission: Boolean
        get() = appContext.checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            appContext.checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

    override suspend fun ensurePermission(): Boolean = permissionMutex.withLock {
        if (hasPermission) true
        else bridge.request(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): GeoPoint? {
        if (!hasPermission) return null
        val cts = CancellationTokenSource()
        return try {
            // Overall timeout so a fused provider that never fires can't hang the app in a
            // loading state — mirrors the iOS 10s guard.
            withTimeoutOrNull(12_000) {
                val fresh = suspendCancellableCoroutine { cont ->
                    cont.invokeOnCancellation { cts.cancel() }
                    fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                        .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                        .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                }
                val loc = fresh ?: fused.lastLocation.await()
                loc?.let { GeoPoint(it.latitude, it.longitude) }
            }
        } catch (t: Throwable) {
            null
        } finally {
            cts.cancel()
        }
    }
}
