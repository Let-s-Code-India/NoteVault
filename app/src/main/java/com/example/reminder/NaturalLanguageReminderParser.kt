package com.example.reminder

import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class ParsedReminderResult(
    val cleanTitle: String,
    val customNote: String = "",
    val triggerTime: Long,
    val repeatType: String = "NONE", // "NONE", "DAILY", "WEEKLY", "MONTHLY", "CUSTOM_DAYS"
    val repeatInterval: Int = 1,
    val weeklyDays: String = "",
    val matchedTag: String? = null
)

object NaturalLanguageReminderParser {

    fun parse(input: String): ParsedReminderResult {
        var text = input.trim()
        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance().apply {
            // Default to 1 hour from now or tomorrow 9am if late
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var matchedTag: String? = null
        val tagMatcher = Pattern.compile("(#[a-zA-Z0-9_-]+)").matcher(text)
        if (tagMatcher.find()) {
            matchedTag = tagMatcher.group(1)
            text = text.replace(matchedTag, "").trim()
        }

        var repeatType = "NONE"
        var repeatInterval = 1
        var weeklyDays = ""

        val lowerText = text.lowercase(Locale.ROOT)

        // Parse Recurrence
        when {
            lowerText.contains("every weekday") || lowerText.contains("on weekdays") -> {
                repeatType = "WEEKLY"
                weeklyDays = "1,2,3,4,5" // Mon..Fri
                text = removeMatch(text, "(?i)every weekday|on weekdays")
            }
            lowerText.contains("every day") || lowerText.contains("daily") -> {
                repeatType = "DAILY"
                text = removeMatch(text, "(?i)every day|daily")
            }
            lowerText.contains("every week") || lowerText.contains("weekly") -> {
                repeatType = "WEEKLY"
                text = removeMatch(text, "(?i)every week|weekly")
            }
            lowerText.contains("every month") || lowerText.contains("monthly") -> {
                repeatType = "MONTHLY"
                text = removeMatch(text, "(?i)every month|monthly")
            }
            lowerText.contains("every monday") || lowerText.contains("every mon") -> {
                repeatType = "WEEKLY"; weeklyDays = "1"
                text = removeMatch(text, "(?i)every monday|every mon")
            }
            lowerText.contains("every tuesday") || lowerText.contains("every tue") -> {
                repeatType = "WEEKLY"; weeklyDays = "2"
                text = removeMatch(text, "(?i)every tuesday|every tue")
            }
            lowerText.contains("every wednesday") || lowerText.contains("every wed") -> {
                repeatType = "WEEKLY"; weeklyDays = "3"
                text = removeMatch(text, "(?i)every wednesday|every wed")
            }
            lowerText.contains("every thursday") || lowerText.contains("every thu") -> {
                repeatType = "WEEKLY"; weeklyDays = "4"
                text = removeMatch(text, "(?i)every thursday|every thu")
            }
            lowerText.contains("every friday") || lowerText.contains("every fri") -> {
                repeatType = "WEEKLY"; weeklyDays = "5"
                text = removeMatch(text, "(?i)every friday|every fri")
            }
            lowerText.contains("every saturday") || lowerText.contains("every sat") -> {
                repeatType = "WEEKLY"; weeklyDays = "6"
                text = removeMatch(text, "(?i)every saturday|every sat")
            }
            lowerText.contains("every sunday") || lowerText.contains("every sun") -> {
                repeatType = "WEEKLY"; weeklyDays = "7"
                text = removeMatch(text, "(?i)every sunday|every sun")
            }
        }

        // Custom interval regex e.g. "every 3 days"
        val customDaysMatch = Pattern.compile("(?i)every (\\d+) days").matcher(text)
        if (customDaysMatch.find()) {
            repeatType = "CUSTOM_DAYS"
            repeatInterval = customDaysMatch.group(1)?.toIntOrNull() ?: 1
            text = text.replace(customDaysMatch.group(0) ?: "", "").trim()
        }

        // Parse Relative offset (e.g. "in 2 hours", "in 30 mins", "in 3 days")
        var timeSetByRelative = false
        val relMinMatch = Pattern.compile("(?i)in (\\d+)\\s*(min|mins|minute|minutes)").matcher(text)
        if (relMinMatch.find()) {
            val mins = relMinMatch.group(1)?.toIntOrNull() ?: 30
            calendar.timeInMillis = now.timeInMillis + mins * 60 * 1000L
            timeSetByRelative = true
            text = text.replace(relMinMatch.group(0) ?: "", "").trim()
        }

        val relHrMatch = Pattern.compile("(?i)in (\\d+)\\s*(hr|hrs|hour|hours)").matcher(text)
        if (!timeSetByRelative && relHrMatch.find()) {
            val hrs = relHrMatch.group(1)?.toIntOrNull() ?: 1
            calendar.timeInMillis = now.timeInMillis + hrs * 3600 * 1000L
            timeSetByRelative = true
            text = text.replace(relHrMatch.group(0) ?: "", "").trim()
        }

        val relDayMatch = Pattern.compile("(?i)in (\\d+)\\s*(day|days)").matcher(text)
        if (!timeSetByRelative && relDayMatch.find()) {
            val days = relDayMatch.group(1)?.toIntOrNull() ?: 1
            calendar.timeInMillis = now.timeInMillis + days * 86400 * 1000L
            timeSetByRelative = true
            text = text.replace(relDayMatch.group(0) ?: "", "").trim()
        }

        if (!timeSetByRelative) {
            // Parse Relative Day
            var dayOffset = 0
            var setSpecificTimeHour = -1
            var setSpecificTimeMinute = 0

            val currentLower = text.lowercase(Locale.ROOT)
            when {
                currentLower.contains("tomorrow morning") -> {
                    dayOffset = 1; setSpecificTimeHour = 9; setSpecificTimeMinute = 0
                    text = removeMatch(text, "(?i)tomorrow morning")
                }
                currentLower.contains("tomorrow afternoon") -> {
                    dayOffset = 1; setSpecificTimeHour = 14; setSpecificTimeMinute = 0
                    text = removeMatch(text, "(?i)tomorrow afternoon")
                }
                currentLower.contains("tomorrow evening") -> {
                    dayOffset = 1; setSpecificTimeHour = 19; setSpecificTimeMinute = 0
                    text = removeMatch(text, "(?i)tomorrow evening")
                }
                currentLower.contains("tomorrow") -> {
                    dayOffset = 1
                    text = removeMatch(text, "(?i)tomorrow")
                }
                currentLower.contains("tonight") -> {
                    dayOffset = 0; setSpecificTimeHour = 20; setSpecificTimeMinute = 0
                    text = removeMatch(text, "(?i)tonight")
                }
                currentLower.contains("today") -> {
                    dayOffset = 0
                    text = removeMatch(text, "(?i)today")
                }
                currentLower.contains("this weekend") || currentLower.contains("weekend") -> {
                    // Saturday 9 AM
                    val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
                    val daysUntilSaturday = (Calendar.SATURDAY - currentDayOfWeek + 7) % 7
                    dayOffset = if (daysUntilSaturday == 0) 7 else daysUntilSaturday
                    setSpecificTimeHour = 9; setSpecificTimeMinute = 0
                    text = removeMatch(text, "(?i)this weekend|weekend")
                }
            }

            // Days of week ("next Monday", "Monday", "next Fri")
            val dayNames = mapOf(
                "monday" to Calendar.MONDAY, "mon" to Calendar.MONDAY,
                "tuesday" to Calendar.TUESDAY, "tue" to Calendar.TUESDAY,
                "wednesday" to Calendar.WEDNESDAY, "wed" to Calendar.WEDNESDAY,
                "thursday" to Calendar.THURSDAY, "thu" to Calendar.THURSDAY,
                "friday" to Calendar.FRIDAY, "fri" to Calendar.FRIDAY,
                "saturday" to Calendar.SATURDAY, "sat" to Calendar.SATURDAY,
                "sunday" to Calendar.SUNDAY, "sun" to Calendar.SUNDAY
            )

            for ((dayStr, calendarDay) in dayNames) {
                val p = Pattern.compile("(?i)(next\\s+)?$dayStr").matcher(text)
                if (p.find()) {
                    val currentDay = now.get(Calendar.DAY_OF_WEEK)
                    var diff = (calendarDay - currentDay + 7) % 7
                    if (diff == 0) diff = 7
                    dayOffset = diff
                    text = text.replace(p.group(0) ?: "", "").trim()
                    break
                }
            }

            // Apply day offset
            if (dayOffset > 0) {
                calendar.timeInMillis = now.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, dayOffset)
            }

            // Parse Time e.g. "at 9am", "at 9:30pm", "at 14:00", "8pm", "10am"
            val timeMatch = Pattern.compile("(?i)(at\\s+)?(\\d{1,2})(:(\\d{2}))?\\s*(am|pm)?").matcher(text)
            var matchedTimeStr = ""
            if (timeMatch.find()) {
                val hourGroup = timeMatch.group(2)?.toIntOrNull()
                val minGroup = timeMatch.group(4)?.toIntOrNull() ?: 0
                val amPmGroup = timeMatch.group(5)?.lowercase(Locale.ROOT)

                if (hourGroup != null) {
                    var hour = hourGroup
                    if (amPmGroup == "pm" && hour < 12) hour += 12
                    if (amPmGroup == "am" && hour == 12) hour = 0

                    if (hour in 0..23) {
                        setSpecificTimeHour = hour
                        setSpecificTimeMinute = minGroup
                        matchedTimeStr = timeMatch.group(0) ?: ""
                    }
                }
            }

            if (matchedTimeStr.isNotBlank()) {
                text = text.replace(matchedTimeStr, "").trim()
            }

            if (setSpecificTimeHour >= 0) {
                calendar.set(Calendar.HOUR_OF_DAY, setSpecificTimeHour)
                calendar.set(Calendar.MINUTE, setSpecificTimeMinute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }

            // If parsed time is in the past, push to tomorrow
            if (calendar.timeInMillis <= now.timeInMillis && dayOffset == 0 && setSpecificTimeHour < 0) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Clean title
        var cleanTitle = text
            .replace(Regex("(?i)^remind me to\\s+"), "")
            .replace(Regex("(?i)^remind me\\s+"), "")
            .replace(Regex("(?i)^reminder:\\s*"), "")
            .replace(Regex("(?i)^remind\\s+"), "")
            .trim()

        // Strip trailing/leading prepositions
        cleanTitle = cleanTitle.replace(Regex("(?i)^at\\s+"), "")
            .replace(Regex("(?i)^on\\s+"), "")
            .replace(Regex("(?i)^for\\s+"), "")
            .replace(Regex("(?i)\\s+at$"), "")
            .replace(Regex("(?i)\\s+on$"), "")
            .trim()

        if (cleanTitle.isBlank()) {
            cleanTitle = "Reminder"
        }

        return ParsedReminderResult(
            cleanTitle = cleanTitle.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
            customNote = "",
            triggerTime = calendar.timeInMillis,
            repeatType = repeatType,
            repeatInterval = repeatInterval,
            weeklyDays = weeklyDays,
            matchedTag = matchedTag
        )
    }

    private fun removeMatch(input: String, regex: String): String {
        return input.replace(Regex(regex), "").trim()
    }
}
