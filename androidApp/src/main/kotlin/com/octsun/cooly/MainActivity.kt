package com.octsun.cooly

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.octsun.cooly.alerts.AndroidAlertsController
import com.octsun.cooly.alerts.NotificationPermissionBridge
import com.octsun.cooly.platform.AndroidAdsController
import com.octsun.cooly.platform.AndroidDirectionsLauncher
import com.octsun.cooly.platform.AndroidLinkOpener
import com.octsun.cooly.platform.AndroidLocationService
import com.octsun.cooly.platform.AndroidPreferencesStore
import com.octsun.cooly.platform.AppServices
import com.octsun.cooly.platform.PermissionBridge
import com.octsun.cooly.ui.CoolyApp
import com.octsun.cooly.widget.AndroidWidgetUpdater

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        permissionBridge.onResult(result.values.any { it })
    }

    // POST_NOTIFICATIONS (Android 13+) for danger alerts.
    private val notificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationBridge.onResult?.invoke(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // White status-bar icons (they sit over the colored top bar in BOTH themes);
        // nav-bar icons follow the system light/dark theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)

        permissionBridge.launcher = permissionLauncher
        notificationBridge.launcher = notificationLauncher

        // No ads in this build — AdMob/UMP SDKs are not bundled, so nothing to initialize.

        val alertsController = AndroidAlertsController(this, notificationBridge)
        // Reboot/update can drop scheduled work — re-assert it when alerts are on.
        alertsController.ensureScheduledIfEnabled()

        val services = AppServices(
            location = AndroidLocationService(this, permissionBridge),
            directions = AndroidDirectionsLauncher(this),
            ads = AndroidAdsController(this),
            links = AndroidLinkOpener(this),
            preferences = AndroidPreferencesStore(this),
            widgets = AndroidWidgetUpdater(this),
            alerts = alertsController,
        )

        setContent {
            CoolyApp(services)
        }
    }

    override fun onDestroy() {
        // Drop the launcher references so the retained bridges can't launch through a dead Activity.
        if (permissionBridge.launcher === permissionLauncher) {
            permissionBridge.launcher = null
        }
        if (notificationBridge.launcher === notificationLauncher) {
            notificationBridge.launcher = null
        }
        super.onDestroy()
    }

    companion object {
        /**
         * Process-scoped: the ViewModel (which survives rotation) reaches these bridges through
         * the AppServices captured at first creation, so they must outlive any single
         * Activity. Each onCreate rebinds the launchers to the live Activity.
         */
        private val permissionBridge = PermissionBridge()
        private val notificationBridge = NotificationPermissionBridge()
    }
}
