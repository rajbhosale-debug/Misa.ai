//! AI Model Management System
//!
//! Handles model orchestration, switching, and execution across:
//! - Local Ollama models (Mixtral, CodeLlama, etc.)
//! - Cloud APIs (OpenAI, Claude, Gemini)
//! - Vision models for image analysis
//! - Voice models for speech recognition and synthesis
//! - Automatic model switching based on task requirements

use anyhow::Result;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;
use tracing::{info, warn, error};

use crate::kernel::{ModelConfig, ModelSwitchingPreferences, TaskPriority};
use crate::errors::{MisaError, Result as MisaResult};

/// Model manager for orchestrating AI models
pub struct ModelManager {
    config: ModelConfig,
    local_models: Arc<RwLock<HashMap<String, LocalModel>>>,
    cloud_models: Arc<RwLock<HashMap<String, CloudModel>>>,
    current_model: Arc<RwLock<String>>,
    performance_metrics: Arc<RwLock<HashMap<String, ModelPerformance>>>,
    ollama_client: OllamaClient,
    cloud_clients: Arc<RwLock<HashMap<String, CloudClient>>>,
}

/// Local model information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LocalModel {
    pub id: String,
    pub name: String,
    pub model_type: ModelType,
    pub capabilities: ModelCapabilities,
    pub size_gb: f32,
    pub quantization: String,
    pub parameters: String,
    pub device_preference: DevicePreference,
    pub loaded: bool,
}

/// Cloud model information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CloudModel {
    pub id: String,
    pub name: String,
    pub provider: String,
    pub model_type: ModelType,
    pub capabilities: ModelCapabilities,
    pub cost_per_million_tokens: f32,
    pub context_length: usize,
    pub max_tokens_per_minute: u32,
}

/// Model type enumeration
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub enum ModelType {
    Chat,
    Coding,
    Vision,
    Reasoning,
    Summarization,
    SpeechToText,
    TextToSpeech,
    Embedding,
    Multimodal,
}

/// Model capabilities
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModelCapabilities {
    pub supports_functions: bool,
    pub supports_vision: bool,
    pub supports_streaming: bool,
    pub max_context_length: usize,
    pub supports_system_prompts: bool,
    pub supports_json_mode: bool,
    pub languages: Vec<String>,
    pub specialties: Vec<String>,
}

/// Device preference for model execution
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum DevicePreference {
    Cpu,
    Gpu,
    Neuromorphic,
    Hybrid,
}

/// Model performance metrics
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ModelPerformance {
    pub avg_response_time_ms: f64,
    pub success_rate: f32,
    pub tokens_per_second: f32,
    pub memory_usage_mb: u64,
    pub energy_efficiency: f32,
    pub last_used: chrono::DateTime<chrono::Utc>,
    pub total_requests: u64,
}

/// Ollama client for local models
pub struct OllamaClient {
    base_url: String,
    client: reqwest::Client,
}

/// Cloud client abstraction
pub struct CloudClient {
    provider: String,
    api_key: String,
    base_url: String,
    client: reqwest::Client,
}

/// Model execution request
#[derive(Debug, Deserialize)]
pub struct ModelRequest {
    pub prompt: String,
    pub model_id: Option<String>,
    pub context: Option<serde_json::Value>,
    pub stream: bool,
    pub max_tokens: Option<u32>,
    pub temperature: Option<f32>,
    pub tools: Option<Vec<serde_json::Value>>,
}

/// Model execution response
#[derive(Debug, Serialize)]
pub struct ModelResponse {
    pub content: String,
    pub model_id: String,
    pub tokens_used: u32,
    pub response_time_ms: u64,
    pub finish_reason: String,
    pub metadata: serde_json::Value,
}

impl ModelManager {
    /// Create a new model manager
    pub async fn new(config: ModelConfig) -> MisaResult<Self> {
        let ollama_client = OllamaClient::new(config.local_server_url.clone());

        let mut cloud_clients = HashMap::new();
        for (provider, provider_config) in &config.cloud_providers {
            cloud_clients.insert(
                provider.clone(),
                CloudClient::new(provider.clone(), provider_config.clone()),
            );
        }

        let manager = Self {
            config: config.clone(),
            local_models: Arc::new(RwLock::new(HashMap::new())),
            cloud_models: Arc::new(RwLock::new(HashMap::new())),
            current_model: Arc::new(RwLock::new(config.default_model)),
            performance_metrics: Arc::new(RwLock::new(HashMap::new())),
            ollama_client,
            cloud_clients: Arc::new(RwLock::new(cloud_clients)),
        };

        // Initialize model catalogs
        manager.discover_local_models().await?;
        manager.register_cloud_models().await?;

        Ok(manager)
    }

