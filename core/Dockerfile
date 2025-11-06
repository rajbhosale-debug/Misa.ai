# MISA.AI Core Kernel Dockerfile
FROM rust:1.75-slim as builder

# Install build dependencies
RUN apt-get update && apt-get install -y \
    pkg-config \
    libssl-dev \
    clang \
    libclang-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy Cargo files
COPY Cargo.toml Cargo.lock ./

# Create dummy main.rs to cache dependencies
RUN mkdir src && echo "fn main() {}" > src/main.rs
RUN cargo build --release && rm -rf src

# Copy source code
COPY src ./src

# Build the kernel
RUN touch src/main.rs && cargo build --release

# Runtime image
FROM debian:bookworm-slim

# Install runtime dependencies
RUN apt-get update && apt-get install -y \
    ca-certificates \
    curl \
    libssl3 \
    && rm -rf /var/lib/apt/lists/*

# Create app user
RUN useradd -r -s /bin/false misa

WORKDIR /app

# Copy binary from builder
COPY --from=builder /app/target/release/misa-kernel /usr/local/bin/misa-kernel

# Create directories
RUN mkdir -p /app/data /app/config /app/logs && \
    chown -R misa:misa /app

# Switch to non-root user
USER misa

# Expose ports
EXPOSE 8080 50051 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# Set environment variables
ENV RUST_LOG=info
ENV MISA_DATA_DIR=/app/data
ENV MISA_CONFIG_PATH=/app/config.toml

# Default command
CMD ["misa-kernel"]