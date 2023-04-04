package com.thinkbloxph.chatwithai.helper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.time.Month
import java.time.Year
import java.util.regex.Pattern
import java.time.temporal.ChronoUnit


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

            setAlarm(reminderTime, alarmTitle)
        } else {
            println("Invalid date and time provided")
        }
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

    fun setAlarm(context: Context, datetime: LocalDateTime, title: String) {

        // Convert LocalDateTime to milliseconds
        val millis = datetime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Save the alarm details in SharedPreferences
        val sharedPreferences = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
        val alarmSet = sharedPreferences.getStringSet("alarmSet", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        alarmSet.add("$millis,$title")
        sharedPreferences.edit().putStringSet("alarmSet", alarmSet).apply()

        // Create an intent to broadcast the alarm
        val intent = Intent(context, MyAlarmReceiver::class.java).apply {
            action = "com.thinkbloxph.chatwithai.ALARM"
            putExtra("title", title)
        }

        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Get the AlarmManager service
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Set the alarm
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, millis, pendingIntent)

        // Show a toast message
        Toast.makeText(context, "Alarm set for $title", Toast.LENGTH_SHORT).show()
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

   /* fun parseReminderText(text: String): Pair<LocalDateTime?, String> {
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
    }*/

    /*fun parseReminderText1(input: String): Pair<LocalDateTime?, String> {
        val dateTimePatterns = listOf(
            Pair(Pattern.compile("(\\d{1,2})\\s+(\\w+)\\s+(\\d{4})\\s+at\\s+(\\d{1,2}):(\\d{2})(\\w{2})"), "d MMMM yyyy h:mma"),
            Pair(Pattern.compile("tomorrow\\s+at\\s+(\\d{1,2}):(\\d{2})(\\w{2})"), "d MMMM yyyy h:mma"),
            Pair(Pattern.compile("(\\d+)\\s+hours\\s+from\\s+now"), "")
        )

        for (pattern in dateTimePatterns) {
            val matcher = pattern.first.matcher(input)
            if (matcher.find()) {
                if (pattern.second.isNotEmpty()) {
                    val formatter = DateTimeFormatter.ofPattern(pattern.second)
                    val formattedInput = "${matcher.group(1)} ${matcher.group(2)} ${matcher.group(3)} ${matcher.group(4)}:${matcher.group(5)}${matcher.group(6)}"
                    var localDT = LocalDateTime.parse(formattedInput, formatter)
                    Pair(localDT, "OK, I have set a reminder for ${localDT.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
                } else {
                    val hoursFromNow = matcher.group(1).toInt()
                    var localDT =  LocalDateTime.now().plusHours(hoursFromNow.toLong())
                    Pair(localDT, "OK, I have set a reminder for ${localDT.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
                }
            }
        }

        return  Pair(null, "Sorry, I couldn't understand your request.")
    }*/

    fun parseReminderText2(input: String): Pair<LocalDateTime?, String> {
        val dateTimePatterns = listOf(
            Pair(Pattern.compile("(\\d{1,2})\\s+(\\w+)\\s+(\\d{4})\\s+at\\s+(\\d{1,2}):(\\d{2})(\\w{2})"), "d MMMM yyyy h:mma"),
            Pair(Pattern.compile("tomorrow\\s+at\\s+(\\d{1,2}):(\\d{2})(\\w{2})"), "d MMMM yyyy h:mma"),
            Pair(Pattern.compile("(\\d+)\\s+(hour|minute)s?\\s+from\\s+now"), "")
        )

        for (pattern in dateTimePatterns) {
            val matcher = pattern.first.matcher(input)
            if (matcher.find()) {
                if (pattern.second.isNotEmpty()) {
                    val formatter = DateTimeFormatter.ofPattern(pattern.second)
                    val formattedInput = "${matcher.group(1)} ${matcher.group(2)} ${matcher.group(3)} ${matcher.group(4)}:${matcher.group(5)}${matcher.group(6)}"
                    //return LocalDateTime.parse(formattedInput, formatter)
                    var localDT = LocalDateTime.parse(formattedInput, formatter)
                    Pair(localDT, "OK, I have set a reminder for ${localDT.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
                } else {
                    val timeValue = matcher.group(1).toInt()
                    val timeUnit = matcher.group(2)
                    var localDT = when (timeUnit) {
                        "hour" -> LocalDateTime.now().plus(timeValue.toLong(), ChronoUnit.HOURS)
                        "minute" -> LocalDateTime.now().plus(timeValue.toLong(), ChronoUnit.MINUTES)
                        else -> null
                    }
                    if (localDT != null) {
                        Pair(localDT, "OK, I have set a reminder for ${localDT.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
                    }else{
                        return  Pair(null, "Sorry, I couldn't understand your request.")
                    }
                }
            }
        }

        return  Pair(null, "Sorry, I couldn't understand your request.")
    }

    fun parseReminderText3(input: String): Pair<LocalDateTime?, String> {
        val dateTimePatterns = listOf(
            Pair(Pattern.compile("(\\d{1,2})\\s+(\\w+)\\s+(\\d{4})\\s+at\\s+(\\d{1,2}):(\\d{2})(\\w{2})"), "d MMMM yyyy h:mma"),
            Pair(Pattern.compile("tomorrow\\s+at\\s+(\\d{1,2}):(\\d{2})(\\w{2})"), "d MMMM yyyy h:mma"),
            Pair(Pattern.compile("(\\d+)\\s*(hour|minute)s?\\s+from\\s+now"), "")
        )

        for (pattern in dateTimePatterns) {
            val matcher = pattern.first.matcher(input)
            if (matcher.find()) {
                if (pattern.second.isNotEmpty()) {
                    val formatter = DateTimeFormatter.ofPattern(pattern.second)
                    val formattedInput = "${matcher.group(1)} ${matcher.group(2)} ${matcher.group(3)} ${matcher.group(4)}:${matcher.group(5)}${matcher.group(6)}"
                    //return LocalDateTime.parse(formattedInput, formatter)
                    var localDT = LocalDateTime.parse(formattedInput, formatter)
                    Pair(localDT, "OK, I have set a reminder for ${localDT.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
                } else {
                    val timeValue = matcher.group(1).toInt()
                    val timeUnit = matcher.group(2)
                    var localDT = when (timeUnit) {
                        "hour" -> LocalDateTime.now().plus(timeValue.toLong(), ChronoUnit.HOURS)
                        "minute" -> LocalDateTime.now().plus(timeValue.toLong(), ChronoUnit.MINUTES)
                        else -> null
                    }

                    if (localDT != null) {
                        Pair(localDT, "OK, I have set a reminder for ${localDT.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
                    }else{
                        return  Pair(null, "Sorry, I couldn't understand your request.")
                    }
                }
            }
        }

        return  Pair(null, "Sorry, I couldn't understand your request.")
    }

    fun parseReminderText4(input: String): Pair<LocalDateTime?, String> {
        val dateTimePatterns = listOf(
            Pair(Pattern.compile("(\\d{1,2})\\s+(\\w+)\\s+(\\d{4})\\s+at\\s+(\\d{1,2}):(\\d{2})(\\w{2})"), "d MMMM yyyy h:mma"),
            Pair(Pattern.compile("tomorrow\\s+at\\s+(\\d{1,2}):(\\d{2})(\\w{2})"), "d MMMM yyyy h:mma"),
            Pair(Pattern.compile("(\\d+)\\s*(hour|minute)s?\\s+from\\s+now"), ""),
            Pair(Pattern.compile("after\\s+(\\d+)\\s*(hour|minute)s?"), "")
        )

        for (pattern in dateTimePatterns) {
            val matcher = pattern.first.matcher(input)
            if (matcher.find()) {
                if (pattern.second.isNotEmpty()) {
                    val formatter = DateTimeFormatter.ofPattern(pattern.second)
                    val formattedInput = "${matcher.group(1)} ${matcher.group(2)} ${matcher.group(3)} ${matcher.group(4)}:${matcher.group(5)}${matcher.group(6)}"
                    //return LocalDateTime.parse(formattedInput, formatter)
                    var localDT = LocalDateTime.parse(formattedInput, formatter)
                    return  Pair(localDT, "OK, I have set a reminder for ${localDT.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
                } else {
                    val timeValue = matcher.group(1).toInt()
                    val timeUnit = matcher.group(2)
                    var localDT = when (timeUnit) {
                        "hour" -> LocalDateTime.now().plus(timeValue.toLong(), ChronoUnit.HOURS)
                        "minute" -> LocalDateTime.now().plus(timeValue.toLong(), ChronoUnit.MINUTES)
                        else -> null
                    }

                    if (localDT != null) {
                        return Pair(localDT, "OK, I have set a reminder for ${localDT.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
                    }else{
                        return  Pair(null, "Sorry, I couldn't understand your request. 2nd")
                    }
                }
            }
        }

        return  Pair(null, "Sorry, I couldn't understand your request. 1st")
    }

    fun parseReminderText5(input: String): Pair<LocalDateTime?, String> {
        val dateTimePatterns = listOf(
            Pair(Pattern.compile("(\\d{1,2})\\s+(\\w+)\\s+(\\d{4})\\s+at\\s+(\\d{1,2}):(\\d{2})(\\w{2})"), "d MMMM yyyy h:mma"),
            Pair(Pattern.compile("tomorrow\\s+at\\s+(\\d{1,2}):(\\d{2})(\\w{2})"), "d MMMM yyyy h:mma"),
            Pair(Pattern.compile("(\\d+)\\s*(hour|minute)s?\\s+from\\s+now"), ""),
            Pair(Pattern.compile("wake me up after\\s+(\\d+)\\s*(hour|minute)s?"), "")
        )

        for (pattern in dateTimePatterns) {
            val matcher = pattern.first.matcher(input)
            if (matcher.find()) {
                if (pattern.second.isNotEmpty()) {
                    val formatter = DateTimeFormatter.ofPattern(pattern.second)
                    val formattedInput = "${matcher.group(1)} ${matcher.group(2)} ${matcher.group(3)} ${matcher.group(4)}:${matcher.group(5)}${matcher.group(6)}"
                    //return LocalDateTime.parse(formattedInput, formatter)
                    var localDT = LocalDateTime.parse(formattedInput, formatter)
                    return  Pair(localDT, "OK, I have set a reminder for ${localDT.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
                } else {
                    val timeValue = matcher.group(1).toInt()
                    val timeUnit = matcher.group(2)
                    var localDT = when (timeUnit) {
                        "hour" -> LocalDateTime.now().plus(timeValue.toLong(), ChronoUnit.HOURS)
                        "minute" -> LocalDateTime.now().plus(timeValue.toLong(), ChronoUnit.MINUTES)
                        else -> null
                    }

                    if (localDT != null) {
                        return Pair(localDT, "OK, I have set a reminder for ${localDT.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
                    }else{
                        return  Pair(null, "Sorry, I couldn't understand your request. 2nd")
                    }
                }
            }
        }
        return  Pair(null, "Sorry, I couldn't understand your request. 2nd")
    }

    fun parseReminderText6(input: String): Pair<LocalDateTime?, String> {
        val dateTimePatterns = listOf(
            Pair(Pattern.compile("(\\d{1,2})\\s+(\\w+)\\s+(\\d{4})\\s+at\\s+(\\d{1,2}):(\\d{2})(\\w{2})"), "d MMMM yyyy h:mma"),
            Pair(Pattern.compile("tomorrow\\s+at\\s+(\\d{1,2}):(\\d{2})(\\w{2})"), "d MMMM yyyy h:mma"),
            Pair(Pattern.compile("(\\d+)\\s*(hour|minute)s?\\s+from\\s+now"), ""),
            Pair(Pattern.compile("wake\\s+me\\s+up\\s+after\\s+(\\d+)\\s*(hour|minute)s?"), "")
        )

        for (pattern in dateTimePatterns) {
            val matcher = pattern.first.matcher(input)
            if (matcher.find()) {
                if (pattern.second.isNotEmpty()) {
                    val formatter = DateTimeFormatter.ofPattern(pattern.second)
                    val formattedInput = "${matcher.group(1)} ${matcher.group(2)} ${matcher.group(3)} ${matcher.group(4)}:${matcher.group(5)}${matcher.group(6)}"
                    //return LocalDateTime.parse(formattedInput, formatter)
                    var localDT = LocalDateTime.parse(formattedInput, formatter)
                    return  Pair(localDT, "OK, I have set a reminder for ${localDT.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
                } else {
                    val timeValue = matcher.group(1).toInt()
                    val timeUnit = matcher.group(2)
                    var localDT = when (timeUnit) {
                        "hour" -> LocalDateTime.now().plus(timeValue.toLong(), ChronoUnit.HOURS)
                        "minute" -> LocalDateTime.now().plus(timeValue.toLong(), ChronoUnit.MINUTES)
                        else -> null
                    }

                    if (localDT != null) {
                        return Pair(localDT, "OK, I have set a reminder for ${localDT.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
                    }else{
                        return  Pair(null, "Sorry, I couldn't understand your request. 2nd")
                    }
                }
            }
        }

        return  Pair(null, "Sorry, I couldn't understand your request. 2nd")
    }

    fun parseReminderText7(input: String): Pair<LocalDateTime?, String> {
        val currentTime = LocalDateTime.now()
        val dateFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy h:mma")
        val hoursPattern = Pattern.compile("(\\d+)\\s?h(?:ou)?rs?\\s?from now")
        val minutesPattern = Pattern.compile("(\\d+)\\s?minutes?\\s?from now")
        val afterMinutesPattern = Pattern.compile("after (\\d+)\\s?(?:minutes?|mins?)")
        val oneHourPattern = Pattern.compile("1\\s?h(?:ou)?r(?: from)? now")
        val afterOneHourPattern = Pattern.compile("after 1\\s?h(?:ou)?r")

        return when {
            input.contains("tomorrow", ignoreCase = true) -> {
                val time = input.substringAfter("tomorrow at ").trim()
                val date = currentTime.plusDays(1).format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                val localDT = LocalDateTime.parse("$date $time", dateFormat)
                Pair(localDT, "OK, I have set a reminder for ${localDT.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
            }
            hoursPattern.matcher(input).find() -> {
                val hours = hoursPattern.matcher(input).apply { find() }.group(1).toInt()
                val updatedTime = currentTime.plusHours(hours.toLong())
                Pair(updatedTime, "OK, I have set a reminder for ${updatedTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
            }
            minutesPattern.matcher(input).find() || afterMinutesPattern.matcher(input).find() -> {
                val minutes = if (minutesPattern.matcher(input).find()) {
                    minutesPattern.matcher(input).apply { find() }.group(1).toInt()
                } else {
                    afterMinutesPattern.matcher(input).apply { find() }.group(1).toInt()
                }
                val updatedTime = currentTime.plusMinutes(minutes.toLong())
                Pair(updatedTime, "OK, I have set a reminder for ${updatedTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
            }
            oneHourPattern.matcher(input).find() || afterOneHourPattern.matcher(input).find() -> {
                val updatedTime = currentTime.plusHours(1)
                Pair(updatedTime, "OK, I have set a reminder for ${updatedTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
            }
            else -> {
                val date = input.substringAfter("on ").substringBefore(" at ").trim()
                val time = input.substringAfter(" at ").trim()
                if (date.isNotEmpty() && time.isNotEmpty()) {
                    val localDT = LocalDateTime.parse("$date $time", dateFormat)
                    Pair(localDT, "OK, I have set a reminder for ${localDT.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
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
        val hoursMinutesPattern = Pattern.compile("(\\d+)\\s?h(?:ou)?rs?\\s?and\\s?(\\d+)\\s?minutes?\\s?from now")
        val oneHourPattern = Pattern.compile("1\\s?h(?:ou)?r(?: from)? now")
        val afterOneHourPattern = Pattern.compile("after 1\\s?h(?:ou)?r")
        val secondsPattern = Pattern.compile("after (\\d+)\\s?(?:seconds?|secs?)")

        return when {
            input.contains("tomorrow", ignoreCase = true) -> {
                val time = input.substringAfter("tomorrow at ").trim()
                val date = currentTime.plusDays(1).format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                val localDT = LocalDateTime.parse("$date $time", dateFormat)
                Pair(localDT, "OK, I have set a reminder for ${localDT.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
            }
            hoursPattern.matcher(input).find() -> {
                val hours = hoursPattern.matcher(input).apply { find() }.group(1).toInt()
                val updatedTime = currentTime.plusHours(hours.toLong())
                Pair(updatedTime, "OK, I have set a reminder for ${updatedTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
            }
            minutesPattern.matcher(input).find() || afterMinutesPattern.matcher(input).find() -> {
                val minutes = if (minutesPattern.matcher(input).find()) {
                    minutesPattern.matcher(input).apply { find() }.group(1).toInt()
                } else {
                    afterMinutesPattern.matcher(input).apply { find() }.group(1).toInt()
                }
                val updatedTime = currentTime.plusMinutes(minutes.toLong())
                Pair(updatedTime, "OK, I have set a reminder for ${updatedTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
            }
            hoursMinutesPattern.matcher(input).find() -> {
                val matcher = hoursMinutesPattern.matcher(input).apply { find() }
                val hours = matcher.group(1).toInt()
                val minutes = matcher.group(2).toInt()
                val updatedTime = currentTime.plusHours(hours.toLong()).plusMinutes(minutes.toLong())
                Pair(updatedTime, "OK, I have set a reminder for ${updatedTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
            }
            oneHourPattern.matcher(input).find() || afterOneHourPattern.matcher(input).find() -> {
                val updatedTime = currentTime.plusHours(1)
                Pair(updatedTime, "OK, I have set a reminder for ${updatedTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
            }
            secondsPattern.matcher(input).find() -> {
                val seconds = secondsPattern.matcher(input).apply { find() }.group(1).toInt()
                val updatedTime = currentTime.plusSeconds(seconds.toLong())
                Pair(updatedTime, "OK, I have set a reminder for ${updatedTime.format(DateTimeFormatter.ofPattern("hh:mm:ss a"))}.")
            }
            else -> {
                val date = input.substringAfter("on ").substringBefore(" at ").trim()
                val time = input.substringAfter(" at ").trim()
                if (date.isNotEmpty() && time.isNotEmpty()) {
                    val localDT = LocalDateTime.parse("$date $time", dateFormat)
                    Pair(localDT, "OK, I have set a reminder for ${localDT.format(DateTimeFormatter.ofPattern("hh:mm a"))}.")
                } else {
                    Pair(null, "Sorry, I couldn't understand your request.")
                }
            }
        }
    }
}