    /// Initialize the model manager
    pub async fn initialize(&self) -> MisaResult<()> {
        info!("Initializing model manager");

        // Load default model
        let default_model = self.config.default_model.clone();
        if let Err(e) = self.switch_model(&default_model, None, None).await {
            warn!("Failed to load default model {}: {}", default_model, e);
        }

        info!("Model manager initialized");
        Ok(())
    }

    /// Discover available local models via Ollama
    async fn discover_local_models(&self) -> MisaResult<()> {
        info!("Discovering local models via Ollama");

        match self.ollama_client.list_models().await {
            Ok(models) => {
                let mut local_models = self.local_models.write().await;
                for model_info in models {
                    let local_model = LocalModel {
                        id: model_info.name.clone(),
                        name: model_info.name,
                        model_type: self.classify_model_type(&model_info.name),
                        capabilities: self.infer_model_capabilities(&model_info.name),
                        size_gb: model_info.size as f32 / 1024.0, // Convert bytes to GB
                        quantization: "Q4_0".to_string(), // Default assumption
                        parameters: "unknown".to_string(),
                        device_preference: DevicePreference::Hybrid,
                        loaded: false,
                    };

                    local_models.insert(model_info.name, local_model);
                }
                info!("Discovered {} local models", local_models.len());
            }
            Err(e) => {
                warn!("Failed to discover local models: {}", e);
            }
        }

        Ok(())
    }

    /// Register cloud model configurations
    async fn register_cloud_models(&self) -> MisaResult<()> {
        info!("Registering cloud models");

        let mut cloud_models = self.cloud_models.write().await;

        // Register OpenAI models
        if let Some(openai_config) = self.config.cloud_providers.get("openai") {
            for model_name in &openai_config.models {
                let cloud_model = CloudModel {
                    id: model_name.clone(),
                    name: model_name.clone(),
                    provider: "openai".to_string(),
                    model_type: self.classify_model_type(model_name),
                    capabilities: self.get_cloud_model_capabilities("openai", model_name),
                    cost_per_million_tokens: self.get_model_cost("openai", model_name),
                    context_length: self.get_model_context_length("openai", model_name),
                    max_tokens_per_minute: 3000,
                };
                cloud_models.insert(format!("openai:{}", model_name), cloud_model);
            }
        }

        info!("Registered {} cloud models", cloud_models.len());
        Ok(())
    }

    /// Switch to a different model
    pub async fn switch_model(
        &self,
        model_id: &str,
        task_type: Option<&str>,
        preferences: Option<&ModelSwitchingPreferences>,
    ) -> MisaResult<String> {
        info!("Switching to model: {}", model_id);

        // Load the model if it's local
        if self.is_local_model(model_id) {
            self.load_local_model(model_id).await?;
        }

        // Update current model
        let mut current = self.current_model.write().await;
        *current = model_id.to_string();

        info!("Switched to model: {}", model_id);
        Ok(model_id.to_string())
    }

    /// Select optimal model for a given task
    pub async fn select_model_for_task(
        &self,
        task_type: &str,
        device_preferences: Option<&[String]>,
        priority: &TaskPriority,
    ) -> MisaResult<String> {
        let model_type = self.task_type_to_enum(task_type);

        // Get candidate models
        let candidates = self.get_models_by_type(&model_type).await?;

        if candidates.is_empty() {
            return Err(MisaError::Model(format!("No models available for task type: {}", task_type)));
        }

        // Select best model based on criteria
        let best_model = self.rank_models_for_task(candidates, device_preferences, priority).await?;

        Ok(best_model)
    }

    /// Execute a task on the specified model
    pub async fn execute_task(
        &self,
        task: &str,
        model_id: &str,
        context: Option<&serde_json::Value>,
    ) -> MisaResult<serde_json::Value> {
        let start_time = std::time::Instant::now();

        let request = ModelRequest {
            prompt: task.to_string(),
            model_id: Some(model_id.to_string()),
            context: context.cloned(),
            stream: false,
            max_tokens: None,
            temperature: None,
            tools: None,
        };

        let response = if self.is_local_model(model_id) {
            self.execute_local_model(request).await?
        } else {
            self.execute_cloud_model(request).await?
        };

        let execution_time = start_time.elapsed().as_millis() as u64;

        // Update performance metrics
        self.update_performance_metrics(model_id, execution_time, true).await;

        Ok(serde_json::to_value(response)?)
    }

