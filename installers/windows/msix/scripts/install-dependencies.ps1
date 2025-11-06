# MISA.AI Dependencies Installation Script
# This script installs runtime dependencies for MISA.AI

param(
    [switch]$Force,
    [switch]$Quiet,
    [string]$InstallPath = "$env:ProgramFiles\MISA.AI"
)

function Write-Log {
    param([string]$Message, [string]$Level = "INFO")
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logMessage = "[$timestamp] [$Level] $Message"

    if (-not $Quiet) {
        switch ($Level) {
            "ERROR" { Write-Host $logMessage -ForegroundColor Red }
            "WARN"  { Write-Host $logMessage -ForegroundColor Yellow }
            "INFO"  { Write-Host $logMessage -ForegroundColor Green }
            default { Write-Host $logMessage }
        }
    }
}

function Test-DockerInstallation {
    Write-Log "Checking Docker Desktop installation..."

    $dockerService = Get-Service -Name "Docker Desktop Service" -ErrorAction SilentlyContinue
    $dockerDesktop = Get-Process -Name "Docker Desktop" -ErrorAction SilentlyContinue

    if (-not $dockerService -and -not $dockerDesktop) {
        return $false
    }

    # Test if Docker is running
    try {
        $dockerVersion = & docker --version 2>$null
        if ($dockerVersion) {
            Write-Log "Docker is installed and running: $dockerVersion"
            return $true
        }
    }
    catch {
        Write-Log "Docker is installed but not running" "WARN"
        return $false
    }

    return $false
}

function Install-DockerDesktop {
    Write-Log "Installing Docker Desktop..."

    try {
        # Use bundled Docker Desktop installer
        $dockerInstaller = Join-Path $PSScriptRoot "..\bundles\DockerDesktopInstaller.exe"

        if (-not (Test-Path $dockerInstaller)) {
            # Fallback to web download
            $dockerUrl = "https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe"
            $dockerFile = Join-Path $env:TEMP "DockerDesktopInstaller.exe"
            Write-Log "Downloading Docker Desktop..."
            Invoke-WebRequest -Uri $dockerUrl -OutFile $dockerFile -UseBasicParsing
        } else {
            $dockerFile = $dockerInstaller
            Write-Log "Using bundled Docker Desktop installer"
        }

        Write-Log "Installing Docker Desktop (this may take several minutes)..."
        $process = Start-Process -FilePath $dockerFile -ArgumentList "install", "--quiet", "--accept-license" -Wait -PassThru

        if ($process.ExitCode -eq 0) {
            Write-Log "Docker Desktop installed successfully"

            # Start Docker Desktop service
            Write-Log "Starting Docker Desktop..."
            Start-Process -FilePath "C:\Program Files\Docker\Docker\Docker Desktop.exe" -WindowStyle Hidden

            # Wait for Docker to be ready
            $maxWait = 120  # 2 minutes
            $waited = 0
            do {
                Start-Sleep 5
                $waited += 5

                try {
                    $dockerVersion = & docker --version 2>$null
                    if ($dockerVersion) {
                        Write-Log "Docker Desktop is ready"
                        break
                    }
                }
                catch {
                    if ($waited -ge $maxWait) {
                        Write-Log "Docker Desktop startup timeout" "WARN"
                        break
                    }
                    Write-Log "Waiting for Docker Desktop to start... ($waited/$maxWait seconds)"
                }
            } while ($true)
        } else {
            Write-Log "Docker Desktop installation failed with exit code: $($process.ExitCode)" "ERROR"
            throw "Docker Desktop installation failed"
        }

        # Cleanup downloaded file if it was downloaded
        if ($dockerFile -ne $dockerInstaller) {
            Remove-Item $dockerFile -Force -ErrorAction SilentlyContinue
        }
    }
    catch {
        Write-Log "Failed to install Docker Desktop`n$_" "ERROR"
        throw
    }
}

function Test-OllamaInstallation {
    Write-Log "Checking Ollama installation..."

    try {
        $ollamaVersion = & ollama --version 2>$null
        if ($ollamaVersion) {
            Write-Log "Ollama is installed: $ollamaVersion"
            return $true
        }
    }
    catch {
        return $false
    }

    return $false
}

