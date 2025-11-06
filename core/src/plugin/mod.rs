//! Plugin System Module
//! Provides comprehensive plugin SDK and runtime for extensibility

use std::collections::HashMap;
use std::sync::Arc;
use std::path::PathBuf;
use tokio::sync::RwLock;
use anyhow::{anyhow, Result};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

pub mod sdk;
pub mod runtime;
pub mod registry;
pub mod loader;
pub mod sandbox;
pub mod api;
pub mod events;

pub use sdk::*;
pub use runtime::*;
pub use registry::*;
pub use loader::*;
pub use sandbox::*;
pub use api::*;
pub use events::*;

/// Plugin System Core
/// Manages plugin lifecycle, security, and communication
pub struct PluginSystem {
    registry: Arc<RwLock<PluginRegistry>>,
    runtime: Arc<PluginRuntime>,
    loader: Arc<PluginLoader>,
    api: Arc<PluginAPI>,
    events: Arc<EventBus>,
    config: PluginSystemConfig,
}

impl PluginSystem {
    /// Create new plugin system
    pub fn new(config: PluginSystemConfig) -> Result<Self> {
        let registry = Arc::new(RwLock::new(PluginRegistry::new()));
        let runtime = Arc::new(PluginRuntime::new(config.runtime.clone())?);
        let loader = Arc::new(PluginLoader::new(config.loader.clone())?);
        let api = Arc::new(PluginAPI::new(config.api.clone())?);
        let events = Arc::new(EventBus::new());

        Ok(Self {
            registry,
            runtime,
            loader,
            api,
            events,
            config,
        })
    }

    /// Initialize the plugin system
    pub async fn initialize(&self) -> Result<()> {
        // Initialize all subsystems
        self.registry.write().await.initialize().await?;
        self.runtime.initialize().await?;
        self.loader.initialize().await?;
        self.api.initialize().await?;
        self.events.initialize().await?;

        // Load core plugins
        self.load_core_plugins().await?;

        log::info!("Plugin system initialized successfully");
        Ok(())
    }

    /// Install plugin from file
    pub async fn install_plugin(&self, plugin_path: PathBuf) -> Result<String> {
        // Load plugin metadata
        let metadata = self.loader.load_metadata(&plugin_path).await?;

        // Validate plugin
        self.validate_plugin(&metadata)?;

        // Check for conflicts
        self.check_plugin_conflicts(&metadata).await?;

        // Install plugin
        let plugin_id = self.loader.install_plugin(plugin_path, &metadata).await?;

        // Register plugin
        self.registry.write().await.register_plugin(metadata.clone()).await?;

        // Emit plugin installed event
        self.events.emit_plugin_event(PluginEvent::Installed {
            plugin_id: plugin_id.clone(),
            metadata,
        }).await?;

        Ok(plugin_id)
    }

    /// Uninstall plugin
    pub async fn uninstall_plugin(&self, plugin_id: &str) -> Result<()> {
        // Stop plugin if running
        if self.is_plugin_running(plugin_id).await {
            self.stop_plugin(plugin_id).await?;
        }

        // Get plugin metadata
        let metadata = self.registry.read().await
            .get_plugin_metadata(plugin_id)
            .ok_or_else(|| anyhow!("Plugin not found: {}", plugin_id))?
            .clone();

        // Uninstall plugin
        self.loader.uninstall_plugin(plugin_id).await?;

        // Unregister plugin
        self.registry.write().await.unregister_plugin(plugin_id).await?;

        // Emit plugin uninstalled event
        self.events.emit_plugin_event(PluginEvent::Uninstalled {
            plugin_id: plugin_id.to_string(),
            metadata,
        }).await?;

        Ok(())
    }