    /// Shutdown the model manager
    pub async fn shutdown(&self) -> MisaResult<()> {
        info!("Shutting down model manager");

        // Unload all local models
        let local_models = self.local_models.read().await;
        for model in local_models.values() {
            if model.loaded {
                if let Err(e) = self.unload_local_model(&model.id).await {
                    warn!("Failed to unload model {}: {}", model.id, e);
                }
            }
        }

        info!("Model manager shut down");
        Ok(())
    }

    /// Helper methods

    fn classify_model_type(&self, model_name: &str) -> ModelType {
        let name_lower = model_name.to_lowercase();

        if name_lower.contains("codellama") || name_lower.contains("wizardcoder") {
            ModelType::Coding
        } else if name_lower.contains("vision") || name_lower.contains("clip") {
            ModelType::Vision
        } else if name_lower.contains("mixtral") || name_lower.contains("llama") {
            ModelType::Chat
        } else if name_lower.contains("embedding") {
            ModelType::Embedding
        } else {
            ModelType::Chat
        }
    }

    fn infer_model_capabilities(&self, model_name: &str) -> ModelCapabilities {
        let name_lower = model_name.to_lowercase();

        ModelCapabilities {
            supports_functions: name_lower.contains("mixtral") || name_lower.contains("gpt-4"),
            supports_vision: name_lower.contains("vision") || name_lower.contains("multimodal"),
            supports_streaming: true,
            max_context_length: if name_lower.contains("32k") { 32768 } else { 4096 },
            supports_system_prompts: true,
            supports_json_mode: name_lower.contains("gpt-4"),
            languages: vec!["en".to_string()],
            specialties: if name_lower.contains("codellama") {
                vec!["coding".to_string(), "technical".to_string()]
            } else {
                vec!["general".to_string()]
            },
        }
    }

    fn get_cloud_model_capabilities(&self, provider: &str, model: &str) -> ModelCapabilities {
        match (provider, model) {
            ("openai", "gpt-4") => ModelCapabilities {
                supports_functions: true,
                supports_vision: model.contains("vision"),
                supports_streaming: true,
                max_context_length: 8192,
                supports_system_prompts: true,
                supports_json_mode: true,
                languages: vec!["en".to_string(), "zh".to_string(), "es".to_string()],
                specialties: vec!["reasoning".to_string(), "coding".to_string()],
            },
            _ => self.infer_model_capabilities(model),
        }
    }

    fn get_model_cost(&self, provider: &str, model: &str) -> f32 {
        match (provider, model) {
            ("openai", "gpt-4") => 30.0,
            ("openai", "gpt-3.5-turbo") => 2.0,
            _ => 5.0, // Default cost
        }
    }

    fn get_model_context_length(&self, provider: &str, model: &str) -> usize {
        match (provider, model) {
            ("openai", "gpt-4") => 8192,
            ("openai", "gpt-3.5-turbo") => 4096,
            _ => 4096,
        }
    }

    fn task_type_to_enum(&self, task_type: &str) -> ModelType {
        match task_type.to_lowercase().as_str() {
            "coding" => ModelType::Coding,
            "vision" => ModelType::Vision,
            "reasoning" => ModelType::Reasoning,
            "summarization" => ModelType::Summarization,
            "speech" => ModelType::SpeechToText,
            "tts" => ModelType::TextToSpeech,
            _ => ModelType::Chat,
        }
    }

    async fn get_models_by_type(&self, model_type: &ModelType) -> MisaResult<Vec<String>> {
        let mut models = Vec::new();

        // Add local models
        let local_models = self.local_models.read().await;
        for (id, model) in local_models.iter() {
            if model.model_type == *model_type {
                models.push(id.clone());
            }
        }

        // Add cloud models
        let cloud_models = self.cloud_models.read().await;
        for (id, model) in cloud_models.iter() {
            if model.model_type == *model_type {
                models.push(id.clone());
            }
        }

        Ok(models)
    }

