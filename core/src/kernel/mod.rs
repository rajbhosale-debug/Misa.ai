//! MISA Kernel - Central Orchestration Layer
//!
//! The kernel is the heart of MISA.AI, responsible for:
//! - Model selection and task routing
//! - Device communication and coordination
//! - Context management and memory routing
//! - Permission enforcement and security
//! - Plugin lifecycle management
//! - Real-time API endpoints

use anyhow::Result;
use axum::{
    extract::{ws::WebSocketUpgrade, State},
    http::StatusCode,
    response::{IntoResponse, Response},
    routing::{get, post},
    Json, Router,
};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::net::SocketAddr;
use std::sync::Arc;
use tokio::sync::RwLock;
use tracing::{info, warn, error};

use crate::models::{ModelManager, ModelType, ModelCapabilities};
use crate::security::SecurityManager;
use crate::device::DeviceManager;
use crate::memory::MemoryManager;
use crate::privacy::PrivacyControls;
use crate::errors::{MisaError, Result as MisaResult};

/// Main kernel orchestrator
pub struct MisaKernel {
    config: KernelConfig,
    data_dir: String,
    security_manager: SecurityManager,
    model_manager: ModelManager,
    device_manager: DeviceManager,
    memory_manager: MemoryManager,
    privacy_controls: PrivacyControls,
    active_plugins: Arc<RwLock<HashMap<String, PluginInstance>>>,
}

/// Kernel configuration
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct KernelConfig {
    /// AI model configuration
    pub models: ModelConfig,
    /// Device management settings
    pub devices: DeviceConfig,
    /// Security and privacy settings
    pub security: SecurityConfig,
    /// Memory and context settings
    pub memory: MemoryConfig,
    /// Network and API settings
    pub network: NetworkConfig,
}

