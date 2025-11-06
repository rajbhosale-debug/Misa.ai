//! Tauri command handlers for the MISA.AI Desktop Application
//! Provides the bridge between frontend and backend functionality

use tauri::{State, Window};
use serde::{Deserialize, Serialize};
use crate::{MisaAppState, AppResult, AppError};
use crate::config::Config;
use crate::device::DeviceInfo;
use crate::file::{FileNode, FileUploadParams, FileSearchParams};
use crate::focus::{FocusSession, FocusSessionParams, FocusStats};
use crate::vision::{ScreenCaptureParams, UIElement, TextRegion};
use crate::system::SystemInfo;
use crate::ai::{AIRequest, AIResponse, AIRecommendationType};

// =============================================================================
// CORE COMMANDS
// =============================================================================

/// Get application information
#[tauri::command]
pub async fn get_app_info() -> Result<crate::AppInfo, String> {
    Ok(crate::AppInfo::default())
}

/// Get current configuration
#[tauri::command]
pub async fn get_config(state: State<'_, MisaAppState>) -> Result<Config, String> {
    Ok(state.get_config())
}

/// Update configuration
#[tauri::command]
pub async fn update_config(
    config: Config,
    state: State<'_, MisaAppState>
) -> Result<(), String> {
    state.update_config(config).await.map_err(|e| e.to_string())
}

// =============================================================================
// DEVICE COMMANDS
// =============================================================================

/// Start device discovery
#[tauri::command]
pub async fn start_device_discovery(
    state: State<'_, MisaAppState>
) -> Result<(), String> {
    state.device_manager.start_discovery().await
        .map_err(|e| e.to_string())
}

/// Stop device discovery
#[tauri::command]
pub async fn stop_device_discovery(
    state: State<'_, MisaAppState>
) -> Result<(), String> {
    state.device_manager.stop_discovery().await
        .map_err(|e| e.to_string())
}

/// Get connected devices
#[tauri::command]
pub async fn get_connected_devices(
    state: State<'_, MisaAppState>
) -> Result<Vec<DeviceInfo>, String> {
    state.device_manager.get_connected_devices().await
        .map_err(|e| e.to_string())
}

/// Send message to device
#[tauri::command]
pub async fn send_message_to_device(
    device_id: String,
    message: String,
    state: State<'_, MisaAppState>
) -> Result<(), String> {
    state.device_manager.send_message(device_id, message).await
        .map_err(|e| e.to_string())
}

/// Connect to device
#[tauri::command]
pub async fn connect_to_device(
    device_id: String,
    state: State<'_, MisaAppState>
) -> Result<(), String> {
    state.device_manager.connect_to_device(device_id).await
        .map_err(|e| e.to_string())
}

/// Disconnect from device
#[tauri::command]
pub async fn disconnect_from_device(
    device_id: String,
    state: State<'_, MisaAppState>
) -> Result<(), String> {
    state.device_manager.disconnect_from_device(device_id).await
        .map_err(|e| e.to_string())
}

// =============================================================================
// VISION COMMANDS
// =============================================================================

/// Capture screen
#[tauri::command]
pub async fn capture_screen(
    params: ScreenCaptureParams,
    state: State<'_, MisaAppState>
) -> Result<String, String> { // Returns capture ID
    let capture_id = state.vision_manager.capture_screen(params).await
        .map_err(|e| e.to_string())?;
    Ok(capture_id)
}

/// Detect UI elements in captured screen
#[tauri::command]
pub async fn detect_ui_elements(
    capture_id: String,
    state: State<'_, MisaAppState>
) -> Result<Vec<UIElement>, String> {
    state.vision_manager.detect_ui_elements(capture_id).await
        .map_err(|e| e.to_string())
}

/// Extract text from captured screen
#[tauri::command]
pub async fn extract_text_from_image(
    capture_id: String,
    state: State<'_, MisaAppState>
) -> Result<Vec<TextRegion>, String> {
    state.vision_manager.extract_text(capture_id).await
        .map_err(|e| e.to_string())
}

/// Get capture thumbnail
#[tauri::command]
pub async fn get_capture_thumbnail(
    capture_id: String,
    state: State<'_, MisaAppState>
) -> Result<String, String> { // Returns base64 image
    state.vision_manager.get_thumbnail(capture_id).await
        .map_err(|e| e.to_string())
}

/// Perform intelligent screenshot analysis
#[tauri::command]
pub async fn intelligent_screenshot(
    params: ScreenCaptureParams,
    state: State<'_, MisaAppState>
) -> Result<crate::vision::IntelligentScreenshot, String> {
    state.vision_manager.intelligent_screenshot(params).await
        .map_err(|e| e.to_string())
}

// =============================================================================
// FILE COMMANDS
// =============================================================================

