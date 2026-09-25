package com.octsun.cooly.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.octsun.cooly.MainActivity
import com.octsun.cooly.R
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Home-screen widget showing the last heat/air reading the app persisted. It carries no
 * network or location of its own — the app snapshots each reading to SharedPreferences and
 * pokes [updateAll]; the widget just renders it. Tapping opens the app for a live refresh.
 */
class CoolyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { renderInto(context, manager, it) }
    }

    companion object {
        private const val PREFS = "cooly_prefs"

        /** Called from the app after a fresh reading is stored. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CoolyWidgetProvider::class.java))
            ids.forEach { renderInto(context, manager, it) }
        }

        private fun renderInto(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val views = RemoteViews(context.packageName, R.layout.widget_cooly)
            val korean = Locale.getDefault().language == "ko"

            val hasTemp = prefs.getBoolean("widget_has_temp", false)
            val hasAqi = prefs.getBoolean("widget_has_aqi", false)
            val feelsC = prefs.getString("widget_feelslike_c", null)?.toDoubleOrNull()
            val aqi = prefs.getString("widget_aqi", null)?.toIntOrNull()
            val risk = prefs.getString("widget_risk", null)
            val aqiLevel = prefs.getString("widget_aqi_level", null)
            val time = prefs.getString("widget_time", null)?.toLongOrNull()
            val fahrenheit = prefs.getString("temperature_unit", "C") == "F"

            // Feels-like (stored °C; convert for the user's unit).
            if (hasTemp && feelsC != null) {
                val shown = if (fahrenheit) feelsC * 9 / 5 + 32 else feelsC
                views.setTextViewText(R.id.widget_feels, "${shown.roundToInt()}°")
                views.setTextColor(R.id.widget_feels, riskColor(risk))
            } else {
                views.setTextViewText(R.id.widget_feels, "—")
                views.setTextColor(R.id.widget_feels, 0xFF102A43.toInt())
            }
            views.setTextViewText(R.id.widget_feels_label, if (korean) "체감온도" else "Feels like")

            // Risk dot + label.
            views.setInt(R.id.widget_risk_dot, "setColorFilter", riskColor(risk))
            views.setTextViewText(R.id.widget_risk, riskText(risk, korean))
            views.setTextColor(R.id.widget_risk, riskColor(risk))

            // Air quality line.
            if (hasAqi && aqi != null) {
                val aqiLabel = if (korean) "대기질" else "AQI"
                views.setTextViewText(R.id.widget_aqi, "$aqiLabel $aqi · ${aqiText(aqiLevel, korean)}")
            } else {
                views.setTextViewText(R.id.widget_aqi, "")
            }

            // Freshness / empty guidance.
            views.setTextViewText(R.id.widget_updated, freshness(time, hasTemp || hasAqi, korean))

            // Whole widget opens the app for a live refresh.
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pending = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)

            manager.updateAppWidget(widgetId, views)
        }

        private fun riskColor(risk: String?): Int = when (risk) {
            "DANGER" -> 0xFFE0464B.toInt()
            "CAUTION" -> 0xFFE8A21C.toInt()
            "SAFE" -> 0xFF2E9E5B.toInt()
            else -> 0xFF9AA7B4.toInt()
        }

        private fun riskText(risk: String?, ko: Boolean): String = when (risk) {
            "DANGER" -> if (ko) "위험" else "Danger"
            "CAUTION" -> if (ko) "주의" else "Caution"
            "SAFE" -> if (ko) "안전" else "Safe"
            else -> ""
        }

        private fun aqiText(level: String?, ko: Boolean): String = when (level) {
            "GOOD" -> if (ko) "좋음" else "Good"
            "MODERATE" -> if (ko) "보통" else "Moderate"
            "UNHEALTHY_SENSITIVE" -> if (ko) "민감군 주의" else "Sensitive"
            "UNHEALTHY" -> if (ko) "나쁨" else "Unhealthy"
            "VERY_UNHEALTHY" -> if (ko) "매우 나쁨" else "Very poor"
            "HAZARDOUS" -> if (ko) "위험" else "Hazardous"
            else -> ""
        }

        private fun freshness(time: Long?, hasData: Boolean, ko: Boolean): String {
            if (!hasData || time == null) return if (ko) "탭하여 불러오기" else "Tap to load"
            val mins = ((System.currentTimeMillis() - time) / 60000L).coerceAtLeast(0)
            return when {
                mins < 1 -> if (ko) "방금 업데이트" else "Updated just now"
                mins < 60 -> if (ko) "${mins}분 전" else "${mins} min ago"
                else -> {
                    val h = mins / 60
                    if (ko) "${h}시간 전" else "${h} h ago"
                }
            }
        }
    }
}

/** Bridges the shared [com.octsun.cooly.platform.WidgetUpdater] to the widget provider. */
class AndroidWidgetUpdater(context: Context) : com.octsun.cooly.platform.WidgetUpdater {
    private val appContext = context.applicationContext
    override fun refresh() = CoolyWidgetProvider.updateAll(appContext)
}
