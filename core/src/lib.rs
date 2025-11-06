//! MISA.AI Core Library
//!
//! This is the core kernel library for the MISA.AI platform, providing:
//! - AI model orchestration and switching
//! - Device communication and remote control
//! - Security and privacy management
//! - Memory and context management
//! - Plugin system orchestration

pub mod kernel;
pub mod models;
pub mod security;
pub mod device;
pub mod memory;
pub mod privacy;

// Re-export core types for easier use
pub use kernel::{MisaKernel, KernelConfig};
pub use models::{ModelManager, ModelType, ModelCapabilities};
pub use security::{SecurityManager, AuthManager, EncryptionManager};
pub use device::{DeviceManager, RemoteDesktopManager};
pub use memory::{MemoryManager, ContextEngine};
pub use privacy::{PrivacyControls, ConsentManager};

/// Core version information
pub const VERSION: &str = env!("CARGO_PKG_VERSION");

/// Core error types
pub mod errors {
    use thiserror::Error;

    #[derive(Error, Debug)]
    pub enum MisaError {
        #[error("Configuration error: {0}")]
        Config(String),

        #[error("Security error: {0}")]
        Security(String),

        #[error("Model error: {0}")]
        Model(String),

        #[error("Device error: {0}")]
        Device(String),

        #[error("Memory error: {0}")]
        Memory(String),

        #[error("Plugin error: {0}")]
        Plugin(String),

        #[error("Network error: {0}")]
        Network(#[from] reqwest::Error),

        #[error("Database error: {0}")]
        Database(#[from] sqlx::Error),

        #[error("IO error: {0}")]
        Io(#[from] std::io::Error),

        #[error("Serialization error: {0}")]
        Serialization(#[from] serde_json::Error),

        #[error("Encryption error: {0}")]
        Encryption(String),
    }

    pub type Result<T> = std::result::Result<T, MisaError>;
}