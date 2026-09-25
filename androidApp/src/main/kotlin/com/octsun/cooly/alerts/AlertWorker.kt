package com.octsun.cooly.alerts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.octsun.cooly.MainActivity
import com.octsun.cooly.R
import com.octsun.cooly.data.AlertChecker
import com.octsun.cooly.domain.model.AqiLevel
import com.octsun.cooly.domain.model.RiskLevel
import com.octsun.cooly.domain.model.TemperatureUnit
import com.octsun.cooly.i18n.stringsFor
import com.octsun.cooly.ui.components.formatTemperature
import java.util.Locale

/**
 * Periodic background danger check. Reads the LAST location the app loaded data for
 * (stored in prefs) and re-fetches weather/AQI for it — no background location access,
 * no foreground service. Posts a local notification when conditions turn dangerous.
 */
class AlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("alerts_enabled", false)) return Result.success()
        val lat = prefs.getString("last_lat", null)?.toDoubleOrNull() ?: return Result.success()
        val lng = prefs.getString("last_lng", null)?.toDoubleOrNull() ?: return Result.success()

        // Android 13+: bail quietly if the user revoked the notification permission.
        if (Build.VERSION.SDK_INT >= 33 &&
            applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val env = AlertChecker().check(lat, lng) ?: return Result.success()

        // The background check just fetched current conditions — refresh the home-screen widget
        // with them so it never shows hours-old risk while a live check ran. Writes
        // the same cooly_prefs keys UserPreferences.setLastReading uses.
        prefs.edit()
            .putString("widget_feelslike_c", env.feelsLikeTemp.toString())
            .putString("widget_aqi", env.aqi.toString())
            .putString("widget_risk", env.riskLevel.name)
            .putString("widget_aqi_level", env.aqiLevel.name)
            .putBoolean("widget_has_temp", env.hasTemp)
            .putBoolean("widget_has_aqi", env.hasAqi)
            .putString("widget_time", System.currentTimeMillis().toString())
            .apply()
        com.octsun.cooly.widget.CoolyWidgetProvider.updateAll(applicationContext)

        if (env.riskLevel != RiskLevel.DANGER) {
            // Left the danger zone: reset so the NEXT danger episode notifies again.
            prefs.edit().putString("alert_last_kind", "").apply()
            return Result.success()
        }

        val strings = stringsFor(Locale.getDefault().language)
        val airDanger = env.hasAqi &&
            (env.aqiLevel == AqiLevel.VERY_UNHEALTHY || env.aqiLevel == AqiLevel.HAZARDOUS)
        val kind = if (airDanger) "air" else "heat"

        // Don't re-notify an ongoing danger episode more than once every 6 hours —
        // regardless of which axis (heat/air) currently dominates, or a location
        // oscillating around the AQI-150 line would ping-pong hourly.
        val lastKind = prefs.getString("alert_last_kind", "") ?: ""
        val lastAt = prefs.getLong("alert_last_at", 0L)
        val now = System.currentTimeMillis()
        if (lastKind.isNotEmpty() && now - lastAt < REALERT_MS) return Result.success()

        val fahrenheit = prefs.getString("temperature_unit", "C") == "F"
        val unit = if (fahrenheit) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS
        val (title, body) = if (airDanger) {
            strings.alertAirTitle to strings.alertAirBody(env.aqi, strings.aqiLevel(env.aqiLevel))
        } else {
            strings.alertHeatTitle to strings.alertHeatBody(formatTemperature(env.feelsLikeTemp, unit))
        }
        notify(channelName = strings.dangerAlerts, title = title, body = body)
        prefs.edit().putString("alert_last_kind", kind).putLong("alert_last_at", now).apply()
        return Result.success()
    }

    private fun notify(channelName: String, title: String, body: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // User disabled notifications app-wide (pre-13 path): posting would silently drop.
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= 26) {
            // createNotificationChannel also renames an existing channel → localizes lazily.
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH),
            )
        }
        val intent = Intent(applicationContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            applicationContext, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val PREFS = "cooly_prefs"
        const val CHANNEL_ID = "danger_alerts"
        const val NOTIFICATION_ID = 100
        private const val REALERT_MS = 6 * 60 * 60 * 1000L
    }
}
