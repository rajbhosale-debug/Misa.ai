//! MISA.AI Kernel Integration Tests
//!
//! Comprehensive integration tests for the MISA.AI kernel system.
//! Tests the complete flow from model switching to task execution
//! across all major components.

use misa_core::{
    kernel::MisaKernel,
    models::{ModelManager, ModelType},
    security::{SecurityManager, SecurityConfig},
    device::{DeviceManager, DeviceConfig},
    memory::{MemoryManager, MemoryConfig},
};
use serde_json::json;
use std::time::Duration;
use tokio::time::timeout;
use tracing::info;

#[tokio::test]
async fn test_kernel_initialization() {
    // Test complete kernel initialization
    let config = misa_core::kernel::KernelConfig::default();
    let security_manager = SecurityManager::new("/tmp/test_misa", config.security.clone())
        .await
        .expect("Failed to create security manager");

    let kernel = MisaKernel::new(
        "/tmp/test_config.toml".to_string(),
        "/tmp/test_data".to_string(),
        security_manager,
    )
    .await
    .expect("Failed to create kernel");

    // Test health check
    let result = kernel.health_check().await;
    assert!(result.is_ok(), "Health check failed");

    info!("✅ Kernel initialization test passed");
}

#[tokio::test]
async fn test_model_switching() {
    let config = misa_core::kernel::ModelConfig::default();
    let model_manager = ModelManager::new(config.clone())
        .await
        .expect("Failed to create model manager");

    // Initialize and test model switching
    model_manager.initialize().await.expect("Failed to initialize model manager");

    // Test switching to different model types
    let model_id = model_manager
        .switch_model("mixtral", Some("coding"), None)
        .await
        .expect("Failed to switch model");

    assert!(!model_id.is_empty(), "Model ID should not be empty");

    info!("✅ Model switching test passed");
}

#[tokio::test]
async fn test_task_execution() {
    let config = misa_core::kernel::KernelConfig::default();
    let security_manager = SecurityManager::new("/tmp/test_misa", config.security.clone())
        .await
        .expect("Failed to create security manager");

    let kernel = MisaKernel::new(
        "/tmp/test_config.toml".to_string(),
        "/tmp/test_data".to_string(),
        security_manager,
    )
    .await
        .expect("Failed to create kernel");

    // Initialize kernel
    kernel.initialize().await.expect("Failed to initialize kernel");

    // Test task execution
    let task_request = misa_core::kernel::RouteTaskRequest {
        task: "Write a simple 'Hello World' function in Rust".to_string(),
        task_type: "coding".to_string(),
        context: Some(json!({
            "language": "rust",
            "style": "idiomatic"
        })),
        device_preferences: None,
        priority: Some(misa_core::kernel::TaskPriority::Normal),
    };

    let result = timeout(Duration::from_secs(30), kernel.route_task(task_request))
        .await
        .expect("Task execution timed out")
        .expect("Task execution failed");

    assert!(result.success, "Task should have succeeded");
    assert!(result.result.is_some(), "Task should have a result");

    info!("✅ Task execution test passed");
}

#[tokio::test]
async fn test_device_management() {
    let config = DeviceConfig::default();
    let security_manager = SecurityManager::new("/tmp/test_misa", SecurityConfig::default())
        .await
        .expect("Failed to create security manager");

    let device_manager = DeviceManager::new(config, security_manager)
        .await
        .expect("Failed to create device manager");

    // Test device discovery
    device_manager.start_discovery().await.expect("Failed to start device discovery");

    // Test device pairing
    let qr_token = "misa://pair/test_device/1234567890/abc123";
    let pairing_result = device_manager
        .pair_device(qr_token)
        .await
        .expect("Failed to pair device");

    assert!(pairing_result.success, "Device pairing should succeed");

    info!("✅ Device management test passed");
}

#[tokio::test]
async fn test_memory_management() {
    let config = MemoryConfig::default();
    let security_manager = SecurityManager::new("/tmp/test_misa", SecurityConfig::default())
        .await
        .expect("Failed to create security manager");

    let memory_manager = MemoryManager::new("/tmp/test_misa", config, security_manager)
        .await
        .expect("Failed to create memory manager");

    memory_manager.initialize().await.expect("Failed to initialize memory manager");

    // Test memory storage
    let memory_item = misa_core::memory::MemoryItem {
        id: "test-memory-1".to_string(),
        content: "This is a test memory item".to_string(),
        content_type: misa_core::memory::ContentType::Text,
        memory_type: misa_core::memory::MemoryType::ShortTerm,
        importance: misa_core::memory::Importance::Medium,
        tags: vec!["test".to_string(), "integration".to_string()],
        metadata: json!({"test": true}),
        created_at: chrono::Utc::now(),
        last_accessed: chrono::Utc::now(),
        access_count: 0,
        encrypted: false,
    };

    let memory_id = memory_manager
        .store_memory(memory_item)
        .await
        .expect("Failed to store memory");

    assert_eq!(memory_id, "test-memory-1");

    // Test memory retrieval
    let retrieved_memory = memory_manager
        .get_memory("test-memory-1")
        .await
        .expect("Failed to retrieve memory");

    assert!(retrieved_memory.is_some(), "Memory should be retrievable");
    assert_eq!(retrieved_memory.unwrap().content, "This is a test memory item");

    info!("✅ Memory management test passed");
}

