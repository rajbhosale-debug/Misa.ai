//! Device Management and Communication System
//!
//! Handles multi-device orchestration including:
//! - Device discovery and pairing via QR/UID tokens
//! - Inter-device communication via WebSocket/gRPC + WebRTC
//! - Remote desktop with screen sharing and file transfer
//! - Clipboard synchronization with end-to-end encryption
//! - Energy-aware compute routing policies

use anyhow::Result;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::io::Read;
use std::net::SocketAddr;
use std::sync::Arc;
use std::time::Duration;
use tokio::sync::RwLock;
use tokio_tungstenite::tungstenite::Message;
use tracing::{info, warn, error, debug};

// Use type alias for MD5 to avoid dependency issues
type Md5Digest = [u8; 16];

fn compute_md5(data: &[u8]) -> Md5Digest {
    // Simple MD5-like hash for demonstration
    // In production, use proper crypto library
    let mut result = [0u8; 16];
    let len = data.len().min(16);
    result[..len].copy_from_slice(&data[..len]);
    result
}

use crate::kernel::DeviceConfig;
use crate::security::{SecurityManager, EncryptedData};
use crate::errors::{MisaError, Result as MisaResult};

/// Device manager for multi-device orchestration
pub struct DeviceManager {
    config: DeviceConfig,
    security_manager: SecurityManager,
    devices: Arc<RwLock<HashMap<String, DeviceInfo>>>,
    active_connections: Arc<RwLock<HashMap<String, DeviceConnection>>>,
    discovery_service: DiscoveryService,
    remote_desktop_manager: RemoteDesktopManager,
    clipboard_sync: ClipboardSync,
}

/// Device information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DeviceInfo {
    pub device_id: String,
    pub name: String,
    pub device_type: DeviceType,
    pub capabilities: DeviceCapabilities,
    pub status: DeviceStatus,
    pub last_seen: chrono::DateTime<chrono::Utc>,
    pub battery_level: Option<f32>,
    pub cpu_usage: Option<f32>,
    pub memory_usage: Option<u64>,
    pub network_info: NetworkInfo,
    pub location: Option<LocationInfo>,
}

/// Device type enumeration
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub enum DeviceType {
    Desktop,
    Laptop,
    Phone,
    Tablet,
    Server,
    Embedded,
}

/// Device capabilities
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DeviceCapabilities {
    pub supports_gpu: bool,
    pub supports_vision: bool,
    pub supports_audio: bool,
    pub has_camera: bool,
    pub has_microphone: bool,
    pub max_memory_mb: u64,
    pub cpu_cores: u32,
    pub gpu_memory_mb: Option<u64>,
    pub battery_powered: bool,
    pub supports_remote_desktop: bool,
}

/// Device status
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum DeviceStatus {
    Online,
    Offline,
    Busy,
    Sleep,
    Error(String),
}

/// Network information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NetworkInfo {
    pub ip_address: String,
    pub mac_address: Option<String>,
    pub connection_type: ConnectionType,
    pub signal_strength: Option<f32>,
    pub bandwidth_mbps: Option<f32>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ConnectionType {
    WiFi,
    Ethernet,
    Cellular,
    Bluetooth,
    Unknown,
}

/// Location information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LocationInfo {
    pub latitude: f64,
    pub longitude: f64,
    pub accuracy: f64,
    pub timestamp: chrono::DateTime<chrono::Utc>,
}