    async fn rank_models_for_task(
        &self,
        candidates: Vec<String>,
        device_preferences: Option<&[String]>,
        priority: &TaskPriority,
    ) -> MisaResult<String> {
        if candidates.is_empty() {
            return Err(MisaError::Model("No candidate models available".to_string()));
        }

        // Simple ranking: prefer local models first, then by performance
        let mut scored_models = Vec::new();

        for candidate in candidates {
            let mut score = 0.0;

            // Prefer local models if configured
            if self.is_local_model(&candidate) && self.config.switching_preferences.prefer_local {
                score += 10.0;
            }

            // Add performance score
            if let Some(metrics) = self.get_performance_metrics(&candidate).await {
                score += metrics.success_rate as f64 * 5.0;
                score += 1000.0 / (metrics.avg_response_time_ms + 1.0);
            }

            scored_models.push((candidate, score));
        }

        // Sort by score (descending)
        scored_models.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap());

        Ok(scored_models[0].0.clone())
    }

    fn is_local_model(&self, model_id: &str) -> bool {
        !model_id.contains(':')
    }

    async fn load_local_model(&self, model_id: &str) -> MisaResult<()> {
        info!("Loading local model: {}", model_id);

        match self.ollama_client.pull_model(model_id).await {
            Ok(_) => {
                // Update model status
                let mut local_models = self.local_models.write().await;
                if let Some(model) = local_models.get_mut(model_id) {
                    model.loaded = true;
                }
                info!("Local model loaded: {}", model_id);
                Ok(())
            }
            Err(e) => {
                error!("Failed to load local model {}: {}", model_id, e);
                Err(MisaError::Model(format!("Failed to load model {}: {}", model_id, e)))
            }
        }
    }

    async fn unload_local_model(&self, model_id: &str) -> MisaResult<()> {
        info!("Unloading local model: {}", model_id);

        let mut local_models = self.local_models.write().await;
        if let Some(model) = local_models.get_mut(model_id) {
            model.loaded = false;
        }

        Ok(())
    }

    async fn execute_local_model(&self, request: ModelRequest) -> MisaResult<ModelResponse> {
        let model_id = request.model_id.as_ref().unwrap();
        self.ollama_client.generate_response(request).await
    }

    async fn execute_cloud_model(&self, request: ModelRequest) -> MisaResult<ModelResponse> {
        let model_id = request.model_id.as_ref().unwrap();

        if let Some((provider, model_name)) = model_id.split_once(':') {
            let cloud_clients = self.cloud_clients.read().await;
            if let Some(client) = cloud_clients.get(provider) {
                return client.generate_response(model_name, request).await;
            }
        }

        Err(MisaError::Model(format!("Unknown cloud model: {}", model_id)))
    }

    async fn get_performance_metrics(&self, model_id: &str) -> Option<ModelPerformance> {
        let metrics = self.performance_metrics.read().await;
        metrics.get(model_id).cloned()
    }

    async fn update_performance_metrics(&self, model_id: &str, response_time_ms: u64, success: bool) {
        let mut metrics = self.performance_metrics.write().await;
        let entry = metrics.entry(model_id.to_string()).or_insert_with(|| ModelPerformance {
            avg_response_time_ms: response_time_ms as f64,
            success_rate: if success { 1.0 } else { 0.0 },
            tokens_per_second: 0.0,
            memory_usage_mb: 0,
            energy_efficiency: 1.0,
            last_used: chrono::Utc::now(),
            total_requests: 0,
        });

        entry.total_requests += 1;
        entry.last_used = chrono::Utc::now();

        // Update rolling averages
        let alpha = 0.1; // Smoothing factor
        entry.avg_response_time_ms = alpha * response_time_ms as f64 + (1.0 - alpha) * entry.avg_response_time_ms;
        entry.success_rate = alpha * if success { 1.0 } else { 0.0 } + (1.0 - alpha) * entry.success_rate;
    }
}

// Implement Clone for required types
impl Clone for ModelManager {
    fn clone(&self) -> Self {
        Self {
            config: self.config.clone(),
            local_models: Arc::clone(&self.local_models),
            cloud_models: Arc::clone(&self.cloud_models),
            current_model: Arc::clone(&self.current_model),
            performance_metrics: Arc::clone(&self.performance_metrics),
            ollama_client: OllamaClient::new(self.config.local_server_url.clone()),
            cloud_clients: Arc::clone(&self.cloud_clients),
        }
    }
}

// Ollama client implementation
impl OllamaClient {
    pub fn new(base_url: String) -> Self {
        Self {
            base_url,
            client: reqwest::Client::new(),
        }
    }

