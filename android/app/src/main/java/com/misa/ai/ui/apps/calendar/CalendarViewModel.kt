package com.misa.ai.ui.apps.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misa.ai.domain.usecase.calendar.*
import com.misa.ai.domain.model.CalendarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getEventsUseCase: GetEventsUseCase,
    private val createEventUseCase: CreateEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val deleteEventUseCase: DeleteEventUseCase,
    private val importCalendarUseCase: ImportCalendarUseCase,
    private val syncCalendarsUseCase: SyncCalendarsUseCase,
    private val getAISuggestionsUseCase: GetCalendarAISuggestionsUseCase,
    private val ocrUseCase: CalendarOCRUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadEvents()
        observeDateChanges()
    }

    private fun loadEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getEventsUseCase().catch { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            }.collect { events ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        events = events,
                        error = null
                    )
                }
            }
        }
    }

    private fun observeDateChanges() {
        viewModelScope.launch {
            snapshotFlow { _uiState.value.selectedDate }
                .distinctUntilChanged()
                .collect { selectedDate ->
                    updateSelectedDateEvents(selectedDate)
                }
        }
    }

    private fun updateSelectedDateEvents(selectedDate: LocalDate) {
        _uiState.update { currentState ->
            val selectedDateEvents = currentState.events.filter { event ->
                event.startTime.toLocalDate() == selectedDate
            }
            currentState.copy(selectedDateEvents = selectedDateEvents)
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun createEvent() {
        viewModelScope.launch {
            // Open event creation dialog
            _uiState.update {
                it.copy(
                    isCreatingEvent = true,
                    editingEvent = null
                )
            }
        }
    }

    fun editEvent(eventId: String) {
        viewModelScope.launch {
            val event = _uiState.value.events.find { it.id == eventId }
            _uiState.update {
                it.copy(
                    isCreatingEvent = true,
                    editingEvent = event
                )
            }
        }
    }

    fun saveEvent(event: CalendarEvent) {
        viewModelScope.launch {
            try {
                if (event.id.isEmpty()) {
                    createEventUseCase(event)
                } else {
                    updateEventUseCase(event)
                }
                _uiState.update {
                    it.copy(
                        isCreatingEvent = false,
                        editingEvent = null
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Failed to save event: ${error.message}"
                    )
                }
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            try {
                deleteEventUseCase(eventId)
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Failed to delete event: ${error.message}"
                    )
                }
            }
        }
    }

    fun importCalendarFile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }

            try {
                importCalendarUseCase().collect { result ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            isImporting = false,
                            importResult = result
                        )
                    }
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        error = "Import failed: ${error.message}"
                    )
                }
            }
        }
    }

    fun syncCalendars() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }

            try {
                syncCalendarsUseCase()
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        lastSyncTime = LocalDateTime.now()
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        error = "Sync failed: ${error.message}"
                    )
                }
            }
        }
    }

    fun getAISchedulingSuggestions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSuggestions = true) }

            try {
                val suggestions = getAISuggestionsUseCase(_uiState.value.selectedDate)
                _uiState.update {
                    it.copy(
                        isLoadingSuggestions = false,
                        aiSuggestions = suggestions
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingSuggestions = false,
                        error = "Failed to get AI suggestions: ${error.message}"
                    )
                }
            }
        }
    }

    fun processOCRImage(imagePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingOCR = true) }

            try {
                val parsedEvents = ocrUseCase(imagePath)
                _uiState.update {
                    it.copy(
                        isProcessingOCR = false,
                        ocrResult = parsedEvents
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessingOCR = false,
                        error = "OCR processing failed: ${error.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearImportResult() {
        _uiState.update { it.copy(importResult = null) }
    }

    fun clearAISSuggestions() {
        _uiState.update { it.copy(aiSuggestions = emptyList()) }
    }
}

data class CalendarUiState(
    val currentDate: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val events: List<CalendarEvent> = emptyList(),
    val selectedDateEvents: List<CalendarEvent> = emptyList(),
    val isLoading: Boolean = false,
    val isCreatingEvent: Boolean = false,
    val editingEvent: CalendarEvent? = null,
    val isImporting: Boolean = false,
    val isSyncing: Boolean = false,
    val isProcessingOCR: Boolean = false,
    val isLoadingSuggestions: Boolean = false,
    val lastSyncTime: LocalDateTime? = null,
    val importResult: ImportResult? = null,
    val aiSuggestions: List<AISchedulingSuggestion> = emptyList(),
    val ocrResult: List<CalendarEvent> = emptyList(),
    val error: String? = null
)

data class ImportResult(
    val totalEvents: Int,
    val successfulImports: Int,
    val conflicts: List<ImportConflict>,
    val source: String
)

data class AISchedulingSuggestion(
    val id: String,
    val type: SuggestionType,
    val title: String,
    val description: String,
    val proposedTime: LocalDateTime,
    val confidence: Float,
    val priority: SuggestionPriority
)

enum class SuggestionType {
    FREE_TIME,
    DEADLINE_REMINDER,
    TRAVEL_TIME,
    PREPARATION_NEEDED,
    RECURRING_OPTIMIZATION
}

enum class SuggestionPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class ImportConflict(
    val existingEvent: CalendarEvent,
    val newEvent: CalendarEvent,
    val conflictType: ConflictType
)

enum class ConflictType {
    TIME_OVERLAP,
    DUPLICATE_EVENT,
    MISSING_INFORMATION
}