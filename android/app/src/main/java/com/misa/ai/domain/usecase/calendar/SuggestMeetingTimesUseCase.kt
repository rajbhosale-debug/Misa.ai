package com.misa.ai.domain.usecase.calendar

import com.misa.ai.domain.model.*
import com.misa.ai.domain.repository.CalendarRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.DayOfWeek
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Use case for suggesting optimal meeting times
 * Uses AI-like algorithms to find the best meeting times based on availability, preferences, and patterns
 */
class SuggestMeetingTimesUseCase @Inject constructor(
    private val calendarRepository: CalendarRepository
) {

    /**
     * Suggest meeting times for a group of attendees
     */
    suspend fun suggestMeetingTimes(
        duration: Duration,
        attendees: List<String>,
        preferredTimes: List<TimeRange>,
        dateRange: DateRange,
        options: SuggestionOptions = SuggestionOptions()
    ): Result<List<MeetingSuggestion>> {
        try {
            val suggestions = mutableListOf<MeetingSuggestion>()

            // Get availability for all attendees
            val attendeeAvailability = getAttendeeAvailability(
                attendees,
                dateRange.startDate,
                dateRange.endDate
            )

            // Generate candidate time slots
            val candidateSlots = generateCandidateSlots(
                dateRange,
                preferredTimes,
                duration,
                options
            )

            // Score each candidate slot
            for (slot in candidateSlots) {
                val score = scoreTimeSlot(slot, attendeeAvailability, attendees, options)
                if (score >= options.minScore) {
                    val conflicts = getConflicts(slot, attendeeAvailability)
                    val reasons = generateScoringReasons(slot, attendeeAvailability, options)

                    suggestions.add(
                        MeetingSuggestion(
                            startTime = slot.startTime,
                            endTime = slot.endTime,
                            score = score,
                            reasons = reasons,
                            conflicts = conflicts
                        )
                    )
                }
            }

            // Sort by score (highest first) and limit results
            val sortedSuggestions = suggestions
                .sortedByDescending { it.score }
                .take(options.maxSuggestions)

            return Result.success(sortedSuggestions)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Suggest meeting times for a single user (internal meeting)
     */
    suspend fun suggestInternalMeetingTimes(
        duration: Duration,
        preferredTimes: List<TimeRange>,
        dateRange: DateRange,
        options: SuggestionOptions = SuggestionOptions()
    ): Result<List<MeetingSuggestion>> {
        return suggestMeetingTimes(
            duration = duration,
            attendees = listOf("me"),
            preferredTimes = preferredTimes,
            dateRange = dateRange,
            options = options.copy(considerWorkingHours = true)
        )
    }

    /**
     * Get availability for all attendees
     */
    private suspend fun getAttendeeAvailability(
        attendees: List<String>,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Map<String, List<TimeSlot>> {
        val availabilityMap = mutableMapOf<String, List<TimeSlot>>()

        for (attendee in attendees) {
            try {
                val availability = calendarRepository.getAvailability(startDate, endDate, listOf(attendee))
                    .getOrThrow()
                availabilityMap[attendee] = availability
            } catch (e: Exception) {
                // If we can't get availability for an attendee, assume they're unavailable
                availabilityMap[attendee] = emptyList()
            }
        }

        return availabilityMap
    }

    /**
     * Generate candidate time slots based on preferences and constraints
     */
    private fun generateCandidateSlots(
        dateRange: DateRange,
        preferredTimes: List<TimeRange>,
        duration: Duration,
        options: SuggestionOptions
    ): List<TimeSlot> {
        val slots = mutableListOf<TimeSlot>()
        var currentDateTime = dateRange.startDate

        while (currentDateTime.isBefore(dateRange.endDate)) {
            // Skip weekends if not allowed
            if (!options.includeWeekends && isWeekend(currentDateTime)) {
                currentDateTime = currentDateTime.plusDays(1)
                continue
            }

            // Check if current day matches preferred days
            if (options.preferredDays.isNotEmpty() &&
                currentDateTime.dayOfWeek !in options.preferredDays) {
                currentDateTime = currentDateTime.plusDays(1)
                continue
            }

            // Generate slots for preferred times
            for (timeRange in preferredTimes) {
                val slotStart = parseTimeRange(currentDateTime, timeRange.startTime)
                val slotEnd = parseTimeRange(currentDateTime, timeRange.endTime)

                // Create slots within the preferred time range
                var slotStartIter = slotStart
                while (slotStartIter.plus(duration).isBefore(slotEnd) ||
                       slotStartIter.plus(duration).isEqual(slotEnd)) {

                    val slotEndIter = slotStartIter.plus(duration)

                    // Check if slot is within working hours if required
                    if (options.considerWorkingHours && !isWithinWorkingHours(slotStartIter, slotEndIter)) {
                        slotStartIter = slotStartIter.plusMinutes(options.slotIncrementMinutes)
                        continue
                    }

                    slots.add(
                        TimeSlot(
                            startTime = slotStartIter,
                            endTime = slotEndIter,
                            availability = AvailabilityStatus.Available
                        )
                    )

                    slotStartIter = slotStartIter.plusMinutes(options.slotIncrementMinutes)
                }
            }

            currentDateTime = currentDateTime.plusDays(1)
        }

        return slots
    }

    /**
     * Score a time slot based on various factors
     */
    private fun scoreTimeSlot(
        slot: TimeSlot,
        attendeeAvailability: Map<String, List<TimeSlot>>,
        attendees: List<String>,
        options: SuggestionOptions
    ): Double {
        var score = 0.0

        // Base score for being in preferred time range
        score += 30.0

        // Score based on attendee availability
        val availabilityScore = calculateAvailabilityScore(slot, attendeeAvailability, attendees)
        score += availabilityScore * 40.0

        // Score based on time of day preferences
        val timeOfDayScore = calculateTimeOfDayScore(slot.startTime, options)
        score += timeOfDayScore * 20.0

        // Score based on day of week preferences
        val dayOfWeekScore = calculateDayOfWeekScore(slot.startTime, options)
        score += dayOfWeekScore * 10.0

        // Penalty for slots too far in the future
        val daysFromNow = ChronoUnit.DAYS.between(LocalDateTime.now(), slot.startTime).toInt()
        if (daysFromNow > 7) {
            score -= (daysFromNow - 7) * 2.0
        }

        // Penalty for slots too soon
        if (daysFromNow < 1) {
            score -= (1 - daysFromNow) * 5.0
        }

        return score.coerceIn(0.0, 100.0) / 100.0
    }

    /**
     * Calculate availability score for a time slot
     */
    private fun calculateAvailabilityScore(
        slot: TimeSlot,
        attendeeAvailability: Map<String, List<TimeSlot>>,
        attendees: List<String>
    ): Double {
        if (attendees.isEmpty()) return 1.0

        var totalScore = 0.0
        var availableCount = 0

        for (attendee in attendees) {
            val availability = attendeeAvailability[attendee] ?: emptyList()
            val attendeeScore = getAttendeeScore(slot, availability)
            totalScore += attendeeScore

            if (attendeeScore > 0.5) {
                availableCount++
            }
        }

        // Combine average availability score with ratio of available attendees
        val averageScore = totalScore / attendees.size
        val availabilityRatio = availableCount.toDouble() / attendees.size

        return (averageScore * 0.7) + (availabilityRatio * 0.3)
    }

    /**
     * Get availability score for a single attendee
     */
    private fun getAttendeeScore(slot: TimeSlot, availability: List<TimeSlot>): Double {
        for (availableSlot in availability) {
            when {
                // Perfect match
                availableSlot.startTime <= slot.startTime && availableSlot.endTime >= slot.endTime &&
                availableSlot.availability == AvailabilityStatus.Available -> {
                    return 1.0
                }
                // Partial match with available status
                availableSlot.startTime <= slot.endTime && availableSlot.endTime >= slot.startTime &&
                availableSlot.availability == AvailabilityStatus.Available -> {
                    return 0.8
                }
                // Tentative status
                availableSlot.availability == AvailabilityStatus.Tentative -> {
                    return 0.3
                }
            }
        }

        return 0.0 // No availability found
    }

    /**
     * Calculate time of day scoring
     */
    private fun calculateTimeOfDayScore(startTime: LocalDateTime, options: SuggestionOptions): Double {
        val hour = startTime.hour

        return when {
            // Morning hours (9-12) - preferred
            hour in 9..11 -> 1.0
            // Early afternoon (12-15) - good
            hour in 12..15 -> 0.9
            // Late afternoon (16-17) - acceptable
            hour in 16..17 -> 0.7
            // Early morning (6-8) - less preferred
            hour in 6..8 -> 0.4
            // Evening (18-20) - less preferred
            hour in 18..20 -> 0.3
            // Late evening (21-22) - poor
            hour in 21..22 -> 0.1
            // Night/early morning (23-5) - very poor
            else -> 0.0
        }
    }

    /**
     * Calculate day of week scoring
     */
    private fun calculateDayOfWeekScore(startTime: LocalDateTime, options: SuggestionOptions): Double {
        val dayOfWeek = startTime.dayOfWeek

        return when (dayOfWeek) {
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY -> 1.0
            DayOfWeek.FRIDAY -> 0.9
            DayOfWeek.SATURDAY -> if (options.includeWeekends) 0.7 else 0.0
            DayOfWeek.SUNDAY -> if (options.includeWeekends) 0.6 else 0.0
        }
    }

    /**
     * Get conflicts for a time slot
     */
    private fun getConflicts(
        slot: TimeSlot,
        attendeeAvailability: Map<String, List<TimeSlot>>
    ): List<String> {
        val conflicts = mutableListOf<String>()

        for ((attendee, availability) in attendeeAvailability) {
            for (availableSlot in availability) {
                if (availableSlot.startTime < slot.endTime && availableSlot.endTime > slot.startTime) {
                    if (availableSlot.availability != AvailabilityStatus.Available) {
                        conflicts.add(attendee)
                        break
                    }
                }
            }
        }

        return conflicts
    }

    /**
     * Generate scoring reasons
     */
    private fun generateScoringReasons(
        slot: TimeSlot,
        attendeeAvailability: Map<String, List<TimeSlot>>,
        options: SuggestionOptions
    ): List<String> {
        val reasons = mutableListOf<String>()

        val availableCount = attendeeAvailability.values.count { availability ->
            availability.any { availableSlot ->
                availableSlot.startTime <= slot.startTime && availableSlot.endTime >= slot.endTime &&
                availableSlot.availability == AvailabilityStatus.Available
            }
        }

        if (availableCount == attendeeAvailability.size) {
            reasons.add("All attendees are available")
        } else {
            reasons.add("$availableCount of ${attendeeAvailability.size} attendees available")
        }

        if (isWithinWorkingHours(slot.startTime, slot.endTime)) {
            reasons.add("Within working hours")
        }

        val timeOfDayScore = calculateTimeOfDayScore(slot.startTime, options)
        if (timeOfDayScore >= 0.8) {
            reasons.add("Optimal time of day")
        }

        return reasons
    }

    /**
     * Helper methods
     */
    private fun isWeekend(dateTime: LocalDateTime): Boolean {
        return dateTime.dayOfWeek == DayOfWeek.SATURDAY || dateTime.dayOfWeek == DayOfWeek.SUNDAY
    }

    private fun parseTimeRange(date: LocalDateTime, timeString: String): LocalDateTime {
        val parts = timeString.split(":")
        return date.withHour(parts[0].toInt()).withMinute(parts[1].toInt()).withSecond(0)
    }

    private fun isWithinWorkingHours(startTime: LocalDateTime, endTime: LocalDateTime): Boolean {
        return startTime.hour in 9..17 && endTime.hour in 9..17
    }
}

/**
 * Options for meeting time suggestions
 */
data class SuggestionOptions(
    val includeWeekends: Boolean = false,
    val preferredDays: List<DayOfWeek> = emptyList(),
    val considerWorkingHours: Boolean = true,
    val slotIncrementMinutes: Int = 15,
    val maxSuggestions: Int = 10,
    val minScore: Double = 0.3
)