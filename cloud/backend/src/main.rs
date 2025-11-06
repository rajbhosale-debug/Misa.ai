//! MISA.AI Cloud Backend Services
//! Provides comprehensive backend API and services for the MISA.AI ecosystem

use std::sync::Arc;
use axum::{
    Router,
    routing::{get, post, put, delete},
    Extension,
    http::StatusCode,
    response::Json,
};
use tower::ServiceBuilder;
use tower_http::{
    cors::{CorsLayer, Any},
    trace::TraceLayer,
    compression::CompressionLayer,
    limit::RequestBodyLimitLayer,
};
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};
use misa_cloud_backend::{
    config::AppConfig,
    server::Server,
    handlers::{
        auth,
        users,
        calendar,
        tasks,
        notes,
        files,
        focus,
        vision,
        plugins,
        analytics,
        notifications,
        webhooks,
    },
    middleware::{auth_middleware, rate_limit_middleware, logging_middleware},
    database::Database,
    cache::Cache,
    storage::Storage,
    websocket::WebSocketManager,
    background::BackgroundJobManager,
    metrics::MetricsCollector,
};

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    // Load configuration
    dotenvy::dotenv().ok();
    let config = AppConfig::from_env()?;

    // Initialize tracing
    tracing_subscriber::registry()
        .with(tracing_subscriber::EnvFilter::new(
            &config.log_level
        ))
        .with(tracing_subscriber::fmt::layer())
        .init();

    log::info!("Starting MISA.AI Cloud Backend v{}", env!("CARGO_PKG_VERSION"));

    // Initialize core services
    let database = Database::new(&config.database).await?;
    let cache = Cache::new(&config.cache).await?;
    let storage = Storage::new(&config.storage).await?;
    let ws_manager = Arc::new(WebSocketManager::new());
    let bg_jobs = BackgroundJobManager::new(&config.background_jobs, database.clone()).await?;
    let metrics = Arc::new(MetricsCollector::new());

    // Start background services
    bg_jobs.start().await?;

    // Create server instance
    let server = Server::new(
        config.clone(),
        database,
        cache,
        storage,
        ws_manager.clone(),
        bg_jobs,
        metrics.clone(),
    );

    // Build application router
    let app = build_app_router(server).await?;

    // Start metrics endpoint
    if config.metrics.enabled {
        let metrics_app = build_metrics_router(metrics);
        let metrics_port = config.metrics.port;

        tokio::spawn(async move {
            let listener = tokio::net::TcpListener::bind(format!("0.0.0.0:{}", metrics_port))
                .await
                .expect("Failed to bind metrics port");

            log::info!("Metrics server listening on port {}", metrics_port);

            axum::serve(listener, metrics_app)
                .await
                .expect("Failed to start metrics server");
        });
    }

    // Start HTTP server
    let bind_addr = format!("{}:{}", config.server.host, config.server.port);
    let listener = tokio::net::TcpListener::bind(&bind_addr).await?;

    log::info!("Server listening on {}", bind_addr);
    log::info!("MISA.AI Cloud Backend started successfully");

    // Run server
    axum::serve(listener, app)
        .await
        .expect("Failed to start server");

    Ok(())
}

/// Build the main application router
async fn build_app_router(server: Server) -> anyhow::Result<Router> {
    let app = Router::new()
        // Health check endpoint
        .route("/health", get(health_check))

        // API version 1 routes
        .nest("/api/v1", api_v1_routes())

        // WebSocket routes
        .route("/ws", get(websocket_handler))

        // Webhook routes
        .nest("/webhooks", webhook_routes())

        // Apply global middleware
        .layer(
            ServiceBuilder::new()
                .layer(TraceLayer::new_for_http())
                .layer(CompressionLayer::new())
                .layer(RequestBodyLimitLayer::new(100 * 1024 * 1024)) // 100MB
                .layer(CorsLayer::new()
                    .allow_origin(Any)
                    .allow_methods(Any)
                    .allow_headers(Any))
                .layer(logging_middleware())
                .layer(rate_limit_middleware())
        )
        .layer(Extension(server.config.clone()))
        .layer(Extension(server.database.clone()))
        .layer(Extension(server.cache.clone()))
        .layer(Extension(server.storage.clone()))
        .layer(Extension(server.ws_manager.clone()))
        .layer(Extension(server.metrics.clone()));

    Ok(app)
}

