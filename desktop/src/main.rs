//! MISA.AI Desktop Application Main Entry Point
//! Provides native desktop experience with system integration

#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::sync::Arc;
use tauri::{Manager, State};
use misa_desktop_lib::{MisaApp, MisaAppState};

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    // Initialize logging
    env_logger::Builder::from_env(env_logger::Env::default().default_filter_or("info")).init();

    // Create application state
    let app_state = Arc::new(MisaAppState::new().await?);

    // Build Tauri application
    tauri::Builder::default()
        .manage(app_state.clone())
        .setup(move |app| {
            // Initialize MISA application
            let app_handle = app.handle();
            tauri::async_runtime::spawn(async move {
                if let Err(e) = MisaApp::initialize(app_handle, app_state).await {
                    log::error!("Failed to initialize MISA app: {}", e);
                }
            });
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            // Core commands
            misa_desktop_lib::commands::get_app_info,
            misa_desktop_lib::commands::get_config,
            misa_desktop_lib::commands::update_config,

            // Device commands
            misa_desktop_lib::commands::start_device_discovery,
            misa_desktop_lib::commands::get_connected_devices,
            misa_desktop_lib::commands::send_message_to_device,

            // Vision commands
            misa_desktop_lib::commands::capture_screen,
            misa_desktop_lib::commands::detect_ui_elements,
            misa_desktop_lib::commands::extract_text_from_image,

            // File commands
            misa_desktop_lib::commands::list_files,
            misa_desktop_lib::commands::upload_file,
            misa_desktop_lib::commands::download_file,
            misa_desktop_lib::commands::create_folder,

            // Focus commands
            misa_desktop_lib::commands::start_focus_session,
            misa_desktop_lib::commands::stop_focus_session,
            misa_desktop_lib::commands::get_focus_stats,

            // System commands
            misa_desktop_lib::commands::get_system_info,
            misa_desktop_lib::commands::set_powersave_mode,
            misa_desktop_lib::commands::show_notification,

            // AI commands
            misa_desktop_lib::commands::process_natural_language,
            misa_desktop_lib::commands::get_ai_recommendations,
            misa_desktop_lib::commands::generate_summary
        ])
        .system_tray(misa_desktop_lib::tray::create_system_tray())
        .on_system_tray_event(misa_desktop_lib::tray::handle_system_tray_event)
        .window(
            tauri::WindowBuilder::new(
                "main",
                tauri::WindowUrl::App("/index.html".into())
            )
            .title("MISA.AI Desktop")
            .min_inner_size(1000.0, 700.0)
            .inner_size(1400.0, 900.0)
            .center()
            .decorations(true)
            .transparent(false)
            .always_on_top(false)
            .skip_taskbar(false)
            .fullscreen(false)
            .resizable(true)
            .maximizable(true)
            .minimizable(true)
            .closable(true)
            .theme(Some(tauri::Theme::Light))
        )
        .run(tauri::generate_context!())
        .expect("error while running tauri application");

    Ok(())
}