/// List files in directory
#[tauri::command]
pub async fn list_files(
    parent_id: Option<String>,
    search_params: Option<FileSearchParams>,
    state: State<'_, MisaAppState>
) -> Result<Vec<FileNode>, String> {
    let params = search_params.unwrap_or_default();
    state.file_manager.list_files(parent_id, params).await
        .map_err(|e| e.to_string())
}

/// Upload file
#[tauri::command]
pub async fn upload_file(
    params: FileUploadParams,
    state: State<'_, MisaAppState>
) -> Result<String, String> { // Returns file ID
    let file_id = state.file_manager.upload_file(params).await
        .map_err(|e| e.to_string())?;
    Ok(file_id)
}

/// Download file
#[tauri::command]
pub async fn download_file(
    file_id: String,
    local_path: String,
    state: State<'_, MisaAppState>
) -> Result<(), String> {
    state.file_manager.download_file(file_id, local_path).await
        .map_err(|e| e.to_string())
}

/// Create folder
#[tauri::command]
pub async fn create_folder(
    name: String,
    parent_id: Option<String>,
    state: State<'_, MisaAppState>
) -> Result<String, String> { // Returns folder ID
    let folder_id = state.file_manager.create_folder(name, parent_id).await
        .map_err(|e| e.to_string())?;
    Ok(folder_id)
}

/// Delete file or folder
#[tauri::command]
pub async fn delete_file(
    file_id: String,
    permanent: bool,
    state: State<'_, MisaAppState>
) -> Result<(), String> {
    state.file_manager.delete_file(file_id, permanent).await
        .map_err(|e| e.to_string())
}

/// Move file
#[tauri::command]
pub async fn move_file(
    file_id: String,
    new_parent_id: String,
    state: State<'_, MisaAppState>
) -> Result<(), String> {
    state.file_manager.move_file(file_id, new_parent_id).await
        .map_err(|e| e.to_string())
}

/// Copy file
#[tauri::command]
pub async fn copy_file(
    file_id: String,
    new_parent_id: String,
    new_name: Option<String>,
    state: State<'_, MisaAppState>
) -> Result<String, String> { // Returns new file ID
    let new_file_id = state.file_manager.copy_file(file_id, new_parent_id, new_name).await
        .map_err(|e| e.to_string())?;
    Ok(new_file_id)
}

/// Rename file
#[tauri::command]
pub async fn rename_file(
    file_id: String,
    new_name: String,
    state: State<'_, MisaAppState>
) -> Result<(), String> {
    state.file_manager.rename_file(file_id, new_name).await
        .map_err(|e| e.to_string())
}

/// Search files
#[tauri::command]
pub async fn search_files(
    query: String,
    filters: Option<FileSearchParams>,
    state: State<'_, MisaAppState>
) -> Result<Vec<FileNode>, String> {
    let mut params = filters.unwrap_or_default();
    params.query = Some(query);
    state.file_manager.search_files(params).await
        .map_err(|e| e.to_string())
}

/// Get file metadata
#[tauri::command]
pub async fn get_file_metadata(
    file_id: String,
    state: State<'_, MisaAppState>
) -> Result<crate::file::FileMetadata, String> {
    state.file_manager.get_file_metadata(file_id).await
        .map_err(|e| e.to_string())
}

/// Update file metadata
#[tauri::command]
pub async fn update_file_metadata(
    file_id: String,
    metadata: crate::file::FileMetadataUpdate,
    state: State<'_, MisaAppState>
) -> Result<(), String> {
    state.file_manager.update_file_metadata(file_id, metadata).await
        .map_err(|e| e.to_string())
}

/// Share file
#[tauri::command]
pub async fn share_file(
    file_id: String,
    share_params: crate::file::ShareParams,
    state: State<'_, MisaAppState>
) -> Result<String, String> { // Returns share ID
    let share_id = state.file_manager.share_file(file_id, share_params).await
        .map_err(|e| e.to_string())?;
    Ok(share_id)
}

// =============================================================================
// FOCUS COMMANDS
// =============================================================================

/// Start focus session
#[tauri::command]
pub async fn start_focus_session(
    params: FocusSessionParams,
    state: State<'_, MisaAppState>
) -> Result<String, String> { // Returns session ID
    let session_id = state.focus_manager.start_session(params).await
        .map_err(|e| e.to_string())?;
    Ok(session_id)
}

/// Stop focus session
#[tauri::command]
pub async fn stop_focus_session(
    session_id: String,
    state: State<'_, MisaAppState>
) -> Result<(), String> {
    state.focus_manager.stop_session(session_id).await
        .map_err(|e| e.to_string())
}

/// Pause focus session
#[tauri::command]
pub async fn pause_focus_session(
    session_id: String,
    state: State<'_, MisaAppState>
) -> Result<(), String> {
    state.focus_manager.pause_session(session_id).await
        .map_err(|e| e.to_string())
}

