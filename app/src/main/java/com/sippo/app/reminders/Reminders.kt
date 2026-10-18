package com.sippo.app.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sippo.app.MainActivity
import com.sippo.app.R
import com.sippo.app.data.Progress
import com.sippo.app.data.Settings
import com.sippo.app.data.SippoRepository
import com.sippo.app.data.formatAmount
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object Reminders {
    private const val CHANNEL = "drink_reminders"
    private const val WORK = "drink_reminder"
    const val ACTIVE_START_HOUR = 8
    const val ACTIVE_END_HOUR = 22

    fun createChannel(context: Context) {
        val sound = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.bubble_pop}")
        val channel = NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(R.string.reminder_channel_desc)
            setSound(sound, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** (Re)schedules or cancels the periodic reminder to match [s]. */
    fun schedule(context: Context, s: Settings) {
        val wm = WorkManager.getInstance(context)
        if (!s.remind || !s.onboarded) {
            wm.cancelUniqueWork(WORK)
            return
        }
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(s.intervalMin.toLong(), TimeUnit.MINUTES)
            .setInitialDelay(s.intervalMin.toLong(), TimeUnit.MINUTES)
            .addTag("interval-${s.intervalMin}")
            .build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // checked by canNotify()
    internal fun notify(context: Context, title: String, body: String) {
        if (!canNotify(context)) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF5B63EE.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(1, n)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post.
        }
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val (s, entries) = SippoRepository.get(applicationContext).snapshot()
        if (!s.remind || !s.onboarded) return Result.success()
        val now = LocalDateTime.now()
        if (now.hour < Reminders.ACTIVE_START_HOUR || now.hour >= Reminders.ACTIVE_END_HOUR) return Result.success()

        val today = now.toLocalDate().toEpochDay()
        val total = Progress.dailyTotals(entries)[today] ?: 0f
        if (total >= s.goalMl) return Result.success()

        if (s.premium && s.smart) {
            // Smart: stay quiet when a drink was logged recently or the user is on pace for the day.
            val minute = now.hour * 60 + now.minute
            val last = entries.filter { it.day == today }.maxOfOrNull { it.minute }
            if (last != null && minute - last < s.intervalMin / 2) return Result.success()
            val start = Reminders.ACTIVE_START_HOUR * 60
            val span = (Reminders.ACTIVE_END_HOUR - Reminders.ACTIVE_START_HOUR) * 60f
            val expected = s.goalMl * ((minute - start) / span).coerceIn(0f, 1f)
            if (total >= expected) return Result.success()
        }

        val left = (s.goalMl - total).toInt()
        Reminders.notify(
            applicationContext,
            "Time for a sip!",
            "${s.mascot.label} is getting thirsty. ${formatAmount(left, s.units)} to go today.",
        )
        return Result.success()
    }
}
