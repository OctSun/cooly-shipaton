package com.octsun.cooly.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.octsun.cooly.domain.model.GeoPoint

/** Hands walking directions to Google Maps, falling back to any geo: handler. */
class AndroidDirectionsLauncher(context: Context) : DirectionsLauncher {

    private val appContext = context.applicationContext

    override fun openWalkingDirections(destination: GeoPoint, label: String) {
        val lat = destination.lat
        val lng = destination.lng

        // Prefer Google Maps turn-by-turn walking navigation.
        val navUri = Uri.parse("google.navigation:q=$lat,$lng&mode=w")
        val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (navIntent.resolveActivity(appContext.packageManager) != null) {
            appContext.startActivity(navIntent)
            return
        }

        // Fallback: generic geo URI any maps app can handle.
        val encodedLabel = Uri.encode(label)
        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encodedLabel)")
        val geoIntent = Intent(Intent.ACTION_VIEW, geoUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (geoIntent.resolveActivity(appContext.packageManager) != null &&
            runCatching { appContext.startActivity(geoIntent) }.isSuccess
        ) {
            return
        }
        // No maps app at all — never let the Directions button feel dead; show coordinates.
        Toast.makeText(appContext, "$label — $lat, $lng", Toast.LENGTH_LONG).show()
    }
}
