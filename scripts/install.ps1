# MISA.AI Windows Installation Script
# PowerShell version for Windows users

param(
    [switch]$Uninstall
)

# Colors for output
$Colors = @{
    Red = "Red"
    Green = "Green"
    Yellow = "Yellow"
    Blue = "Blue"
    White = "White"
}

function Write-ColorOutput($Message, $Color) {
    Write-Host $Message -ForegroundColor $Colors[$Color]
}

function Print-Banner {
    Write-ColorOutput "  ___  __   __  __   __   ___  __   __   " -Color Blue
    Write-ColorOutput " | __| \ \ / /  \ \ / /  | __| \ \ / /   " -Color Blue
    Write-ColorOutput " | _|   \ V /    \ V /   | _|   \ V /    " -Color Blue
    Write-ColorOutput " |___|   |_|      |_|    |___|   |_|     " -Color Blue
    Write-ColorOutput "                                           " -Color Blue
    Write-ColorOutput "    🤖 Intelligent Assistant Platform     " -Color Blue
    Write-ColorOutput "    Privacy-First AI with Enterprise Grade Security" -Color Blue
    Write-ColorOutput ""
}

function Write-Success($Message) {
    Write-ColorOutput "✅ $Message" -Color Green
}

function Write-Error($Message) {
    Write-ColorOutput "❌ $Message" -Color Red
}

function Write-Warning($Message) {
    Write-ColorOutput "⚠️  $Message" -Color Yellow
}

function Write-Info($Message) {
    Write-ColorOutput "ℹ️  $Message" -Color Blue
}

function Test-Docker {
    Write-Info "Checking Docker installation..."

    try {
        $dockerVersion = docker --version 2>$null
        if ($dockerVersion) {
            Write-Success "Docker is installed: $dockerVersion"

            # Check if Docker is running
            try {
                $dockerInfo = docker info 2>$null
                if ($dockerInfo) {
                    Write-Success "Docker is running"
                    return $true
                } else {
                    Write-Error "Docker is installed but not running"
                    Write-Info "Please start Docker Desktop"
                    return $false
                }
            } catch {
                Write-Error "Docker is not running. Please start Docker Desktop"
                return $false
            }
        }
    } catch {
        Write-Error "Docker is not installed"
        Write-Info "Please install Docker Desktop from: https://www.docker.com/products/docker-desktop"
        return $false
    }
}

function Install-MisaAI {
    Print-Banner

    Write-Info "Starting MISA.AI installation..."
    Write-Info ""

    # Check prerequisites
    if (-not (Test-Docker)) {
        Write-Error "Docker is required but not available. Please install/start Docker Desktop and try again."
        exit 1
    }

    $InstallDir = "$env:USERPROFILE\.misa-ai"

    Write-Info "Creating installation directory: $InstallDir"

    if (Test-Path $InstallDir) {
        Write-Warning "Installation directory already exists."
        $Response = Read-Host "Do you want to remove existing installation? (y/N)"
        if ($Response -eq 'y' -or $Response -eq 'Y') {
            Remove-Item -Recurse -Force $InstallDir
            Write-Success "Removed existing installation"
        } else {
            Write-Info "Updating existing installation..."
        }
    }

    # Create directories
    New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null
    New-Item -ItemType Directory -Path "$InstallDir\data" -Force | Out-Null
    New-Item -ItemType Directory -Path "$InstallDir\config" -Force | Out-Null
    New-Item -ItemType Directory -Path "$InstallDir\logs" -Force | Out-Null

    Write-Success "Installation directory created"

    # Copy Docker configuration
    Write-Info "Setting up Docker configuration..."

    $DockerDir = Join-Path $InstallDir "docker"
    if (Test-Path "infrastructure\docker") {
        Copy-Item -Recurse "infrastructure\docker\*" $DockerDir -Force
        Write-Success "Docker configuration copied"
    } else {
        Write-Error "Docker configuration not found. Make sure you're in the MISA.AI root directory."
        exit 1
    }

    # Create environment file
    $EnvFile = Join-Path $DockerDir ".env"
    if (-not (Test-Path $EnvFile)) {
        Write-Info "Creating environment configuration..."

        $EnvContent = @"
# MISA.AI Docker Environment Configuration
# Database Configuration
POSTGRES_PASSWORD=misa_secure_password_change_me
POSTGRES_DB=misa_ai
POSTGRES_USER=misa

# Redis Configuration
REDIS_PASSWORD=redis_secure_change_me

# Grafana Configuration
GRAFANA_PASSWORD=admin123_change_me

# MinIO Object Storage
MINIO_ROOT_USER=admin
MINIO_ROOT_PASSWORD=minioadmin_change_me

# RabbitMQ Configuration
RABBITMQ_USER=misa
RABBITMQ_PASSWORD=misa_rabbitmq_change_me

# AI Model API Keys (Optional)
# OPENAI_API_KEY=your_openai_key_here
# ANTHROPIC_API_KEY=your_claude_key_here
# GOOGLE_AI_API_KEY=your_gemini_key_here

# Security Keys
JWT_SECRET=your_jwt_secret_here_minimum_32_characters
ENCRYPTION_KEY=your_encryption_key_here_exactly_64_hex_chars

# Feature Flags
ENABLE_CLOUD_MODELS=false
ENABLE_TELEMETRY=false
ENABLE_CRASH_REPORTS=false
"@

        $EnvContent | Out-File -FilePath $EnvFile -Encoding UTF8
        Write-Success "Environment file created"
    }

    # Start Docker services
    Write-Info "Starting MISA.AI services..."

    Set-Location $DockerDir

    try {
        Write-Info "Pulling Docker images..."
        docker-compose pull

        Write-Info "Starting services..."
        docker-compose up -d

        Write-Success "Services started"

        # Wait for services to be ready
        Write-Info "Waiting for services to initialize..."
        Start-Sleep -Seconds 30

        # Check if services are running
        $containers = docker-compose ps
        if ($containers -match "Up") {
            Write-Success "MISA.AI services are running"
        } else {
            Write-Warning "Some services may not be running correctly"
            Write-Info "Check service status with: docker-compose ps"
        }

        # Download default AI model
        Write-Info "Downloading default AI model (Mixtral)..."
        try {
            # Wait for Ollama to be ready
            $maxAttempts = 30
            $attempt = 0

            do {
                Start-Sleep -Seconds 2
                $attempt++
                $ollamaReady = docker exec misa-ollama ollama list 2>$null
            } until ($ollamaReady -or $attempt -eq $maxAttempts)

            if ($ollamaReady) {
                docker exec misa-ollama ollama pull mixtral
                Write-Success "Mixtral model downloaded"
            } else {
                Write-Warning "Could not download Mixtral model. You can download it later with:"
                Write-Info "docker exec misa-ollama ollama pull mixtral"
            }
        } catch {
            Write-Warning "Failed to download Mixtral model"
        }

        # Create management scripts
        Create-ManagementScripts

        Show-Success

    } catch {
        Write-Error "Failed to start services: $_"
        exit 1
    }
}