/// API v1 routes
fn api_v1_routes() -> Router {
    Router::new()
        // Authentication routes
        .nest("/auth", auth_routes())

        // User management routes
        .nest("/users", user_routes())

        // Calendar routes
        .nest("/calendar", calendar_routes())

        // Tasks routes
        .nest("/tasks", task_routes())

        // Notes routes
        .nest("/notes", note_routes())

        // Files routes
        .nest("/files", file_routes())

        // Focus routes
        .nest("/focus", focus_routes())

        // Vision routes
        .nest("/vision", vision_routes())

        // Plugins routes
        .nest("/plugins", plugin_routes())

        // Analytics routes
        .nest("/analytics", analytics_routes())

        // Notifications routes
        .nest("/notifications", notification_routes())
}

/// Authentication routes
fn auth_routes() -> Router {
    Router::new()
        .route("/register", post(auth::register))
        .route("/login", post(auth::login))
        .route("/logout", post(auth::logout))
        .route("/refresh", post(auth::refresh_token))
        .route("/verify", post(auth::verify_email))
        .route("/reset-password", post(auth::reset_password))
        .route("/change-password", post(auth::change_password))
        .route("/mfa/setup", post(auth::setup_mfa))
        .route("/mfa/verify", post(auth::verify_mfa))
}

/// User management routes
fn user_routes() -> Router {
    Router::new()
        .route("/", get(users::get_current_user).put(users::update_profile))
        .route("/search", get(users::search_users))
        .route("/:user_id", get(users::get_user_by_id))
        .route("/:user_id/settings", get(users::get_user_settings).put(users::update_user_settings))
        .route("/:user_id/preferences", get(users::get_user_preferences).put(users::update_user_preferences))
        .route("/:user_id/sessions", get(users::get_user_sessions))
        .route("/:user_id/sessions/:session_id", delete(users::revoke_session))
        .route("/:user_id/permissions", get(users::get_user_permissions))
        .route("/:user_id/avatars", post(users::upload_avatar))
        .route("/:user_id/deactivate", post(users::deactivate_user))
        .route("/:user_id/reactivate", post(users::reactivate_user))
}

/// Calendar routes
fn calendar_routes() -> Router {
    Router::new()
        .route("/", get(calendar::get_calendars).post(calendar::create_calendar))
        .route("/:calendar_id", get(calendar::get_calendar).put(calendar::update_calendar).delete(calendar::delete_calendar))
        .route("/:calendar_id/events", get(calendar::get_events).post(calendar::create_event))
        .route("/:calendar_id/events/:event_id", get(calendar::get_event).put(calendar::update_event).delete(calendar::delete_event))
        .route("/:calendar_id/share", post(calendar::share_calendar))
        .route("/:calendar_id/sync", post(calendar::sync_calendar))
        .route("/:calendar_id/export", get(calendar::export_calendar))
        .route("/:calendar_id/import", post(calendar::import_calendar))
}

/// Task management routes
fn task_routes() -> Router {
    Router::new()
        .route("/", get(tasks::get_tasks).post(tasks::create_task))
        .route("/:task_id", get(tasks::get_task).put(tasks::update_task).delete(tasks::delete_task))
        .route("/:task_id/subtasks", get(tasks::get_subtasks).post(tasks::create_subtask))
        .route("/:task_id/comments", get(tasks::get_comments).post(tasks::add_comment))
        .route("/:task_id/attachments", get(tasks::get_attachments).post(tasks::upload_attachment))
        .route("/:task_id/time-entries", get(tasks::get_time_entries).post(tasks::add_time_entry))
        .route("/:task_id/dependencies", get(tasks::get_dependencies).post(tasks::add_dependency))
        .route("/search", get(tasks::search_tasks))
        .route("/templates", get(tasks::get_templates).post(tasks::create_template))
        .route("/automation", get(tasks::get_automations).post(tasks::create_automation))
        .route("/analytics", get(tasks::get_task_analytics))
}

