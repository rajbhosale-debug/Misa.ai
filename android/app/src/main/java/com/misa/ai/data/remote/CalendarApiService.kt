package com.misa.ai.data.remote

import com.misa.ai.domain.model.*
import retrofit2.Response
import retrofit2.http.*
import kotlinx.coroutines.flow.Flow

/**
 * Remote API service for calendar synchronization
 * Handles communication with external calendar services (Google Calendar, Outlook, etc.)
 */
interface CalendarApiService {

    /**
     * Get events for a specific calendar
     */
    @GET("calendars/{calendarId}/events")
    suspend fun getEventsForCalendar(
        @Path("calendarId") calendarId: String,
        @Query("timeMin") timeMin: String,
        @Query("timeMax") timeMax: String
    ): Response<List<CalendarEvent>>

    /**
     * Create a new event
     */
    @POST("calendars/{calendarId}/events")
    suspend fun createEvent(
        @Path("calendarId") calendarId: String,
        @Body event: CalendarEvent
    ): Response<CalendarEvent>

    /**
     * Update an existing event
     */
    @PUT("calendars/{calendarId}/events/{eventId}")
    suspend fun updateEvent(
        @Path("calendarId") calendarId: String,
        @Path("eventId") eventId: String,
        @Body event: CalendarEvent
    ): Response<CalendarEvent>

    /**
     * Delete an event
     */
    @DELETE("calendars/{calendarId}/events/{eventId}")
    suspend fun deleteEvent(
        @Path("calendarId") calendarId: String,
        @Path("eventId") eventId: String
    ): Response<Unit>

    /**
     * Get all calendars for the user
     */
    @GET("calendars")
    suspend fun getCalendars(): Response<List<Calendar>>

    /**
     * Create a new calendar
     */
    @POST("calendars")
    suspend fun createCalendar(@Body calendar: Calendar): Response<Calendar>

    /**
     * Update a calendar
     */
    @PUT("calendars/{calendarId}")
    suspend fun updateCalendar(
        @Path("calendarId") calendarId: String,
        @Body calendar: Calendar
    ): Response<Calendar>

    /**
     * Delete a calendar
     */
    @DELETE("calendars/{calendarId}")
    suspend fun deleteCalendar(@Path("calendarId") calendarId: String): Response<Unit>

    /**
     * Get event attendees and their availability
     */
    @GET("events/{eventId}/availability")
    suspend fun getEventAvailability(
        @Path("eventId") eventId: String,
        @Query("attendees") attendees: List<String>
    ): Response<List<TimeSlot>>

    /**
     * Import events from external source
     */
    @Multipart
    @POST("calendars/{calendarId}/import")
    suspend fun importEvents(
        @Path("calendarId") calendarId: String,
        @Part("file") file: okhttp3.MultipartBody.Part,
        @Part("format") format: String
    ): Response<ImportResult>

    /**
     * Export events to external format
     */
    @POST("calendars/{calendarId}/export")
    suspend fun exportEvents(
        @Path("calendarId") calendarId: String,
        @Body request: ExportRequest
    ): Response<okhttp3.ResponseBody>

    /**
     * Search events across all calendars
     */
    @GET("search/events")
    suspend fun searchEvents(
        @Query("q") query: String,
        @Query("timeMin") timeMin: String?,
        @Query("timeMax") timeMax: String?,
        @Query("limit") limit: Int?
    ): Response<List<CalendarEvent>>

    /**
     * Get suggested meeting times
     */
    @POST("suggestions/meeting-times")
    suspend fun getMeetingSuggestions(
        @Body request: MeetingSuggestionRequest
    ): Response<List<MeetingSuggestion>>

    /**
     * Batch operations
     */
    @POST("calendars/{calendarId}/events/batch")
    suspend fun batchCreateEvents(
        @Path("calendarId") calendarId: String,
        @Body events: List<CalendarEvent>
    ): Response<List<CalendarEvent>>

    @PUT("calendars/{calendarId}/events/batch")
    suspend fun batchUpdateEvents(
        @Path("calendarId") calendarId: String,
        @Body events: List<CalendarEvent>
    ): Response<List<CalendarEvent>>

    @DELETE("calendars/{calendarId}/events/batch")
    suspend fun batchDeleteEvents(
        @Path("calendarId") calendarId: String,
        @Body eventIds: List<String>
    ): Response<Unit>
}

/**
 * Request data for exporting events
 */
data class ExportRequest(
    val eventIds: List<String>,
    val format: ExportFormat,
    val includeAttachments: Boolean = false
)

/**
 * Request data for meeting suggestions
 */
data class MeetingSuggestionRequest(
    val duration: kotlin.time.Duration,
    val attendees: List<String>,
    val preferredTimes: List<TimeRange>,
    val dateRange: DateRange,
    val maxSuggestions: Int = 10
)

/**
 * Google Calendar specific API service
 */
interface GoogleCalendarApiService {

