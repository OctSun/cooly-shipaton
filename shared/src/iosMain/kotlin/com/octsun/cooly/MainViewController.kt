package com.octsun.cooly

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.octsun.cooly.platform.AppServices
import com.octsun.cooly.platform.IosAdsController
import com.octsun.cooly.platform.IosDirectionsLauncher
import com.octsun.cooly.platform.IosLinkOpener
import com.octsun.cooly.platform.IosLocationService
import com.octsun.cooly.platform.IosPreferencesStore
import com.octsun.cooly.ui.CoolyApp

fun MainViewController() = ComposeUIViewController {
    // remember: the service graph (CLLocationManager etc.) must survive recompositions
    // of the root — the retained ViewModel keeps the first instances.
    val services = remember {
        AppServices(
            location = IosLocationService(),
            directions = IosDirectionsLauncher(),
            ads = IosAdsController(),
            links = IosLinkOpener(),
            preferences = IosPreferencesStore(),
        )
    }
    CoolyApp(services)
}
