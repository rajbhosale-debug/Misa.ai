//! MISA.AI Desktop Application Library
//! Core functionality and modules for the desktop application

pub mod app;
pub mod commands;
pub mod config;
pub mod core;
pub mod device;
pub mod file;
pub mod focus;
pub mod notification;
pub mod system;
pub mod tray;
pub mod vision;
pub mod ai;

use std::sync::Arc;
use anyhow::Result;
use parking_lot::RwLock;
use tokio::sync::broadcast;

// Re-export main components
pub use app::MisaApp;
pub use config::{Config, ConfigManager};
pub use device::DeviceManager;
pub use file::FileManager;
pub use focus::FocusManager;
pub use notification::NotificationManager;
pub use system::SystemManager;
pub use vision::VisionManager;
pub use ai::AIManager;

/// Application state shared across Tauri commands
pub struct MisaAppState {
    pub config_manager: Arc<RwLock<ConfigManager>>,
    pub device_manager: Arc<DeviceManager>,
    pub file_manager: Arc<FileManager>,
    pub focus_manager: Arc<FocusManager>,
    pub notification_manager: Arc<NotificationManager>,
    pub system_manager: Arc<SystemManager>,
    pub vision_manager: Arc<VisionManager>,
    pub ai_manager: Arc<AIManager>,
    pub event_bus: broadcast::Sender<AppEvent>,
}

impl MisaAppState {
    /// Create new application state
    pub async fn new() -> Result<Self> {
        let config_manager = Arc::new(RwLock::new(ConfigManager::new().await?));
        let device_manager = Arc::new(DeviceManager::new().await?);
        let file_manager = Arc::new(FileManager::new().await?);
        let focus_manager = Arc::new(FocusManager::new().await?);
        let notification_manager = Arc::new(NotificationManager::new().await?);
        let system_manager = Arc::new(SystemManager::new().await?);
        let vision_manager = Arc::new(VisionManager::new().await?);
        let ai_manager = Arc::new(AIManager::new().await?);

        let (event_tx, _) = broadcast::channel(1000);

        Ok(Self {
            config_manager,
            device_manager,
            file_manager,
            focus_manager,
            notification_manager,
            system_manager,
            vision_manager,
            ai_manager,
            event_bus: event_tx,
        })
    }

    /// Get configuration
    pub fn get_config(&self) -> Config {
        self.config_manager.read().get_config()
    }

    /// Update configuration
    pub async fn update_config(&self, config: Config) -> Result<()> {
        self.config_manager.write().update_config(config).await
    }

    /// Emit event to all subscribers
    pub fn emit_event(&self, event: AppEvent) -> Result<()> {
        match self.event_bus.send(event) {
            Ok(_) => Ok(()),
            Err(e) => Err(anyhow::anyhow!("Failed to emit event: {}", e)),
        }
    }

    /// Subscribe to events
    pub fn subscribe_events(&self) -> broadcast::Receiver<AppEvent> {
        self.event_bus.subscribe()
    }
}

/// Application events
#[derive(Debug, Clone)]
pub enum AppEvent {
    // Device events
    DeviceConnected(String),
    DeviceDisconnected(String),
    DeviceMessageReceived { device_id: String, message: String },

    // File events
    FileUploaded(String),
    FileDownloaded(String),
    FileSyncCompleted { file_id: String, success: bool },

    // Focus events
    FocusSessionStarted(String),
    FocusSessionCompleted(String),
    FocusSessionInterrupted(String),

    // System events
    SystemSuspend,
    SystemResume,
    LowBattery,

    // Vision events
    ScreenCaptured(String),
    UIElementsDetected { capture_id: String, count: usize },
    TextExtracted { capture_id: String, text: String },

    // AI events
    AIResponseReceived { request_id: String, response: String },
    AISummaryGenerated { content_id: String, summary: String },

    // Configuration events
    ConfigUpdated,
    SettingsChanged(String),

    // Application events
    AppReady,
    AppShutdown,
    ErrorOccurred(String),
}

/// Application information
#[derive(Debug, Clone, serde::Serialize)]
pub struct AppInfo {
    pub name: String,
    pub version: String,
    pub description: String,
    pub authors: Vec<String>,
    pub license: String,
    pub repository: String,
    pub build_date: String,
}

