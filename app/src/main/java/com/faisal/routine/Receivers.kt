package com.faisal.routine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fired by the scheduler's alarms. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Scheduler.refresh(context)
    }
}

/** Phone restarted, app updated, or the clock/time zone changed: set alarms again. */
class SystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Scheduler.refresh(context)
    }
}
