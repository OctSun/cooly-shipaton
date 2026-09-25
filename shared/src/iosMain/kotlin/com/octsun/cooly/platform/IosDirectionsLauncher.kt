package com.octsun.cooly.platform

import com.octsun.cooly.domain.model.GeoPoint
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/** Opens Google Maps (if installed) or Apple Maps for walking directions. */
class IosDirectionsLauncher : DirectionsLauncher {
    override fun openWalkingDirections(destination: GeoPoint, label: String) {
        val lat = destination.lat
        val lng = destination.lng
        val app = UIApplication.sharedApplication

        val googleUrl = NSURL(string = "comgooglemaps://?daddr=$lat,$lng&directionsmode=walking")
        if (app.canOpenURL(googleUrl)) {
            app.openURL(googleUrl, options = emptyMap<Any?, Any?>(), completionHandler = null)
            return
        }
        val appleUrl = NSURL(string = "http://maps.apple.com/?daddr=$lat,$lng&dirflg=w")
        app.openURL(appleUrl, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }
}