function Install-Ollama {
    Write-Log "Installing Ollama..."

    try {
        # Use bundled Ollama installer if available
        $ollamaInstaller = Join-Path $PSScriptRoot "..\bundles\ollama-windows.exe"

        if (Test-Path $ollamaInstaller) {
            Write-Log "Using bundled Ollama installer"
            $ollamaFile = $ollamaInstaller
        } else {
            # Download from official source
            $ollamaUrl = "https://ollama.ai/download/OllamaSetup.exe"
            $ollamaFile = Join-Path $env:TEMP "OllamaSetup.exe"
            Write-Log "Downloading Ollama..."
            Invoke-WebRequest -Uri $ollamaUrl -OutFile $ollamaFile -UseBasicParsing
        }

        Write-Log "Installing Ollama..."
        $process = Start-Process -FilePath $ollamaFile -ArgumentList "/S" -Wait -PassThru

        if ($process.ExitCode -eq 0) {
            Write-Log "Ollama installed successfully"

            # Add Ollama to PATH if not already there
            $ollamaPath = "$env:ProgramFiles\Ollama"
            $currentPath = [Environment]::GetEnvironmentVariable("PATH", "Machine")

            if ($currentPath -notlike "*$ollamaPath*") {
                Write-Log "Adding Ollama to system PATH..."
                [Environment]::SetEnvironmentVariable("PATH", "$currentPath;$ollamaPath", "Machine")
                $env:PATH = "$env:PATH;$ollamaPath"
            }

            # Start Ollama service
            Write-Log "Starting Ollama service..."
            Start-Process -FilePath "$ollamaPath\ollama.exe" -ArgumentList "serve" -WindowStyle Hidden

            # Wait a moment for service to start
            Start-Sleep 3

            # Verify installation
            try {
                $ollamaVersion = & ollama --version 2>$null
                if ($ollamaVersion) {
                    Write-Log "Ollama is running: $ollamaVersion"
                }
            }
            catch {
                Write-Log "Ollama installed but service may need manual start" "WARN"
            }
        } else {
            Write-Log "Ollama installation failed with exit code: $($process.ExitCode)" "ERROR"
            throw "Ollama installation failed"
        }

        # Cleanup downloaded file if it was downloaded
        if ($ollamaFile -ne $ollamaInstaller) {
            Remove-Item $ollamaFile -Force -ErrorAction SilentlyContinue
        }
    }
    catch {
        Write-Log "Failed to install Ollama`n$_" "ERROR"
        throw
    }
}

function Install-AIModels {
    param([string[]]$Models = @("mixtral"))

    Write-Log "Installing AI models..."

    # Check if Ollama is ready
    $maxWait = 30
    $waited = 0
    do {
        try {
            $ollamaList = & ollama list 2>$null
            if ($LASTEXITCODE -eq 0) {
                break
            }
        }
        catch {
            if ($waited -ge $maxWait) {
                Write-Log "Timeout waiting for Ollama to be ready" "WARN"
                return
            }
        }

        Start-Sleep 2
        $waited += 2
        Write-Log "Waiting for Ollama to be ready... ($waited/$maxWait seconds)"
    } while ($true)

    # Install requested models
    foreach ($model in $Models) {
        try {
            Write-Log "Installing model: $model"
            $process = Start-Process -FilePath "ollama" -ArgumentList "pull", $model -Wait -PassThru

            if ($process.ExitCode -eq 0) {
                Write-Log "Successfully installed model: $model"
            } else {
                Write-Log "Failed to install model: $model (exit code: $($process.ExitCode))" "WARN"
            }
        }
        catch {
            Write-Log "Error installing model $model`n$_" "WARN"
        }
    }
}

function Configure-Firewall {
    Write-Log "Configuring Windows Firewall..."

    try {
        # Allow MISA.AI through Windows Firewall
        $firewallRules = @(
            @{Name="MISA.AI - Main Application"; Program="$InstallPath\MisaAI.exe"; Ports=""},
            @{Name="MISA.AI - Kernel Service"; Program="$InstallPath\MisaKernel.exe"; Ports="8080,8443"},
            @{Name="MISA.AI - Web Interface"; Program=""; Ports="3000"}
        )

        foreach ($rule in $firewallRules) {
            try {
                if ($rule.Program -and (Test-Path $rule.Program)) {
                    Write-Log "Adding firewall rule: $($rule.Name)"
                    New-NetFirewallRule -DisplayName $rule.Name -Direction Inbound -Program $rule.Program -Action Allow -Protocol TCP -ErrorAction SilentlyContinue
                }

                if ($rule.Ports) {
                    Write-Log "Adding firewall rule for ports $($rule.Ports): $($rule.Name)"
                    New-NetFirewallRule -DisplayName $rule.Name -Direction Inbound -Protocol TCP -LocalPort $rule.Ports -Action Allow -ErrorAction SilentlyContinue
                }
            }
            catch {
                Write-Log "Failed to create firewall rule: $($rule.Name)`n$_" "WARN"
            }
        }
    }
    catch {
        Write-Log "Firewall configuration failed`n$_" "WARN"
    }
}

# Main execution
try {
    Write-Log "Starting MISA.AI dependencies installation..."
    Write-Log "Install path: $InstallPath"

    # Check administrator privileges
    $currentUser = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($currentUser)
    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        Write-Log "Administrator privileges required for dependencies installation" "WARN"
        # Continue anyway for user-level installations
    }

    # Install Docker Desktop if not present
    if (-not (Test-DockerInstallation) -or $Force) {
        Install-DockerDesktop
    } else {
        Write-Log "Docker Desktop is already installed"
    }

    # Install Ollama if not present
    if (-not (Test-OllamaInstallation) -or $Force) {
        Install-Ollama
    } else {
        Write-Log "Ollama is already installed"
    }

    # Install basic AI models
    Install-AIModels -Models @("mixtral")

    # Configure firewall
    Configure-Firewall

    Write-Log "Dependencies installation completed successfully!"
}
catch {
    Write-Log "Dependencies installation failed:`n$_" "ERROR"
    exit 1
}

exit 0