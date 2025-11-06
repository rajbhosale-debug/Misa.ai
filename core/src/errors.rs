//! MISA.AI Error Handling Module
//!
//! Comprehensive error types for all MISA.AI components

use thiserror::Error;

/// MISA.AI Result type alias
pub type Result<T> = std::result::Result<T, MisaError>;

/// Main MISA.AI error enum
#[derive(Error, Debug)]
pub enum MisaError {
    /// Configuration errors
    #[error("Configuration error: {0}")]
    Configuration(String),

    /// Security-related errors
    #[error("Security error: {0}")]
    Security(String),

    /// Device management errors
    #[error("Device error: {0}")]
    Device(String),

    /// Model/AI inference errors
    #[error("Model error: {0}")]
    Model(String),

    /// Memory/storage errors
    #[error("Memory error: {0}")]
    Memory(String),

    /// Plugin system errors
    #[error("Plugin error: {0}")]
    Plugin(String),

    /// Network/communication errors
    #[error("Network error: {0}")]
    Network(#[from] reqwest::Error),

    /// Database errors
    #[error("Database error: {0}")]
    Database(#[from] sqlx::Error),

    /// I/O errors
    #[error("IO error: {0}")]
    Io(#[from] std::io::Error),

    /// Serialization errors
    #[error("Serialization error: {0}")]
    Serialization(#[from] serde_json::Error),

    /// TOML parsing errors
    #[error("TOML parsing error: {0}")]
    TomlParsing(#[from] toml::de::Error),

    /// UUID generation errors
    #[error("UUID error: {0}")]
    Uuid(#[from] uuid::Error),

    /// Authentication errors
    #[error("Authentication error: {0}")]
    Authentication(String),

    /// Authorization errors
    #[error("Authorization error: {0}")]
    Authorization(String),

    /// Permission errors
    #[error("Permission denied: {0}")]
    Permission(String),

    /// Resource not found errors
    #[error("Resource not found: {0}")]
    NotFound(String),

    /// Validation errors
    #[error("Validation error: {0}")]
    Validation(String),

    /// Timeout errors
    #[error("Operation timed out: {0}")]
    Timeout(String),

    /// Rate limiting errors
    #[error("Rate limit exceeded: {0}")]
    RateLimit(String),

    /// Cryptographic errors
    #[error("Cryptographic error: {0}")]
    Cryptographic(String),

    /// Parsing errors
    #[error("Parse error: {0}")]
    Parse(String),

    /// External service errors
    #[error("External service error: {0}")]
    ExternalService(String),

    /// Consent/privacy errors
    #[error("Privacy error: {0}")]
    Privacy(String),

    /// Compliance errors
    #[error("Compliance error: {0}")]
    Compliance(String),

    /// File system errors
    #[error("File system error: {0}")]
    FileSystem(String),

    /// Audio processing errors
    #[error("Audio processing error: {0}")]
    Audio(String),

    /// Vision/OCR processing errors
    #[error("Vision processing error: {0}")]
    Vision(String),

    /// Task automation errors
    #[error("Task automation error: {0}")]
    TaskAutomation(String),

    /// Workflow errors
    #[error("Workflow error: {0}")]
    Workflow(String),

    /// Remote desktop errors
    #[error("Remote desktop error: {0}")]
    RemoteDesktop(String),

    /// File transfer errors
    #[error("File transfer error: {0}")]
    FileTransfer(String),

    /// Internal errors
    #[error("Internal error: {0}")]
    Internal(String),

    /// Generic error with message
    #[error("{0}")]
    Generic(String),
}

/// Device-specific error codes
#[derive(Error, Debug)]
pub enum DeviceError {
    #[error("Device not found: {device_id}")]
    DeviceNotFound { device_id: String },

    #[error("Device offline: {device_id}")]
    DeviceOffline { device_id: String },

    #[error("Connection failed to device: {device_id}")]
    ConnectionFailed { device_id: String },

    #[error("Device pairing failed: {reason}")]
    PairingFailed { reason: String },

    #[error("Permission denied for device: {device_id}")]
    PermissionDenied { device_id: String },

    #[error("Unsupported operation on device: {device_id}")]
    UnsupportedOperation { device_id: String },

    #[error("Device battery too low: {battery_level}%")]
    LowBattery { battery_level: f32 },

    #[error("Device thermal throttling: {thermal_state}")]
    ThermalThrottling { thermal_state: String },
}

/// Model-specific error codes
#[derive(Error, Debug)]
pub enum ModelError {
    #[error("Model not found: {model_id}")]
    ModelNotFound { model_id: String },

    #[error("Model loading failed: {model_id}")]
    ModelLoadingFailed { model_id: String },

    #[error("Model inference failed: {reason}")]
    InferenceFailed { reason: String },

    #[error("Insufficient resources for model: {model_id}")]
    InsufficientResources { model_id: String },

    #[error("Model context too long: {current_tokens} tokens, max: {max_tokens}")]
    ContextTooLong { current_tokens: u32, max_tokens: u32 },

    #[error("Model not supported: {model_name}")]
    ModelNotSupported { model_name: String },

    #[error("Invalid model parameters: {parameters}")]
    InvalidParameters { parameters: String },

    #[error("Model timeout after {timeout_seconds}s")]
    ModelTimeout { timeout_seconds: u64 },
}

/// Privacy-specific error codes
#[derive(Error, Debug)]
pub enum PrivacyError {
    #[error("Consent required for: {action}")]
    ConsentRequired { action: String },

    #[error("Consent not found: {consent_id}")]
    ConsentNotFound { consent_id: String },

    #[error("Consent expired: {consent_id}")]
    ConsentExpired { consent_id: String },

    #[error("Data access denied: {data_type}")]
    DataAccessDenied { data_type: String },

    #[error("Data deletion failed: {reason}")]
    DataDeletionFailed { reason: String },

    #[error("Data export failed: {reason}")]
    DataExportFailed { reason: String },

    #[error("Privacy filter failed: {filter_id}")]
    FilterFailed { filter_id: String },

    #[error("Anonymization failed: {reason}")]
    AnonymizationFailed { reason: String },

    #[error("Compliance check failed: {regulation}")]
    ComplianceCheckFailed { regulation: String },
}

/// Plugin-specific error codes
#[derive(Error, Debug)]
pub enum PluginError {
    #[error("Plugin not found: {plugin_id}")]
    PluginNotFound { plugin_id: String },

    #[error("Plugin loading failed: {plugin_id}")]
    PluginLoadingFailed { plugin_id: String },

    #[error("Plugin execution failed: {plugin_id}")]
    PluginExecutionFailed { plugin_id: String },

    #[error("Plugin permissions insufficient: {required_permissions:?}")]
    InsufficientPermissions { required_permissions: Vec<String> },

    #[error("Plugin sandbox violation: {violation}")]
    SandboxViolation { violation: String },

    #[error("Plugin incompatible: {reason}")]
    PluginIncompatible { reason: String },

    #[error("Plugin resource limit exceeded: {resource}")]
    ResourceLimitExceeded { resource: String },
}

impl From<DeviceError> for MisaError {
    fn from(err: DeviceError) -> Self {
        MisaError::Device(err.to_string())
    }
}

impl From<ModelError> for MisaError {
    fn from(err: ModelError) -> Self {
        MisaError::Model(err.to_string())
    }
}

impl From<PrivacyError> for MisaError {
    fn from(err: PrivacyError) -> Self {
        MisaError::Privacy(err.to_string())
    }
}

impl From<PluginError> for MisaError {
    fn from(err: PluginError) -> Self {
        MisaError::Plugin(err.to_string())
    }
}

/// Error severity levels
#[derive(Debug, Clone, PartialEq, Eq, PartialOrd, Ord)]
pub enum ErrorSeverity {
    Low,
    Medium,
    High,
    Critical,
}

/// Error context for logging and debugging
#[derive(Debug, Clone)]
pub struct ErrorContext {
    pub error_id: String,
    pub severity: ErrorSeverity,
    pub component: String,
    pub operation: String,
    pub user_id: Option<String>,
    pub device_id: Option<String>,
    pub session_id: Option<String>,
    pub timestamp: chrono::DateTime<chrono::Utc>,
    pub metadata: serde_json::Value,
    pub stack_trace: Option<String>,
}

impl ErrorContext {
    pub fn new(
        component: &str,
        operation: &str,
        severity: ErrorSeverity,
    ) -> Self {
        Self {
            error_id: uuid::Uuid::new_v4().to_string(),
            severity,
            component: component.to_string(),
            operation: operation.to_string(),
            user_id: None,
            device_id: None,
            session_id: None,
            timestamp: chrono::Utc::now(),
            metadata: serde_json::Value::Null,
            stack_trace: None,
        }
    }

    pub fn with_user_id(mut self, user_id: &str) -> Self {
        self.user_id = Some(user_id.to_string());
        self
    }

    pub fn with_device_id(mut self, device_id: &str) -> Self {
        self.device_id = Some(device_id.to_string());
        self
    }

    pub fn with_session_id(mut self, session_id: &str) -> Self {
        self.session_id = Some(session_id.to_string());
        self
    }

    pub fn with_metadata(mut self, metadata: serde_json::Value) -> Self {
        self.metadata = metadata;
        self
    }

    pub fn with_stack_trace(mut self, stack_trace: String) -> Self {
        self.stack_trace = Some(stack_trace);
        self
    }
}

/// Result with error context
pub struct ContextualResult<T> {
    pub result: Result<T>,
    pub context: ErrorContext,
}

impl<T> ContextualResult<T> {
    pub fn new(result: Result<T>, context: ErrorContext) -> Self {
        Self { result, context }
    }

    pub fn map<U, F>(self, f: F) -> ContextualResult<U>
    where
        F: FnOnce(T) -> U,
    {
        ContextualResult {
            result: self.result.map(f),
            context: self.context,
        }
    }

    pub fn and_then<U, F>(self, f: F) -> ContextualResult<U>
    where
        F: FnOnce(T) -> Result<U>,
    {
        ContextualResult {
            result: self.result.and_then(f),
            context: self.context,
        }
    }
}

/// Error reporting and metrics
pub struct ErrorReporter {
    error_counts: std::collections::HashMap<String, u64>,
    recent_errors: Vec<ErrorContext>,
    max_recent_errors: usize,
}

impl ErrorReporter {
    pub fn new(max_recent_errors: usize) -> Self {
        Self {
            error_counts: std::collections::HashMap::new(),
            recent_errors: Vec::new(),
            max_recent_errors,
        }
    }

    pub fn report_error(&mut self, error: &MisaError, context: ErrorContext) {
        // Increment error count
        let error_type = format!("{:?}", std::mem::discriminant(error));
        *self.error_counts.entry(error_type).or_insert(0) += 1;

        // Add to recent errors
        self.recent_errors.push(context);

        // Maintain max size
        if self.recent_errors.len() > self.max_recent_errors {
            self.recent_errors.remove(0);
        }

        // Log the error
        tracing::error!(
            error_id = context.error_id,
            component = context.component,
            operation = context.operation,
            severity = ?context.severity,
            error = %error,
            "MISA.AI error occurred"
        );
    }

    pub fn get_error_counts(&self) -> &std::collections::HashMap<String, u64> {
        &self.error_counts
    }

    pub fn get_recent_errors(&self) -> &[ErrorContext] {
        &self.recent_errors
    }

    pub fn clear_recent_errors(&mut self) {
        self.recent_errors.clear();
    }

    pub fn get_error_rate_per_minute(&self) -> f64 {
        if self.recent_errors.is_empty() {
            return 0.0;
        }

        let now = chrono::Utc::now();
        let one_minute_ago = now - chrono::Duration::minutes(1);

        let recent_count = self.recent_errors.iter()
            .filter(|e| e.timestamp > one_minute_ago)
            .count();

        recent_count as f64
    }
}

impl Default for ErrorReporter {
    fn default() -> Self {
        Self::new(1000)
    }
}

/// Convenience macro for creating contextual errors
#[macro_export]
macro_rules! contextual_error {
    ($error:expr, $component:expr, $operation:expr, $severity:expr) => {
        $crate::errors::ContextualResult::new(
            Err($error),
            $crate::errors::ErrorContext::new($component, $operation, $severity)
        )
    };
    ($error:expr, $component:expr, $operation:expr) => {
        contextual_error!($error, $component, $operation, $crate::errors::ErrorSeverity::Medium)
    };
}

/// Convenience macro for logging and returning errors
#[macro_export]
macro_rules! bail_with_context {
    ($error:expr, $component:expr, $operation:expr $(, $metadata:expr)?) => {
        return Err($crate::errors::MisaError::from($error))
    };
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_error_context_creation() {
        let context = ErrorContext::new("test", "operation", ErrorSeverity::High)
            .with_user_id("user123")
            .with_metadata(serde_json::json!({"key": "value"}));

        assert_eq!(context.component, "test");
        assert_eq!(context.operation, "operation");
        assert_eq!(context.severity, ErrorSeverity::High);
        assert_eq!(context.user_id, Some("user123".to_string()));
    }

    #[test]
    fn test_error_reporter() {
        let mut reporter = ErrorReporter::new(10);
        let context = ErrorContext::new("test", "operation", ErrorSeverity::Medium);

        reporter.report_error(&MisaError::Generic("test error".to_string()), context.clone());

        assert_eq!(reporter.get_error_counts().len(), 1);
        assert_eq!(reporter.get_recent_errors().len(), 1);
        assert!(reporter.get_error_rate_per_minute() >= 0.0);
    }

    #[test]
    fn test_error_conversions() {
        let device_error = DeviceError::DeviceNotFound { device_id: "test".to_string() };
        let misa_error: MisaError = device_error.into();
        assert!(matches!(misa_error, MisaError::Device(_)));
    }
}