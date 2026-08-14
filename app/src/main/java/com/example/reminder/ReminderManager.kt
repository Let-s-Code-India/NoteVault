package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.ReminderEntity
import java.util.Calendar

object ReminderManager {
    const val ACTION_REMINDER = "com.example.notevault.ACTION_REMINDER_TRIGGER"
    const val ACTION_SNOOZE = "com.example.notevault.ACTION_SNOOZE"
    const val ACTION_DISMISS = "com.example.notevault.ACTION_DISMISS"

    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

    fun scheduleNoteReminder(context: Context, noteId: String, title: String, triggerTime: Long) {
        val reminder = ReminderEntity(
            title = title,
            targetType = "NOTE",
            targetId = noteId,
            triggerTime = triggerTime
        )
        scheduleReminder(context, reminder)
    }

    fun scheduleTaskReminder(context: Context, taskId: String, title: String, triggerTime: Long) {
        val reminder = ReminderEntity(
            title = title,
            targetType = "TASK",
            targetId = taskId,
            triggerTime = triggerTime
        )
        scheduleReminder(context, reminder)
    }

    fun scheduleReminder(context: Context, reminder: ReminderEntity) {
        if (reminder.triggerTime <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }

        val requestCode = reminder.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExactAlarms(context)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerTime, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminder.triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminder.triggerTime, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun snoozeReminder(context: Context, reminderId: String, snoozeMinutes: Int = 15) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (canScheduleExactAlarms(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
            return alarmManager.canScheduleExactAlarms()
        }
        return true
    }

    fun calculateNextTriggerTime(reminder: ReminderEntity): Long? {
        val currentCount = reminder.occurrenceCount + 1
        if (reminder.endType == "AFTER_COUNT" && reminder.maxOccurrences > 0 && currentCount >= reminder.maxOccurrences) {
            return null
        }

        val cal = Calendar.getInstance().apply {
            timeInMillis = reminder.triggerTime
        }

        val now = System.currentTimeMillis()
        while (cal.timeInMillis <= now) {
            when (reminder.repeatType) {
                "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                "WEEKLY" -> {
                    if (reminder.weeklyDays.isNotBlank()) {
                        val daysList = reminder.weeklyDays.split(",").mapNotNull { it.trim().toIntOrNull() }
                        if (daysList.isNotEmpty()) {
                            // Find next matching weekday
                            do {
                                cal.add(Calendar.DAY_OF_YEAR, 1)
                                // Calendar day: Mon=2, Tue=3.. Sun=1 or mapped 1=Mon..7=Sun
                                val calDayIndex = when (cal.get(Calendar.DAY_OF_WEEK)) {
                                    Calendar.MONDAY -> 1
                                    Calendar.TUESDAY -> 2
                                    Calendar.WEDNESDAY -> 3
                                    Calendar.THURSDAY -> 4
                                    Calendar.FRIDAY -> 5
                                    Calendar.SATURDAY -> 6
                                    Calendar.SUNDAY -> 7
                                    else -> 1
                                }
                                if (daysList.contains(calDayIndex)) break
                            } while (true)
                        } else {
                            cal.add(Calendar.WEEK_OF_YEAR, 1)
                        }
                    } else {
                        cal.add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }
                "MONTHLY" -> cal.add(Calendar.MONTH, 1)
                "CUSTOM_DAYS" -> cal.add(Calendar.DAY_OF_YEAR, reminder.repeatInterval.coerceAtLeast(1))
                else -> return null
            }
        }

        val nextTime = cal.timeInMillis
        if (reminder.endType == "ON_DATE" && reminder.endDate != null && nextTime > reminder.endDate) {
            return null
        }

        return nextTime
    }
}