impl Default for KernelConfig {
    fn default() -> Self {
        Self {
            models: ModelConfig::default(),
            devices: DeviceConfig::default(),
            security: SecurityConfig::default(),
            memory: MemoryConfig::default(),
            network: NetworkConfig::default(),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModelConfig {
    /// Default model for general tasks
    pub default_model: String,
    /// Local model server URL (Ollama)
    pub local_server_url: String,
    /// Cloud API configurations
    pub cloud_providers: HashMap<String, CloudProviderConfig>,
    /// Model switching preferences
    pub switching_preferences: ModelSwitchingPreferences,
}

impl Default for ModelConfig {
    fn default() -> Self {
        let mut cloud_providers = HashMap::new();
        cloud_providers.insert("openai".to_string(), CloudProviderConfig {
            api_key: std::env::var("OPENAI_API_KEY").unwrap_or_default(),
            base_url: "https://api.openai.com/v1".to_string(),
            models: vec!["gpt-4".to_string(), "gpt-3.5-turbo".to_string()],
        });

        Self {
            default_model: "mixtral".to_string(),
            local_server_url: "http://localhost:11434".to_string(),
            cloud_providers,
            switching_preferences: ModelSwitchingPreferences::default(),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CloudProviderConfig {
    pub api_key: String,
    pub base_url: String,
    pub models: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModelSwitchingPreferences {
    /// Prefer local models when available
    pub prefer_local: bool,
    /// GPU acceleration threshold
    pub gpu_threshold: f32,
    /// Cost optimization level (0.0 - 1.0)
    pub cost_optimization: f32,
    /// Quality optimization level (0.0 - 1.0)
    pub quality_optimization: f32,
}

impl Default for ModelSwitchingPreferences {
    fn default() -> Self {
        Self {
            prefer_local: true,
            gpu_threshold: 0.7,
            cost_optimization: 0.6,
            quality_optimization: 0.8,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DeviceConfig {
    /// Enable device discovery
    pub discovery_enabled: bool,
    /// Remote desktop enabled
    pub remote_desktop_enabled: bool,
    /// File transfer settings
    pub file_transfer: FileTransferConfig,
    /// Energy management
    pub energy_management: EnergyConfig,
}

impl Default for DeviceConfig {
    fn default() -> Self {
        Self {
            discovery_enabled: true,
            remote_desktop_enabled: true,
            file_transfer: FileTransferConfig::default(),
            energy_management: EnergyConfig::default(),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileTransferConfig {
    /// Maximum file size (MB)
    pub max_file_size_mb: u64,
    /// Allowed file types
    pub allowed_types: Vec<String>,
    /// Encryption required
    pub encryption_required: bool,
}

impl Default for FileTransferConfig {
    fn default() -> Self {
        Self {
            max_file_size_mb: 1024,
            allowed_types: vec!["*".to_string()], // All types allowed
            encryption_required: true,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EnergyConfig {
    /// Battery-saving mode
    pub battery_saver_enabled: bool,
    /// Performance throttling on battery
    pub throttle_on_battery: bool,
    /// Auto-switch to cloud on low battery
    pub cloud_fallback_battery: f32, // percentage
}

impl Default for EnergyConfig {
    fn default() -> Self {
        Self {
            battery_saver_enabled: true,
            throttle_on_battery: true,
            cloud_fallback_battery: 20.0,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SecurityConfig {
    /// Authentication required
    pub auth_required: bool,
    /// Biometric authentication
    pub biometric_auth: bool,
    /// Session timeout (minutes)
    pub session_timeout_minutes: u64,
    /// Plugin sandboxing
    pub plugin_sandboxing: bool,
    /// Audit logging
    pub audit_logging: bool,
}

impl Default for SecurityConfig {
    fn default() -> Self {
        Self {
            auth_required: true,
            biometric_auth: true,
            session_timeout_minutes: 30,
            plugin_sandboxing: true,
            audit_logging: true,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MemoryConfig {
    /// Local database path
    pub local_db_path: String,
    /// Memory retention days
    pub retention_days: u32,
    /// Compression enabled
    pub compression_enabled: bool,
    /// Encryption enabled
    pub encryption_enabled: bool,
}

impl Default for MemoryConfig {
    fn default() -> Self {
        Self {
            local_db_path: "misa_memory.db".to_string(),
            retention_days: 365,
            compression_enabled: true,
            encryption_enabled: true,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NetworkConfig {
    /// WebSocket port
    pub websocket_port: u16,
    /// gRPC port
    pub grpc_port: u16,
    /// TLS enabled
    pub tls_enabled: bool,
    /// Certificate path
    pub cert_path: Option<String>,
    /// Private key path
    pub key_path: Option<String>,
}

impl Default for NetworkConfig {
    fn default() -> Self {
        Self {
            websocket_port: 8080,
            grpc_port: 50051,
            tls_enabled: false,
            cert_path: None,
            key_path: None,
        }
    }
}

/// Plugin instance information
#[derive(Debug, Clone)]
pub struct PluginInstance {
    pub id: String,
    pub name: String,
    pub version: String,
    pub capabilities: Vec<String>,
    pub permissions: Vec<String>,
    pub pid: Option<u32>,
    pub status: PluginStatus,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum PluginStatus {
    Running,
    Stopped,
    Error(String),
    Starting,
    Stopping,
}

/// API request structures
#[derive(Debug, Deserialize)]
pub struct SwitchModelRequest {
    pub model_id: String,
    pub task_type: Option<String>,
    pub preferences: Option<ModelSwitchingPreferences>,
}

#[derive(Debug, Deserialize)]
pub struct RouteTaskRequest {
    pub task: String,
    pub task_type: String,
    pub context: Option<serde_json::Value>,
    pub device_preferences: Option<Vec<String>>,
    pub priority: Option<TaskPriority>,
}

#[derive(Debug, Serialize, Deserialize)]
pub enum TaskPriority {
    Low,
    Normal,
    High,
    Critical,
}

impl Default for TaskPriority {
    fn default() -> Self {
        Self::Normal
    }
}

#[derive(Debug, Serialize)]
pub struct TaskResponse {
    pub success: bool,
    pub task_id: String,
    pub assigned_device: Option<String>,
    pub assigned_model: String,
    pub estimated_duration: Option<u64>,
    pub result: Option<serde_json::Value>,
    pub error: Option<String>,
}

impl MisaKernel {
    /// Create a new kernel instance
    pub async fn new(
        config_path: String,
        data_dir: String,
        security_manager: SecurityManager,
    ) -> MisaResult<Self> {
        // Load configuration
        let config = Self::load_config(&config_path).unwrap_or_default();

        // Initialize managers
        let model_manager = ModelManager::new(config.models.clone()).await?;
        let device_manager = DeviceManager::new(config.devices.clone()).await?;
        let memory_manager = MemoryManager::new(&data_dir, config.memory.clone()).await?;
        let privacy_controls = PrivacyControls::new(config.security.clone()).await?;

        info!("MISA Kernel initialized successfully");

        Ok(Self {
            config,
            data_dir,
            security_manager,
            model_manager,
            device_manager,
            memory_manager,
            privacy_controls,
            active_plugins: Arc::new(RwLock::new(HashMap::new())),
        })
    }

    /// Start the kernel service
    pub async fn start(&self, bind_addr: SocketAddr) -> MisaResult<()> {
        // Initialize subsystems
        self.model_manager.initialize().await?;
        self.device_manager.start_discovery().await?;
        self.memory_manager.initialize().await?;

        // Start API server
        let app = self.create_router();

        info!("Starting kernel API server on {}", bind_addr);

        let listener = tokio::net::TcpListener::bind(bind_addr).await
            .map_err(|e| MisaError::Network(e))?;

        axum::serve(listener, app).await
            .map_err(|e| MisaError::Network(e.into()))?;

        Ok(())
    }

    /// Shutdown the kernel gracefully
    pub async fn shutdown(&self) -> MisaResult<()> {
        info!("Shutting down kernel subsystems");

        // Stop all plugins
        let mut plugins = self.active_plugins.write().await;
        for (id, plugin) in plugins.iter_mut() {
            if let PluginStatus::Running = plugin.status {
                self.stop_plugin(id).await?;
            }
        }

        // Shutdown subsystems
        self.device_manager.shutdown().await?;
        self.memory_manager.shutdown().await?;
        self.model_manager.shutdown().await?;

        info!("Kernel shutdown complete");
        Ok(())
    }

    /// Switch to a different AI model
    pub async fn switch_model(&self, request: SwitchModelRequest) -> MisaResult<String> {
        self.model_manager.switch_model(
            &request.model_id,
            request.task_type.as_deref(),
            request.preferences.as_ref(),
        ).await
    }

    /// Route a task to appropriate model and device
    pub async fn route_task(&self, request: RouteTaskRequest) -> MisaResult<TaskResponse> {
        // Analyze task requirements
        let task_type = self.analyze_task_type(&request.task, &request.task_type);

        // Select optimal model
        let model_id = self.model_manager.select_model_for_task(
            &task_type,
            request.device_preferences.as_deref(),
            request.priority.as_ref().unwrap_or(&TaskPriority::Normal),
        ).await?;

        // Select optimal device if specified
        let assigned_device = if let Some(preferences) = &request.device_preferences {
            self.device_manager.select_device(preferences).await?
        } else {
            None
        };

        // Execute task
        let result = self.execute_task(&request.task, &model_id, request.context.as_ref()).await?;

        Ok(TaskResponse {
            success: true,
            task_id: uuid::Uuid::new_v4().to_string(),
            assigned_device,
            assigned_model: model_id,
            estimated_duration: None,
            result: Some(result),
            error: None,
        })
    }

    /// Load kernel configuration from file
    fn load_config(path: &str) -> Option<KernelConfig> {
        match std::fs::read_to_string(path) {
            Ok(content) => {
                match toml::from_str(&content) {
                    Ok(config) => Some(config),
                    Err(e) => {
                        warn!("Failed to parse config file {}: {}", path, e);
                        None
                    }
                }
            }
            Err(_) => {
                info!("Config file {} not found, using defaults", path);
                None
            }
        }
    }

    /// Create the Axum router for API endpoints
    fn create_router(&self) -> Router {
        Router::new()
            .route("/health", get(health_check))
            .route("/api/v1/kernel/switch_model", post(switch_model_handler))
            .route("/api/v1/kernel/route_task", post(route_task_handler))
            .route("/ws", get(websocket_handler))
            .with_state(Arc::new(self.clone()))
    }

    /// Analyze task type from content and hint
    fn analyze_task_type(&self, task: &str, hint: &str) -> String {
        // Simple heuristic-based task type analysis
        if task.to_lowercase().contains("code") || hint.to_lowercase().contains("coding") {
            "coding".to_string()
        } else if task.to_lowercase().contains("image") || hint.to_lowercase().contains("vision") {
            "vision".to_string()
        } else if task.to_lowercase().contains("summarize") || hint.to_lowercase().contains("summarization") {
            "summarization".to_string()
        } else {
            hint.to_string()
        }
    }

    /// Execute a task on the specified model
    async fn execute_task(&self, task: &str, model_id: &str, context: Option<&serde_json::Value>) -> MisaResult<serde_json::Value> {
        self.model_manager.execute_task(task, model_id, context).await
    }

    /// Plugin management methods
    async fn start_plugin(&self, plugin_id: &str) -> MisaResult<()> {
        info!("Starting plugin: {}", plugin_id);
        // Implementation for plugin startup
        Ok(())
    }

    async fn stop_plugin(&self, plugin_id: &str) -> MisaResult<()> {
        info!("Stopping plugin: {}", plugin_id);
        // Implementation for plugin shutdown
        Ok(())
    }
}

// Clone implementation for Axum State
impl Clone for MisaKernel {
    fn clone(&self) -> Self {
        Self {
            config: self.config.clone(),
            data_dir: self.data_dir.clone(),
            security_manager: self.security_manager.clone(),
            model_manager: self.model_manager.clone(),
            device_manager: self.device_manager.clone(),
            memory_manager: self.memory_manager.clone(),
            privacy_controls: self.privacy_controls.clone(),
            active_plugins: Arc::clone(&self.active_plugins),
        }
    }
}

// API Handlers
async fn health_check() -> impl IntoResponse {
    Json(serde_json::json!({
        "status": "healthy",
        "version": crate::VERSION,
        "timestamp": chrono::Utc::now().to_rfc3339()
    }))
}

async fn switch_model_handler(
    State(kernel): State<Arc<MisaKernel>>,
    Json(request): Json<SwitchModelRequest>,
) -> Result<impl IntoResponse, StatusCode> {
    match kernel.switch_model(request).await {
        Ok(model_id) => Ok(Json(serde_json::json!({
            "success": true,
            "model_id": model_id
        }))),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

async fn route_task_handler(
    State(kernel): State<Arc<MisaKernel>>,
    Json(request): Json<RouteTaskRequest>,
) -> Result<impl IntoResponse, StatusCode> {
    match kernel.route_task(request).await {
        Ok(response) => Ok(Json(response)),
        Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
    }
}

async fn websocket_handler(
    ws: WebSocketUpgrade,
    State(kernel): State<Arc<MisaKernel>>,
) -> impl IntoResponse {
    ws.on_upgrade(move |socket| handle_websocket(socket, kernel))
}

async fn handle_websocket(
    mut socket: axum::extract::ws::WebSocket,
    kernel: Arc<MisaKernel>,
) {
    info!("WebSocket connection established");

    while let Some(msg) = socket.recv().await {
        match msg {
            Ok(axum::extract::ws::Message::Text(text)) => {
                // Handle JSON-RPC requests
                if let Err(e) = handle_json_rpc(&text, &kernel, &mut socket).await {
                    error!("JSON-RPC error: {}", e);
                }
            }
            Ok(axum::extract::ws::Message::Close(_)) => {
                info!("WebSocket connection closed");
                break;
            }
            Err(e) => {
                error!("WebSocket error: {}", e);
                break;
            }
            _ => {}
        }
    }
}

async fn handle_json_rpc(
    text: &str,
    kernel: &MisaKernel,
    socket: &mut axum::extract::ws::WebSocket,
) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    let rpc_request: serde_json::Value = serde_json::from_str(text)?;

    let method = rpc_request["method"].as_str().ok_or("Missing method")?;
    let id = rpc_request["id"].clone();
    let params = rpc_request["params"].clone();

    let result = match method {
        "kernel.switch_model" => {
            let request: SwitchModelRequest = serde_json::from_value(params)?;
            kernel.switch_model(request).await.map(|v| serde_json::to_value(v)?)?
        }
        "kernel.route_task" => {
            let request: RouteTaskRequest = serde_json::from_value(params)?;
            kernel.route_task(request).await.map(|v| serde_json::to_value(v)?)?
        }
        _ => serde_json::json!({"error": "Unknown method"}),
    };

    let response = serde_json::json!({
        "jsonrpc": "2.0",
        "id": id,
        "result": result
    });

    let response_text = response.to_string();
    socket.send(axum::extract::ws::Message::Text(response_text)).await?;

    Ok(())
}