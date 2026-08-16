package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val reminderDao = db.reminderDao()
                    val upcomingReminders = reminderDao.getUpcomingRemindersList()

                    val now = System.currentTimeMillis()
                    upcomingReminders.forEach { reminder ->
                        if (reminder.triggerTime > now) {
                            ReminderManager.scheduleReminder(context, reminder)
                        } else {
                            // If it missed its time while powered off, handle or fire
                            if (reminder.repeatType != "NONE") {
                                val next = ReminderManager.calculateNextTriggerTime(reminder)
                                if (next != null) {
                                    val updated = reminder.copy(triggerTime = next)
                                    reminderDao.insertOrUpdateReminder(updated)
                                    ReminderManager.scheduleReminder(context, updated)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
