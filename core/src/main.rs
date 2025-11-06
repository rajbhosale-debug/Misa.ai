use anyhow::Result;
use clap::Parser;
use std::net::SocketAddr;
use tracing::{info, error, Level};
use tracing_subscriber;

mod kernel;
mod models;
mod security;
mod device;
mod memory;
mod privacy;

use kernel::MisaKernel;
use security::SecurityManager;

#[derive(Parser, Debug)]
#[command(author, version, about, long_about = None)]
struct Args {
    /// Socket address to bind the kernel service
    #[arg(short, long, default_value = "127.0.0.1:8080")]
    bind: SocketAddr,

    /// Enable debug logging
    #[arg(short, long)]
    debug: bool,

    /// Configuration file path
    #[arg(short, long, default_value = "config.toml")]
    config: String,

    /// Data directory for local storage
    #[arg(long, default_value = "./data")]
    data_dir: String,
}

#[tokio::main]
async fn main() -> Result<()> {
    let args = Args::parse();

    // Initialize logging
    let log_level = if args.debug { Level::DEBUG } else { Level::INFO };
    tracing_subscriber::fmt()
        .with_max_level(log_level)
        .with_target(false)
        .init();

    info!("Starting MISA.AI Kernel v{}", env!("CARGO_PKG_VERSION"));
    info!("Bind address: {}", args.bind);
    info!("Data directory: {}", args.data_dir);

    // Initialize security manager
    let security_manager = SecurityManager::new(&args.data_dir).await?;

    // Initialize and start the kernel
    let kernel = MisaKernel::new(args.config, args.data_dir, security_manager).await?;

    // Start the kernel service
    if let Err(e) = kernel.start(args.bind).await {
        error!("Failed to start kernel service: {}", e);
        return Err(e);
    }

    info!("MISA.AI Kernel started successfully");
    info!("Press Ctrl+C to stop");

    // Wait for shutdown signal
    tokio::signal::ctrl_c().await?;
    info!("Received shutdown signal");

    // Graceful shutdown
    kernel.shutdown().await?;
    info!("MISA.AI Kernel shut down gracefully");

    Ok(())
}