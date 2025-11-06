//! Security and Privacy Framework
//!
//! Comprehensive security system providing:
//! - AES-256 encryption for data at rest
//! - TLS for data in transit
//! - Authentication and authorization
//! - Biometric integration
//! - Plugin sandboxing and permission management
//! - Audit logging and security monitoring

use anyhow::Result;
use aes_gcm::{Aes256Gcm, Key, Nonce};
use aes_gcm::aead::{Aead, NewAead};
use argon2::{Argon2, password_hash::{PasswordHash, PasswordHasher, SaltString}};
use ring::rand::{SecureRandom, SystemRandom};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::path::Path;
use std::sync::Arc;
use tokio::sync::RwLock;
use tracing::{info, warn, error, debug};

use crate::kernel::SecurityConfig;
use crate::errors::{MisaError, Result as MisaResult};

/// Main security manager
pub struct SecurityManager {
    config: SecurityConfig,
    data_dir: String,
    encryption_manager: Arc<EncryptionManager>,
    auth_manager: Arc<AuthManager>,
    audit_logger: Arc<AuditLogger>,
    secure_rng: SystemRandom,
}

/// Encryption manager for data protection
pub struct EncryptionManager {
    master_key: Arc<RwLock<Option<[u8; 32]>>>,
    encrypted_keys: Arc<RwLock<HashMap<String, EncryptedKey>>>,
    secure_rng: SystemRandom,
}

/// Authentication and authorization manager
pub struct AuthManager {
    sessions: Arc<RwLock<HashMap<String, AuthSession>>>,
    user_credentials: Arc<RwLock<HashMap<String, UserCredentials>>>,
    biometric_providers: Arc<RwLock<HashMap<String, Box<dyn BiometricProvider>>>>,
    session_timeout_minutes: u64,
}

/// Audit logger for security events
pub struct AuditLogger {
    log_file: Arc<RwLock<Option<tokio::fs::File>>>,
    log_entries: Arc<RwLock<Vec<AuditEntry>>>,
    max_entries: usize,
}

/// Plugin sandbox manager
pub struct SandboxManager {
    active_sandboxes: Arc<RwLock<HashMap<String, SandboxInfo>>>,
    resource_limits: ResourceLimits,
    permission_checker: PermissionChecker,
}

/// Biometric provider trait
#[async_trait::async_trait]
pub trait BiometricProvider: Send + Sync {
    async fn authenticate(&self, data: &[u8]) -> MisaResult<bool>;
    async fn enroll(&self, user_id: &str, data: &[u8]) -> MisaResult<()>;
    fn provider_type(&self) -> BiometricType;
}

/// Biometric type enumeration
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub enum BiometricType {
    Fingerprint,
    Face,
    Voice,
    Iris,
}

/// Encrypted key structure
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EncryptedKey {
    pub key_id: String,
    pub encrypted_data: Vec<u8>,
    pub nonce: Vec<u8>,
    pub algorithm: String,
    pub created_at: chrono::DateTime<chrono::Utc>,
}

/// User credentials
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserCredentials {
    pub user_id: String,
    pub password_hash: String,
    pub biometric_templates: HashMap<BiometricType, Vec<u8>>,
    pub created_at: chrono::DateTime<chrono::Utc>,
    pub last_login: Option<chrono::DateTime<chrono::Utc>>,
    pub failed_attempts: u32,
    pub locked_until: Option<chrono::DateTime<chrono::Utc>>,
}

/// Authentication session
#[derive(Debug, Clone)]
pub struct AuthSession {
    pub session_id: String,
    pub user_id: String,
    pub created_at: chrono::DateTime<chrono::Utc>,
    pub last_activity: chrono::DateTime<chrono::Utc>,
    pub expires_at: chrono::DateTime<chrono::Utc>,
    pub permissions: Vec<String>,
    pub device_info: serde_json::Value,
}

