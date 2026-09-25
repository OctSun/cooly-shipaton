package com.octsun.cooly.alerts

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.octsun.cooly.i18n.stringsFor
import com.octsun.cooly.platform.AlertsController
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Requests the POST_NOTIFICATIONS permission when needed (Android 13+). Process-scoped
 * like PermissionBridge: the launcher is rebound to the live Activity on each onCreate.
 * [onResult] is invoked with the grant result so the controller can react.
 */
class NotificationPermissionBridge {
    var launcher: ActivityResultLauncher<String>? = null
    var onResult: ((Boolean) -> Unit)? = null
}

/**
 * Schedules the hourly danger check via WorkManager. No foreground service; the worker
 * runs opportunistically and reads the last stored location from prefs.
 */
class AndroidAlertsController(
    context: Context,
    private val bridge: NotificationPermissionBridge,
) : AlertsController {

    private val appContext = context.applicationContext

    init {
        bridge.onResult = { granted ->
            if (granted) {
                // The enable-time one-shot raced the permission dialog — run it now that
                // posting is actually allowed, so the user gets instant feedback.
                runImmediateCheck()
            } else {
                // Denied: the toggle stays on (worker no-ops safely), but never silently —
                // tell the user alerts can't arrive until notifications are allowed.
                val strings = stringsFor(Locale.getDefault().language)
                Toast.makeText(appContext, strings.notificationsOffHint, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        val wm = WorkManager.getInstance(appContext)
        if (!enabled) {
            wm.cancelUniqueWork(PERIODIC_WORK)
            return
        }
        schedule(wm)
        if (Build.VERSION.SDK_INT >= 33) {
            // Android 13+: ask first; the immediate check runs from the grant callback.
            bridge.launcher?.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            runImmediateCheck()
        }
    }

    /** Idempotent re-schedule on app start so a reboot/update never silently drops alerts. */
    fun ensureScheduledIfEnabled() {
        val prefs = appContext.getSharedPreferences(AlertWorker.PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean("alerts_enabled", false)) {
            schedule(WorkManager.getInstance(appContext))
        }
    }

    private fun runImmediateCheck() {
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            ONESHOT_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<AlertWorker>().setConstraints(NETWORK).build(),
        )
    }

    private fun schedule(wm: WorkManager) {
        wm.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<AlertWorker>(1, TimeUnit.HOURS).setConstraints(NETWORK).build(),
        )
    }

    private companion object {
        const val PERIODIC_WORK = "cooly_danger_alerts"
        const val ONESHOT_WORK = "cooly_danger_alerts_now"
        val NETWORK = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    }
}
