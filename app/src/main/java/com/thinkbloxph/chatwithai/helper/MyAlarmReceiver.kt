package com.thinkbloxph.chatwithai.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.widget.Toast

class MyAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule alarms after device reboot
            rescheduleAlarms(context)
        } else {
            // Handle the alarm
            val title = intent.getStringExtra("title")

            // Show a toast message
            Toast.makeText(context, "Alarm: $title", Toast.LENGTH_LONG).show()

            // Play the alarm sound
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone.play()
        }
    }

    private fun rescheduleAlarms(context: Context) {
        val sharedPreferences = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
        val alarmSet = sharedPreferences.getStringSet("alarmSet", mutableSetOf())

        if (alarmSet != null) {
            for (alarmInfo in alarmSet) {
                val parts = alarmInfo.split(",")
                val millis = parts[0].toLong()
                val title = parts[1]

                // Check if the alarm time is in the future
                if (millis > System.currentTimeMillis()) {
                    // Reschedule the alarm
                    val intent = Intent(context, MyAlarmReceiver::class.java).apply {
                        action = "com.thinkbloxph.chatwithai.ALARM"
                        putExtra("title", title)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(context, millis.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT)
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
                } else {
                    // Remove the expired alarm from SharedPreferences
                    val updatedAlarmSet = alarmSet.filter { it != alarmInfo }
                    sharedPreferences.edit().putStringSet("alarmSet", updatedAlarmSet.toSet()).apply()
                }
            }
        }
    }
}
