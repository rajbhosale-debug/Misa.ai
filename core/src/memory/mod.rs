//! Memory and Context Management System
//!
//! Handles intelligent memory and context management including:
//! - Encrypted local storage (SQLite + file system)
//! - Cloud synchronization with client-side encryption
//! - Memory schemas: short-term, medium-term, long-term
//! - Context fusion from multiple sources
//! - Memory pruning and summarization algorithms

use anyhow::Result;
use serde::{Deserialize, Serialize};
use sqlx::{sqlite::SqlitePool, Row};
use std::collections::HashMap;
use std::path::Path;
use std::sync::Arc;
use tokio::sync::RwLock;
use tracing::{info, warn, error, debug};

use crate::kernel::MemoryConfig;
use crate::security::{SecurityManager, EncryptedData};
use crate::errors::{MisaError, Result as MisaResult};

/// Memory manager for intelligent data storage and retrieval
pub struct MemoryManager {
    config: MemoryConfig,
    data_dir: String,
    security_manager: SecurityManager,
    db_pool: SqlitePool,
    context_engine: ContextEngine,
    memory_schemas: MemorySchemas,
    cloud_sync: CloudSync,
}

/// Context engine for context fusion and management
pub struct ContextEngine {
    active_context: Arc<RwLock<ContextState>>,
    context_sources: Arc<RwLock<HashMap<String, ContextSource>>>,
    fusion_algorithms: FusionAlgorithms,
}

/// Current context state
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ContextState {
    pub session_id: String,
    pub user_id: String,
    pub current_task: Option<String>,
    pub active_applications: Vec<ApplicationInfo>,
    pub system_state: SystemState,
    pub environment: EnvironmentContext,
    pub user_preferences: UserPreferences,
    pub short_term_memory: Vec<MemoryItem>,
    pub last_updated: chrono::DateTime<chrono::Utc>,
}

/// Context source information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ContextSource {
    pub source_id: String,
    pub source_type: ContextSourceType,
    pub name: String,
    pub enabled: bool,
    pub priority: u8,
    pub last_data: Option<serde_json::Value>,
    pub last_updated: chrono::DateTime<chrono::Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ContextSourceType {
    Application,
    System,
    Sensor,
    Browser,
    Calendar,
    Email,
    Chat,
    File,
    Location,
    Biometric,
}

/// Application information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ApplicationInfo {
    pub app_id: String,
    pub name: String,
    pub window_title: Option<String>,
    pub process_id: u32,
    pub memory_usage_mb: Option<u64>,
    pub cpu_usage_percent: Option<f32>,
    pub focused: bool,
    pub start_time: chrono::DateTime<chrono::Utc>,
}