/// Notes routes
fn note_routes() -> Router {
    Router::new()
        .route("/", get(notes::get_notes).post(notes::create_note))
        .route("/:note_id", get(notes::get_note).put(notes::update_note).delete(notes::delete_note))
        .route("/:note_id/versions", get(notes::get_note_versions))
        .route("/:note_id/collaborators", get(notes::get_collaborators).post(notes::add_collaborator))
        .route("/:note_id/comments", get(notes::get_comments).post(notes::add_comment))
        .route("/:note_id/attachments", get(notes::get_attachments).post(notes::upload_attachment))
        .route("/:note_id/share", post(notes::share_note))
        .route("/notebooks", get(notes::get_notebooks).post(notes::create_notebook))
        .route("/search", get(notes::search_notes))
        .route("/export", post(notes::export_notes))
        .route("/import", post(notes::import_notes))
}

/// File management routes
fn file_routes() -> Router {
    Router::new()
        .route("/", get(files::get_files).post(files::upload_file))
        .route("/:file_id", get(files::get_file).put(files::update_file).delete(files::delete_file))
        .route("/:file_id/download", get(files::download_file))
        .route("/:file_id/thumbnail", get(files::get_thumbnail))
        .route("/:file_id/metadata", get(files::get_metadata).put(files::update_metadata))
        .route("/:file_id/versions", get(files::get_versions))
        .route("/:file_id/share", post(files::share_file))
        .route("/:file_id/comments", get(files::get_comments).post(files::add_comment))
        .route("/folders", get(files::get_folders).post(files::create_folder))
        .route("/search", get(files::search_files))
        .route("/upload/signed-url", post(files::get_signed_upload_url))
        .route("/sync", post(files::sync_files))
        .route("/analytics", get(files::get_file_analytics))
}

/// Focus and productivity routes
fn focus_routes() -> Router {
    Router::new()
        .route("/sessions", get(focus::get_sessions).post(focus::create_session))
        .route("/sessions/:session_id", get(focus::get_session).put(focus::update_session).delete(focus::delete_session))
        .route("/sessions/:session_id/start", post(focus::start_session))
        .route("/sessions/:session_id/pause", post(focus::pause_session))
        .route("/sessions/:session_id/stop", post(focus::stop_session))
        .route("/sessions/:session_id/interruptions", get(focus::get_interruptions).post(focus::add_interruption))
        .route("/templates", get(focus::get_templates).post(focus::create_template))
        .route("/goals", get(focus::get_goals).post(focus::create_goal))
        .route("/statistics", get(focus::get_statistics))
        .route("/analytics", get(focus::get_focus_analytics))
        .route("/insights", get(focus::get_insights))
}

/// Vision and AI routes
fn vision_routes() -> Router {
    Router::new()
        .route("/capture", post(vision::capture_screen))
        .route("/capture/:capture_id", get(vision::get_capture))
        .route("/capture/:capture_id/ui-elements", get(vision::get_ui_elements))
        .route("/capture/:capture_id/text", get(vision::extract_text))
        .route("/capture/:capture_id/analyze", post(vision::analyze_image))
        .route("/ocr", post(vision::ocr_image))
        .route("/image/generate-thumbnail", post(vision::generate_thumbnail))
        .route("/image/analyze-colors", post(vision::analyze_colors))
        .route("/image/detect-objects", post(vision::detect_objects))
        .route("/batch/process", post(vision::batch_process))
}

