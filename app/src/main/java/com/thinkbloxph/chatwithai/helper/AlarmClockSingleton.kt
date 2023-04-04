package com.thinkbloxph.chatwithai.helper

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class AlarmClockSingleton private constructor(private val context: Context) {
    companion object {
        @Volatile private var INSTANCE: AlarmClockSingleton? = null

        fun getInstance(context: Context): AlarmClockSingleton {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AlarmClockSingleton(context).also { INSTANCE = it }
            }
        }
    }

    fun extractDateAndTime(dateString: String): Date {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        return format.parse(dateString)
    }

    fun setAlarm(context: Context, date: Date) {
        val calendar = Calendar.getInstance()
        calendar.time = date

        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, calendar.get(Calendar.HOUR_OF_DAY))
            .putExtra(AlarmClock.EXTRA_MINUTES, calendar.get(Calendar.MINUTE))
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            .putExtra(AlarmClock.EXTRA_MESSAGE, "Wake up!")
        context.startActivity(intent)
    }

    fun setAlarmFromString(dateString: String) {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        val calendar = Calendar.getInstance()

        val pattern = ".*\\b(\\d+)\\s+(min|minute)s?\\b.*"
        if (dateString.matches(pattern.toRegex())) {
            calendar.add(Calendar.MINUTE, dateString.replaceFirst(pattern.toRegex(), "$1").toInt())
        } else {
            try {
                val date = format.parse(dateString)
                calendar.time = date
            } catch (e: ParseException) {
                e.printStackTrace()
            }
        }

        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
        intent.putExtra(AlarmClock.EXTRA_HOUR, calendar.get(Calendar.HOUR_OF_DAY))
        intent.putExtra(AlarmClock.EXTRA_MINUTES, calendar.get(Calendar.MINUTE))
        intent.putExtra(AlarmClock.EXTRA_MESSAGE, "Wake up!")
       context.startActivity(intent)
    }
}

/*
val alarmClock = AlarmClockSingleton.getInstance()
val dateString = "2023-04-03 14:30:00"
val date = alarmClock.extractDateAndTime(dateString)

val context = applicationContext
alarmClock.setAlarm(context, date)
* */