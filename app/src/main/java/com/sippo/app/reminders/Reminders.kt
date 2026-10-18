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
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import com.sippo.app.data.ReminderSound
import com.sippo.app.data.Settings as AppSettings
import com.sippo.app.data.SippoRepository
import com.sippo.app.data.formatAmount
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object Reminders {
    private const val WORK = "drink_reminder"
    private const val LEGACY_CHANNEL = "drink_reminders"

    private fun channelId(sound: ReminderSound) = "drink_reminders_${sound.key}"

    fun popUri(context: Context): Uri =
        Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/bubble_pop")

    /**
     * Creates the channel for [sound] and removes the others. A channel's sound can't change after it is
     * created, so each sound choice gets its own channel.
     */
    fun createChannel(context: Context, sound: ReminderSound = ReminderSound.POP) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(
            channelId(sound),
            "${context.getString(R.string.reminder_channel)} · ${sound.label}",
            if (sound == ReminderSound.SILENT) NotificationManager.IMPORTANCE_LOW else NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.reminder_channel_desc)
            when (sound) {
                ReminderSound.POP -> setSound(popUri(context), attrs)
                ReminderSound.DEFAULT -> setSound(Settings.System.DEFAULT_NOTIFICATION_URI, attrs)
                ReminderSound.SILENT -> setSound(null, null)
            }
        }
        nm.createNotificationChannel(channel)
        (ReminderSound.entries.filter { it != sound }.map(::channelId) + LEGACY_CHANNEL).forEach { nm.deleteNotificationChannel(it) }
    }

    /** Plays [sound] once inside the app, so the user can hear what reminders will sound like. */
    fun preview(context: Context, sound: ReminderSound) {
        val uri = when (sound) {
            ReminderSound.POP -> popUri(context)
            ReminderSound.DEFAULT -> Settings.System.DEFAULT_NOTIFICATION_URI
            ReminderSound.SILENT -> return
        }
        runCatching {
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                setDataSource(context, uri)
                setOnCompletionListener { it.release() }
                setOnErrorListener { mp, _, _ -> mp.release(); true }
                prepare()
                start()
            }
        }
    }

    /** True when [hour] (0–23) falls inside the user's active hours. */
    fun isActive(s: AppSettings, hour: Int) = hour >= s.activeStart && hour < s.activeEnd

    /** (Re)schedules or cancels the periodic reminder to match [s]. */
    fun schedule(context: Context, s: AppSettings) {
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
    internal fun notify(context: Context, sound: ReminderSound, title: String, body: String) {
        if (!canNotify(context)) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        createChannel(context, sound)
        val n = NotificationCompat.Builder(context, channelId(sound))
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
        if (!Reminders.isActive(s, now.hour)) return Result.success()

        val today = now.toLocalDate().toEpochDay()
        val total = Progress.dailyTotals(entries)[today] ?: 0f
        if (total >= s.goalMl) return Result.success()

        if (s.premium && s.smart) {
            // Smart: stay quiet when a drink was logged recently or the user is on pace for the day.
            val minute = now.hour * 60 + now.minute
            val last = entries.filter { it.day == today }.maxOfOrNull { it.minute }
            if (last != null && minute - last < s.intervalMin / 2) return Result.success()
            val start = s.activeStart * 60
            val span = (s.activeEnd - s.activeStart) * 60f
            val expected = s.goalMl * ((minute - start) / span).coerceIn(0f, 1f)
            if (total >= expected) return Result.success()
        }

        val left = (s.goalMl - total).toInt()
        Reminders.notify(
            applicationContext,
            s.sound,
            "Time for a sip!",
            "${s.mascot.label} is getting thirsty. ${formatAmount(left, s.units)} to go today.",
        )
        return Result.success()
    }
}