/// Plugin management routes
fn plugin_routes() -> Router {
    Router::new()
        .route("/", get(plugins::get_plugins).post(plugins::install_plugin))
        .route("/:plugin_id", get(plugins::get_plugin).put(plugins::update_plugin).delete(plugins::uninstall_plugin))
        .route("/:plugin_id/start", post(plugins::start_plugin))
        .route("/:plugin_id/stop", post(plugins::stop_plugin))
        .route("/:plugin_id/enable", post(plugins::enable_plugin))
        .route("/:plugin_id/disable", post(plugins::disable_plugin))
        .route("/:plugin_id/config", get(plugins::get_plugin_config).put(plugins::update_plugin_config))
        .route("/:plugin_id/logs", get(plugins::get_plugin_logs))
        .route("/:plugin_id/metrics", get(plugins::get_plugin_metrics))
        .route("/marketplace", get(plugins::get_marketplace_plugins))
        .route("/marketplace/:plugin_id", get(plugins::get_marketplace_plugin))
        .route("/upload", post(plugins::upload_plugin))
}

/// Analytics routes
fn analytics_routes() -> Router {
    Router::new()
        .route("/dashboard", get(analytics::get_dashboard_stats))
        .route("/usage", get(analytics::get_usage_stats))
        .route("/performance", get(analytics::get_performance_stats))
        .route("/engagement", get(analytics::get_engagement_stats))
        .route("/revenue", get(analytics::get_revenue_stats))
        .route("/users", get(analytics::get_user_analytics))
        .route("/features", get(analytics::get_feature_analytics))
        .route("/errors", get(analytics::get_error_analytics))
        .route("/reports", get(analytics::get_reports).post(analytics::create_report))
        .route("/reports/:report_id", get(analytics::get_report).delete(analytics::delete_report))
        .route("/export", post(analytics::export_data))
}

/// Notification routes
fn notification_routes() -> Router {
    Router::new()
        .route("/", get(notifications::get_notifications))
        .route("/", post(notifications::send_notification))
        .route("/:notification_id", get(notifications::get_notification).put(notifications::mark_read).delete(notifications::delete_notification))
        .route("/preferences", get(notifications::get_preferences).put(notifications::update_preferences))
        .route("/channels", get(notifications::get_channels).post(notifications::create_channel))
        .route("/templates", get(notifications::get_templates).post(notifications::create_template))
        .route("/send/batch", post(notifications::send_batch_notifications))
        .route("/send/email", post(notifications::send_email_notification))
        .route("/send/push", post(notifications::send_push_notification))
        .route("/send/sms", post(notifications::send_sms_notification))
}

/// Webhook routes
fn webhook_routes() -> Router {
    Router::new()
        .route("/stripe", post(webhooks::handle_stripe_webhook))
        .route("/github", post(webhooks::handle_github_webhook))
        .route("/slack", post(webhooks::handle_slack_webhook))
        .route("/custom/:webhook_id", post(webhooks::handle_custom_webhook))
        .route("/", get(webhooks::get_webhooks).post(webhooks::create_webhook))
        .route("/:webhook_id", get(webhooks::get_webhook).put(webhooks::update_webhook).delete(webhooks::delete_webhook))
}

/// Metrics router for monitoring
fn build_metrics_router(metrics: Arc<MetricsCollector>) -> Router {
    Router::new()
        .route("/metrics", get(move || async move {
            metrics.collect().await
        }))
        .route("/health", get(health_check))
}

/// WebSocket handler
async fn websocket_handler(
    ws: axum::extract::WebSocketUpgrade,
    Extension(ws_manager): Extension<Arc<WebSocketManager>>,
) -> Response {
    ws.on_upgrade(move |socket| async move {
        ws_manager.handle_connection(socket).await;
    })
}

/// Health check endpoint
async fn health_check() -> Result<Json<serde_json::Value>, StatusCode> {
    Ok(Json(serde_json::json!({
        "status": "healthy",
        "timestamp": chrono::Utc::now().to_rfc3339(),
        "version": env!("CARGO_PKG_VERSION"),
        "service": "misa-cloud-backend"
    })))
}

/// Response type for endpoints
type Response = axum::response::Response;