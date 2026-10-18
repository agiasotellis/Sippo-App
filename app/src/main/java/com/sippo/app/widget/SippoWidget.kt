package com.sippo.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.sippo.app.MainActivity
import com.sippo.app.R
import com.sippo.app.data.Drink
import com.sippo.app.data.Entry
import com.sippo.app.data.Progress
import com.sippo.app.data.SippoRepository
import com.sippo.app.data.Stored
import com.sippo.app.data.formatAmount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Home-screen widget (Sippo+): today's total and a one-tap 250 ml pour. */
class SippoWidget : AppWidgetProvider() {

    companion object {
        private const val ACTION_POUR = "com.sippo.app.widget.POUR"
        private const val POUR_ML = 250
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        /** Redraws every placed widget from [data]. Safe to call when none are placed. */
        fun refresh(context: Context, data: Stored) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, SippoWidget::class.java))
            if (ids.isEmpty()) return
            val views = render(context, data)
            ids.forEach { mgr.updateAppWidget(it, views) }
        }

        private fun render(context: Context, data: Stored): RemoteViews {
            val s = data.settings
            val today = LocalDateTime.now().toLocalDate().toEpochDay()
            val total = Progress.dailyTotals(data.entries)[today] ?: 0f
            val views = RemoteViews(context.packageName, R.layout.widget_sippo)
            views.setTextViewText(R.id.widget_total, formatAmount(total, s.units))
            views.setTextViewText(R.id.widget_goal, "of ${formatAmount(s.goalMl, s.units)}")
            views.setProgressBar(R.id.widget_progress, 100, ((total / s.goalMl) * 100).toInt().coerceIn(0, 100), false)

            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            if (s.premium) {
                views.setTextViewText(R.id.widget_pour, "+ ${formatAmount(POUR_ML, s.units)} water")
                val pour = PendingIntent.getBroadcast(
                    context, 1, Intent(context, SippoWidget::class.java).setAction(ACTION_POUR),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                views.setOnClickPendingIntent(R.id.widget_pour, pour)
            } else {
                views.setTextViewText(R.id.widget_pour, "Unlock with Sippo+")
                val pw = PendingIntent.getActivity(
                    context, 2, Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_PAYWALL, true),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
                views.setOnClickPendingIntent(R.id.widget_pour, pw)
            }
            return views
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                refresh(context, SippoRepository.get(context).snapshot())
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_POUR) return
        val pending = goAsync()
        scope.launch {
            try {
                val repo = SippoRepository.get(context)
                if (repo.snapshot().settings.premium) {
                    val now = LocalDateTime.now()
                    repo.addEntry(Entry(System.currentTimeMillis(), now.toLocalDate().toEpochDay(), now.hour * 60 + now.minute, Drink.WATER, POUR_ML))
                }
                refresh(context, repo.snapshot())
            } finally {
                pending.finish()
            }
        }
    }
}