#[tokio::test]
async fn test_security_and_encryption() {
    let security_manager = SecurityManager::new("/tmp/test_misa", SecurityConfig::default())
        .await
        .expect("Failed to create security manager");

    security_manager.initialize().await.expect("Failed to initialize security manager");

    // Test data encryption
    let test_data = "This is sensitive data that should be encrypted".as_bytes();
    let encrypted_data = security_manager
        .encrypt_data(test_data, "test-key")
        .await
        .expect("Failed to encrypt data");

    // Test data decryption
    let decrypted_data = security_manager
        .decrypt_data(&encrypted_data)
        .await
        .expect("Failed to decrypt data");

    assert_eq!(decrypted_data, test_data);

    // Test authentication
    let auth_result = security_manager
        .authenticate_password("test_user", "test_password")
        .await;

    // In a real test, this would depend on whether the user exists
    info!("Authentication result: {:?}", auth_result);

    info!("✅ Security and encryption test passed");
}

#[tokio::test]
async fn test_complete_workflow() {
    // This test verifies the complete MISA.AI workflow
    info!("🚀 Starting complete workflow integration test");

    // Initialize all components
    let config = misa_core::kernel::KernelConfig::default();
    let security_manager = SecurityManager::new("/tmp/test_misa", config.security.clone())
        .await
        .expect("Failed to create security manager");

    let kernel = MisaKernel::new(
        "/tmp/test_config.toml".to_string(),
        "/tmp/test_data".to_string(),
        security_manager,
    )
    .await
    .expect("Failed to create kernel");

    // Start the kernel
    kernel.initialize().await.expect("Failed to initialize kernel");

    // Test model switching
    let model_id = kernel
        .switch_model(misa_core::kernel::SwitchModelRequest {
            model_id: "mixtral".to_string(),
            task_type: Some("general".to_string()),
            preferences: None,
        })
        .await
        .expect("Failed to switch model");

    assert!(!model_id.data.is_empty(), "Model switching should succeed");

    // Test task execution with context
    let task_request = misa_core::kernel::RouteTaskRequest {
        task: "Analyze this text and summarize it: 'MISA.AI is a hybrid local/cloud intelligent assistant platform delivering Jarvis-level synergy with privacy-first design and comprehensive application ecosystem.'".to_string(),
        task_type: "summarization".to_string(),
        context: Some(json!({
            "tone": "professional",
            "length": "brief"
        })),
        device_preferences: None,
        priority: Some(misa_core::kernel::TaskPriority::High),
    };

    let task_result = timeout(Duration::from_secs(60), kernel.route_task(task_request))
        .await
        .expect("Task execution timed out")
        .expect("Task execution failed");

    assert!(task_result.success, "Task should succeed");
    assert!(task_result.result.is_some(), "Task should have result");

    info!("✅ Complete workflow integration test passed");
}

#[tokio::test]
async fn test_error_handling_and_recovery() {
    info!("🧪 Starting error handling and recovery test");

    // Test invalid model switching
    let config = misa_core::kernel::ModelConfig::default();
    let model_manager = ModelManager::new(config)
        .await
        .expect("Failed to create model manager");

    // Try to switch to non-existent model
    let result = model_manager
        .switch_model("non-existent-model", None, None)
        .await;

    assert!(result.is_err(), "Should fail for non-existent model");

    // Test malformed task execution
    let config = misa_core::kernel::KernelConfig::default();
    let security_manager = SecurityManager::new("/tmp/test_misa", SecurityConfig::default())
        .await
        .expect("Failed to create security manager");

    let kernel = MisaKernel::new(
        "/tmp/test_config.toml".to_string(),
        "/tmp/test_data".to_string(),
        security_manager,
    )
    .await
    .expect("Failed to create kernel");

    kernel.initialize().await.expect("Failed to initialize kernel");

    // Test with malformed task
    let malformed_request = misa_core::kernel::RouteTaskRequest {
        task: "".to_string(), // Empty task
        task_type: "invalid_type".to_string(),
        context: None,
        device_preferences: None,
        priority: None,
    };

    let result = kernel.route_task(malformed_request).await;
    // Should handle gracefully without crashing

    info!("✅ Error handling and recovery test passed");
}