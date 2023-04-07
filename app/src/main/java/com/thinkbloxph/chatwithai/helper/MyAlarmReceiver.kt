package com.thinkbloxph.chatwithai.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.widget.Toast
import android.media.MediaPlayer
import android.util.Log
import com.thinkbloxph.chatwithai.TAG
import java.util.concurrent.CountDownLatch

private const val INNER_TAG = "MyAlarmReceiver"
class MyAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule alarms after device reboot
            rescheduleAlarms(context)
        } else {
            // Handle the alarm
            val alarmId = intent.getIntExtra("alarmId", -1)
            val title = intent.getStringExtra("title")

            Log.d(TAG, "[${INNER_TAG}]: Handle the alarm active alarmId: $alarmId")
            Log.d(TAG, "[${INNER_TAG}]: Handle the title: $title")

            // Store the id of the active alarm in SharedPreferences
            val sharedPreferences = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
            sharedPreferences.edit().putInt("activeAlarmId", alarmId).apply()
            sharedPreferences.edit().putString("activeTitle", title).apply()

            // Show a toast message
            Toast.makeText(context, "Alarm: $title", Toast.LENGTH_LONG).show()

            // Play the alarm sound
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer.create(context, uri)
            mediaPlayer?.start()
        }
    }

    fun stopAlarmSound() {
        Log.d(TAG, "[${INNER_TAG}]: stop Alarm Sound!! ")

        if(mediaPlayer!=null){
            Log.d(TAG, "[${INNER_TAG}]: stop Alarm Sound mediaPlayer is not null!! ")
        }else{
            Log.d(TAG, "[${INNER_TAG}]: stop Alarm Sound mediaPlayer is null!! ")
        }

        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
                release()
            }
            mediaPlayer = null
        }
    }

    companion object {
        private var instance: MyAlarmReceiver? = null
        private var mediaPlayer: MediaPlayer? = null
        fun getInstance(): MyAlarmReceiver {
            if (instance == null) {
                instance = MyAlarmReceiver()
            }
            return instance!!
        }
    }

    private fun rescheduleAlarms(context: Context) {
        val sharedPreferences = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
        val alarmSet = sharedPreferences.getStringSet("alarmSet", mutableSetOf())

        if (alarmSet != null) {
            for (alarmInfo in alarmSet) {
                val parts = alarmInfo.split(",")
                val alarmId = parts[0].toInt()
                val millis = parts[0].toLong()
                val title = parts[1]

                // Check if the alarm time is in the future
                if (millis > System.currentTimeMillis()) {
                    // Reschedule the alarm
                    val intent = Intent(context, MyAlarmReceiver::class.java).apply {
                        action = "com.thinkbloxph.chatwithai.ALARM"
                        putExtra("title", title)
                        putExtra("alarmId", alarmId)
                    }

                    //val pendingIntent = PendingIntent.getBroadcast(context, millis.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT)
                    //val pendingIntent = PendingIntent.getBroadcast(context, alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                    val pendingIntent = PendingIntent.getBroadcast(context, alarmId, intent, 0)

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