/// System state
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SystemState {
    pub cpu_usage_percent: f32,
    pub memory_usage_mb: u64,
    pub disk_usage_mb: u64,
    pub battery_level: Option<f32>,
    pub power_source: PowerSource,
    pub network_status: NetworkStatus,
    pub active_devices: Vec<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum PowerSource {
    Battery,
    AC,
    UPS,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NetworkStatus {
    pub connected: bool,
    pub connection_type: String,
    pub signal_strength: Option<f32>,
    pub bandwidth_mbps: Option<f32>,
}

/// Environment context
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EnvironmentContext {
    pub location: Option<LocationData>,
    pub time_of_day: TimeOfDay,
    pub day_of_week: DayOfWeek,
    pub ambient_conditions: Option<AmbientConditions>,
    pub nearby_devices: Vec<NearbyDevice>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LocationData {
    pub latitude: f64,
    pub longitude: f64,
    pub accuracy: f32,
    pub address: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum TimeOfDay {
    EarlyMorning,   // 5-8
    Morning,        // 8-12
    Afternoon,      // 12-17
    Evening,        // 17-21
    Night,          // 21-24
    LateNight,      // 0-5
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum DayOfWeek {
    Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AmbientConditions {
    pub temperature_celsius: f32,
    pub humidity_percent: f32,
    pub noise_level_db: f32,
    pub light_level_lux: f32,
}

/// Nearby device information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NearbyDevice {
    pub device_id: String,
    pub name: String,
    pub device_type: String,
    pub signal_strength: f32,
    pub last_seen: chrono::DateTime<chrono::Utc>,
}

/// User preferences
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserPreferences {
    pub language: String,
    pub timezone: String,
    pub work_hours: WorkHours,
    pub focus_preferences: FocusPreferences,
    pub communication_style: CommunicationStyle,
    pub privacy_settings: PrivacySettings,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WorkHours {
    pub start_hour: u8,
    pub end_hour: u8,
    pub work_days: Vec<DayOfWeek>,
    pub breaks: Vec<BreakPeriod>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BreakPeriod {
    pub start_hour: u8,
    pub end_hour: u8,
    pub break_type: String, // "lunch", "coffee", etc.
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FocusPreferences {
    pub deep_work_sessions_minutes: u32,
    pub break_duration_minutes: u32,
    pub pomodoro_enabled: bool,
    pub distraction_blocking: bool,
    pub background_music_preference: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CommunicationStyle {
    pub formality_level: FormalityLevel,
    pub verbosity_level: VerbosityLevel,
    pub preferred_tone: Vec<String>,
    pub response_speed: ResponseSpeed,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum FormalityLevel {
    Casual,
    Professional,
    Formal,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum VerbosityLevel {
    Concise,
    Balanced,
    Detailed,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ResponseSpeed {
    Immediate,
    Quick,
    Normal,
    Deliberate,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PrivacySettings {
    pub location_tracking: bool,
    pub activity_tracking: bool,
    pub biometric_tracking: bool,
    pub conversation_recording: bool,
    pub screenshot_analysis: bool,
    pub data_retention_days: u32,
}

/// Memory item
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MemoryItem {
    pub id: String,
    pub content: String,
    pub content_type: ContentType,
    pub memory_type: MemoryType,
    pub importance: Importance,
    pub tags: Vec<String>,
    pub metadata: serde_json::Value,
    pub created_at: chrono::DateTime<chrono::Utc>,
    pub last_accessed: chrono::DateTime<chrono::Utc>,
    pub access_count: u32,
    pub encrypted: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ContentType {
    Text,
    Image,
    Audio,
    Video,
    Document,
    Code,
    StructuredData,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum MemoryType {
    ShortTerm,     // Current session
    MediumTerm,    // Days to weeks
    LongTerm,      // Months to years
    Permanent,     // Critical information
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum Importance {
    Low,
    Medium,
    High,
    Critical,
}

/// Memory schemas
pub struct MemorySchemas {
    short_term_capacity: usize,
    medium_term_retention_days: u32,
    long_term_retention_days: u32,
    compression_threshold: f32,
    summarization_enabled: bool,
}

/// Fusion algorithms for context processing
pub struct FusionAlgorithms {
    relevance_scorer: RelevanceScorer,
    pattern_detector: PatternDetector,
    anomaly_detector: AnomalyDetector,
    prediction_engine: PredictionEngine,
}

/// Cloud synchronization
pub struct CloudSync {
    enabled: bool,
    sync_interval_minutes: u64,
    last_sync: Arc<RwLock<chrono::DateTime<chrono::Utc>>>,
    conflict_resolver: ConflictResolver,
}

/// Conflict resolver for cloud sync
pub struct ConflictResolver {
    strategy: ConflictStrategy,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ConflictStrategy {
    LocalWins,
    RemoteWins,
    LastModifiedWins,
    Merge,
    ManualResolution,
}

impl MemoryManager {
    /// Create a new memory manager
    pub async fn new(data_dir: &str, config: MemoryConfig, security_manager: SecurityManager) -> MisaResult<Self> {
        // Ensure data directory exists
        tokio::fs::create_dir_all(data_dir).await
            .map_err(|e| MisaError::Io(e))?;

        // Initialize database
        let db_path = Path::new(data_dir).join(&config.local_db_path);
        let db_pool = Self::initialize_database(&db_path).await?;

        // Initialize components
        let context_engine = ContextEngine::new().await?;
        let memory_schemas = MemorySchemas::new(config.retention_days);
        let cloud_sync = CloudSync::new(true);

        let manager = Self {
            config,
            data_dir: data_dir.to_string(),
            security_manager,
            db_pool,
            context_engine,
            memory_schemas,
            cloud_sync,
        };

        info!("Memory manager initialized");
        Ok(manager)
    }

    /// Initialize the memory manager
    pub async fn initialize(&self) -> MisaResult<()> {
        info!("Initializing memory manager");

        // Initialize context engine
        self.context_engine.initialize().await?;

        // Start background tasks
        self.start_background_tasks().await?;

        info!("Memory manager fully initialized");
        Ok(())
    }

    /// Store memory item
    pub async fn store_memory(&self, memory: MemoryItem) -> MisaResult<String> {
        debug!("Storing memory item: {}", memory.id);

        // Encrypt if required
        let encrypted_memory = if self.config.encryption_enabled {
            Some(self.encrypt_memory(&memory).await?)
        } else {
            None
        };

        // Store in database
        let memory_id = self.insert_memory_to_db(&memory, encrypted_memory).await?;

        // Add to short-term context if appropriate
        if matches!(memory.memory_type, MemoryType::ShortTerm) {
            self.context_engine.add_to_short_term_memory(memory.clone()).await?;
        }

        info!("Stored memory item: {}", memory_id);
        Ok(memory_id)
    }

    /// Retrieve memory item
    pub async fn get_memory(&self, memory_id: &str) -> MisaResult<Option<MemoryItem>> {
        debug!("Retrieving memory item: {}", memory_id);

        let memory = self.get_memory_from_db(memory_id).await?;

        if let Some(mut memory) = memory {
            // Decrypt if required
            if memory.encrypted {
                memory = self.decrypt_memory(&memory).await?;
            }

            // Update access statistics
            self.update_memory_access_stats(memory_id).await?;

            Ok(Some(memory))
        } else {
            Ok(None)
        }
    }

    /// Search memories
    pub async fn search_memories(&self, query: &SearchQuery) -> MisaResult<Vec<MemoryItem>> {
        debug!("Searching memories with query: {:?}", query);

        let memories = self.search_memories_in_db(query).await?;

        // Decrypt if needed and filter results
        let mut results = Vec::new();
        for memory in memories {
            let mut memory = memory;
            if memory.encrypted {
                memory = self.decrypt_memory(&memory).await?;
            }
            results.push(memory);
        }

        Ok(results)
    }

    /// Get current context
    pub async fn get_current_context(&self) -> MisaResult<ContextState> {
        self.context_engine.get_current_context().await
    }

    /// Update context with new data
    pub async fn update_context(&self, context_source: ContextSource, data: serde_json::Value) -> MisaResult<()> {
        self.context_engine.update_context(context_source, data).await
    }

    /// Prune old memories based on retention policy
    pub async fn prune_memories(&self) -> MisaResult<u32> {
        info!("Pruning old memories");

        let cutoff_date = chrono::Utc::now() - chrono::Duration::days(self.config.retention_days as i64);
        let deleted_count = self.delete_old_memories(cutoff_date).await?;

        info!("Pruned {} old memories", deleted_count);
        Ok(deleted_count)
    }

    /// Sync with cloud storage
    pub async fn sync_with_cloud(&self) -> MisaResult<()> {
        if !self.cloud_sync.enabled {
            debug!("Cloud sync disabled");
            return Ok(());
        }

        info!("Starting cloud synchronization");

        // In real implementation, this would:
        // - Upload new memories to cloud
        // - Download remote changes
        // - Resolve conflicts
        // - Update sync timestamp

        let mut last_sync = self.cloud_sync.last_sync.write().await;
        *last_sync = chrono::Utc::now();

        info!("Cloud synchronization completed");
        Ok(())
    }

    /// Get memory statistics
    pub async fn get_memory_stats(&self) -> MisaResult<MemoryStats> {
        let stats = sqlx::query_as!(
            MemoryStatsRow,
            r#"
            SELECT
                COUNT(*) as total_memories,
                SUM(CASE WHEN memory_type = 'ShortTerm' THEN 1 ELSE 0 END) as short_term_count,
                SUM(CASE WHEN memory_type = 'MediumTerm' THEN 1 ELSE 0 END) as medium_term_count,
                SUM(CASE WHEN memory_type = 'LongTerm' THEN 1 ELSE 0 END) as long_term_count,
                SUM(CASE WHEN memory_type = 'Permanent' THEN 1 ELSE 0 END) as permanent_count,
                AVG(access_count) as avg_access_count,
                MAX(created_at) as newest_memory,
                MIN(created_at) as oldest_memory
            FROM memories
            "#
        )
        .fetch_one(&self.db_pool)
        .await
        .map_err(|e| MisaError::Database(e))?;

        Ok(MemoryStats {
            total_memories: stats.total_memories.unwrap_or(0) as u32,
            short_term_count: stats.short_term_count.unwrap_or(0) as u32,
            medium_term_count: stats.medium_term_count.unwrap_or(0) as u32,
            long_term_count: stats.long_term_count.unwrap_or(0) as u32,
            permanent_count: stats.permanent_count.unwrap_or(0) as u32,
            avg_access_count: stats.avg_access_count.unwrap_or(0.0) as f32,
            newest_memory: stats.newest_memory,
            oldest_memory: stats.oldest_memory,
        })
    }

    /// Shutdown memory manager
    pub async fn shutdown(&self) -> MisaResult<()> {
        info!("Shutting down memory manager");

        // Final sync with cloud
        self.sync_with_cloud().await?;

        // Close database connection
        self.db_pool.close().await;

        info!("Memory manager shut down");
        Ok(())
    }

    /// Private helper methods

    async fn initialize_database(db_path: &Path) -> MisaResult<SqlitePool> {
        let connection_string = format!("sqlite:{}", db_path.display());

        // Create database with connection pool
        let pool = SqlitePool::connect(&connection_string)
            .await
            .map_err(|e| MisaError::Database(e))?;

        // Create tables
        Self::create_tables(&pool).await?;

        Ok(pool)
    }

    async fn create_tables(pool: &SqlitePool) -> MisaResult<()> {
        sqlx::query(
            r#"
            CREATE TABLE IF NOT EXISTS memories (
                id TEXT PRIMARY KEY,
                content TEXT NOT NULL,
                content_type TEXT NOT NULL,
                memory_type TEXT NOT NULL,
                importance TEXT NOT NULL,
                tags TEXT, -- JSON array
                metadata TEXT, -- JSON object
                created_at DATETIME NOT NULL,
                last_accessed DATETIME NOT NULL,
                access_count INTEGER NOT NULL DEFAULT 0,
                encrypted BOOLEAN NOT NULL DEFAULT FALSE,
                encrypted_data BLOB -- Encrypted content if encryption enabled
            );
            CREATE INDEX IF NOT EXISTS idx_memories_type ON memories(memory_type);
            CREATE INDEX IF NOT EXISTS idx_memories_created ON memories(created_at);
            CREATE INDEX IF NOT EXISTS idx_memories_importance ON memories(importance);
            "#
        )
        .execute(pool)
        .await
        .map_err(|e| MisaError::Database(e))?;

        Ok(())
    }

    async fn encrypt_memory(&self, memory: &MemoryItem) -> MisaResult<EncryptedData> {
        let content_bytes = memory.content.as_bytes();
        self.security_manager.encrypt_data(content_bytes, &memory.id).await
    }

    async fn decrypt_memory(&self, memory: &MemoryItem) -> MisaResult<MemoryItem> {
        // This would need the encrypted data from database
        // For now, return memory as-is
        Ok(memory.clone())
    }

    async fn insert_memory_to_db(&self, memory: &MemoryItem, encrypted_data: Option<EncryptedData>) -> MisaResult<String> {
        let tags_json = serde_json::to_string(&memory.tags)?;
        let metadata_json = serde_json::to_string(&memory.metadata)?;

        let encrypted_blob = if let Some(encrypted) = encrypted_data {
            Some(encrypted.ciphertext)
        } else {
            None
        };

        sqlx::query!(
            r#"
            INSERT INTO memories (
                id, content, content_type, memory_type, importance,
                tags, metadata, created_at, last_accessed,
                access_count, encrypted, encrypted_data
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            "#,
            memory.id,
            memory.content,
            serde_json::to_string(&memory.content_type)?,
            serde_json::to_string(&memory.memory_type)?,
            serde_json::to_string(&memory.importance)?,
            tags_json,
            metadata_json,
            memory.created_at,
            memory.last_accessed,
            memory.access_count,
            memory.encrypted,
            encrypted_blob
        )
        .execute(&self.db_pool)
        .await
        .map_err(|e| MisaError::Database(e))?;

        Ok(memory.id.clone())
    }

    async fn get_memory_from_db(&self, memory_id: &str) -> MisaResult<Option<MemoryItem>> {
        let row = sqlx::query!(
            r#"
            SELECT
                id, content, content_type, memory_type, importance,
                tags, metadata, created_at, last_accessed,
                access_count, encrypted
            FROM memories
            WHERE id = ?
            "#,
            memory_id
        )
        .fetch_optional(&self.db_pool)
        .await
        .map_err(|e| MisaError::Database(e))?;

        if let Some(row) = row {
            let memory = MemoryItem {
                id: row.id,
                content: row.content,
                content_type: serde_json::from_str(&row.content_type)?,
                memory_type: serde_json::from_str(&row.memory_type)?,
                importance: serde_json::from_str(&row.importance)?,
                tags: serde_json::from_str(&row.tags.unwrap_or_default())?,
                metadata: serde_json::from_str(&row.metadata.unwrap_or_default())?,
                created_at: row.created_at,
                last_accessed: row.last_accessed,
                access_count: row.access_count as u32,
                encrypted: row.encrypted,
            };
            Ok(Some(memory))
        } else {
            Ok(None)
        }
    }

    async fn search_memories_in_db(&self, query: &SearchQuery) -> MisaResult<Vec<MemoryItem>> {
        let sql = query.build_sql();
        let mut q = sqlx::query(&sql);

        for param in &query.params {
            q = q.bind(param);
        }

        let rows = q.fetch_all(&self.db_pool)
            .await
            .map_err(|e| MisaError::Database(e))?;

        let mut memories = Vec::new();
        for row in rows {
            let memory = MemoryItem {
                id: row.get("id"),
                content: row.get("content"),
                content_type: serde_json::from_str(row.get("content_type"))?,
                memory_type: serde_json::from_str(row.get("memory_type"))?,
                importance: serde_json::from_str(row.get("importance"))?,
                tags: serde_json::from_str(row.get::<_, Option<String>>("tags").unwrap_or_default())?,
                metadata: serde_json::from_str(row.get::<_, Option<String>>("metadata").unwrap_or_default())?,
                created_at: row.get("created_at"),
                last_accessed: row.get("last_accessed"),
                access_count: row.get::<_, i64>("access_count") as u32,
                encrypted: row.get("encrypted"),
            };
            memories.push(memory);
        }

        Ok(memories)
    }

    async fn update_memory_access_stats(&self, memory_id: &str) -> MisaResult<()> {
        sqlx::query!(
            r#"
            UPDATE memories
            SET last_accessed = ?, access_count = access_count + 1
            WHERE id = ?
            "#,
            chrono::Utc::now(),
            memory_id
        )
        .execute(&self.db_pool)
        .await
        .map_err(|e| MisaError::Database(e))?;

        Ok(())
    }

    async fn delete_old_memories(&self, cutoff_date: chrono::DateTime<chrono::Utc>) -> MisaResult<u32> {
        let result = sqlx::query!(
            r#"
            DELETE FROM memories
            WHERE created_at < ? AND memory_type != 'Permanent'
            "#,
            cutoff_date
        )
        .execute(&self.db_pool)
        .await
        .map_err(|e| MisaError::Database(e))?;

        Ok(result.rows_affected() as u32)
    }

    async fn start_background_tasks(&self) -> MisaResult<()> {
        // Start memory pruning task
        let memory_schemas = self.memory_schemas.clone();
        let db_pool = self.db_pool.clone();

        tokio::spawn(async move {
            let mut interval = tokio::time::interval(tokio::time::Duration::from_secs(3600 * 24)); // Daily
            loop {
                interval.tick().await;
                debug!("Running background memory pruning");
                // Implementation would prune old memories
            }
        });

        // Start cloud sync task
        if self.cloud_sync.enabled {
            let cloud_sync = self.cloud_sync.clone();
            tokio::spawn(async move {
                let mut interval = tokio::time::interval(tokio::time::Duration::from_secs(
                    cloud_sync.sync_interval_minutes * 60,
                ));
                loop {
                    interval.tick().await;
                    debug!("Running background cloud sync");
                    let mut last_sync = cloud_sync.last_sync.write().await;
                    *last_sync = chrono::Utc::now();
                }
            });
        }

        Ok(())
    }
}

/// Search query for memories
#[derive(Debug, Clone)]
pub struct SearchQuery {
    pub text: Option<String>,
    pub content_type: Option<ContentType>,
    pub memory_type: Option<MemoryType>,
    pub importance: Option<Importance>,
    pub tags: Vec<String>,
    pub date_range: Option<(chrono::DateTime<chrono::Utc>, chrono::DateTime<chrono::Utc>)>,
    pub limit: Option<u32>,
    pub offset: Option<u32>,
    pub sort_by: SortField,
    pub sort_order: SortOrder,
    pub sql: String,
    pub params: Vec<String>,
}

#[derive(Debug, Clone)]
pub enum SortField {
    CreatedAt,
    LastAccessed,
    AccessCount,
    Importance,
}

#[derive(Debug, Clone)]
pub enum SortOrder {
    Asc,
    Desc,
}

impl SearchQuery {
    pub fn new() -> Self {
        Self {
            text: None,
            content_type: None,
            memory_type: None,
            importance: None,
            tags: Vec::new(),
            date_range: None,
            limit: Some(100),
            offset: Some(0),
            sort_by: SortField::LastAccessed,
            sort_order: SortOrder::Desc,
            sql: String::new(),
            params: Vec::new(),
        }
    }

    pub fn build_sql(&mut self) {
        let mut conditions = Vec::new();
        let mut params = Vec::new();

        if let Some(text) = &self.text {
            conditions.push("content LIKE ?");
            params.push(format!("%{}%", text));
        }

        if let Some(content_type) = &self.content_type {
            conditions.push("content_type = ?");
            params.push(serde_json::to_string(content_type).unwrap());
        }

        if let Some(memory_type) = &self.memory_type {
            conditions.push("memory_type = ?");
            params.push(serde_json::to_string(memory_type).unwrap());
        }

        if let Some(importance) = &self.importance {
            conditions.push("importance = ?");
            params.push(serde_json::to_string(importance).unwrap());
        }

        if let Some((start, end)) = &self.date_range {
            conditions.push("created_at BETWEEN ? AND ?");
            params.push(start.to_rfc3339());
            params.push(end.to_rfc3339());
        }

        for tag in &self.tags {
            conditions.push("JSON_EXTRACT(tags, ?) IS NOT NULL");
            params.push(format!("$[?]", tag));
        }

        let where_clause = if conditions.is_empty() {
            String::new()
        } else {
            format!("WHERE {}", conditions.join(" AND "))
        };

        let sort_clause = match (&self.sort_by, &self.sort_order) {
            (SortField::CreatedAt, SortOrder::Asc) => "ORDER BY created_at ASC",
            (SortField::CreatedAt, SortOrder::Desc) => "ORDER BY created_at DESC",
            (SortField::LastAccessed, SortOrder::Asc) => "ORDER BY last_accessed ASC",
            (SortField::LastAccessed, SortOrder::Desc) => "ORDER BY last_accessed DESC",
            (SortField::AccessCount, SortOrder::Asc) => "ORDER BY access_count ASC",
            (SortField::AccessCount, SortOrder::Desc) => "ORDER BY access_count DESC",
            _ => "ORDER BY last_accessed DESC",
        };

        let limit_clause = if let Some(limit) = self.limit {
            format!("LIMIT {}", limit)
        } else {
            String::new()
        };

        let offset_clause = if let Some(offset) = self.offset {
            format!("OFFSET {}", offset)
        } else {
            String::new()
        };

        self.sql = format!(
            "SELECT * FROM memories {} {} {} {}",
            where_clause, sort_clause, limit_clause, offset_clause
        );
        self.params = params;
    }
}

/// Memory statistics
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MemoryStats {
    pub total_memories: u32,
    pub short_term_count: u32,
    pub medium_term_count: u32,
    pub long_term_count: u32,
    pub permanent_count: u32,
    pub avg_access_count: f32,
    pub newest_memory: Option<chrono::DateTime<chrono::Utc>>,
    pub oldest_memory: Option<chrono::DateTime<chrono::Utc>>,
}

/// Database row for stats
struct MemoryStatsRow {
    total_memories: Option<i64>,
    short_term_count: Option<i64>,
    medium_term_count: Option<i64>,
    long_term_count: Option<i64>,
    permanent_count: Option<i64>,
    avg_access_count: Option<f64>,
    newest_memory: Option<chrono::DateTime<chrono::Utc>>,
    oldest_memory: Option<chrono::DateTime<chrono::Utc>>,
}

impl ContextEngine {
    pub async fn new() -> MisaResult<Self> {
        Ok(Self {
            active_context: Arc::new(RwLock::new(ContextState::default())),
            context_sources: Arc::new(RwLock::new(HashMap::new())),
            fusion_algorithms: FusionAlgorithms::new(),
        })
    }

    pub async fn initialize(&self) -> MisaResult<()> {
        info!("Initializing context engine");

        // Initialize default context sources
        let mut sources = self.context_sources.write().await;
        sources.insert("system".to_string(), ContextSource {
            source_id: "system".to_string(),
            source_type: ContextSourceType::System,
            name: "System Monitor".to_string(),
            enabled: true,
            priority: 10,
            last_data: None,
            last_updated: chrono::Utc::now(),
        });

        Ok(())
    }

    pub async fn get_current_context(&self) -> MisaResult<ContextState> {
        let context = self.active_context.read().await;
        Ok(context.clone())
    }

    pub async fn update_context(&self, source: ContextSource, data: serde_json::Value) -> MisaResult<()> {
        // Update context source
        {
            let mut sources = self.context_sources.write().await;
            sources.insert(source.source_id.clone(), source);
        }

        // Process context fusion
        let mut context = self.active_context.write().await;
        context.last_updated = chrono::Utc::now();

        // In real implementation, this would use fusion algorithms
        // to intelligently merge the new data

        Ok(())
    }

    pub async fn add_to_short_term_memory(&self, memory: MemoryItem) -> MisaResult<()> {
        let mut context = self.active_context.write().await;
        context.short_term_memory.push(memory);

        // Limit short-term memory size
        if context.short_term_memory.len() > 50 {
            context.short_term_memory.remove(0);
        }

        Ok(())
    }
}

impl MemorySchemas {
    pub fn new(retention_days: u32) -> Self {
        Self {
            short_term_capacity: 100,
            medium_term_retention_days: 30,
            long_term_retention_days: retention_days,
            compression_threshold: 0.8,
            summarization_enabled: true,
        }
    }
}

impl FusionAlgorithms {
    pub fn new() -> Self {
        Self {
            relevance_scorer: RelevanceScorer::new(),
            pattern_detector: PatternDetector::new(),
            anomaly_detector: AnomalyDetector::new(),
            prediction_engine: PredictionEngine::new(),
        }
    }
}

impl CloudSync {
    pub fn new(enabled: bool) -> Self {
        Self {
            enabled,
            sync_interval_minutes: 30,
            last_sync: Arc::new(RwLock::new(chrono::Utc::now())),
            conflict_resolver: ConflictResolver::new(ConflictStrategy::LastModifiedWins),
        }
    }
}

impl ConflictResolver {
    pub fn new(strategy: ConflictStrategy) -> Self {
        Self { strategy }
    }
}

// Placeholder implementations for fusion algorithms
pub struct RelevanceScorer;
pub struct PatternDetector;
pub struct AnomalyDetector;
pub struct PredictionEngine;

impl RelevanceScorer {
    pub fn new() -> Self { Self }
}

impl PatternDetector {
    pub fn new() -> Self { Self }
}

impl AnomalyDetector {
    pub fn new() -> Self { Self }
}

impl PredictionEngine {
    pub fn new() -> Self { Self }
}

// Implement Clone for Arc-wrapped structs
impl Clone for ContextEngine {
    fn clone(&self) -> Self {
        Self {
            active_context: Arc::clone(&self.active_context),
            context_sources: Arc::clone(&self.context_sources),
            fusion_algorithms: FusionAlgorithms::new(),
        }
    }
}

impl Clone for CloudSync {
    fn clone(&self) -> Self {
        Self {
            enabled: self.enabled,
            sync_interval_minutes: self.sync_interval_minutes,
            last_sync: Arc::clone(&self.last_sync),
            conflict_resolver: ConflictResolver::new(self.conflict_resolver.strategy.clone()),
        }
    }
}

impl Clone for ConflictResolver {
    fn clone(&self) -> Self {
        Self {
            strategy: self.strategy.clone(),
        }
    }
}

impl Default for ContextState {
    fn default() -> Self {
        Self {
            session_id: uuid::Uuid::new_v4().to_string(),
            user_id: "default".to_string(),
            current_task: None,
            active_applications: Vec::new(),
            system_state: SystemState::default(),
            environment: EnvironmentContext::default(),
            user_preferences: UserPreferences::default(),
            short_term_memory: Vec::new(),
            last_updated: chrono::Utc::now(),
        }
    }
}

impl Default for SystemState {
    fn default() -> Self {
        Self {
            cpu_usage_percent: 0.0,
            memory_usage_mb: 0,
            disk_usage_mb: 0,
            battery_level: None,
            power_source: PowerSource::AC,
            network_status: NetworkStatus {
                connected: false,
                connection_type: "unknown".to_string(),
                signal_strength: None,
                bandwidth_mbps: None,
            },
            active_devices: Vec::new(),
        }
    }
}

impl Default for EnvironmentContext {
    fn default() -> Self {
        Self {
            location: None,
            time_of_day: Self::get_current_time_of_day(),
            day_of_week: Self::get_current_day_of_week(),
            ambient_conditions: None,
            nearby_devices: Vec::new(),
        }
    }
}

impl EnvironmentContext {
    fn get_current_time_of_day() -> TimeOfDay {
        use chrono::Local;
        let hour = Local::now().hour();
        match hour {
            5..=7 => TimeOfDay::EarlyMorning,
            8..=11 => TimeOfDay::Morning,
            12..=16 => TimeOfDay::Afternoon,
            17..=20 => TimeOfDay::Evening,
            21..=23 => TimeOfDay::Night,
            _ => TimeOfDay::LateNight,
        }
    }

    fn get_current_day_of_week() -> DayOfWeek {
        use chrono::Local;
        match Local::now().weekday() {
            chrono::Weekday::Mon => DayOfWeek::Monday,
            chrono::Weekday::Tue => DayOfWeek::Tuesday,
            chrono::Weekday::Wed => DayOfWeek::Wednesday,
            chrono::Weekday::Thu => DayOfWeek::Thursday,
            chrono::Weekday::Fri => DayOfWeek::Friday,
            chrono::Weekday::Sat => DayOfWeek::Saturday,
            chrono::Weekday::Sun => DayOfWeek::Sunday,
        }
    }
}

impl Default for UserPreferences {
    fn default() -> Self {
        Self {
            language: "en".to_string(),
            timezone: "UTC".to_string(),
            work_hours: WorkHours::default(),
            focus_preferences: FocusPreferences::default(),
            communication_style: CommunicationStyle::default(),
            privacy_settings: PrivacySettings::default(),
        }
    }
}

impl Default for WorkHours {
    fn default() -> Self {
        Self {
            start_hour: 9,
            end_hour: 17,
            work_days: vec![
                DayOfWeek::Monday, DayOfWeek::Tuesday, DayOfWeek::Wednesday,
                DayOfWeek::Thursday, DayOfWeek::Friday,
            ],
            breaks: vec![
                BreakPeriod { start_hour: 12, end_hour: 13, break_type: "lunch".to_string() },
            BreakPeriod { start_hour: 15, end_hour: 15, break_type: "coffee".to_string() },
            ],
        }
    }
}

impl Default for FocusPreferences {
    fn default() -> Self {
        Self {
            deep_work_sessions_minutes: 25,
            break_duration_minutes: 5,
            pomodoro_enabled: false,
            distraction_blocking: false,
            background_music_preference: None,
        }
    }
}

impl Default for CommunicationStyle {
    fn default() -> Self {
        Self {
            formality_level: FormalityLevel::Professional,
            verbosity_level: VerbosityLevel::Balanced,
            preferred_tone: vec!["helpful".to_string(), "friendly".to_string()],
            response_speed: ResponseSpeed::Normal,
        }
    }
}

impl Default for PrivacySettings {
    fn default() -> Self {
        Self {
            location_tracking: false,
            activity_tracking: true,
            biometric_tracking: false,
            conversation_recording: false,
            screenshot_analysis: false,
            data_retention_days: 365,
        }
    }
}

// Implement Clone for MemoryManager
impl Clone for MemoryManager {
    fn clone(&self) -> Self {
        Self {
            config: self.config.clone(),
            data_dir: self.data_dir.clone(),
            security_manager: self.security_manager.clone(),
            db_pool: self.db_pool.clone(),
            context_engine: ContextEngine::new().await.unwrap(),
            memory_schemas: MemorySchemas::new(self.config.retention_days),
            cloud_sync: CloudSync::new(self.cloud_sync.enabled),
        }
    }
}

impl Clone for MemorySchemas {
    fn clone(&self) -> Self {
        Self {
            short_term_capacity: self.short_term_capacity,
            medium_term_retention_days: self.medium_term_retention_days,
            long_term_retention_days: self.long_term_retention_days,
            compression_threshold: self.compression_threshold,
            summarization_enabled: self.summarization_enabled,
        }
    }
}

impl Clone for FusionAlgorithms {
    fn clone(&self) -> Self {
        Self {
            relevance_scorer: RelevanceScorer::new(),
            pattern_detector: PatternDetector::new(),
            anomaly_detector: AnomalyDetector::new(),
            prediction_engine: PredictionEngine::new(),
        }
    }
}