    /// Start plugin
    pub async fn start_plugin(&self, plugin_id: &str) -> Result<()> {
        // Check if plugin is already running
        if self.is_plugin_running(plugin_id).await {
            return Ok(());
        }

        // Get plugin metadata
        let metadata = self.registry.read().await
            .get_plugin_metadata(plugin_id)
            .ok_or_else(|| anyhow!("Plugin not found: {}", plugin_id))?
            .clone();

        // Create plugin instance
        let instance = self.runtime.create_plugin_instance(plugin_id, &metadata).await?;

        // Start plugin in sandbox
        let sandbox = self.runtime.create_sandbox(&metadata).await?;
        let handle = sandbox.start_plugin(instance).await?;

        // Register running instance
        self.registry.write().await
            .register_plugin_instance(plugin_id.to_string(), handle).await?;

        // Emit plugin started event
        self.events.emit_plugin_event(PluginEvent::Started {
            plugin_id: plugin_id.to_string(),
            metadata,
        }).await?;

        Ok(())
    }

    /// Stop plugin
    pub async fn stop_plugin(&self, plugin_id: &str) -> Result<()> {
        // Get plugin instance
        let instance = self.registry.write().await
            .get_plugin_instance(plugin_id)
            .ok_or_else(|| anyhow!("Plugin instance not found: {}", plugin_id))?;

        // Stop plugin
        instance.stop().await?;

        // Unregister instance
        self.registry.write().await.unregister_plugin_instance(plugin_id).await?;

        // Get plugin metadata for event
        let metadata = self.registry.read().await
            .get_plugin_metadata(plugin_id)
            .cloned();

        // Emit plugin stopped event
        if let Some(metadata) = metadata {
            self.events.emit_plugin_event(PluginEvent::Stopped {
                plugin_id: plugin_id.to_string(),
                metadata,
            }).await?;
        }

        Ok(())
    }

    /// Enable plugin
    pub async fn enable_plugin(&self, plugin_id: &str) -> Result<()> {
        let metadata = self.registry.read().await
            .get_plugin_metadata(plugin_id)
            .ok_or_else(|| anyhow!("Plugin not found: {}", plugin_id))?;

        if !metadata.enabled {
            self.registry.write().await
                .update_plugin_enabled(plugin_id, true).await?;

            // Auto-start if configured
            if metadata.auto_start {
                self.start_plugin(plugin_id).await?;
            }
        }

        Ok(())
    }

    /// Disable plugin
    pub async fn disable_plugin(&self, plugin_id: &str) -> Result<()> {
        // Stop plugin if running
        if self.is_plugin_running(plugin_id).await {
            self.stop_plugin(plugin_id).await?;
        }

        self.registry.write().await
            .update_plugin_enabled(plugin_id, false).await?;

        Ok(())
    }

    /// Get plugin status
    pub async fn get_plugin_status(&self, plugin_id: &str) -> Result<PluginStatus> {
        let metadata = self.registry.read().await
            .get_plugin_metadata(plugin_id)
            .ok_or_else(|| anyhow!("Plugin not found: {}", plugin_id))?;

        let is_running = self.is_plugin_running(plugin_id).await;

        Ok(PluginStatus {
            plugin_id: plugin_id.to_string(),
            name: metadata.name.clone(),
            version: metadata.version.clone(),
            enabled: metadata.enabled,
            running: is_running,
            auto_start: metadata.auto_start,
            permissions: metadata.permissions.clone(),
            resource_usage: self.get_plugin_resource_usage(plugin_id).await?,
            last_error: None,
        })
    }

    /// List all plugins
    pub async fn list_plugins(&self) -> Result<Vec<PluginInfo>> {
        let registry = self.registry.read().await;
        let mut plugins = Vec::new();

        for metadata in registry.get_all_plugins() {
            let is_running = self.is_plugin_running(&metadata.id).await;
            let resource_usage = self.get_plugin_resource_usage(&metadata.id).await.ok();

            plugins.push(PluginInfo {
                metadata: metadata.clone(),
                status: if is_running {
                    PluginState::Running
                } else if metadata.enabled {
                    PluginState::Stopped
                } else {
                    PluginState::Disabled
                },
                resource_usage,
            });
        }

        Ok(plugins)
    }

    /// Execute plugin command
    pub async fn execute_plugin_command(
        &self,
        plugin_id: &str,
        command: &str,
        args: HashMap<String, serde_json::Value>
    ) -> Result<serde_json::Value> {
        // Get plugin instance
        let instance = self.registry.read().await
            .get_plugin_instance(plugin_id)
            .ok_or_else(|| anyhow!("Plugin not running: {}", plugin_id))?;

        // Execute command
        instance.execute_command(command, args).await
    }