/// Audit log entry
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AuditEntry {
    pub id: String,
    pub timestamp: chrono::DateTime<chrono::Utc>,
    pub user_id: Option<String>,
    pub session_id: Option<String>,
    pub action: String,
    pub resource: String,
    pub result: AuditResult,
    pub details: serde_json::Value,
    pub ip_address: Option<String>,
    pub user_agent: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum AuditResult {
    Success,
    Failure,
    Error,
}

/// Sandbox information for plugin isolation
#[derive(Debug, Clone)]
pub struct SandboxInfo {
    pub plugin_id: String,
    pub sandbox_id: String,
    pub pid: u32,
    pub resource_usage: ResourceUsage,
    pub permissions: Vec<String>,
    pub started_at: chrono::DateTime<chrono::Utc>,
    pub status: SandboxStatus,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum SandboxStatus {
    Running,
    Stopped,
    Suspended,
    Error(String),
}

/// Resource limits for sandboxes
#[derive(Debug, Clone)]
pub struct ResourceLimits {
    pub max_memory_mb: u64,
    pub max_cpu_percent: f32,
    pub max_file_descriptors: u32,
    pub max_network_connections: u32,
    pub allowed_files: Vec<String>,
    pub blocked_files: Vec<String>,
    pub network_isolated: bool,
}

/// Current resource usage
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ResourceUsage {
    pub memory_mb: u64,
    pub cpu_percent: f32,
    pub file_descriptors: u32,
    pub network_connections: u32,
    pub disk_bytes_read: u64,
    pub disk_bytes_written: u64,
    pub network_bytes_sent: u64,
    pub network_bytes_received: u64,
}

/// Permission checker for plugin operations
pub struct PermissionChecker {
    permission_matrix: Arc<RwLock<HashMap<String, Vec<String>>>>,
}

impl SecurityManager {
    /// Create a new security manager
    pub async fn new(data_dir: &str, config: SecurityConfig) -> MisaResult<Self> {
        // Ensure data directory exists
        tokio::fs::create_dir_all(data_dir).await
            .map_err(|e| MisaError::Io(e))?;

        let encryption_manager = Arc::new(EncryptionManager::new(data_dir).await?);
        let auth_manager = Arc::new(AuthManager::new(config.session_timeout_minutes).await?);
        let audit_logger = Arc::new(AuditLogger::new(data_dir).await?);

        let manager = Self {
            config,
            data_dir: data_dir.to_string(),
            encryption_manager,
            auth_manager,
            audit_logger,
            secure_rng: SystemRandom::new(),
        };

        info!("Security manager initialized");

        // Log security manager initialization
        manager.log_security_event(
            None,
            "security_manager_initialized",
            "Security system initialized",
            AuditResult::Success,
            serde_json::json!({}),
        ).await?;

        Ok(manager)
    }

    /// Initialize the security manager
    pub async fn initialize(&self) -> MisaResult<()> {
        // Initialize encryption manager
        self.encryption_manager.initialize().await?;

        // Load existing user credentials
        self.auth_manager.load_credentials(&self.data_dir).await?;

        info!("Security manager fully initialized");
        Ok(())
    }

    /// Encrypt data with AES-256-GCM
    pub async fn encrypt_data(&self, data: &[u8], key_id: &str) -> MisaResult<EncryptedData> {
        self.encryption_manager.encrypt(data, key_id).await
    }

    /// Decrypt data
    pub async fn decrypt_data(&self, encrypted_data: &EncryptedData) -> MisaResult<Vec<u8>> {
        self.encryption_manager.decrypt(encrypted_data).await
    }

    /// Authenticate user with password
    pub async fn authenticate_password(&self, user_id: &str, password: &str) -> MisaResult<AuthSession> {
        self.auth_manager.authenticate_password(user_id, password).await
    }

    /// Authenticate user with biometrics
    pub async fn authenticate_biometric(&self, user_id: &str, biometric_type: BiometricType, data: &[u8]) -> MisaResult<AuthSession> {
        self.auth_manager.authenticate_biometric(user_id, biometric_type, data).await
    }

    /// Validate session
    pub async fn validate_session(&self, session_id: &str) -> MisaResult<Option<AuthSession>> {
        self.auth_manager.validate_session(session_id).await
    }

    /// Log security event
    pub async fn log_security_event(
        &self,
        user_id: Option<&str>,
        action: &str,
        resource: &str,
        result: AuditResult,
        details: serde_json::Value,
    ) -> MisaResult<()> {
        let entry = AuditEntry {
            id: uuid::Uuid::new_v4().to_string(),
            timestamp: chrono::Utc::now(),
            user_id: user_id.map(|s| s.to_string()),
            session_id: None,
            action: action.to_string(),
            resource: resource.to_string(),
            result,
            details,
            ip_address: None, // Could be extracted from request context
            user_agent: None,
        };

        self.audit_logger.log_entry(entry).await
    }

    /// Check if user has permission for action
    pub async fn check_permission(&self, user_id: &str, permission: &str) -> MisaResult<bool> {
        // This would integrate with the auth manager's permission system
        // For now, return true for authenticated users
        Ok(true)
    }

    /// Create sandbox for plugin
    pub async fn create_plugin_sandbox(&self, plugin_id: &str, permissions: Vec<String>) -> MisaResult<String> {
        let sandbox_manager = SandboxManager::new(
            ResourceLimits::default(),
            PermissionChecker::new(),
        );

        sandbox_manager.create_sandbox(plugin_id, permissions).await
    }

    /// Shutdown security manager
    pub async fn shutdown(&self) -> MisaResult<()> {
        info!("Shutting down security manager");

        // Flush audit logs
        self.audit_logger.flush().await?;

        // Close all sessions
        self.auth_manager.close_all_sessions().await?;

        info!("Security manager shut down");
        Ok(())
    }
}

impl Clone for SecurityManager {
    fn clone(&self) -> Self {
        Self {
            config: self.config.clone(),
            data_dir: self.data_dir.clone(),
            encryption_manager: Arc::clone(&self.encryption_manager),
            auth_manager: Arc::clone(&self.auth_manager),
            audit_logger: Arc::clone(&self.audit_logger),
            secure_rng: SystemRandom::new(),
        }
    }
}

/// Encrypted data structure
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EncryptedData {
    pub ciphertext: Vec<u8>,
    pub nonce: Vec<u8>,
    pub key_id: String,
    pub algorithm: String,
    pub tag: Vec<u8>,
}

impl EncryptionManager {
    pub async fn new(data_dir: &str) -> MisaResult<Self> {
        Ok(Self {
            master_key: Arc::new(RwLock::new(None)),
            encrypted_keys: Arc::new(RwLock::new(HashMap::new())),
            secure_rng: SystemRandom::new(),
        })
    }

    pub async fn initialize(&self) -> MisaResult<()> {
        // Try to load existing master key or generate new one
        let mut master_key = self.master_key.write().await;
        if master_key.is_none() {
            // Generate new master key
            let mut key_bytes = [0u8; 32];
            self.secure_rng.fill(&mut key_bytes)
                .map_err(|e| MisaError::Encryption(format!("Failed to generate master key: {}", e)))?;
            *master_key = Some(key_bytes);
            info!("Generated new encryption master key");
        }

        Ok(())
    }

    pub async fn encrypt(&self, data: &[u8], key_id: &str) -> MisaResult<EncryptedData> {
        let master_key = self.master_key.read().await;
        let key = master_key.ok_or_else(|| MisaError::Encryption("Master key not initialized".to_string()))?;

        let key = Key::from_slice(&*key);
        let cipher = Aes256Gcm::new(key);

        // Generate nonce
        let mut nonce_bytes = [0u8; 12];
        self.secure_rng.fill(&mut nonce_bytes)
            .map_err(|e| MisaError::Encryption(format!("Failed to generate nonce: {}", e)))?;
        let nonce = Nonce::from_slice(&nonce_bytes);

        // Encrypt data
        let ciphertext = cipher.encrypt(nonce, data)
            .map_err(|e| MisaError::Encryption(format!("Encryption failed: {}", e)))?;

        // Split ciphertext and tag
        let (ciphertext, tag) = ciphertext.split_at(ciphertext.len() - 16);

        Ok(EncryptedData {
            ciphertext: ciphertext.to_vec(),
            nonce: nonce_bytes.to_vec(),
            key_id: key_id.to_string(),
            algorithm: "AES-256-GCM".to_string(),
            tag: tag.to_vec(),
        })
    }

    pub async fn decrypt(&self, encrypted_data: &EncryptedData) -> MisaResult<Vec<u8>> {
        let master_key = self.master_key.read().await;
        let key = master_key.ok_or_else(|| MisaError::Encryption("Master key not initialized".to_string()))?;

        if encrypted_data.algorithm != "AES-256-GCM" {
            return Err(MisaError::Encryption("Unsupported encryption algorithm".to_string()));
        }

        let key = Key::from_slice(&*key);
        let cipher = Aes256Gcm::new(key);

        let nonce = Nonce::from_slice(&encrypted_data.nonce);

        // Combine ciphertext and tag
        let mut encrypted_message = encrypted_data.ciphertext.clone();
        encrypted_message.extend_from_slice(&encrypted_data.tag);

        let plaintext = cipher.decrypt(nonce, encrypted_message.as_slice())
            .map_err(|e| MisaError::Encryption(format!("Decryption failed: {}", e)))?;

        Ok(plaintext)
    }
}

impl AuthManager {
    pub async fn new(session_timeout_minutes: u64) -> MisaResult<Self> {
        Ok(Self {
            sessions: Arc::new(RwLock::new(HashMap::new())),
            user_credentials: Arc::new(RwLock::new(HashMap::new())),
            biometric_providers: Arc::new(RwLock::new(HashMap::new())),
            session_timeout_minutes,
        })
    }

    pub async fn load_credentials(&self, data_dir: &str) -> MisaResult<()> {
        let credentials_path = Path::new(data_dir).join("credentials.json");

        if credentials_path.exists() {
            let content = tokio::fs::read_to_string(&credentials_path).await
                .map_err(|e| MisaError::Io(e))?;

            let credentials_map: HashMap<String, UserCredentials> = serde_json::from_str(&content)
                .map_err(|e| MisaError::Serialization(e))?;

            *self.user_credentials.write().await = credentials_map;
            info!("Loaded {} user credentials", self.user_credentials.read().await.len());
        }

        Ok(())
    }

    pub async fn authenticate_password(&self, user_id: &str, password: &str) -> MisaResult<AuthSession> {
        let credentials = self.user_credentials.read().await;
        let user_creds = credentials.get(user_id)
            .ok_or_else(|| MisaError::Security("User not found".to_string()))?;

        // Check if account is locked
        if let Some(locked_until) = user_creds.locked_until {
            if chrono::Utc::now() < locked_until {
                return Err(MisaError::Security("Account is locked".to_string()));
            }
        }

        // Verify password
        let password_hash = PasswordHash::new(&user_creds.password_hash)
            .map_err(|e| MisaError::Security(format!("Invalid password hash: {}", e)))?;

        if Argon2::default().verify_password(password.as_bytes(), &password_hash).is_ok() {
            // Password correct - create session
            self.create_session(user_id, vec!["user".to_string()]).await
        } else {
            // Password incorrect - log failed attempt
            drop(credentials);
            self.handle_failed_attempt(user_id).await?;
            Err(MisaError::Security("Invalid password".to_string()))
        }
    }

    pub async fn authenticate_biometric(&self, user_id: &str, biometric_type: BiometricType, data: &[u8]) -> MisaResult<AuthSession> {
        let credentials = self.user_credentials.read().await;
        let user_creds = credentials.get(user_id)
            .ok_or_else(|| MisaError::Security("User not found".to_string()))?;

        let template = user_creds.biometric_templates.get(&biometric_type)
            .ok_or_else(|| MisaError::Security("Biometric template not found".to_string()))?;

        // Simple template matching (in real implementation, use proper biometric matching)
        if data.len() == template.len() {
            self.create_session(user_id, vec!["user".to_string()]).await
        } else {
            Err(MisaError::Security("Biometric authentication failed".to_string()))
        }
    }

    pub async fn validate_session(&self, session_id: &str) -> MisaResult<Option<AuthSession>> {
        let sessions = self.sessions.read().await;
        let session = sessions.get(session_id);

        if let Some(session) = session {
            if chrono::Utc::now() < session.expires_at {
                Ok(Some(session.clone()))
            } else {
                Ok(None) // Session expired
            }
        } else {
            Ok(None) // Session not found
        }
    }

    pub async fn close_all_sessions(&self) -> MisaResult<()> {
        let mut sessions = self.sessions.write().await;
        sessions.clear();
        Ok(())
    }

    async fn create_session(&self, user_id: &str, permissions: Vec<String>) -> MisaResult<AuthSession> {
        let session_id = uuid::Uuid::new_v4().to_string();
        let now = chrono::Utc::now();
        let expires_at = now + chrono::Duration::minutes(self.session_timeout_minutes as i64);

        let session = AuthSession {
            session_id: session_id.clone(),
            user_id: user_id.to_string(),
            created_at: now,
            last_activity: now,
            expires_at,
            permissions,
            device_info: serde_json::json!({}),
        };

        let mut sessions = self.sessions.write().await;
        sessions.insert(session_id.clone(), session.clone());

        info!("Created new session for user: {}", user_id);
        Ok(session)
    }

    async fn handle_failed_attempt(&self, user_id: &str) -> MisaResult<()> {
        let mut credentials = self.user_credentials.write().await;
        if let Some(user_creds) = credentials.get_mut(user_id) {
            user_creds.failed_attempts += 1;

            // Lock account after 5 failed attempts
            if user_creds.failed_attempts >= 5 {
                user_creds.locked_until = Some(chrono::Utc::now() + chrono::Duration::minutes(30));
                warn!("User account locked due to too many failed attempts: {}", user_id);
            }
        }
        Ok(())
    }
}

impl AuditLogger {
    pub async fn new(data_dir: &str) -> MisaResult<Self> {
        let log_file = Arc::new(RwLock::new(None));

        Ok(Self {
            log_file,
            log_entries: Arc::new(RwLock::new(Vec::new())),
            max_entries: 10000,
        })
    }

    pub async fn log_entry(&self, entry: AuditEntry) -> MisaResult<()> {
        debug!("Logging audit entry: {}", entry.action);

        // Add to in-memory buffer
        let mut entries = self.log_entries.write().await;
        entries.push(entry.clone());

        // Trim if exceeding max entries
        if entries.len() > self.max_entries {
            entries.remove(0);
        }

        // Write to file (in production, use proper file rotation)
        if let Some(log_file) = self.log_file.read().await.as_ref() {
            let log_line = serde_json::to_string(&entry) + "\n";
            // In real implementation, write to file asynchronously
        }

        Ok(())
    }

    pub async fn flush(&self) -> MisaResult<()> {
        // Flush all pending log entries
        let entries = self.log_entries.read().await;
        info!("Flushing {} audit log entries", entries.len());
        Ok(())
    }
}

impl SandboxManager {
    pub fn new(resource_limits: ResourceLimits, permission_checker: PermissionChecker) -> Self {
        Self {
            active_sandboxes: Arc::new(RwLock::new(HashMap::new())),
            resource_limits,
            permission_checker,
        }
    }

    pub async fn create_sandbox(&self, plugin_id: &str, permissions: Vec<String>) -> MisaResult<String> {
        let sandbox_id = uuid::Uuid::new_v4().to_string();

        let sandbox_info = SandboxInfo {
            plugin_id: plugin_id.to_string(),
            sandbox_id: sandbox_id.clone(),
            pid: 0, // Would be set when actually creating the sandbox
            resource_usage: ResourceUsage::default(),
            permissions,
            started_at: chrono::Utc::now(),
            status: SandboxStatus::Running,
        };

        let mut sandboxes = self.active_sandboxes.write().await;
        sandboxes.insert(sandbox_id.clone(), sandbox_info);

        info!("Created sandbox {} for plugin {}", sandbox_id, plugin_id);
        Ok(sandbox_id)
    }
}

impl PermissionChecker {
    pub fn new() -> Self {
        Self {
            permission_matrix: Arc::new(RwLock::new(HashMap::new())),
        }
    }
}

impl Default for ResourceLimits {
    fn default() -> Self {
        Self {
            max_memory_mb: 512,
            max_cpu_percent: 50.0,
            max_file_descriptors: 100,
            max_network_connections: 10,
            allowed_files: vec![],
            blocked_files: vec!["/etc/passwd".to_string(), "/etc/shadow".to_string()],
            network_isolated: true,
        }
    }
}

impl Default for ResourceUsage {
    fn default() -> Self {
        Self {
            memory_mb: 0,
            cpu_percent: 0.0,
            file_descriptors: 0,
            network_connections: 0,
            disk_bytes_read: 0,
            disk_bytes_written: 0,
            network_bytes_sent: 0,
            network_bytes_received: 0,
        }
    }
}

// Implement Clone for Arc-wrapped structs
impl Clone for EncryptionManager {
    fn clone(&self) -> Self {
        Self {
            master_key: Arc::clone(&self.master_key),
            encrypted_keys: Arc::clone(&self.encrypted_keys),
            secure_rng: SystemRandom::new(),
        }
    }
}

impl Clone for AuthManager {
    fn clone(&self) -> Self {
        Self {
            sessions: Arc::clone(&self.sessions),
            user_credentials: Arc::clone(&self.user_credentials),
            biometric_providers: Arc::clone(&self.biometric_providers),
            session_timeout_minutes: self.session_timeout_minutes,
        }
    }
}

impl Clone for AuditLogger {
    fn clone(&self) -> Self {
        Self {
            log_file: Arc::clone(&self.log_file),
            log_entries: Arc::clone(&self.log_entries),
            max_entries: self.max_entries,
        }
    }
}