/// Resume focus session
#[tauri::command]
pub async fn resume_focus_session(
    session_id: String,
    state: State<'_, MisaAppState>
) -> Result<(), String> {
    state.focus_manager.resume_session(session_id).await
        .map_err(|e| e.to_string())
}

/// Get current focus session
#[tauri::command]
pub async fn get_current_focus_session(
    state: State<'_, MisaAppState>
) -> Result<Option<FocusSession>, String> {
    state.focus_manager.get_current_session().await
        .map_err(|e| e.to_string())
}

/// Get focus session by ID
#[tauri::command]
pub async fn get_focus_session(
    session_id: String,
    state: State<'_, MisaAppState>
) -> Result<Option<FocusSession>, String> {
    state.focus_manager.get_session(session_id).await
        .map_err(|e| e.to_string())
}

/// Get focus statistics
#[tauri::command]
pub async fn get_focus_stats(
    period: Option<String>, // "day", "week", "month", "year"
    state: State<'_, MisaAppState>
) -> Result<FocusStats, String> {
    let period = period.unwrap_or_else(|| "week".to_string());
    state.focus_manager.get_stats(&period).await
        .map_err(|e| e.to_string())
}

/// Get focus session history
#[tauri::command]
pub async fn get_focus_session_history(
    limit: Option<u32>,
    offset: Option<u32>,
    state: State<'_, MisaAppState>
) -> Result<Vec<FocusSession>, String> {
    let limit = limit.unwrap_or(50);
    let offset = offset.unwrap_or(0);
    state.focus_manager.get_session_history(limit, offset).await
        .map_err(|e| e.to_string())
}

/// Add interruption to focus session
#[tauri::command]
pub async fn add_focus_interruption(
    session_id: String,
    interruption_type: String,
    reason: String,
    state: State<'_, MisaAppState>
) -> Result<(), String> {
    let interruption_type = interruption_type.parse()
        .map_err(|e| format!("Invalid interruption type: {}", e))?;
    state.focus_manager.add_interruption(session_id, interruption_type, reason).await
        .map_err(|e| e.to_string())
}

/// Update focus session settings
#[tauri::command]
pub async fn update_focus_settings(
    session_id: String,
    settings: crate::focus::FocusSessionSettings,
    state: State<'_, MisaAppState>
) -> Result<(), String> {
    state.focus_manager.update_session_settings(session_id, settings).await
        .map_err(|e| e.to_string())
}

// =============================================================================
// SYSTEM COMMANDS
// =============================================================================

/// Get system information
#[tauri::command]
pub async fn get_system_info(
    state: State<'_, MisaAppState>
) -> Result<SystemInfo, String> {
    state.system_manager.get_system_info().await
        .map_err(|e| e.to_string())
}

/// Set power save mode
#[tauri::command]
pub async fn set_powersave_mode(
    enabled: bool,
    state: State<'_, MisaAppState>
) -> Result<(), String> {
    state.system_manager.set_powersave_mode(enabled).await
        .map_err(|e| e.to_string())
}

/// Show system notification
#[tauri::command]
pub async fn show_notification(
    title: String,
    body: String,
    icon: Option<String>,
    window: Window,
) -> Result<(), String> {
    crate::notification::show_notification(&window, &title, &body, icon.as_deref())
        .map_err(|e| e.to_string())
}

/// Get battery status
#[tauri::command]
pub async fn get_battery_status(
    state: State<'_, MisaAppState>
) -> Result<crate::system::BatteryStatus, String> {
    state.system_manager.get_battery_status().await
        .map_err(|e| e.to_string())
}

/// Get network status
#[tauri::command]
pub async fn get_network_status(
    state: State<'_, MisaAppState>
) -> Result<crate::system::NetworkStatus, String> {
    state.system_manager.get_network_status().await
        .map_err(|e| e.to_string())
}

/// Get running processes
#[tauri::command]
pub async fn get_running_processes(
    state: State<'_, MisaAppState>
) -> Result<Vec<crate::system::ProcessInfo>, String> {
    state.system_manager.get_running_processes().await
        .map_err(|e| e.to_string())
}

/// Kill process
#[tauri::command]
pub async fn kill_process(
    process_id: u32,
    state: State<'_, MisaAppState>
) -> Result<(), String> {
    state.system_manager.kill_process(process_id).await
        .map_err(|e| e.to_string())
}

/// Set system theme
#[tauri::command]
pub async fn set_system_theme(
    theme: String,
    window: Window,
) -> Result<(), String> {
    let theme = theme.parse()
        .map_err(|e| format!("Invalid theme: {}", e))?;
    crate::system::set_theme(&window, theme).map_err(|e| e.to_string())
}

// =============================================================================
// AI COMMANDS
// =============================================================================