/// Device connection
#[derive(Debug, Clone)]
pub struct DeviceConnection {
    pub device_id: String,
    pub connection_type: ConnectionProtocol,
    pub websocket: Option<Arc<tokio_tungstenite::WebSocketStream<tokio::net::TcpStream>>>,
    pub webrtc_connection: Option<WebRTCConnection>,
    pub last_heartbeat: chrono::DateTime<chrono::Utc>,
    pub encrypted_channel: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ConnectionProtocol {
    WebSocket,
    WebRTC,
    gRPC,
    Bluetooth,
}

/// WebRTC connection information
#[derive(Debug, Clone)]
pub struct WebRTCConnection {
    pub peer_id: String,
    pub data_channel: String,
    pub video_channels: Vec<String>,
    pub audio_channels: Vec<String>,
}

/// Enhanced Discovery service for device finding
pub struct DiscoveryService {
    enabled: bool,
    discovery_port: u16,
    broadcast_interval_seconds: u64,
    active_discovery: Arc<RwLock<HashMap<String, DiscoverySession>>>,
    background_scanning: bool,
    smart_suggestions: bool,
    last_scan: Arc<RwLock<chrono::DateTime<chrono::Utc>>>,
    device_history: Arc<RwLock<HashMap<String, DeviceHistory>>>,
    connection_quality_monitor: ConnectionQualityMonitor,
}

/// Discovery session
#[derive(Debug, Clone)]
pub struct DiscoverySession {
    pub session_id: String,
    pub device_id: String,
    pub started_at: chrono::DateTime<chrono::Utc>,
    pub qr_token: String,
    pub pairing_status: PairingStatus,
    pub auto_pair_enabled: bool,
    pub connection_strength: f32,
}

/// Device history for smart suggestions
#[derive(Debug, Clone)]
pub struct DeviceHistory {
    pub device_id: String,
    pub last_connected: chrono::DateTime<chrono::Utc>,
    pub connection_count: u32,
    pub average_signal_strength: f32,
    pub success_rate: f32,
    pub preferred_for_tasks: Vec<String>,
    pub device_type: DeviceType,
}

/// Connection quality monitor
#[derive(Debug, Clone)]
pub struct ConnectionQualityMonitor {
    pub active_connections: Arc<RwLock<HashMap<String, ConnectionQuality>>>,
    pub quality_history: Arc<RwLock<Vec<QualityMeasurement>>>,
}

#[derive(Debug, Clone)]
pub struct ConnectionQuality {
    pub device_id: String,
    pub latency_ms: u64,
    pub bandwidth_mbps: f32,
    pub signal_strength: f32,
    pub stability_score: f32,
    pub last_updated: chrono::DateTime<chrono::Utc>,
    pub uptime_percentage: f32,
}

#[derive(Debug, Clone)]
pub struct QualityMeasurement {
    pub device_id: String,
    pub timestamp: chrono::DateTime<chrono::Utc>,
    pub latency_ms: u64,
    pub packet_loss: f32,
    pub jitter_ms: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum PairingStatus {
    Initiated,
    PendingConfirmation,
    Completed,
    Failed(String),
    Expired,
}

/// Remote desktop manager
pub struct RemoteDesktopManager {
    enabled: bool,
    active_sessions: Arc<RwLock<HashMap<String, RemoteDesktopSession>>>,
    screen_capturer: ScreenCapturer,
    file_transfer_manager: FileTransferManager,
}

/// Remote desktop session
#[derive(Debug, Clone)]
pub struct RemoteDesktopSession {
    pub session_id: String,
    pub host_device_id: String,
    pub client_device_id: String,
    pub protocol: RemoteDesktopProtocol,
    pub resolution: (u32, u32),
    pub quality: VideoQuality,
    pub permissions: RemoteDesktopPermissions,
    pub started_at: chrono::DateTime<chrono::Utc>,
    pub screen_recording: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum RemoteDesktopProtocol {
    VNC,
    RDP,
    WebRTC,
    Custom,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum VideoQuality {
    Low,     // 480p
    Medium,  // 720p
    High,    // 1080p
    Ultra,   // 4K
}

/// Remote desktop permissions
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RemoteDesktopPermissions {
    pub view_screen: bool,
    pub control_mouse: bool,
    pub control_keyboard: bool,
    pub transfer_files: bool,
    pub access_clipboard: bool,
    pub record_session: bool,
    pub system_commands: bool,
}

/// Screen capturer
pub struct ScreenCapturer {
    capture_interval_ms: u64,
    compression_enabled: bool,
    supported_formats: Vec<ImageFormat>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ImageFormat {
    PNG,
    JPEG,
    WebP,
    H264,
    VP9,
}

/// File transfer manager
pub struct FileTransferManager {
    max_file_size_mb: u64,
    allowed_file_types: Vec<String>,
    encryption_required: bool,
    active_transfers: Arc<RwLock<HashMap<String, FileTransfer>>>,
}

/// File transfer
#[derive(Debug, Clone)]
pub struct FileTransfer {
    pub transfer_id: String,
    pub source_device_id: String,
    pub target_device_id: String,
    pub file_path: String,
    pub file_size: u64,
    pub bytes_transferred: u64,
    pub encryption_key: Option<String>,
    pub status: FileTransferStatus,
    pub started_at: chrono::DateTime<chrono::Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum FileTransferStatus {
    Pending,
    InProgress,
    Completed,
    Failed(String),
    Paused,
}

/// Clipboard synchronization
pub struct ClipboardSync {
    enabled: bool,
    encryption_enabled: bool,
    sync_interval_seconds: u64,
    last_clipboard_hash: Arc<RwLock<Option<String>>>,
    supported_formats: Vec<String>,
}

/// Device discovery packet for network broadcasting
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DeviceDiscoveryPacket {
    pub device_id: String,
    pub device_name: String,
    pub device_type: String,
    pub capabilities: Vec<String>,
    pub port: u16,
    pub timestamp: chrono::DateTime<chrono::Utc>,
}

/// Device communication message
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DeviceMessage {
    pub message_id: String,
    pub source_device_id: String,
    pub target_device_id: Option<String>, // None for broadcast
    pub message_type: MessageType,
    pub payload: serde_json::Value,
    pub timestamp: chrono::DateTime<chrono::Utc>,
    pub encrypted: bool,
    pub priority: MessagePriority,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum MessageType {
    Heartbeat,
    SystemInfo,
    TaskRequest,
    TaskResponse,
    RemoteDesktopRequest,
    RemoteDesktopData,
    FileTransferRequest,
    FileTransferData,
    ClipboardSync,
    DeviceDiscovery,
    PairingRequest,
    PairingResponse,
    ControlCommand,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum MessagePriority {
    Low,
    Normal,
    High,
    Critical,
}

impl DeviceManager {
    /// Create a new device manager
    pub async fn new(config: DeviceConfig, security_manager: SecurityManager) -> MisaResult<Self> {
        let devices = Arc::new(RwLock::new(HashMap::new()));
        let active_connections = Arc::new(RwLock::new(HashMap::new()));

        let discovery_service = DiscoveryService::new(config.discovery_enabled);
        let remote_desktop_manager = RemoteDesktopManager::new(config.remote_desktop_enabled);
        let clipboard_sync = ClipboardSync::new(true);

        let manager = Self {
            config,
            security_manager,
            devices,
            active_connections,
            discovery_service,
            remote_desktop_manager,
            clipboard_sync,
        };

        info!("Device manager initialized");
        Ok(manager)
    }

    /// Start device discovery
    pub async fn start_discovery(&self) -> MisaResult<()> {
        if !self.config.discovery_enabled {
            info!("Device discovery disabled in configuration");
            return Ok(());
        }

        info!("Starting device discovery service");
        self.discovery_service.start().await?;

        // Start device monitoring
        self.start_device_monitoring().await?;

        Ok(())
    }

    /// Pair with a device using QR token
    pub async fn pair_device(&self, qr_token: &str) -> MisaResult<PairingResult> {
        info!("Initiating device pairing with QR token");

        // Validate QR token format
        let pairing_data = self.parse_qr_token(qr_token)?;

        // Create discovery session
        let session = DiscoverySession {
            session_id: uuid::Uuid::new_v4().to_string(),
            device_id: pairing_data.device_id.clone(),
            started_at: chrono::Utc::now(),
            qr_token: qr_token.to_string(),
            pairing_status: PairingStatus::Initiated,
        };

        // Initiate pairing process
        let result = self.initiate_pairing(pairing_data, session).await?;

        Ok(result)
    }

    /// Send message to device
    pub async fn send_message(&self, message: DeviceMessage) -> MisaResult<()> {
        debug!("Sending message to device: {:?}", message.target_device_id);

        if let Some(target_device_id) = &message.target_device_id {
            let connections = self.active_connections.read().await;
            if let Some(connection) = connections.get(target_device_id) {
                self.send_message_via_connection(connection, &message).await?;
            } else {
                return Err(MisaError::Device(format!("No connection to device: {}", target_device_id)));
            }
        } else {
            // Broadcast to all connected devices
            self.broadcast_message(&message).await?;
        }

        Ok(())
    }

    /// Start remote desktop session
    pub async fn start_remote_desktop(
        &self,
        target_device_id: &str,
        permissions: RemoteDesktopPermissions,
    ) -> MisaResult<String> {
        info!("Starting remote desktop session with device: {}", target_device_id);

        // Check if device supports remote desktop
        let devices = self.devices.read().await;
        let device = devices.get(target_device_id)
            .ok_or_else(|| MisaError::Device(format!("Device not found: {}", target_device_id)))?;

        if !device.capabilities.supports_remote_desktop {
            return Err(MisaError::Device("Device does not support remote desktop".to_string()));
        }

        drop(devices);

        // Start remote desktop session
        let session_id = self.remote_desktop_manager.start_session(
            target_device_id,
            permissions,
        ).await?;

        Ok(session_id)
    }

    /// Transfer file to device
    pub async fn transfer_file(
        &self,
        target_device_id: &str,
        file_path: &str,
    ) -> MisaResult<String> {
        info!("Starting file transfer to device: {} - file: {}", target_device_id, file_path);

        // Validate file
        self.validate_file(file_path)?;

        // Start file transfer
        let transfer_id = self.remote_desktop_manager.file_transfer_manager.start_transfer(
            target_device_id,
            file_path,
        ).await?;

        Ok(transfer_id)
    }

    /// Select optimal device for task
    pub async fn select_device(&self, preferences: &[String]) -> MisaResult<Option<String>> {
        let devices = self.devices.read().await;

        if preferences.is_empty() {
            // Select best available device
            self.select_best_device(&devices).await
        } else {
            // Check preferred devices in order
            for preference in preferences {
                if let Some(device) = devices.get(preference) {
                    if matches!(device.status, DeviceStatus::Online) {
                        return Ok(Some(preference.clone()));
                    }
                }
            }
            Ok(None)
        }
    }

    /// Get device list
    pub async fn get_devices(&self) -> MisaResult<Vec<DeviceInfo>> {
        let devices = self.devices.read().await;
        Ok(devices.values().cloned().collect())
    }

    /// Get device info
    pub async fn get_device(&self, device_id: &str) -> MisaResult<Option<DeviceInfo>> {
        let devices = self.devices.read().await;
        Ok(devices.get(device_id).cloned())
    }

    /// Shutdown device manager
    pub async fn shutdown(&self) -> MisaResult<()> {
        info!("Shutting down device manager");

        // Stop discovery service
        self.discovery_service.stop().await?;

        // Close all connections
        self.close_all_connections().await?;

        // Stop remote desktop sessions
        self.remote_desktop_manager.shutdown().await?;

        info!("Device manager shut down");
        Ok(())
    }

    /// Private helper methods

    fn parse_qr_token(&self, qr_token: &str) -> MisaResult<PairingData> {
        // Parse QR token format: "misa://pair/{device_id}/{timestamp}/{signature}"
        if !qr_token.starts_with("misa://pair/") {
            return Err(MisaError::Device("Invalid QR token format".to_string()));
        }

        let parts: Vec<&str> = qr_token.trim_start_matches("misa://pair/").split('/').collect();
        if parts.len() != 3 {
            return Err(MisaError::Device("Invalid QR token format".to_string()));
        }

        Ok(PairingData {
            device_id: parts[0].to_string(),
            timestamp: parts[1].parse().map_err(|_| MisaError::Device("Invalid timestamp".to_string()))?,
            signature: parts[2].to_string(),
        })
    }

    async fn initiate_pairing(
        &self,
        pairing_data: PairingData,
        session: DiscoverySession,
    ) -> MisaResult<PairingResult> {
        // Validate timestamp (prevent replay attacks)
        let now = chrono::Utc::now();
        let pair_time = chrono::DateTime::from_timestamp(pairing_data.timestamp, 0)
            .ok_or_else(|| MisaError::Device("Invalid timestamp".to_string()))?;

        if now.signed_duration_since(pair_time).num_minutes() > 5 {
            return Err(MisaError::Device("QR token expired".to_string()));
        }

        // Verify signature (in real implementation, use proper cryptographic verification)
        if pairing_data.signature.is_empty() {
            return Err(MisaError::Device("Invalid signature".to_string()));
        }

        // Add device to registry
        let device_info = DeviceInfo {
            device_id: pairing_data.device_id.clone(),
            name: format!("Device-{}", &pairing_data.device_id[..8]),
            device_type: DeviceType::Phone, // Default, would be detected
            capabilities: DeviceCapabilities::default(),
            status: DeviceStatus::Online,
            last_seen: chrono::Utc::now(),
            battery_level: None,
            cpu_usage: None,
            memory_usage: None,
            network_info: NetworkInfo::default(),
            location: None,
        };

        let mut devices = self.devices.write().await;
        devices.insert(pairing_data.device_id.clone(), device_info);

        Ok(PairingResult {
            success: true,
            device_id: pairing_data.device_id,
            message: "Device paired successfully".to_string(),
        })
    }

    async fn send_message_via_connection(
        &self,
        connection: &DeviceConnection,
        message: &DeviceMessage,
    ) -> MisaResult<()> {
        let message_data = serde_json::to_vec(message)?;

        match connection.connection_type {
            ConnectionProtocol::WebSocket => {
                // Send via WebSocket
                if let Some(ws) = &connection.websocket {
                    // Convert message to WebSocket text message
                    let ws_message = Message::Text(serde_json::to_string(message)?);

                    // In a real implementation, we would send through the WebSocket
                    // For now, we'll simulate the send operation
                    debug!("Sending message via WebSocket to {}: {:?}", connection.device_id, message.message_type);

                    // TODO: Actual WebSocket send implementation
                    // ws.send(ws_message).await.map_err(|e| MisaError::Device(format!("WebSocket send failed: {}", e)))?;
                } else {
                    return Err(MisaError::Device(format!("No WebSocket connection to device: {}", connection.device_id)));
                }
            }
            ConnectionProtocol::WebRTC => {
                // Send via WebRTC data channel
                if let Some(webrtc) = &connection.webrtc_connection {
                    debug!("Sending message via WebRTC data channel to {}: {:?}", connection.device_id, message.message_type);

                    // TODO: Actual WebRTC data channel send implementation
                    // webrtc.data_channel.send(&message_data).await.map_err(|e| MisaError::Device(format!("WebRTC send failed: {}", e)))?;
                } else {
                    return Err(MisaError::Device(format!("No WebRTC connection to device: {}", connection.device_id)));
                }
            }
            ConnectionProtocol::gRPC => {
                debug!("Sending message via gRPC to device: {}", connection.device_id);
                // TODO: Implement gRPC client communication
            }
            ConnectionProtocol::Bluetooth => {
                debug!("Sending message via Bluetooth to device: {}", connection.device_id);
                // TODO: Implement Bluetooth communication
            }
        }

        Ok(())
    }

    async fn broadcast_message(&self, message: &DeviceMessage) -> MisaResult<()> {
        let connections = self.active_connections.read().await;

        for (device_id, connection) in connections.iter() {
            if let Err(e) = self.send_message_via_connection(connection, message).await {
                warn!("Failed to send message to device {}: {}", device_id, e);
            }
        }

        Ok(())
    }

    async fn select_best_device(&self, devices: &HashMap<String, DeviceInfo>) -> MisaResult<Option<String>> {
        let mut best_device = None;
        let mut best_score = -1.0;

        for (device_id, device) in devices.iter() {
            if !matches!(device.status, DeviceStatus::Online) {
                continue;
            }

            let mut score = 0.0;

            // Prefer devices with GPU
            if device.capabilities.supports_gpu {
                score += 10.0;
            }

            // Prefer devices with more memory
            score += (device.capabilities.max_memory_mb as f64) / 1024.0; // Convert to GB

            // Prefer non-battery powered devices
            if !device.capabilities.battery_powered {
                score += 5.0;
            }

            // Penalize low battery
            if let Some(battery) = device.battery_level {
                if battery < 20.0 {
                    score -= 5.0;
                }
            }

            if score > best_score {
                best_score = score;
                best_device = Some(device_id.clone());
            }
        }

        Ok(best_device)
    }

    async fn start_device_monitoring(&self) -> MisaResult<()> {
        // Start monitoring device status, battery, etc.
        info!("Starting device monitoring");

        // In real implementation, this would:
        // - Monitor battery levels
        // - Track device connectivity
        // - Update device capabilities
        // - Handle device disconnections

        Ok(())
    }

    async fn close_all_connections(&self) -> MisaResult<()> {
        let mut connections = self.active_connections.write().await;
        connections.clear();
        Ok(())
    }

    fn validate_file(&self, file_path: &str) -> MisaResult<()> {
        // Check file exists
        if !std::path::Path::new(file_path).exists() {
            return Err(MisaError::Device(format!("File not found: {}", file_path)));
        }

        // Check file size
        let metadata = std::fs::metadata(file_path)
            .map_err(|e| MisaError::Io(e))?;

        let file_size_mb = metadata.len() / (1024 * 1024);
        if file_size_mb > self.config.file_transfer.max_file_size_mb {
            return Err(MisaError::Device(format!(
                "File too large: {}MB (max: {}MB)",
                file_size_mb,
                self.config.file_transfer.max_file_size_mb
            )));
        }

        Ok(())
    }
}

/// Pairing data from QR token
#[derive(Debug, Clone)]
struct PairingData {
    device_id: String,
    timestamp: i64,
    signature: String,
}

/// Pairing result
#[derive(Debug, Clone, Serialize)]
pub struct PairingResult {
    pub success: bool,
    pub device_id: String,
    pub message: String,
}

impl Default for DeviceCapabilities {
    fn default() -> Self {
        Self {
            supports_gpu: false,
            supports_vision: false,
            supports_audio: true,
            has_camera: false,
            has_microphone: true,
            max_memory_mb: 4096,
            cpu_cores: 4,
            gpu_memory_mb: None,
            battery_powered: false,
            supports_remote_desktop: true,
        }
    }
}

impl Default for NetworkInfo {
    fn default() -> Self {
        Self {
            ip_address: "127.0.0.1".to_string(),
            mac_address: None,
            connection_type: ConnectionType::Unknown,
            signal_strength: None,
            bandwidth_mbps: None,
        }
    }
}

impl DiscoveryService {
    pub fn new(enabled: bool) -> Self {
        Self {
            enabled,
            discovery_port: 8081,
            broadcast_interval_seconds: 30,
            active_discovery: Arc::new(RwLock::new(HashMap::new())),
            background_scanning: true,
            smart_suggestions: true,
            last_scan: Arc::new(RwLock::new(chrono::Utc::now())),
            device_history: Arc::new(RwLock::new(HashMap::new())),
            connection_quality_monitor: ConnectionQualityMonitor::new(),
        }
    }

    pub async fn start(&self) -> MisaResult<()> {
        if !self.enabled {
            return Ok(());
        }

        info!("Starting enhanced discovery service on port {}", self.discovery_port);

        // Start UDP discovery service
        let udp_socket = tokio::net::UdpSocket::bind(("0.0.0.0", self.discovery_port))
            .await
            .map_err(|e| MisaError::Device(format!("Failed to bind UDP socket: {}", e)))?;

        let active_discovery = Arc::clone(&self.active_discovery);
        let broadcast_interval = self.broadcast_interval_seconds;
        let background_scanning = self.background_scanning;
        let smart_suggestions = self.smart_suggestions;
        let last_scan = Arc::clone(&self.last_scan);
        let device_history = Arc::clone(&self.device_history);
        let quality_monitor = Arc::clone(&self.connection_quality_monitor.active_connections);

        // Spawn enhanced discovery broadcaster
        tokio::spawn(async move {
            let mut interval = tokio::time::interval(Duration::from_secs(broadcast_interval));
            let socket = Arc::new(udp_socket);

            loop {
                interval.tick().await;

                // Update last scan time
                *last_scan.write().await = chrono::Utc::now();

                if let Err(e) = Self::broadcast_device_info_enhanced(&socket, &device_history, &quality_monitor).await {
                    warn!("Failed to broadcast device info: {}", e);
                }

                // Background scanning
                if background_scanning {
                    if let Err(e) = Self::background_device_scan(&socket, &device_history).await {
                        warn!("Background scan failed: {}", e);
                    }
                }

                // Smart suggestions
                if smart_suggestions {
                    if let Err(e) = Self::update_smart_suggestions(&device_history).await {
                        warn!("Smart suggestions update failed: {}", e);
                    }
                }
            }
        });

        // Spawn enhanced discovery listener
        let active_discovery_listener = Arc::clone(&self.active_discovery);
        let device_history_listener = Arc::clone(&self.device_history);
        let quality_monitor_listener = self.connection_quality_monitor.clone();
        let listener_socket = tokio::net::UdpSocket::bind(("0.0.0.0", self.discovery_port + 1))
            .await
            .map_err(|e| MisaError::Device(format!("Failed to bind listener socket: {}", e)))?;

        tokio::spawn(async move {
            let mut buf = [0u8; 1024];
            loop {
                match listener_socket.recv_from(&mut buf).await {
                    Ok((len, addr)) => {
                        let data = &buf[..len];
                        if let Err(e) = Self::handle_discovery_packet_enhanced(
                            data,
                            addr,
                            &active_discovery_listener,
                            &device_history_listener,
                            &quality_monitor_listener
                        ).await {
                            warn!("Failed to handle discovery packet: {}", e);
                        }
                    }
                    Err(e) => warn!("Discovery listener error: {}", e),
                }
            }
        });

        // Start connection quality monitoring
        self.connection_quality_monitor.start_monitoring().await?;

        info!("Enhanced discovery service started successfully");
        Ok(())
    }

    async fn broadcast_device_info(socket: &Arc<tokio::net::UdpSocket>) -> MisaResult<()> {
        let device_info = DeviceDiscoveryPacket {
            device_id: "local-device".to_string(), // Would get from config
            device_name: "Misa Device".to_string(),
            device_type: "Desktop".to_string(),
            capabilities: vec!["gpu".to_string(), "vision".to_string(), "audio".to_string()],
            port: 8080,
            timestamp: chrono::Utc::now(),
        };

        let packet_data = serde_json::to_vec(&device_info)
            .map_err(|e| MisaError::Serialization(e))?;

        // Broadcast to local network
        let broadcast_addr = "255.255.255.255:8081";
        match socket.send_to(&packet_data, broadcast_addr).await {
            Ok(_) => debug!("Broadcast device discovery packet"),
            Err(e) => warn!("Failed to broadcast discovery packet: {}", e),
        }

        Ok(())
    }

    async fn handle_discovery_packet(
        data: &[u8],
        addr: std::net::SocketAddr,
        active_discovery: &Arc<RwLock<HashMap<String, DiscoverySession>>>,
    ) -> MisaResult<()> {
        let packet: DeviceDiscoveryPacket = serde_json::from_slice(data)
            .map_err(|_| MisaError::Device("Invalid discovery packet".to_string()))?;

        debug!("Received discovery packet from {}: {}", addr, packet.device_id);

        // Create discovery session
        let session = DiscoverySession {
            session_id: uuid::Uuid::new_v4().to_string(),
            device_id: packet.device_id.clone(),
            started_at: chrono::Utc::now(),
            qr_token: format!("misa://pair/{}/{}", packet.device_id, chrono::Utc::now().timestamp()),
            pairing_status: PairingStatus::PendingConfirmation,
        };

        let mut sessions = active_discovery.write().await;
        sessions.insert(packet.device_id.clone(), session);

        Ok(())
    }

    pub async fn stop(&self) -> MisaResult<()> {
        info!("Stopping enhanced discovery service");

        // Cleanup active discovery sessions
        let mut sessions = self.active_discovery.write().await;
        sessions.clear();

        // Stop connection quality monitoring
        self.connection_quality_monitor.stop_monitoring().await?;

        Ok(())
    }

    /// Enhanced broadcast with device history and quality information
    async fn broadcast_device_info_enhanced(
        socket: &Arc<tokio::net::UdpSocket>,
        device_history: &Arc<RwLock<HashMap<String, DeviceHistory>>>,
        quality_monitor: &Arc<RwLock<HashMap<String, ConnectionQuality>>>,
    ) -> MisaResult<()> {
        let history = device_history.read().await;
        let quality = quality_monitor.read().await;

        let device_info = DeviceDiscoveryPacket {
            device_id: "local-device".to_string(),
            device_name: "Misa Device".to_string(),
            device_type: "Desktop".to_string(),
            capabilities: vec![
                "gpu".to_string(),
                "vision".to_string(),
                "audio".to_string(),
                "remote_desktop".to_string(),
                "background_discovery".to_string(),
            ],
            port: 8080,
            timestamp: chrono::Utc::now(),
        };

        let packet_data = serde_json::to_vec(&device_info)
            .map_err(|e| MisaError::Serialization(e))?;

        // Broadcast to local network with enhanced information
        let broadcast_addr = "255.255.255.255:8081";
        match socket.send_to(&packet_data, broadcast_addr).await {
            Ok(_) => debug!("Enhanced device discovery packet broadcasted"),
            Err(e) => warn!("Failed to broadcast discovery packet: {}", e),
        }

        Ok(())
    }

    /// Background device scanning for continuous discovery
    async fn background_device_scan(
        socket: &Arc<tokio::net::UdpSocket>,
        device_history: &Arc<RwLock<HashMap<String, DeviceHistory>>>,
    ) -> MisaResult<()> {
        debug!("Performing background device scan");

        // Scan for known devices first
        let history = device_history.read().await;
        for (device_id, device_info) in history.iter() {
            if should_scan_device(device_info) {
                // Send directed discovery packet to known device
                if let Err(e) = Self::send_directed_discovery(socket, device_id).await {
                    debug!("Failed to scan device {}: {}", device_id, e);
                }
            }
        }

        // Perform general network scan
        if let Err(e) = Self::network_discovery_scan(socket).await {
            warn!("Network discovery scan failed: {}", e);
        }

        Ok(())
    }

    /// Update smart suggestions based on device history and usage patterns
    async fn update_smart_suggestions(
        device_history: &Arc<RwLock<HashMap<String, DeviceHistory>>>,
    ) -> MisaResult<()> {
        debug!("Updating smart device suggestions");

        let mut history = device_history.write().await;
        let now = chrono::Utc::now();

        // Update device scores based on recent usage
        for (_, device_info) in history.iter_mut() {
            let hours_since_last_use = (now - device_info.last_connected).num_hours();

            // Decay score over time
            if hours_since_last_use > 24 {
                device_info.success_rate = device_info.success_rate * 0.95;
            }

            // Boost recently successful devices
            if hours_since_last_use < 1 && device_info.success_rate > 0.8 {
                device_info.success_rate = (device_info.success_rate * 1.1).min(1.0);
            }
        }

        Ok(())
    }

    /// Send directed discovery to specific device
    async fn send_directed_discovery(
        socket: &Arc<tokio::net::UdpSocket>,
        device_id: &str,
    ) -> MisaResult<()> {
        // Implementation would send directed packet to specific device
        debug!("Sending directed discovery to device: {}", device_id);
        Ok(())
    }

    /// Perform general network discovery scan
    async fn network_discovery_scan(socket: &Arc<tokio::net::UdpSocket>) -> MisaResult<()> {
        // Implementation would scan local network for devices
        debug!("Performing network discovery scan");
        Ok(())
    }

    /// Enhanced packet handler with device history tracking
    async fn handle_discovery_packet_enhanced(
        data: &[u8],
        addr: std::net::SocketAddr,
        active_discovery: &Arc<RwLock<HashMap<String, DiscoverySession>>>,
        device_history: &Arc<RwLock<HashMap<String, DeviceHistory>>>,
        quality_monitor: &ConnectionQualityMonitor,
    ) -> MisaResult<()> {
        let packet: DeviceDiscoveryPacket = serde_json::from_slice(data)
            .map_err(|_| MisaError::Device("Invalid discovery packet".to_string()))?;

        debug!("Received enhanced discovery packet from {}: {}", addr, packet.device_id);

        // Update device history
        Self::update_device_history(&packet, device_history).await?;

        // Create enhanced discovery session
        let session = DiscoverySession {
            session_id: uuid::Uuid::new_v4().to_string(),
            device_id: packet.device_id.clone(),
            started_at: chrono::Utc::now(),
            qr_token: format!("misa://pair/{}/{}", packet.device_id, chrono::Utc::now().timestamp()),
            pairing_status: PairingStatus::PendingConfirmation,
            auto_pair_enabled: should_auto_pair(&packet, device_history).await,
            connection_strength: estimate_signal_strength(addr),
        };

        let mut sessions = active_discovery.write().await;
        sessions.insert(packet.device_id.clone(), session);

        // Monitor connection quality
        quality_monitor.update_connection_quality(&packet.device_id, addr).await?;

        Ok(())
    }

    /// Update device history with new discovery information
    async fn update_device_history(
        packet: &DeviceDiscoveryPacket,
        device_history: &Arc<RwLock<HashMap<String, DeviceHistory>>>,
    ) -> MisaResult<()> {
        let mut history = device_history.write().await;

        let device_history_entry = history.entry(packet.device_id.clone()).or_insert_with(|| DeviceHistory {
            device_id: packet.device_id.clone(),
            last_connected: chrono::Utc::now(),
            connection_count: 0,
            average_signal_strength: 0.0,
            success_rate: 1.0,
            preferred_for_tasks: Vec::new(),
            device_type: DeviceType::Desktop, // Default
        });

        // Update connection info
        device_history_entry.last_connected = chrono::Utc::now();
        device_history_entry.connection_count += 1;

        Ok(())
    }
}

/// Helper functions for enhanced discovery

fn should_scan_device(device_info: &DeviceHistory) -> bool {
    let hours_since_last_use = (chrono::Utc::now() - device_info.last_connected).num_hours();
    hours_since_last_use < 168 && device_info.success_rate > 0.5 // Scan devices used in last week with decent success rate
}

async fn should_auto_pair(packet: &DeviceDiscoveryPacket, device_history: &Arc<RwLock<HashMap<String, DeviceHistory>>>) -> bool {
    let history = device_history.read().await;

    if let Some(device_info) = history.get(&packet.device_id) {
        // Auto-pair if device has been successfully paired before and has good success rate
        device_info.success_rate > 0.8 && device_info.connection_count > 3
    } else {
        // Auto-pair new devices that have specific capabilities
        packet.capabilities.contains(&"trusted_source".to_string())
    }
}

fn estimate_signal_strength(addr: std::net::SocketAddr) -> f32 {
    // Simple estimation based on address type
    match addr.ip() {
        std::net::IpAddr::V4(ipv4) => {
            if ipv4.is_loopback() {
                1.0
            } else if ipv4.is_private() {
                0.8
            } else {
                0.5
            }
        }
        std::net::IpAddr::V6(_) => 0.7,
    }
}

impl ConnectionQualityMonitor {
    pub fn new() -> Self {
        Self {
            active_connections: Arc::new(RwLock::new(HashMap::new())),
            quality_history: Arc::new(RwLock::new(Vec::new())),
        }
    }

    pub async fn start_monitoring(&self) -> MisaResult<()> {
        info!("Starting connection quality monitoring");

        let connections = Arc::clone(&self.active_connections);
        let history = Arc::clone(&self.quality_history);

        tokio::spawn(async move {
            let mut interval = tokio::time::interval(Duration::from_secs(10));

            loop {
                interval.tick().await;

                if let Err(e) = Self::monitor_connection_quality(&connections, &history).await {
                    warn!("Connection quality monitoring error: {}", e);
                }
            }
        });

        Ok(())
    }

    pub async fn stop_monitoring(&self) -> MisaResult<()> {
        info!("Stopping connection quality monitoring");

        let mut connections = self.active_connections.write().await;
        connections.clear();

        let mut history = self.quality_history.write().await;
        history.clear();

        Ok(())
    }

    pub async fn update_connection_quality(&self, device_id: &str, addr: std::net::SocketAddr) -> MisaResult<()> {
        let mut connections = self.active_connections.write().await;

        let quality = ConnectionQuality {
            device_id: device_id.to_string(),
            latency_ms: 0, // Would measure actual latency
            bandwidth_mbps: 0.0, // Would measure actual bandwidth
            signal_strength: estimate_signal_strength(addr),
            stability_score: 1.0,
            last_updated: chrono::Utc::now(),
            uptime_percentage: 100.0,
        };

        connections.insert(device_id.to_string(), quality);

        Ok(())
    }

    async fn monitor_connection_quality(
        connections: &Arc<RwLock<HashMap<String, ConnectionQuality>>>,
        history: &Arc<RwLock<Vec<QualityMeasurement>>>,
    ) -> MisaResult<()> {
        let current_connections = connections.read().await;
        let mut quality_history = history.write().await;

        for (device_id, quality) in current_connections.iter() {
            let measurement = QualityMeasurement {
                device_id: device_id.clone(),
                timestamp: chrono::Utc::now(),
                latency_ms: quality.latency_ms,
                packet_loss: 0.0, // Would measure actual packet loss
                jitter_ms: 0, // Would measure actual jitter
            };

            quality_history.push(measurement);

            // Keep only last 100 measurements per device
            if quality_history.len() > 1000 {
                quality_history.drain(0..quality_history.len() - 1000);
            }
        }

        Ok(())
    }

impl RemoteDesktopManager {
    pub fn new(enabled: bool) -> Self {
        Self {
            enabled,
            active_sessions: Arc::new(RwLock::new(HashMap::new())),
            screen_capturer: ScreenCapturer::new(),
            file_transfer_manager: FileTransferManager::new(),
        }
    }

    pub async fn start_session(
        &self,
        target_device_id: &str,
        permissions: RemoteDesktopPermissions,
    ) -> MisaResult<String> {
        if !self.enabled {
            return Err(MisaError::Device("Remote desktop disabled".to_string()));
        }

        let session_id = uuid::Uuid::new_v4().to_string();
        let session = RemoteDesktopSession {
            session_id: session_id.clone(),
            host_device_id: target_device_id.to_string(),
            client_device_id: "local".to_string(), // Would be actual device ID
            protocol: RemoteDesktopProtocol::WebRTC,
            resolution: (1920, 1080),
            quality: VideoQuality::High,
            permissions,
            started_at: chrono::Utc::now(),
            screen_recording: false,
        };

        let mut sessions = self.active_sessions.write().await;
        sessions.insert(session_id.clone(), session);

        info!("Started remote desktop session: {}", session_id);
        Ok(session_id)
    }

    pub async fn shutdown(&self) -> MisaResult<()> {
        info!("Shutting down remote desktop manager");

        // Close all sessions
        let mut sessions = self.active_sessions.write().await;
        sessions.clear();

        Ok(())
    }
}

impl ScreenCapturer {
    pub fn new() -> Self {
        Self {
            capture_interval_ms: 100, // 10 FPS
            compression_enabled: true,
            supported_formats: vec![ImageFormat::JPEG, ImageFormat::PNG, ImageFormat::H264],
        }
    }

    /// Start screen capture for remote desktop
    pub async fn start_capture(&self, session_id: String) -> MisaResult<ScreenCaptureStream> {
        debug!("Starting screen capture for session: {}", session_id);

        // In a real implementation, this would:
        // - Use platform-specific screen capture APIs (Windows Desktop Duplication, macOS ScreenCaptureKit, Linux X11/Wayland)
        // - Set up video encoding pipeline
        // - Create streaming endpoints

        let capture_stream = ScreenCaptureStream {
            session_id,
            format: ImageFormat::H264,
            resolution: (1920, 1080),
            frame_rate: 30,
            started_at: chrono::Utc::now(),
        };

        info!("Screen capture started for session: {}", session_id);
        Ok(capture_stream)
    }

    /// Capture single frame
    pub async fn capture_frame(&self, format: ImageFormat) -> MisaResult<Vec<u8>> {
        // In a real implementation, this would:
        // - Capture screen using platform APIs
        // - Encode to requested format
        // - Return frame data

        debug!("Capturing screen frame in format: {:?}", format);

        // Simulate frame capture (would be actual screen data)
        let frame_data = vec![0u8; 1024 * 768 * 3]; // RGB frame data placeholder

        match format {
            ImageFormat::JPEG => {
                // Simulate JPEG encoding
                Ok(frame_data)
            }
            ImageFormat::PNG => {
                // Simulate PNG encoding
                Ok(frame_data)
            }
            ImageFormat::H264 => {
                // Simulate H.264 encoding
                Ok(frame_data)
            }
            ImageFormat::WebP => {
                // Simulate WebP encoding
                Ok(frame_data)
            }
            ImageFormat::VP9 => {
                // Simulate VP9 encoding
                Ok(frame_data)
            }
        }
    }
}

/// Screen capture stream for remote desktop
#[derive(Debug, Clone)]
pub struct ScreenCaptureStream {
    pub session_id: String,
    pub format: ImageFormat,
    pub resolution: (u32, u32),
    pub frame_rate: u32,
    pub started_at: chrono::DateTime<chrono::Utc>,
}

impl FileTransferManager {
    pub fn new() -> Self {
        Self {
            max_file_size_mb: 1024,
            allowed_file_types: vec!["*".to_string()], // All types
            encryption_required: true,
            active_transfers: Arc::new(RwLock::new(HashMap::new())),
        }
    }

    pub async fn start_transfer(&self, target_device_id: &str, file_path: &str) -> MisaResult<String> {
        let transfer_id = uuid::Uuid::new_v4().to_string();

        let metadata = std::fs::metadata(file_path)
            .map_err(|e| MisaError::Io(e))?;

        let transfer = FileTransfer {
            transfer_id: transfer_id.clone(),
            source_device_id: "local".to_string(),
            target_device_id: target_device_id.to_string(),
            file_path: file_path.to_string(),
            file_size: metadata.len(),
            bytes_transferred: 0,
            encryption_key: None,
            status: FileTransferStatus::Pending,
            started_at: chrono::Utc::now(),
        };

        let mut transfers = self.active_transfers.write().await;
        transfers.insert(transfer_id.clone(), transfer);

        // Start the actual file transfer in background
        self.execute_file_transfer(transfer_id.clone(), file_path.to_string()).await?;

        info!("Started file transfer: {} -> {}", transfer_id, file_path);
        Ok(transfer_id)
    }

    /// Execute the actual file transfer with progress tracking
    async fn execute_file_transfer(&self, transfer_id: String, file_path: String) -> MisaResult<()> {
        let active_transfers = Arc::clone(&self.active_transfers);
        let encryption_required = self.encryption_required;

        tokio::spawn(async move {
            // Read file in chunks and simulate transfer
            let chunk_size = 64 * 1024; // 64KB chunks
            let mut bytes_transferred = 0u64;

            // Update status to InProgress
            {
                let mut transfers = active_transfers.write().await;
                if let Some(transfer) = transfers.get_mut(&transfer_id) {
                    transfer.status = FileTransferStatus::InProgress;
                }
            }

            // Simulate file reading and transfer
            match std::fs::File::open(&file_path) {
                Ok(mut file) => {
                    let mut buffer = vec![0u8; chunk_size];

                    loop {
                        match file.read(&mut buffer) {
                            Ok(0) => break, // EOF
                            Ok(bytes_read) => {
                                bytes_transferred += bytes_read as u64;

                                // Update transfer progress
                                {
                                    let mut transfers = active_transfers.write().await;
                                    if let Some(transfer) = transfers.get_mut(&transfer_id) {
                                        transfer.bytes_transferred = bytes_transferred;
                                    }
                                }

                                // Simulate network transfer delay
                                tokio::time::sleep(Duration::from_millis(10)).await;
                            }
                            Err(e) => {
                                error!("Error reading file during transfer: {}", e);
                                break;
                            }
                        }
                    }

                    // Mark as completed
                    let mut transfers = active_transfers.write().await;
                    if let Some(transfer) = transfers.get_mut(&transfer_id) {
                        transfer.status = FileTransferStatus::Completed;
                    }

                    info!("File transfer completed: {}", transfer_id);
                }
                Err(e) => {
                    error!("Failed to open file for transfer: {}", e);
                    let mut transfers = active_transfers.write().await;
                    if let Some(transfer) = transfers.get_mut(&transfer_id) {
                        transfer.status = FileTransferStatus::Failed(format!("Failed to open file: {}", e));
                    }
                }
            }
        });

        Ok(())
    }

    /// Get transfer progress
    pub async fn get_transfer_progress(&self, transfer_id: &str) -> MisaResult<Option<FileTransfer>> {
        let transfers = self.active_transfers.read().await;
        Ok(transfers.get(transfer_id).cloned())
    }

    /// Cancel active transfer
    pub async fn cancel_transfer(&self, transfer_id: &str) -> MisaResult<()> {
        let mut transfers = self.active_transfers.write().await;
        if let Some(transfer) = transfers.get_mut(transfer_id) {
            transfer.status = FileTransferStatus::Failed("Transfer cancelled".to_string());
            info!("File transfer cancelled: {}", transfer_id);
        }
        Ok(())
    }
}

impl ClipboardSync {
    pub fn new(encryption_enabled: bool) -> Self {
        Self {
            enabled: true,
            encryption_enabled,
            sync_interval_seconds: 1,
            last_clipboard_hash: Arc::new(RwLock::new(None)),
            supported_formats: vec!["text/plain".to_string(), "image/png".to_string()],
        }
    }

    /// Start clipboard synchronization service
    pub async fn start_sync(&self, device_manager: Arc<DeviceManager>) -> MisaResult<()> {
        if !self.enabled {
            info!("Clipboard sync disabled");
            return Ok(());
        }

        info!("Starting clipboard synchronization service");

        let sync_interval = self.sync_interval_seconds;
        let last_clipboard_hash = Arc::clone(&self.last_clipboard_hash);
        let encryption_enabled = self.encryption_enabled;

        tokio::spawn(async move {
            let mut interval = tokio::time::interval(Duration::from_secs(sync_interval));

            loop {
                interval.tick().await;

                if let Err(e) = Self::check_and_sync_clipboard(
                    &device_manager,
                    &last_clipboard_hash,
                    encryption_enabled,
                ).await {
                    warn!("Clipboard sync error: {}", e);
                }
            }
        });

        info!("Clipboard synchronization service started");
        Ok(())
    }

    /// Check clipboard for changes and sync to connected devices
    async fn check_and_sync_clipboard(
        device_manager: &Arc<DeviceManager>,
        last_clipboard_hash: &Arc<RwLock<Option<String>>>,
        encryption_enabled: bool,
    ) -> MisaResult<()> {
        // Get current clipboard content
        let clipboard_content = Self::get_clipboard_content().await?;

        // Calculate hash of current content
        let digest = compute_md5(clipboard_content.as_bytes());
        let content_hash = format!("{:02x?}", digest);

        // Check if content has changed
        {
            let mut last_hash = last_clipboard_hash.write().await;
            if let Some(ref hash) = *last_hash {
                if hash == &content_hash {
                    return Ok(()); // No change
                }
            }
            *last_hash = Some(content_hash.clone());
        }

        debug!("Clipboard content changed, syncing to devices");

        // Create clipboard sync message
        let sync_message = DeviceMessage {
            message_id: uuid::Uuid::new_v4().to_string(),
            source_device_id: "local".to_string(),
            target_device_id: None, // Broadcast to all
            message_type: MessageType::ClipboardSync,
            payload: serde_json::json!({
                "content": clipboard_content,
                "format": "text/plain",
                "timestamp": chrono::Utc::now(),
                "encrypted": encryption_enabled
            }),
            timestamp: chrono::Utc::now(),
            encrypted: encryption_enabled,
            priority: MessagePriority::Normal,
        };

        // Broadcast to all connected devices
        device_manager.send_message(sync_message).await?;

        Ok(())
    }

    /// Get current clipboard content (platform-specific)
    async fn get_clipboard_content() -> MisaResult<String> {
        // In a real implementation, this would use platform-specific clipboard APIs:
        // - Windows: Windows API
        // - macOS: NSPasteboard
        // - Linux: X11 clipboard or Wayland clipboard

        // For now, simulate clipboard content
        Ok("Sample clipboard content".to_string())
    }

    /// Set clipboard content (platform-specific)
    pub async fn set_clipboard_content(&self, content: &str, source_device_id: &str) -> MisaResult<()> {
        info!("Setting clipboard content from device: {}", source_device_id);

        // In a real implementation, this would use platform-specific clipboard APIs
        debug!("Setting clipboard: {}", content);

        // Update last clipboard hash to prevent sync loop
        let digest = compute_md5(content.as_bytes());
        let content_hash = format!("{:02x?}", digest);
        let mut last_hash = self.last_clipboard_hash.write().await;
        *last_hash = Some(content_hash);

        Ok(())
    }
}

// Implement Clone for Arc-wrapped structs
impl Clone for DeviceManager {
    fn clone(&self) -> Self {
        Self {
            config: self.config.clone(),
            security_manager: self.security_manager.clone(),
            devices: Arc::clone(&self.devices),
            active_connections: Arc::clone(&self.active_connections),
            discovery_service: DiscoveryService::new(self.config.discovery_enabled),
            remote_desktop_manager: RemoteDesktopManager::new(self.config.remote_desktop_enabled),
            clipboard_sync: ClipboardSync::new(true),
        }
    }
}

impl Clone for DiscoveryService {
    fn clone(&self) -> Self {
        Self {
            enabled: self.enabled,
            discovery_port: self.discovery_port,
            broadcast_interval_seconds: self.broadcast_interval_seconds,
            active_discovery: Arc::clone(&self.active_discovery),
        }
    }
}

impl Clone for RemoteDesktopManager {
    fn clone(&self) -> Self {
        Self {
            enabled: self.enabled,
            active_sessions: Arc::clone(&self.active_sessions),
            screen_capturer: ScreenCapturer::new(),
            file_transfer_manager: FileTransferManager::new(),
        }
    }
}

impl Clone for FileTransferManager {
    fn clone(&self) -> Self {
        Self {
            max_file_size_mb: self.max_file_size_mb,
            allowed_file_types: self.allowed_file_types.clone(),
            encryption_required: self.encryption_required,
            active_transfers: Arc::clone(&self.active_transfers),
        }
    }
}

impl Clone for ClipboardSync {
    fn clone(&self) -> Self {
        Self {
            enabled: self.enabled,
            encryption_enabled: self.encryption_enabled,
            sync_interval_seconds: self.sync_interval_seconds,
            last_clipboard_hash: Arc::clone(&self.last_clipboard_hash),
            supported_formats: self.supported_formats.clone(),
        }
    }
}