function Create-ManagementScripts {
    Write-Info "Creating management scripts..."

    $ScriptsDir = $InstallDir

    # Start script
    $StartScript = @"
@echo off
echo 🚀 Starting MISA.AI...
cd /d "$DockerDir"
docker-compose up -d
echo ✅ MISA.AI started
echo 📱 Web App: http://localhost:3000
echo 🔧 Kernel API: http://localhost:8080
pause
"@

    # Stop script
    $StopScript = @"
@echo off
echo 🛑 Stopping MISA.AI...
cd /d "$DockerDir"
docker-compose down
echo ✅ MISA.AI stopped
pause
"@

    # Status script
    $StatusScript = @"
@echo off
echo 📊 MISA.AI Status:
cd /d "$DockerDir"
docker-compose ps
echo.
echo 📱 Web App: http://localhost:3000
echo 🔧 Kernel API: http://localhost:8080
pause
"@

    # Update script
    $UpdateScript = @"
@echo off
echo 🔄 Updating MISA.AI...
cd /d "$DockerDir"
docker-compose pull
docker-compose up -d
echo ✅ MISA.AI updated
pause
"@

    $StartScript | Out-File -FilePath (Join-Path $ScriptsDir "start.bat") -Encoding ASCII
    $StopScript | Out-File -FilePath (Join-Path $ScriptsDir "stop.bat") -Encoding ASCII
    $StatusScript | Out-File -FilePath (Join-Path $ScriptsDir "status.bat") -Encoding ASCII
    $UpdateScript | Out-File -FilePath (Join-Path $ScriptsDir "update.bat") -Encoding ASCII

    Write-Success "Management scripts created"
}

function Show-Success {
    Write-ColorOutput "" -Color Green
    Write-ColorOutput "🎉 MISA.AI Installation Complete!" -Color Green
    Write-ColorOutput "" -Color Green
    Write-ColorOutput "📱 Access Your AI Assistant:" -Color Blue
    Write-ColorOutput "   Web Application: http://localhost:3000" -Color White
    Write-ColorOutput "   Kernel API:      http://localhost:8080" -Color White
    Write-ColorOutput "" -Color Green
    Write-ColorOutput "🔧 Management Commands:" -Color Blue
    Write-ColorOutput "   Start:   $InstallDir\start.bat" -Color White
    Write-ColorOutput "   Stop:    $InstallDir\stop.bat" -Color White
    Write-ColorOutput "   Status:  $InstallDir\status.bat" -Color White
    Write-ColorOutput "   Update:  $InstallDir\update.bat" -Color White
    Write-ColorOutput "" -Color Green
    Write-ColorOutput "📁 Installation Directory:" -Color Blue
    Write-ColorOutput "   $InstallDir" -Color White
    Write-ColorOutput "" -Color Green
    Write-ColorOutput "📚 Next Steps:" -Color Blue
    Write-ColorOutput "   1. Open http://localhost:3000 in your browser" -Color White
    Write-ColorOutput "   2. Complete the setup wizard" -Color White
    Write-ColorOutput "   3. Configure your AI models and preferences" -Color White
    Write-ColorOutput "   4. Start using your intelligent assistant!" -Color White
    Write-ColorOutput "" -Color Green
    Write-ColorOutput "🆘 Need Help?" -Color Blue
    Write-ColorOutput "   • Documentation: https://docs.misa.ai" -Color White
    Write-ColorOutput "   • Community: https://discord.gg/misa-ai" -Color White
    Write-ColorOutput "   • Issues: https://github.com/misa-ai/misa.ai/issues" -Color White
    Write-ColorOutput "" -Color Green
    Write-ColorOutput "✨ Welcome to MISA.AI - Your Privacy-First AI Assistant!" -Color Green
}

function Uninstall-MisaAI {
    Write-Info "🗑️  Uninstalling MISA.AI..."

    $InstallDir = "$env:USERPROFILE\.misa-ai"

    if (Test-Path $InstallDir) {
        Set-Location $InstallDir
        if (Test-Path "docker\docker-compose.yml") {
            cd docker
            docker-compose down -v 2>$null
            cd ..
        }
        Remove-Item -Recurse -Force $InstallDir
        Write-Success "MISA.AI uninstalled successfully"
    } else {
        Write-Info "MISA.AI is not installed"
    }
}

# Main execution
if ($Uninstall) {
    Uninstall-MisaAI
} else {
    Install-MisaAI
}