/// Process natural language request
#[tauri::command]
pub async fn process_natural_language(
    request: AIRequest,
    state: State<'_, MisaAppState>
) -> Result<AIResponse, String> {
    state.ai_manager.process_request(request).await
        .map_err(|e| e.to_string())
}

/// Get AI recommendations
#[tauri::command]
pub async fn get_ai_recommendations(
    recommendation_type: AIRecommendationType,
    context: Option<serde_json::Value>,
    state: State<'_, MisaAppState>
) -> Result<Vec<crate::ai::AIRecommendation>, String> {
    state.ai_manager.get_recommendations(recommendation_type, context).await
        .map_err(|e| e.to_string())
}

/// Generate summary
#[tauri::command]
pub async fn generate_summary(
    content: String,
    content_type: String,
    state: State<'_, MisaAppState>
) -> Result<String, String> {
    state.ai_manager.generate_summary(content, content_type).await
        .map_err(|e| e.to_string())
}

/// Analyze sentiment
#[tauri::command]
pub async fn analyze_sentiment(
    text: String,
    state: State<'_, MisaAppState>
) -> Result<crate::ai::SentimentAnalysis, String> {
    state.ai_manager.analyze_sentiment(text).await
        .map_err(|e| e.to_string())
}

/// Extract entities from text
#[tauri::command]
pub async fn extract_entities(
    text: String,
    state: State<'_, MisaAppState>
) -> Result<Vec<crate::ai::Entity>, String> {
    state.ai_manager.extract_entities(text).await
        .map_err(|e| e.to_string())
}

/// Generate task suggestions
#[tauri::command]
pub async fn generate_task_suggestions(
    input: String,
    state: State<'_, MisaAppState>
) -> Result<Vec<crate::ai::TaskSuggestion>, String> {
    state.ai_manager.generate_task_suggestions(input).await
        .map_err(|e| e.to_string())
}

/// Get productivity insights
#[tauri::command]
pub async fn get_productivity_insights(
    period: Option<String>,
    state: State<'_, MisaAppState>
) -> Result<crate::ai::ProductivityInsights, String> {
    let period = period.unwrap_or_else(|| "week".to_string());
    state.ai_manager.get_productivity_insights(&period).await
        .map_err(|e| e.to_string())
}

// =============================================================================
// EVENT COMMANDS
// =============================================================================

/// Subscribe to application events
#[tauri::command]
pub async fn subscribe_to_events(
    event_types: Vec<String>,
    window: Window,
    state: State<'_, MisaAppState>
) -> Result<(), String> {
    let mut receiver = state.subscribe_events();
    let window_clone = window.clone();

    tokio::spawn(async move {
        while let Ok(event) = receiver.recv().await {
            if should_send_event(&event, &event_types) {
                let event_json = serde_json::to_string(&event).unwrap_or_default();
                if let Err(e) = window_clone.emit("app-event", &event_json) {
                    log::error!("Failed to emit event to window: {}", e);
                    break;
                }
            }
        }
    });

    Ok(())
}

/// Check if event should be sent based on subscription
fn should_send_event(event: &crate::AppEvent, event_types: &[String]) -> bool {
    if event_types.is_empty() {
        return true;
    }

    let event_type = match event {
        crate::AppEvent::DeviceConnected(_) => "device.connected",
        crate::AppEvent::DeviceDisconnected(_) => "device.disconnected",
        crate::AppEvent::DeviceMessageReceived { .. } => "device.message",
        crate::AppEvent::FileUploaded(_) => "file.uploaded",
        crate::AppEvent::FileDownloaded(_) => "file.downloaded",
        crate::AppEvent::FileSyncCompleted { .. } => "file.sync_completed",
        crate::AppEvent::FocusSessionStarted(_) => "focus.session_started",
        crate::AppEvent::FocusSessionCompleted(_) => "focus.session_completed",
        crate::AppEvent::FocusSessionInterrupted(_) => "focus.session_interrupted",
        crate::AppEvent::ScreenCaptured(_) => "vision.screen_captured",
        crate::AppEvent::UIElementsDetected { .. } => "vision.ui_elements_detected",
        crate::AppEvent::TextExtracted { .. } => "vision.text_extracted",
        crate::AppEvent::AIResponseReceived { .. } => "ai.response_received",
        crate::AppEvent::AISummaryGenerated { .. } => "ai.summary_generated",
        crate::AppEvent::ConfigUpdated => "config.updated",
        crate::AppEvent::SettingsChanged(_) => "config.settings_changed",
        crate::AppEvent::AppReady => "app.ready",
        crate::AppEvent::AppShutdown => "app.shutdown",
        crate::AppEvent::ErrorOccurred(_) => "app.error",
        _ => return false,
    };

    event_types.contains(&event_type.to_string())
}