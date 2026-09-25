package com.octsun.cooly.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

/** Opens URLs in the default browser + the app-settings page. */
class AndroidLinkOpener(context: Context) : LinkOpener {
    private val appContext = context.applicationContext

    override fun openUrl(url: String) {
        // No resolveActivity pre-check: with targetSdk 30+ package visibility it returns
        // null for browsers unless <queries> declares https, silently killing the
        // privacy-policy link. Just try, and toast the URL on failure.
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { appContext.startActivity(intent) }
            .onFailure { toast(url) }
    }

    override fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", appContext.packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { appContext.startActivity(intent) }
    }

    private fun toast(text: String) {
        Toast.makeText(appContext, text, Toast.LENGTH_LONG).show()
    }
}

/** SharedPreferences-backed key-value store. */
class AndroidPreferencesStore(context: Context) : PreferencesStore {
    private val prefs = context.applicationContext
        .getSharedPreferences("cooly_prefs", Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)
    override fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()
    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
}
