package com.faisal.routine

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews

/** Home screen widget: current task, progress, time left, next task, streak. */
class RoutineWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        Scheduler.refresh(context)
    }

    override fun onEnabled(context: Context) {
        Scheduler.refresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, manager: AppWidgetManager, id: Int, options: Bundle?
    ) {
        updateAll(context)
    }

    companion object {
        private val COLORS = mapOf(
            "office" to 0xFF8D9CC0.toInt(),
            "gym" to 0xFF4FC497.toInt(),
            "together" to 0xFFA985F0.toInt(),
            "trade" to 0xFF6AA3FF.toInt(),
            "project" to 0xFFF0A83A.toInt(),
            "rest" to 0xFF7D829C.toInt()
        )

        fun updateAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, RoutineWidget::class.java))
            if (ids.isEmpty()) return
            mgr.updateAppWidget(ids, build(ctx))
        }

        private fun build(ctx: Context): RemoteViews {
            val rn = Routine.now()
            val person = RoutineStore.person(ctx)
            val list = RoutineStore.schedule(ctx, person, rn.dayIdx)
            val cur = list.firstOrNull { rn.min >= it.s && rn.min < it.e }
            val next = list.firstOrNull { it.s > rn.min }
            val rest = COLORS.getValue("rest")

            val v = RemoteViews(ctx.packageName, R.layout.widget_routine)
            val name = if (person == "akash") "Akash" else "Faisal"
            v.setTextViewText(R.id.w_day, "${Routine.DAY_KEYS[rn.dayIdx]} · $name")

            val streak = RoutineStore.streak(ctx, person)
            v.setTextViewText(R.id.w_streak, if (streak > 0) "🔥 $streak" else "")

            if (cur != null) {
                v.setTextViewText(R.id.w_task, cur.a)
                v.setInt(R.id.w_dot, "setColorFilter", COLORS[cur.k] ?: rest)
                val pct = ((rn.min - cur.s) * 100) / maxOf(1, cur.e - cur.s)
                v.setProgressBar(R.id.w_progress, 100, pct, false)
                v.setTextViewText(R.id.w_left, "${Routine.dur(cur.e - rn.min)} left · ends ${Routine.fmt(cur.e)}")
            } else {
                val asleep = list.isEmpty() || rn.min >= list.last().e || rn.min < list.first().s
                v.setTextViewText(R.id.w_task, if (asleep) "Sleep time" else "Free time")
                v.setInt(R.id.w_dot, "setColorFilter", rest)
                v.setProgressBar(R.id.w_progress, 100, 0, false)
                v.setTextViewText(R.id.w_left, if (asleep) "Rest well, tomorrow starts early" else "Short gap before the next block")
            }

            val nextText = if (next != null) {
                "Next: ${next.a} · ${Routine.fmt(next.s)}"
            } else {
                RoutineStore.schedule(ctx, person, (rn.dayIdx + 1) % 7).firstOrNull()
                    ?.let { "Tomorrow: ${it.a} · ${Routine.fmt(it.s)}" } ?: ""
            }
            v.setTextViewText(R.id.w_next, nextText)

            val open = PendingIntent.getActivity(
                ctx, 1, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.w_root, open)
            return v
        }
    }
}
