package com.thinkbloxph.chatwithai.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MyAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val title = intent?.getStringExtra("title")
        // Implement the notification or other action to be taken when the alarm goes off
        // You can use the title string to customize the notification message, for example.
    }
}