    @GET("calendars/{calendarId}/events")
    suspend fun getGoogleEvents(
        @Path("calendarId") calendarId: String,
        @Query("timeMin") timeMin: String,
        @Query("timeMax") timeMax: String,
        @Query("singleEvents") singleEvents: Boolean = true,
        @Query("orderBy") orderBy: String = "startTime"
    ): Response<GoogleCalendarResponse>

    @POST("calendars/{calendarId}/events/quickAdd")
    suspend fun quickAddEvent(
        @Path("calendarId") calendarId: String,
        @Query("text") text: String
    ): Response<GoogleEvent>

    @GET("freeBusy")
    suspend fun getFreeBusy(
        @Body request: GoogleFreeBusyRequest
    ): Response<GoogleFreeBusyResponse>
}

/**
 * Microsoft Graph API service for Outlook Calendar
 */
interface OutlookCalendarApiService {

    @GET("me/calendars")
    suspend fun getOutlookCalendars(): Response<OutlookCalendarResponse>

    @GET("me/calendar/events")
    suspend fun getOutlookEvents(
        @Query("$filter") filter: String?,
        @Query("$orderby") orderBy: String?,
        @Query("$top") top: Int?
    ): Response<OutlookEventResponse>

    @POST("me/calendar/events")
    suspend fun createOutlookEvent(@Body event: OutlookEvent): Response<OutlookEvent>

    @GET("me/findMeetingTimes")
    suspend fun findMeetingTimes(@Body request: OutlookMeetingTimesRequest): Response<OutlookMeetingTimesResponse>
}

/**
 * API response models for external calendar services
 */
data class GoogleCalendarResponse(
    val items: List<GoogleEvent>,
    val nextPageToken: String?
)

data class GoogleEvent(
    val id: String,
    val summary: String?,
    val description: String?,
    val start: GoogleEventDateTime,
    val end: GoogleEventDateTime,
    val location: String?,
    val attendees: List<GoogleAttendee>?,
    val recurrence: List<String>?,
    val reminders: GoogleReminders?,
    val extendedProperties: GoogleExtendedProperties?
)

data class GoogleEventDateTime(
    val dateTime: String?,
    val date: String?,
    val timeZone: String?
)

data class GoogleAttendee(
    val email: String,
    val displayName: String?,
    val responseStatus: String,
    val organizer: Boolean,
    val optional: Boolean
)

data class GoogleReminders(
    val useDefault: Boolean,
    val overrides: List<GoogleReminderOverride>?
)

data class GoogleReminderOverride(
    val method: String,
    val minutes: Int
)

data class GoogleExtendedProperties(
    val private: Map<String, String>?,
    val shared: Map<String, String>?
)

data class GoogleFreeBusyRequest(
    val timeMin: String,
    val timeMax: String,
    val items: List<GoogleFreeBusyItem>
)

data class GoogleFreeBusyItem(
    val id: String
)

data class GoogleFreeBusyResponse(
    val calendars: Map<String, GoogleCalendarInfo>
)

data class GoogleCalendarInfo(
    val busy: List<GoogleTimePeriod>
)

data class GoogleTimePeriod(
    val start: String,
    val end: String
)

data class OutlookCalendarResponse(
    val value: List<OutlookCalendar>
)

data class OutlookCalendar(
    val id: String,
    val name: String,
    val canEdit: Boolean
)

data class OutlookEventResponse(
    val value: List<OutlookEvent>
)

data class OutlookEvent(
    val id: String,
    val subject: String?,
    val body: OutlookBody?,
    val start: OutlookDateTime,
    val end: OutlookDateTime,
    val location: OutlookLocation?,
    val attendees: List<OutlookAttendee>?
)

data class OutlookBody(
    val contentType: String,
    val content: String
)

data class OutlookDateTime(
    val dateTime: String,
    val timeZone: String
)

data class OutlookLocation(
    val displayName: String?,
    val address: OutlookAddress?
)

data class OutlookAddress(
    val street: String?,
    val city: String?,
    val state: String?,
    val countryOrRegion: String?,
    val postalCode: String?
)

data class OutlookAttendee(
    val emailAddress: OutlookEmailAddress,
    val status: OutlookResponseStatus
)

data class OutlookEmailAddress(
    val name: String?,
    val address: String
)

data class OutlookResponseStatus(
    val response: String,
    val time: String?
)

data class OutlookMeetingTimesRequest(
    val attendees: List<OutlookAttendee>,
    val timeConstraint: OutlookTimeConstraint,
    val meetingDuration: kotlin.time.Duration
)

data class OutlookTimeConstraint(
    val timeslots: List<OutlookTimeSlot>
)

data class OutlookTimeSlot(
    val start: OutlookDateTime,
    val end: OutlookDateTime
)

data class OutlookMeetingTimesResponse(
    val value: List<OutlookMeetingTimeSuggestion>
)

data class OutlookMeetingTimeSuggestion(
    val meetingTimeSlot: OutlookTimeSlot,
    val confidence: Double,
    val attendeeAvailability: List<OutlookAttendeeAvailability>
)

data class OutlookAttendeeAvailability(
    val attendee: OutlookEmailAddress,
    val availability: String
)