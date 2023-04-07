package com.thinkbloxph.chatwithai.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.thinkbloxph.chatwithai.TAG
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

private const val INNER_TAG = "ReminderManager"

class ReminderManager private constructor(private val context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: ReminderManager? = null

        fun getInstance(context: Context): ReminderManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReminderManager(context).also { INSTANCE = it }
            }
        }
    }

    fun setReminder(currContext: Context, dateTime: LocalDateTime?, title: String) {
        val now = LocalDateTime.now()

        if (dateTime != null) {
            val reminderTime = LocalDateTime.of(
                dateTime.year,
                dateTime.month,
                dateTime.dayOfMonth,
                dateTime.hour,
                dateTime.minute,
                dateTime.second
            )

            if (reminderTime.isBefore(now)) {
                reminderTime.plusDays(1)
            }

            val timeDiff = Duration.between(now, reminderTime)
            val alarmTitle = "$title in ${getTimeString(timeDiff.toMillis())}"

            setAlarm(currContext, reminderTime, alarmTitle)
        } else {
            println("Invalid date and time provided")
        }
    }


    fun setAlarm(currContext: Context, datetime: LocalDateTime, title: String) {
        // Convert LocalDateTime to milliseconds
        val millis = datetime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Save the alarm details in SharedPreferences
        val sharedPreferences = currContext.getSharedPreferences("alarms", Context.MODE_PRIVATE)
        val alarmId = sharedPreferences.getInt("incrementAlarmId", 0) + 1
        sharedPreferences.edit().putInt("incrementAlarmId", alarmId).apply()
        sharedPreferences.edit().putInt("alarmId", alarmId).apply()
        val alarmSet = sharedPreferences.getStringSet("alarmSet", mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()
        alarmSet.add("$alarmId,$millis,$title")
        sharedPreferences.edit().putStringSet("alarmSet", alarmSet).apply()

        // Create an intent to broadcast the alarm
        val intent = Intent(currContext, MyAlarmReceiver::class.java).apply {
            action = "com.thinkbloxph.chatwithai.ALARM"
            putExtra("alarmId", alarmId)
            putExtra("title", title)
        }

        val pendingIntent = PendingIntent.getBroadcast(currContext, alarmId, intent, 0)

        // Get the AlarmManager service
        val alarmManager = currContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Set the alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
        }

        // Show a toast message
        Toast.makeText(currContext, "Alarm set for $title", Toast.LENGTH_SHORT).show()
    }

    /*fun cancelAlarm(currContext: Context) {
        // Retrieve the id of the currently active alarm
        val sharedPreferences = currContext.getSharedPreferences("alarms", Context.MODE_PRIVATE)
        val activeAlarmId = sharedPreferences.getInt("activeAlarmId", -1)
        val activeTitle = sharedPreferences.getString("activeTitle", "no title")

        Log.d(TAG, "[${INNER_TAG}]: cancelAlarm activeAlarmId: $activeAlarmId")
        Log.d(TAG, "[${INNER_TAG}]: cancelAlarm activeTitle: $activeTitle")

        if (activeAlarmId != -1) {
            Log.d(TAG, "[${INNER_TAG}]: got active alarmId activeAlarmId: $activeAlarmId")
            val alarmManager = currContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(currContext, MyAlarmReceiver::class.java).apply {
                action = "com.thinkbloxph.chatwithai.ALARM"
                putExtra("alarmId", activeAlarmId)
                putExtra("title", activeTitle)
            }

            //val pendingIntent = PendingIntent.getBroadcast(currContext, activeAlarmId, intent, PendingIntent.FLAG_CANCEL_CURRENT)
            val pendingIntent = PendingIntent.getBroadcast(currContext, activeAlarmId, intent,0)

            // Cancel the alarm
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()

            // Remove the alarm from SharedPreferences
            val alarmSet = sharedPreferences.getStringSet("alarmSet", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            val alarmToRemove = alarmSet.firstOrNull { it.startsWith("$activeAlarmId,") }

            if (alarmToRemove != null) {
                alarmSet.remove(alarmToRemove)
                sharedPreferences.edit().putStringSet("alarmSet", alarmSet).apply()
            }

            // Remove the id of the active alarm from SharedPreferences
            sharedPreferences.edit().remove("activeAlarmId").apply()
            sharedPreferences.edit().remove("activeTitle").apply()

            MyAlarmReceiver.getInstance().stopAlarmSound()

            // Show a toast message
            Toast.makeText(currContext, "Alarm canceled", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "[${INNER_TAG}]: got active alarm canceled")
        }
    }*/

    /* fun cancelAlarm(currContext: Context) {
         val sharedPreferences = currContext.getSharedPreferences("alarms", Context.MODE_PRIVATE)
         val alarmSet = sharedPreferences.getStringSet("alarmSet", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

         for (alarmInfo in alarmSet) {
             val parts = alarmInfo.split(",")
             val alarmId = parts[0].toInt()
             val title = parts[1]

             val alarmManager = currContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
             val intent = Intent(currContext, MyAlarmReceiver::class.java).apply {
                 action = "com.thinkbloxph.chatwithai.ALARM"
                 putExtra("alarmId", alarmId)
                 putExtra("title", title)
             }
             //val pendingIntent = PendingIntent.getBroadcast(currContext, alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
             val pendingIntent = PendingIntent.getBroadcast(currContext, alarmId, intent, 0)

             // Cancel the alarm
             alarmManager.cancel(pendingIntent)
             Toast.makeText(currContext, "All Alarm canceled alarmId : $alarmId", Toast.LENGTH_SHORT).show()
         }

         // Remove all the alarms from SharedPreferences
         sharedPreferences.edit().putStringSet("alarmSet", setOf()).apply()

         MyAlarmReceiver.getInstance().stopAlarmSound()

         // Show a toast message
         Toast.makeText(currContext, "All Alarms canceled", Toast.LENGTH_SHORT).show()
     }*/

    fun cancelAlarm(currContext: Context) {
        val sharedPreferences = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
        val alarmSet = sharedPreferences.getStringSet("alarmSet", mutableSetOf())
        val alarmInfo = alarmSet?.firstOrNull()

        if (alarmInfo != null) {
            val parts = alarmInfo.split(",")
            val alarmId = parts[0].toInt()
            val title = parts[2]

            val intent = Intent(currContext, MyAlarmReceiver::class.java).apply {
                action = "com.thinkbloxph.chatwithai.ALARM"
                putExtra("alarmId", title)
                putExtra("title", alarmId)
            }

            Log.d(TAG, "[${INNER_TAG}]: cancelAlarm activeAlarmId: $alarmId")
            Log.d(TAG, "[${INNER_TAG}]: cancelAlarm activeTitle: $title")

            val pendingIntent = PendingIntent.getBroadcast(currContext, alarmId, intent, 0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Cancel the alarm
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()

            // Remove the alarm from SharedPreferences
            val alarmSet =
                sharedPreferences.getStringSet("alarmSet", mutableSetOf())?.toMutableSet()
                    ?: mutableSetOf()
            val alarmToRemove = alarmSet.firstOrNull { it.startsWith("$alarmId,") }

            if (alarmToRemove != null) {
                alarmSet.remove(alarmToRemove)
                sharedPreferences.edit().putStringSet("alarmSet", alarmSet).apply()
            }

            // Remove the id of the active alarm from SharedPreferences
            sharedPreferences.edit().remove("activeAlarmId").apply()
            sharedPreferences.edit().remove("activeTitle").apply()

            MyAlarmReceiver.getInstance().stopAlarmSound()

            // Show a toast message
            Toast.makeText(currContext, "Alarm canceled", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "[${INNER_TAG}]: got active alarm canceled")
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

    fun containsStopAlarmKeyword(input: String): Boolean {
        val keywords = listOf("stop alarm", "cancel alarm", "stop reminder", "cancel reminder")
        val regex = "\\b(${keywords.joinToString("|")})\\b".toRegex(RegexOption.IGNORE_CASE)
        return regex.containsMatchIn(input)
    }

    fun isStopAlarmOrReminder(text: String): Boolean {
        val keywords =
            listOf("stop", "cancel", "halt", "end", "terminate", "Stop", "Cancel", "Halt", "End")
        val targets = listOf("alarm", "reminder", "Alarm", "Alarm!", "Reminder", "Reminder!")

        val words = text.lowercase(Locale.getDefault()).split(" ").toSet()

        val filteredKeywords = keywords.filter { it in words }
        val filteredTargets = targets.filter { it in words }

        return filteredKeywords.isNotEmpty() && filteredTargets.isNotEmpty()
    }

    fun parseReminderText1(input: String): Pair<LocalDateTime?, String> {
        val currentTime = LocalDateTime.now()
        val dateFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy h:mma")
        val hoursPattern = Pattern.compile("(\\d+)\\s?h(?:ou)?rs?\\s?from now")
        val minutesPattern = Pattern.compile("(\\d+)\\s?minutes?\\s?from now")
        val afterMinutesPattern = Pattern.compile("after (\\d+)\\s?(?:minutes?|mins?)")
        val hoursMinutesPattern =
            Pattern.compile("(\\d+)\\s?h(?:ou)?rs?\\s?and\\s?(\\d+)\\s?minutes?\\s?from now")
        val oneHourPattern = Pattern.compile("1\\s?h(?:ou)?r(?: from)? now")
        val afterOneHourPattern = Pattern.compile("after 1\\s?h(?:ou)?r")
        val secondsPattern = Pattern.compile("after (\\d+)\\s?(?:seconds?|secs?)")

        return when {
            input.contains("tomorrow", ignoreCase = true) -> {
                val time = input.substringAfter("tomorrow at ").trim()
                val date =
                    currentTime.plusDays(1).format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                val localDT = LocalDateTime.parse("$date $time", dateFormat)
                Pair(
                    localDT,
                    "OK, I have set a reminder for ${localDT.format(DateTimeFormatter.ofPattern("hh:mm a"))}."
                )
            }
            hoursPattern.matcher(input).find() -> {
                val hours = hoursPattern.matcher(input).apply { find() }.group(1).toInt()
                val updatedTime = currentTime.plusHours(hours.toLong())
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm a")
                        )
                    }."
                )
            }
            minutesPattern.matcher(input).find() || afterMinutesPattern.matcher(input).find() -> {
                val minutes = if (minutesPattern.matcher(input).find()) {
                    minutesPattern.matcher(input).apply { find() }.group(1).toInt()
                } else {
                    afterMinutesPattern.matcher(input).apply { find() }.group(1).toInt()
                }
                val updatedTime = currentTime.plusMinutes(minutes.toLong())
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm a")
                        )
                    }."
                )
            }
            hoursMinutesPattern.matcher(input).find() -> {
                val matcher = hoursMinutesPattern.matcher(input).apply { find() }
                val hours = matcher.group(1).toInt()
                val minutes = matcher.group(2).toInt()
                val updatedTime =
                    currentTime.plusHours(hours.toLong()).plusMinutes(minutes.toLong())
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm a")
                        )
                    }."
                )
            }
            oneHourPattern.matcher(input).find() || afterOneHourPattern.matcher(input).find() -> {
                val updatedTime = currentTime.plusHours(1)
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm a")
                        )
                    }."
                )
            }
            secondsPattern.matcher(input).find() -> {
                val seconds = secondsPattern.matcher(input).apply { find() }.group(1).toInt()
                val updatedTime = currentTime.plusSeconds(seconds.toLong())
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm:ss a")
                        )
                    }."
                )
            }
            else -> {
                val date = input.substringAfter("on ").substringBefore(" at ").trim()
                val time = input.substringAfter(" at ").trim()
                if (date.isNotEmpty() && time.isNotEmpty()) {
                    val localDT = LocalDateTime.parse("$date $time", dateFormat)
                    Pair(
                        localDT,
                        "OK, I have set a reminder for ${
                            localDT.format(
                                DateTimeFormatter.ofPattern("hh:mm a")
                            )
                        }."
                    )
                } else {
                    Pair(null, "Sorry, I couldn't understand your request.")
                }
            }
        }
    }

    fun parseReminderText2(input: String): Pair<LocalDateTime?, String> {
        val currentTime = LocalDateTime.now()
        val dateFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy h:mma")
        val hoursPattern = Pattern.compile("(\\d+)\\s?h(?:ou)?rs?\\s?from now")
        val minutesPattern = Pattern.compile("(\\d+)\\s?minutes?\\s?from now")
        val afterMinutesPattern = Pattern.compile("after (\\d+)\\s?(?:minutes?|mins?)")
        val hoursMinutesPattern =
            Pattern.compile("(\\d+)\\s?h(?:ou)?rs?\\s?and\\s?(\\d+)\\s?minutes?\\s?from now")
        val hoursMinutesPattern2 =
            Pattern.compile("after (\\d+)\\s?h(?:ou)?rs?\\s?and\\s?(\\d+)\\s?minutes?")
        val oneHourPattern = Pattern.compile("1\\s?h(?:ou)?r(?: from)? now")
        val afterOneHourPattern = Pattern.compile("after 1\\s?h(?:ou)?r")
        val secondsPattern = Pattern.compile("after (\\d+)\\s?(?:seconds?|secs?)")
        val minutesSecondsPattern =
            Pattern.compile("after (\\d+)\\s?(?:minutes?|mins?) and (\\d+)\\s?(?:seconds?|secs?)")

        return when {
            input.contains("tomorrow", ignoreCase = true) -> {
                val time = input.substringAfter("tomorrow at ").trim()
                val date =
                    currentTime.plusDays(1).format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                val localDT = LocalDateTime.parse("$date $time", dateFormat)
                Pair(
                    localDT,
                    "OK, I have set a reminder for ${localDT.format(DateTimeFormatter.ofPattern("hh:mm a"))}."
                )
            }
            hoursPattern.matcher(input).find() -> {
                val hours = hoursPattern.matcher(input).apply { find() }.group(1).toInt()
                val updatedTime = currentTime.plusHours(hours.toLong())
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm a")
                        )
                    }."
                )
            }
            minutesPattern.matcher(input).find() || afterMinutesPattern.matcher(input).find() -> {
                val minutes = if (minutesPattern.matcher(input).find()) {
                    minutesPattern.matcher(input).apply { find() }.group(1).toInt()
                } else {
                    afterMinutesPattern.matcher(input).apply { find() }.group(1).toInt()
                }
                val updatedTime = currentTime.plusMinutes(minutes.toLong())
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm a")
                        )
                    }."
                )
            }
            hoursMinutesPattern.matcher(input).find() -> {
                val matcher = hoursMinutesPattern.matcher(input).apply { find() }
                val hours = matcher.group(1).toInt()
                val minutes = matcher.group(2).toInt()
                val updatedTime =
                    currentTime.plusHours(hours.toLong()).plusMinutes(minutes.toLong())
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm a")
                        )
                    }."
                )
            }
            hoursMinutesPattern2.matcher(input).find() -> {
                val matcher = hoursMinutesPattern2.matcher(input).apply { find() }
                val hours = matcher.group(1).toInt()
                val minutes = matcher.group(2).toInt()
                val updatedTime =
                    currentTime.plusHours(hours.toLong()).plusMinutes(minutes.toLong())
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm a")
                        )
                    }."
                )
            }
            oneHourPattern.matcher(input).find() || afterOneHourPattern.matcher(input).find() -> {
                val updatedTime = currentTime.plusHours(1)
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm a")
                        )
                    }."
                )
            }
            secondsPattern.matcher(input).find() -> {
                val seconds = secondsPattern.matcher(input).apply { find() }.group(1).toInt()
                val updatedTime = currentTime.plusSeconds(seconds.toLong())
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm:ss a")
                        )
                    }."
                )
            }
            minutesSecondsPattern.matcher(input).find() -> {
                val matcher = minutesSecondsPattern.matcher(input).apply { find() }
                val minutes = matcher.group(1).toInt()
                val seconds = matcher.group(2).toInt()
                val updatedTime =
                    currentTime.plusMinutes(minutes.toLong()).plusSeconds(seconds.toLong())
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm:ss a")
                        )
                    }."
                )
            }
            else -> {
                val date = input.substringAfter("on ").substringBefore(" at ").trim()
                val time = input.substringAfter(" at ").trim()
                if (date.isNotEmpty() && time.isNotEmpty()) {
                    val localDT = LocalDateTime.parse("$date $time", dateFormat)
                    Pair(
                        localDT,
                        "OK, I have set a reminder for ${
                            localDT.format(
                                DateTimeFormatter.ofPattern("hh:mm a")
                            )
                        }."
                    )
                } else {
                    Pair(null, "Sorry, I couldn't understand your request.")
                }
            }
        }
    }

    fun parseReminderText(input: String): Pair<LocalDateTime?, String> {
        val currentTime = LocalDateTime.now()
        val dateFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy h:mma")
        val hoursPattern = Pattern.compile("(\\d+)\\s?h(?:ou)?rs?\\s?from now")
        val minutesPattern = Pattern.compile("(\\d+)\\s?minutes?\\s?from now")
        val afterMinutesPattern = Pattern.compile("after (\\d+)\\s?(?:minutes?|mins?)")
        val hoursMinutesPattern =
            Pattern.compile("(\\d+)\\s?h(?:ou)?rs?\\s?and\\s?(\\d+)\\s?minutes?\\s?from now")
        val hoursMinutesPattern2 =
            Pattern.compile("after (\\d+)\\s?h(?:ou)?rs?\\s?and\\s?(\\d+)\\s?minutes?")
        val oneHourPattern = Pattern.compile("1\\s?h(?:ou)?r(?: from)? now")
        val afterOneHourPattern = Pattern.compile("after 1\\s?h(?:ou)?r")
        val secondsPattern = Pattern.compile("after (\\d+)\\s?(?:seconds?|secs?)")
        val minutesSecondsPattern =
            Pattern.compile("after (\\d+)\\s?(?:minutes?|mins?) and (\\d+)\\s?(?:seconds?|secs?)")
        val minutesWordPattern =
            Pattern.compile("after (one|two)\\s?(?:minutes?|mins?)", Pattern.CASE_INSENSITIVE)

        return when {
            input.contains("tomorrow", ignoreCase = true) -> {
                val time = input.substringAfter("tomorrow at ").trim()
                val date =
                    currentTime.plusDays(1).format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                val localDT = LocalDateTime.parse("$date $time", dateFormat)
                Pair(
                    localDT,
                    "OK, I have set a reminder for ${localDT.format(DateTimeFormatter.ofPattern("hh:mm a"))}."
                )
            }
            hoursPattern.matcher(input).find() -> {
                val hours = hoursPattern.matcher(input).apply { find() }.group(1).toInt()
                val updatedTime = currentTime.plusHours(hours.toLong())
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm a")
                        )
                    }."
                )
            }
            minutesPattern.matcher(input).find() || afterMinutesPattern.matcher(input).find() -> {
                val minutes = if (minutesPattern.matcher(input).find()) {
                    minutesPattern.matcher(input).apply { find() }.group(1).toInt()
                } else {
                    afterMinutesPattern.matcher(input).apply { find() }.group(1).toInt()
                }
                val updatedTime = currentTime.plusMinutes(minutes.toLong())
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm a")
                        )
                    }."
                )
            }
            hoursMinutesPattern.matcher(input).find() -> {
                val matcher = hoursMinutesPattern.matcher(input).apply { find() }
                val hours = matcher.group(1).toInt()
                val minutes = matcher.group(2).toInt()
                val updatedTime =
                    currentTime.plusHours(hours.toLong()).plusMinutes(minutes.toLong())
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm a")
                        )
                    }."
                )
            }
            hoursMinutesPattern2.matcher(input).find() -> {
                val matcher = hoursMinutesPattern2.matcher(input).apply { find() }
                val hours = matcher.group(1).toInt()
                val minutes = matcher.group(2).toInt()
                val updatedTime =
                    currentTime.plusHours(hours.toLong()).plusMinutes(minutes.toLong())
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm a")
                        )
                    }."
                )
            }
            oneHourPattern.matcher(input).find() || afterOneHourPattern.matcher(input).find() -> {
                val updatedTime = currentTime.plusHours(1)
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm a")
                        )
                    }."
                )
            }
            secondsPattern.matcher(input).find() -> {
                val seconds = secondsPattern.matcher(input).apply { find() }.group(1).toInt()
                val updatedTime = currentTime.plusSeconds(seconds.toLong())
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm:ss a")
                        )
                    }."
                )
            }
            minutesSecondsPattern.matcher(input).find() -> {
                val matcher = minutesSecondsPattern.matcher(input).apply { find() }
                val minutes = matcher.group(1).toInt()
                val seconds = matcher.group(2).toInt()
                val updatedTime =
                    currentTime.plusMinutes(minutes.toLong()).plusSeconds(seconds.toLong())
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm:ss a")
                        )
                    }."
                )
            }
            minutesWordPattern.matcher(input).find() -> {
                val word = minutesWordPattern.matcher(input).apply { find() }.group(1).toLowerCase()
                val minutes = when (word) {
                    "one" -> 1
                    "two" -> 2
                    else -> 0
                }
                val updatedTime = currentTime.plusMinutes(minutes.toLong())
                Pair(
                    updatedTime,
                    "OK, I have set a reminder for ${
                        updatedTime.format(
                            DateTimeFormatter.ofPattern("hh:mm a")
                        )
                    }."
                )
            }
            else -> {
                val date = input.substringAfter("on ")
                    .substringBefore(if (input.contains(" at ")) " at " else " on ").trim()
                val time =
                    input.substringAfter(if (input.contains(" at ")) " at " else " on ").trim()
                if (date.isNotEmpty() && time.isNotEmpty()) {
                    val localDT = LocalDateTime.parse("$date $time", dateFormat)
                    Pair(
                        localDT,
                        "OK, I have set a reminder for ${
                            localDT.format(
                                DateTimeFormatter.ofPattern("hh:mm a")
                            )
                        }."
                    )
                } else {
                    Pair(null, "Sorry, I couldn't understand your request.")
                }
            }
        }
    }
}