    /// Send message to plugin
    pub async fn send_plugin_message(
        &self,
        plugin_id: &str,
        message: PluginMessage
    ) -> Result<()> {
        let instance = self.registry.read().await
            .get_plugin_instance(plugin_id)
            .ok_or_else(|| anyhow!("Plugin not running: {}", plugin_id))?;

        instance.handle_message(message).await
    }

    /// Update plugin
    pub async fn update_plugin(&self, plugin_id: &str, new_version: &str) -> Result<()> {
        // Stop plugin
        if self.is_plugin_running(plugin_id).await {
            self.stop_plugin(plugin_id).await?;
        }

        // Get current metadata
        let current_metadata = self.registry.read().await
            .get_plugin_metadata(plugin_id)
            .ok_or_else(|| anyhow!("Plugin not found: {}", plugin_id))?
            .clone();

        // Download and install update
        let update_path = self.loader.download_plugin_update(plugin_id, new_version).await?;
        self.install_plugin(update_path).await?;

        // Clean up old version
        self.loader.cleanup_plugin_version(plugin_id, &current_metadata.version).await?;

        Ok(())
    }

    /// Get plugin logs
    pub async fn get_plugin_logs(&self, plugin_id: &str, limit: Option<usize>) -> Result<Vec<PluginLogEntry>> {
        self.runtime.get_plugin_logs(plugin_id, limit).await
    }

    /// Get plugin metrics
    pub async fn get_plugin_metrics(&self, plugin_id: &str) -> Result<PluginMetrics> {
        self.runtime.get_plugin_metrics(plugin_id).await
    }

    // Private helper methods

    /// Load core plugins
    async fn load_core_plugins(&self) -> Result<()> {
        for core_plugin in &self.config.core_plugins {
            let plugin_path = PathBuf::from(&core_plugin.path);
            match self.install_plugin(plugin_path).await {
                Ok(plugin_id) => {
                    if core_plugin.auto_start {
                        if let Err(e) = self.start_plugin(&plugin_id).await {
                            log::warn!("Failed to start core plugin {}: {}", plugin_id, e);
                        }
                    }
                }
                Err(e) => {
                    log::error!("Failed to load core plugin {}: {}", core_plugin.name, e);
                }
            }
        }
        Ok(())
    }

    /// Validate plugin
    fn validate_plugin(&self, metadata: &PluginMetadata) -> Result<()> {
        // Check required fields
        if metadata.name.is_empty() {
            return Err(anyhow!("Plugin name is required"));
        }

        if metadata.version.is_empty() {
            return Err(anyhow!("Plugin version is required"));
        }

        // Check API version compatibility
        if !self.is_api_version_compatible(&metadata.api_version) {
            return Err(anyhow!(
                "Plugin API version {} is not compatible with system API version {}",
                metadata.api_version,
                self.config.api.version
            ));
        }

        // Check permissions
        for permission in &metadata.permissions {
            if !self.config.allowed_permissions.contains(permission) {
                return Err(anyhow!("Plugin requests unsupported permission: {}", permission));
            }
        }

        Ok(())
    }

    /// Check for plugin conflicts
    async fn check_plugin_conflicts(&self, metadata: &PluginMetadata) -> Result<()> {
        let registry = self.registry.read().await;

        for existing_metadata in registry.get_all_plugins() {
            // Check for ID conflicts
            if existing_metadata.id == metadata.id {
                return Err(anyhow!("Plugin with ID '{}' already exists", metadata.id));
            }

            // Check for name conflicts (if same author)
            if existing_metadata.name == metadata.name && existing_metadata.author == metadata.author {
                return Err(anyhow!("Plugin with name '{}' by author '{}' already exists", metadata.name, metadata.author));
            }

            // Check for resource conflicts
            for resource in &metadata.resources {
                if existing_metadata.resources.contains(resource) {
                    return Err(anyhow!("Plugin resource '{}' conflicts with existing plugin", resource));
                }
            }
        }

        Ok(())
    }