impl Default for AppInfo {
    fn default() -> Self {
        Self {
            name: env!("CARGO_PKG_NAME").to_string(),
            version: env!("CARGO_PKG_VERSION").to_string(),
            description: env!("CARGO_PKG_DESCRIPTION").to_string(),
            authors: env!("CARGO_PKG_AUTHORS").split(':').map(|s| s.trim().to_string()).collect(),
            license: env!("CARGO_PKG_LICENSE").to_string(),
            repository: env!("CARGO_PKG_REPOSITORY").to_string(),
            build_date: option_env!("BUILD_DATE").unwrap_or("unknown").to_string(),
        }
    }
}

/// Application result type
pub type AppResult<T> = Result<T, AppError>;

/// Application errors
#[derive(Debug, thiserror::Error)]
pub enum AppError {
    #[error("Configuration error: {0}")]
    Config(String),

    #[error("Device error: {0}")]
    Device(String),

    #[error("File error: {0}")]
    File(String),

    #[error("Focus error: {0}")]
    Focus(String),

    #[error("Vision error: {0}")]
    Vision(String),

    #[error("AI error: {0}")]
    AI(String),

    #[error("System error: {0}")]
    System(String),

    #[error("Network error: {0}")]
    Network(String),

    #[error("Database error: {0}")]
    Database(String),

    #[error("IO error: {0}")]
    IO(#[from] std::io::Error),

    #[error("Serialization error: {0}")]
    Serialization(#[from] serde_json::Error),

    #[error("Tauri error: {0}")]
    Tauri(#[from] tauri::Error),

    #[error("Internal error: {0}")]
    Internal(String),
}

impl From<AppError> for String {
    fn from(error: AppError) -> Self {
        error.to_string()
    }
}

/// Module initialization
pub async fn initialize_modules() -> AppResult<()> {
    // Initialize logging
    env_logger::init();

    // Initialize database
    crate::database::initialize().await?;

    // Initialize each module
    DeviceManager::initialize().await?;
    FileManager::initialize().await?;
    FocusManager::initialize().await?;
    VisionManager::initialize().await?;
    AIManager::initialize().await?;

    log::info!("All modules initialized successfully");
    Ok(())
}

/// Database module
pub mod database {
    use sqlx::{Pool, Sqlite, SqlitePool};
    use std::sync::Arc;
    use parking_lot::RwLock;
    use anyhow::Result;

    static DB_POOL: std::sync::OnceLock<Arc<RwLock<SqlitePool>>> = std::sync::OnceLock::new();

    /// Initialize database
    pub async fn initialize() -> Result<()> {
        let pool = SqlitePool::connect("sqlite:misa_desktop.db").await?;

        // Run migrations
        sqlx::migrate!("./migrations").run(&pool).await?;

        DB_POOL.set(Arc::new(RwLock::new(pool)))
            .expect("Failed to set database pool");

        log::info!("Database initialized successfully");
        Ok(())
    }

    /// Get database pool
    pub fn get_pool() -> Option<Arc<RwLock<SqlitePool>>> {
        DB_POOL.get().cloned()
    }

    /// Execute database operation
    pub async fn execute<F, R>(operation: F) -> Result<R>
    where
        F: FnOnce(&SqlitePool) -> std::pin::Pin<Box<dyn std::future::Future<Output = Result<R>> + Send>>,
    {
        let pool = DB_POOL.get()
            .ok_or_else(|| anyhow::anyhow!("Database not initialized"))?;

        let pool_guard = pool.read();
        operation(&*pool_guard).await
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use tokio_test;

    #[tokio::test]
    async fn test_app_state_creation() {
        let state = MisaAppState::new().await;
        assert!(state.is_ok());
    }

    #[tokio::test]
    async fn test_app_info_default() {
        let info = AppInfo::default();
        assert_eq!(info.name, "misa-desktop");
        assert!(!info.version.is_empty());
    }

    #[tokio::test]
    async fn test_event_bus() {
        let state = MisaAppState::new().await.unwrap();
        let event = AppEvent::AppReady;

        // Emit event
        let result = state.emit_event(event.clone());
        assert!(result.is_ok());

        // Subscribe and receive event
        let mut receiver = state.subscribe_events();
        let received_event = tokio::time::timeout(
            std::time::Duration::from_millis(100),
            receiver.recv()
        ).await;

        assert!(received_event.is_ok());
        match received_event.unwrap().unwrap() {
            AppEvent::AppReady => {} // Expected
            _ => panic!("Unexpected event type"),
        }
    }
}