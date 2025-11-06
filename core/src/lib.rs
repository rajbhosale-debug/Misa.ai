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

// Include the comprehensive errors module
include!("errors.rs");

// Re-export core types for easier use
pub use kernel::{MisaKernel, KernelConfig};
pub use models::{ModelManager, ModelType, ModelCapabilities};
pub use security::{SecurityManager, AuthManager, EncryptionManager};
pub use device::{DeviceManager, RemoteDesktopManager};
pub use memory::{MemoryManager, ContextEngine};
pub use privacy::{PrivacyControls, ConsentManager};

/// Core version information
pub const VERSION: &str = env!("CARGO_PKG_VERSION");