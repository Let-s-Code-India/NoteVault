package com.example.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.model.ReminderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        private const val CHANNEL_ID = "notevault_reminders_channel"
        private const val CHANNEL_NAME = "NoteVault Local Reminders"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val reminderId = intent.getStringExtra(ReminderManager.EXTRA_REMINDER_ID) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val reminderDao = db.reminderDao()
                val reminder = reminderDao.getReminderById(reminderId)

                when (action) {
                    ReminderManager.ACTION_REMINDER -> {
                        if (reminder != null) {
                            showNotification(context, reminder)

                            // Update status to FIRED
                            val updatedFired = reminder.copy(
                                status = "FIRED",
                                firedAt = System.currentTimeMillis()
                            )
                            reminderDao.insertOrUpdateReminder(updatedFired)

                            // Check and handle recurrence self-rescheduling
                            if (reminder.repeatType != "NONE") {
                                val nextTrigger = ReminderManager.calculateNextTriggerTime(reminder)
                                if (nextTrigger != null) {
                                    val nextReminder = reminder.copy(
                                        status = "UPCOMING",
                                        triggerTime = nextTrigger,
                                        occurrenceCount = reminder.occurrenceCount + 1,
                                        firedAt = null
                                    )
                                    reminderDao.insertOrUpdateReminder(nextReminder)
                                    ReminderManager.scheduleReminder(context, nextReminder)
                                }
                            }
                        } else {
                            // Fallback for legacy calls
                            showFallbackNotification(context, reminderId, "Scheduled Reminder")
                        }
                    }
                    ReminderManager.ACTION_SNOOZE -> {
                        val snoozeMins = intent.getIntExtra(ReminderManager.EXTRA_SNOOZE_MINUTES, 15)
                        if (reminder != null) {
                            val snoozedReminder = reminder.copy(
                                status = "SNOOZED",
                                triggerTime = System.currentTimeMillis() + (snoozeMins * 60 * 1000L)
                            )
                            reminderDao.insertOrUpdateReminder(snoozedReminder)
                            ReminderManager.scheduleReminder(context, snoozedReminder)
                        } else {
                            ReminderManager.snoozeReminder(context, reminderId, snoozeMins)
                        }
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(reminderId.hashCode())
                    }
                    ReminderManager.ACTION_DISMISS -> {
                        if (reminder != null) {
                            reminderDao.insertOrUpdateReminder(reminder.copy(status = "DISMISSED"))
                        }
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(reminderId.hashCode())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, reminder: ReminderEntity) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Local scheduled reminders for NoteVault"
                enableVibration(!reminder.isSilent)

                if (reminder.isSilent) {
                    setSound(null, null)
                } else if (!reminder.soundUri.isNull_or_blank() && reminder.soundUri != "DEFAULT") {
                    try {
                        val soundUri = Uri.parse(reminder.soundUri)
                        val audioAttrs = AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .build()
                        setSound(soundUri, audioAttrs)
                    } catch (e: Exception) {
                        // fallback to default
                    }
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (reminder.targetType == "NOTE") {
                putExtra("target_note_id", reminder.targetId)
            } else if (reminder.targetType == "TASK") {
                putExtra("target_task_id", reminder.targetId)
            }
            putExtra("target_destination", "REMINDERS")
        }

        val pendingContentIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze Intent with snoozeMinutes
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderManager.ACTION_SNOOZE
            putExtra(ReminderManager.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderManager.EXTRA_SNOOZE_MINUTES, reminder.snoozeMinutes)
        }
        val pendingSnoozeIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode() + 10000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss Intent
        val dismissIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderManager.ACTION_DISMISS
            putExtra(ReminderManager.EXTRA_REMINDER_ID, reminder.id)
        }
        val pendingDismissIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode() + 20000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val vibrationPattern = when (reminder.vibrationPattern) {
            "HEARTBEAT" -> longArrayOf(0, 100, 100, 100, 200, 100, 100, 100)
            "SHORT" -> longArrayOf(0, 150)
            "NONE" -> longArrayOf(0, 0)
            else -> longArrayOf(0, 250, 250, 250)
        }

        val bodyText = buildString {
            if (reminder.customNote.isNotBlank()) {
                append("📌 ").append(reminder.customNote)
            }
            if (reminder.linkedTags.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append("🏷️ ").append(reminder.linkedTags)
            }
        }

        val iconRes = when (reminder.targetType) {
            "NOTE" -> android.R.drawable.ic_menu_edit
            "TASK" -> android.R.drawable.checkbox_on_background
            "TAG_DIGEST" -> android.R.drawable.ic_menu_agenda
            else -> android.R.drawable.ic_dialog_info
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle("⏰ " + reminder.title)
            .setContentText(if (bodyText.isNotBlank()) bodyText else "Scheduled local reminder")
            .setStyle(NotificationCompat.BigTextStyle().bigText(if (bodyText.isNotBlank()) bodyText else reminder.title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingContentIntent)
            .setVibrate(if (reminder.isSilent) longArrayOf(0, 0) else vibrationPattern)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze ${reminder.snoozeMinutes}m", pendingSnoozeIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", pendingDismissIntent)

        notificationManager.notify(reminder.id.hashCode(), builder.build())
    }

    private fun showFallbackNotification(context: Context, id: String, title: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📝 Local Reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(id.hashCode(), builder.build())
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
}