    pub async fn list_models(&self) -> Result<Vec<OllamaModelInfo>, Box<dyn std::error::Error + Send + Sync>> {
        let url = format!("{}/api/tags", self.base_url);
        let response: OllamaListResponse = self.client.get(&url).send().await?.json().await?;
        Ok(response.models)
    }

    pub async fn pull_model(&self, model_name: &str) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        let url = format!("{}/api/pull", self.base_url);
        let request = OllamaPullRequest {
            name: model_name.to_string(),
        };

        self.client.post(&url).json(&request).send().await?;
        Ok(())
    }

    pub async fn generate_response(&self, request: ModelRequest) -> MisaResult<ModelResponse> {
        let url = format!("{}/api/generate", self.base_url);
        let ollama_request = OllamaGenerateRequest {
            model: request.model_id.unwrap_or_default(),
            prompt: request.prompt,
            stream: false,
            options: serde_json::json!({
                "temperature": request.temperature.unwrap_or(0.7),
                "num_predict": request.max_tokens.unwrap_or(1000)
            }),
        };

        let response: OllamaGenerateResponse = self.client
            .post(&url)
            .json(&ollama_request)
            .send()
            .await
            .map_err(|e| MisaError::Network(e))?
            .json()
            .await
            .map_err(|e| MisaError::Serialization(e))?;

        Ok(ModelResponse {
            content: response.response,
            model_id: response.model,
            tokens_used: 0, // Ollama doesn't provide token count
            response_time_ms: 0, // Should be measured at higher level
            finish_reason: response.done.to_string(),
            metadata: serde_json::json!({
                "done": response.done,
                "total_duration": response.total_duration,
                "load_duration": response.load_duration
            }),
        })
    }
}

// Cloud client implementation
impl CloudClient {
    pub fn new(provider: String, config: crate::kernel::CloudProviderConfig) -> Self {
        Self {
            provider,
            api_key: config.api_key,
            base_url: config.base_url,
            client: reqwest::Client::new(),
        }
    }

    pub async fn generate_response(&self, model: &str, request: ModelRequest) -> MisaResult<ModelResponse> {
        match self.provider.as_str() {
            "openai" => self.openai_generate(model, request).await,
            _ => Err(MisaError::Model(format!("Unsupported cloud provider: {}", self.provider))),
        }
    }

    async fn openai_generate(&self, model: &str, request: ModelRequest) -> MisaResult<ModelResponse> {
        let url = format!("{}/chat/completions", self.base_url);

        let openai_request = serde_json::json!({
            "model": model,
            "messages": [{"role": "user", "content": request.prompt}],
            "temperature": request.temperature.unwrap_or(0.7),
            "max_tokens": request.max_tokens.unwrap_or(1000),
            "stream": request.stream
        });

        let mut req_builder = self.client.post(&url).json(&openai_request);

        if !self.api_key.is_empty() {
            req_builder = req_builder.bearer_auth(&self.api_key);
        }

        let response: serde_json::Value = req_builder
            .send()
            .await
            .map_err(|e| MisaError::Network(e))?
            .json()
            .await
            .map_err(|e| MisaError::Serialization(e))?;

        let content = response["choices"][0]["message"]["content"]
            .as_str()
            .ok_or_else(|| MisaError::Model("Invalid OpenAI response format".to_string()))?;

        Ok(ModelResponse {
            content: content.to_string(),
            model_id: format!("openai:{}", model),
            tokens_used: response["usage"]["total_tokens"].as_u64().unwrap_or(0) as u32,
            response_time_ms: 0,
            finish_reason: response["choices"][0]["finish_reason"]
                .as_str()
                .unwrap_or("unknown")
                .to_string(),
            metadata: response,
        })
    }
}

// Ollama API structs
#[derive(Debug, Serialize, Deserialize)]
pub struct OllamaModelInfo {
    pub name: String,
    pub size: u64,
    pub digest: String,
    pub modified_at: String,
}

#[derive(Debug, Serialize, Deserialize)]
struct OllamaListResponse {
    pub models: Vec<OllamaModelInfo>,
}

#[derive(Debug, Serialize)]
struct OllamaPullRequest {
    pub name: String,
}

#[derive(Debug, Serialize)]
struct OllamaGenerateRequest {
    pub model: String,
    pub prompt: String,
    pub stream: bool,
    pub options: serde_json::Value,
}

#[derive(Debug, Deserialize)]
struct OllamaGenerateResponse {
    pub model: String,
    pub response: String,
    pub done: bool,
    pub total_duration: Option<u64>,
    pub load_duration: Option<u64>,
}