    /// Check if API version is compatible
    fn is_api_version_compatible(&self, plugin_api_version: &str) -> bool {
        // Simple semantic version compatibility check
        // In a real implementation, this would be more sophisticated
        plugin_api_version.starts_with("1.")
    }

    /// Check if plugin is running
    async fn is_plugin_running(&self, plugin_id: &str) -> bool {
        self.registry.read().await.get_plugin_instance(plugin_id).is_some()
    }

    /// Get plugin resource usage
    async fn get_plugin_resource_usage(&self, plugin_id: &str) -> Result<ResourceUsage> {
        if let Some(instance) = self.registry.read().await.get_plugin_instance(plugin_id) {
            instance.get_resource_usage().await
        } else {
            Ok(ResourceUsage::default())
        }
    }
}

/// Plugin system configuration
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PluginSystemConfig {
    pub runtime: RuntimeConfig,
    pub loader: LoaderConfig,
    pub api: APIConfig,
    pub allowed_permissions: Vec<String>,
    pub core_plugins: Vec<CorePlugin>,
    pub security: SecurityConfig,
}

impl Default for PluginSystemConfig {
    fn default() -> Self {
        Self {
            runtime: RuntimeConfig::default(),
            loader: LoaderConfig::default(),
            api: APIConfig::default(),
            allowed_permissions: vec![
                "file.read".to_string(),
                "file.write".to_string(),
                "network.request".to_string(),
                "system.notify".to_string(),
                "ui.window".to_string(),
            ],
            core_plugins: vec![],
            security: SecurityConfig::default(),
        }
    }
}

/// Core plugin configuration
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CorePlugin {
    pub name: String,
    pub path: String,
    pub auto_start: bool,
}

/// Plugin status information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PluginStatus {
    pub plugin_id: String,
    pub name: String,
    pub version: String,
    pub enabled: bool,
    pub running: bool,
    pub auto_start: bool,
    pub permissions: Vec<String>,
    pub resource_usage: ResourceUsage,
    pub last_error: Option<String>,
}

/// Plugin information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PluginInfo {
    pub metadata: PluginMetadata,
    pub status: PluginState,
    pub resource_usage: Option<ResourceUsage>,
}

/// Plugin states
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum PluginState {
    Installed,
    Running,
    Stopped,
    Disabled,
    Error,
}

/// Resource usage
#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct ResourceUsage {
    pub memory_usage_mb: f64,
    pub cpu_usage_percent: f64,
    pub network_bytes_sent: u64,
    pub network_bytes_received: u64,
    pub disk_read_bytes: u64,
    pub disk_write_bytes: u64,
}

/// Plugin metrics
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PluginMetrics {
    pub uptime: std::time::Duration,
    pub commands_executed: u64,
    pub messages_sent: u64,
    pub messages_received: u64,
    pub errors_count: u64,
    pub last_activity: std::time::SystemTime,
}

/// Plugin log entry
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PluginLogEntry {
    pub timestamp: std::time::SystemTime,
    pub level: LogLevel,
    pub message: String,
    pub context: HashMap<String, String>,
}

/// Log levels
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum LogLevel {
    Trace,
    Debug,
    Info,
    Warn,
    Error,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[tokio::test]
    async fn test_plugin_system_creation() {
        let config = PluginSystemConfig::default();
        let plugin_system = PluginSystem::new(config);
        assert!(plugin_system.is_ok());
    }

    #[tokio::test]
    async fn test_plugin_validation() {
        let config = PluginSystemConfig::default();
        let plugin_system = PluginSystem::new(config).unwrap();

        let valid_metadata = PluginMetadata {
            id: "test-plugin".to_string(),
            name: "Test Plugin".to_string(),
            version: "1.0.0".to_string(),
            api_version: "1.0.0".to_string(),
            ..Default::default()
        };

        assert!(plugin_system.validate_plugin(&valid_metadata).is_ok());

        let invalid_metadata = PluginMetadata {
            id: "".to_string(),
            name: "".to_string(),
            version: "".to_string(),
            api_version: "1.0.0".to_string(),
            ..Default::default()
        };

        assert!(plugin_system.validate_plugin(&invalid_metadata).is_err());
    }
}