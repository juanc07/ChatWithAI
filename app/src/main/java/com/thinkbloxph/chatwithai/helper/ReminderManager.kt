package com.thinkbloxph.chatwithai.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.time.Month
import java.time.Year


class ReminderManager private constructor(private val context: Context) {
    companion object {
        @Volatile private var INSTANCE: ReminderManager? = null

        fun getInstance(context: Context): ReminderManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReminderManager(context).also { INSTANCE = it }
            }
        }
    }

    fun setReminder(dateTime: LocalDateTime?, title: String) {
        val now = LocalDateTime.now()
        val reminderTime = LocalDateTime.of(
            now.year,
            now.month,
            now.dayOfMonth,
            dateTime!!.hour,
            dateTime.minute,
            0
        )
        if (reminderTime.isBefore(now)) {
            reminderTime.plusDays(1)
        }

        val timeDiff = Duration.between(now, reminderTime)
        val alarmTitle = "$title in ${getTimeString(timeDiff.toMillis())}"

        setAlarm(reminderTime, alarmTitle)
    }

    private fun setAlarm(dateTime: LocalDateTime, title: String) {
        // Example implementation:
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MyAlarmReceiver::class.java)
        intent.putExtra("title", title)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)

        val alarmTime = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
        }
    }

    private fun getTimeString(timeDiff: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(timeDiff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeDiff) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeDiff) % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    fun parseReminderText(text: String): Pair<LocalDateTime?, String> {
        val pattern1 = "(?i)wake me up( on)? (tonight|tomorrow|next (week|month)|([0-9]{1,2})(st|nd|rd|th)? (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec))( at )?([0-9]{1,2}):([0-9]{2})(\\s*(am|pm))?"
        val pattern2 = "(?i)wake me up( at)? ([0-9]{1,2}):([0-9]{2})(\\s*(am|pm))? (tonight|tomorrow|next (week|month)|([0-9]{1,2})(st|nd|rd|th)? (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec))"
        val regex1 = Regex(pattern1)
        val regex2 = Regex(pattern2)

        val matchResult1 = regex1.find(text)
        val matchResult2 = regex2.find(text)

        return if (matchResult1 != null) {
            val groups = matchResult1.groupValues
            val month = if (groups[6].isNotEmpty()) Month.valueOf(groups[6].uppercase(Locale.getDefault())) else null
            val year = if (groups[3].isNotEmpty()) Year.now().value else null
            val day = if (groups[4].isNotEmpty()) groups[4].toInt() else LocalDate.now().dayOfMonth
            val hour = groups[10].toInt() + if (groups[12].lowercase(Locale.getDefault()) == "pm" && groups[10] != "12") 12 else 0
            val minute = groups[11].toInt()
            val dateTime = LocalDateTime.of(year ?: LocalDate.now().year, month ?: LocalDate.now().month, day, hour, minute)
            Pair(dateTime, "OK, I have set a reminder for ${dateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
        } else if (matchResult2 != null) {
            val groups = matchResult2.groupValues
            val month = if (groups[9].isNotEmpty()) Month.valueOf(groups[9].uppercase(Locale.getDefault())) else null
            val year = if (groups[6].isNotEmpty()) Year.now().value else null
            val day = if (groups[7].isNotEmpty()) groups[7].toInt() else LocalDate.now().dayOfMonth
            val hour = groups[2].toInt() + if (groups[4].lowercase(Locale.getDefault()) == "pm" && groups[2] != "12") 12 else 0
            val minute = groups[3].toInt()
            val dateTime = LocalDateTime.of(year ?: LocalDate.now().year, month ?: LocalDate.now().month, day, hour, minute)
            Pair(dateTime, "OK, I have set a reminder for ${dateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
        } else {
            Pair(null, "Sorry, I couldn't understand your request.")
        }
    }
}
