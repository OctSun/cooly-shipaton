package com.octsun.cooly.platform

import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

/** Opens URLs in Safari + the app-settings page. */
class IosLinkOpener : LinkOpener {
    override fun openUrl(url: String) {
        val nsUrl = NSURL(string = url)
        UIApplication.sharedApplication.openURL(
            nsUrl,
            options = emptyMap<Any?, Any?>(),
            completionHandler = null,
        )
    }

    override fun openAppSettings() {
        val nsUrl = NSURL(string = UIApplicationOpenSettingsURLString)
        UIApplication.sharedApplication.openURL(
            nsUrl,
            options = emptyMap<Any?, Any?>(),
            completionHandler = null,
        )
    }
}

/** NSUserDefaults-backed key-value store. */
class IosPreferencesStore : PreferencesStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String): String? = defaults.stringForKey(key)
    override fun putString(key: String, value: String) = defaults.setObject(value, key)
    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) == null) default else defaults.boolForKey(key)
    override fun putBoolean(key: String, value: Boolean) = defaults.setBool(value, key)
}
