package com.faisal.routine

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/**
 * Keeps the widget live and fires reminders.
 * Two alarms:
 *  - EVENT: exact, wakes the phone at the next block start/end or reminder time.
 *  - REFRESH: every 5 minutes for the progress bar, only while the phone is awake (no battery cost when asleep).
 */
object Scheduler {
    private const val REQ_EVENT = 1
    private const val REQ_REFRESH = 2
    private const val CHANNEL_ID = "routine_reminders"
    private const val NOTIFICATION_ID = 1001
    private const val REMINDER_MIN = 5

    fun refresh(context: Context) {
        val ctx = context.applicationContext
        checkReminder(ctx)
        RoutineWidget.updateAll(ctx)
        scheduleNext(ctx)
    }

    private fun scheduleNext(ctx: Context) {
        val now = System.currentTimeMillis()
        val rn = Routine.now(now)
        val person = RoutineStore.person(ctx)
        val notify = RoutineStore.notifyOn(ctx)

        val events = ArrayList<Long>()
        for (offset in 0..1) {
            val dayIdx = (rn.dayIdx + offset) % 7
            val dayStart = Routine.addDays(rn.dayStart, offset)
            for (b in RoutineStore.schedule(ctx, person, dayIdx)) {
                events.add(Routine.at(dayStart, b.s))
                events.add(Routine.at(dayStart, b.e))
                if (notify) events.add(Routine.at(dayStart, b.s - REMINDER_MIN))
            }
            // The routine day switches at 6:00 AM
            events.add(Routine.at(dayStart, 1440 + Routine.DAY_START))
        }
        val nextEvent = events.filter { it > now + 1000 }.minOrNull() ?: (now + 60 * 60_000L)

        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val eventPi = pending(ctx, REQ_EVENT)
        val exactAllowed = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        try {
            if (exactAllowed) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextEvent, eventPi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextEvent, eventPi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextEvent, eventPi)
        }

        val five = 5 * 60_000L
        val nextRefresh = (now / five + 1) * five
        am.set(AlarmManager.RTC, nextRefresh, pending(ctx, REQ_REFRESH))
    }

    private fun pending(ctx: Context, code: Int): PendingIntent {
        val intent = Intent(ctx, AlarmReceiver::class.java).setAction("com.faisal.routine.TICK_$code")
        return PendingIntent.getBroadcast(
            ctx, code, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun checkReminder(ctx: Context) {
        if (!RoutineStore.notifyOn(ctx)) return
        val rn = Routine.now()
        val person = RoutineStore.person(ctx)
        val block = RoutineStore.schedule(ctx, person, rn.dayIdx)
            .firstOrNull { it.s - rn.min in 1..REMINDER_MIN } ?: return

        val id = "${rn.dayStart}|${block.s}|$person"
        val prefs = ctx.getSharedPreferences("reminders", Context.MODE_PRIVATE)
        if (prefs.getString("last", null) == id) return
        prefs.edit().putString("last", id).apply()
        post(ctx, block, block.s - rn.min)
    }

    private fun post(ctx: Context, b: Block, inMin: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Routine reminders", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "5 minutes before each block starts" }
        )
        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = Notification.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_clock)
            .setContentTitle("In $inMin min: ${b.a}")
            .setContentText("Starts at ${Routine.fmt(b.s)}")
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFICATION_ID, n)
    }
}
