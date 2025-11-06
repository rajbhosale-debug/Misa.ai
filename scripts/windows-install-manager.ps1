# MISA.AI Windows Installation Manager
# Progressive installation with restart management and cross-platform coordination

param(
    [string]$CoordinatorHost = "",
    [int]$CoordinatorPort = 8081,
    [string]$InstallPhase = "auto",
    [switch]$Resume,
    [switch]$Quiet,
    [switch]$Force
)

# Global Variables
$Global:InstallDir = "$env:LOCALAPPDATA\MISA.AI"
$Global:LogDir = "$env:LOCALAPPDATA\MISA.AI\logs"
$Global:StateFile = "$Global:InstallDir\.install_state"
$Global:RestartMarker = "$Global:InstallDir\.restart_marker"
$Global:ConfigFile = "$Global:InstallDir\config.json"

# Installation Phases
$Global:Phases = @{
    "phase1" = @{
        Name = "Prerequisites"
        Description = "Installing .NET Framework and Visual C++ redistributables"
        Duration = 300
    }
    "phase2" = @{
        Name = "Windows Features"
        Description = "Enabling Hyper-V, Containers, and WSL"
        Duration = 600
    }
    "phase3" = @{
        Name = "Docker Desktop"
        Description = "Installing and configuring Docker Desktop"
        Duration = 180
    }
    "phase4" = @{
        Name = "MISA Services"
        Description = "Deploying MISA.AI services and configuration"
        Duration = 120
    }
    "phase5" = @{
        Name = "Mobile Coordination"
        Description = "Setting up mobile device coordination"
        Duration = 60
    }
}

# Logging Functions
function Write-InstallLog {
    param([string]$Message, [string]$Level = "INFO")

    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logMessage = "[$timestamp] [$Level] $Message"

    if (-not $Quiet) {
        switch ($Level) {
            "ERROR" { Write-Host $logMessage -ForegroundColor Red }
            "WARN"  { Write-Host $logMessage -ForegroundColor Yellow }
            "INFO"  { Write-Host $logMessage -ForegroundColor Green }
            "SUCCESS" { Write-Host $logMessage -ForegroundColor Cyan }
            default { Write-Host $logMessage }
        }
    }

    # Write to log file
    if (-not (Test-Path $Global:LogDir)) {
        New-Item -ItemType Directory -Path $Global:LogDir -Force | Out-Null
    }
    Add-Content -Path "$Global:LogDir\install.log" -Value $logMessage -ErrorAction SilentlyContinue
}

function Write-InstallSuccess {
    param([string]$Message)
    Write-InstallLog $Message "SUCCESS"
}

function Write-InstallError {
    param([string]$Message)
    Write-InstallLog $Message "ERROR"
}

function Write-InstallWarning {
    param([string]$Message)
    Write-InstallLog $Message "WARN"
}

# Installation State Management
function Save-InstallState {
    param([string]$Phase, [hashtable]$Data = @{})

    $stateData = @{
        Phase = $Phase
        Timestamp = Get-Date -Format "yyyy-MM-ddTHH:mm:ss"
        CoordinatorHost = $CoordinatorHost
        CoordinatorPort = $CoordinatorPort
        Progress = $Data
    }

    $stateJson = $stateData | ConvertTo-Json -Depth 10
    $stateJson | Out-File -FilePath $Global:StateFile -Encoding UTF8
    Write-InstallLog "Installation state saved: $Phase"
}

function Get-InstallState {
    if (Test-Path $Global:StateFile) {
        try {
            $stateJson = Get-Content -Path $Global:StateFile -Raw
            $stateData = $stateJson | ConvertFrom-Json
            return $stateData
        }
        catch {
            Write-InstallWarning "Failed to read installation state: $_"
            return $null
        }
    }
    return $null
}

function Create-RestartMarker {
    $timestamp = Get-Date -Format "yyyy-MM-ddTHH:mm:ss"
    $timestamp | Out-File -FilePath $Global:RestartMarker -Encoding UTF8
    Write-InstallLog "Restart marker created"
}

function Test-RestartRequired {
    if (Test-Path $Global:RestartMarker) {
        try {
            $restartTime = Get-Content -Path $Global:RestartMarker
            $restartDateTime = [DateTime]::ParseExact($restartTime, "yyyy-MM-ddTHH:mm:ss", $null)
            $elapsed = (Get-Date) - $restartDateTime

            # If restart was within last 10 minutes, consider it related
            if ($elapsed.TotalMinutes -lt 10) {
                return $true
            }
        }
        catch {
            Write-InstallWarning "Invalid restart marker format"
        }
    }
    return $false
}

# Phase-based Installation Functions
function Start-PhaseInstallation {
    param([string]$StartPhase = "auto")

    Write-InstallLog "Starting phase-based installation"

    # Create installation directory
    if (-not (Test-Path $Global:InstallDir)) {
        New-Item -ItemType Directory -Path $Global:InstallDir -Force | Out-Null
    }

    $currentState = Get-InstallState
    $currentPhase = if ($Resume -and $currentState) { $currentState.Phase } else { $StartPhase }

    if ($currentPhase -eq "auto") {
        $currentPhase = "phase1"
    }

    Write-InstallLog "Starting installation from phase: $currentPhase"

    # Execute phases in order
    $phases = @("phase1", "phase2", "phase3", "phase4", "phase5")
    $startIndex = $phases.IndexOf($currentPhase)

    if ($startIndex -ge 0) {
        for ($i = $startIndex; $i -lt $phases.Count; $i++) {
            $phase = $phases[$i]
            Write-InstallLog "=== Starting $($Global:Phases[$phase].Name) ==="

            try {
                switch ($phase) {
                    "phase1" { Install-Phase1 }
                    "phase2" { Install-Phase2 }
                    "phase3" { Install-Phase3 }
                    "phase4" { Install-Phase4 }
                    "phase5" { Install-Phase5 }
                }

                Save-InstallState -Phase $phase
                Write-InstallSuccess "Completed $($Global:Phases[$phase].Name)"

                # Check if restart is needed after this phase
                if (Test-RestartNeeded -Phase $phase) {
                    Write-InstallWarning "System restart required after $($Global:Phases[$phase].Name)"
                    Create-RestartMarker
                    return $false
                }
            }
            catch {
                Write-InstallError "Failed $($Global:Phases[$phase].Name): $_"
                throw
            }
        }
    }

    # Installation complete
    Remove-Item -Path $Global:StateFile -ErrorAction SilentlyContinue
    Remove-Item -Path $Global:RestartMarker -ErrorAction SilentlyContinue
    Write-InstallSuccess "Installation completed successfully!"

    return $true
}

function Install-Phase1 {
    Write-InstallLog "Installing prerequisites: .NET Framework and Visual C++ redistributables"

    # Import existing prerequisites script
    $prereqsScript = Join-Path $PSScriptRoot "install-prereqs.ps1"
    if (Test-Path $prereqsScript) {
        & $prereqsScript -Quiet:$Quiet
    }
    else {
        Write-InstallWarning "Prerequisites script not found, installing manually"

        # Install .NET Framework 4.8
        Install-NETFramework48

        # Install Visual C++ redistributables
        Install-VisualCppRedistributables
    }

    # Validate prerequisites
    Test-PrerequisitesValidation
}

function Install-Phase2 {
    Write-InstallLog "Enabling Windows features: Hyper-V, Containers, WSL"

    $features = @(
        @{Name = "Microsoft-Hyper-V-All"; DisplayName = "Hyper-V"},
        @{Name = "Containers"; DisplayName = "Containers"},
        @{Name = "Microsoft-Windows-Subsystem-Linux"; DisplayName = "WSL"},
        @{Name = "VirtualMachinePlatform"; DisplayName = "VM Platform"}
    )

    $restartNeeded = $false

    foreach ($feature in $features) {
        $featureState = Get-WindowsOptionalFeature -Online -FeatureName $feature.Name -ErrorAction SilentlyContinue

        if ($featureState -and $featureState.State -eq "Disabled") {
            Write-InstallLog "Enabling $($feature.DisplayName)..."
            try {
                Enable-WindowsOptionalFeature -Online -FeatureName $feature.Name -NoRestart -WarningAction SilentlyContinue
                Write-InstallSuccess "Enabled $($feature.DisplayName)"

                if ($featureState.RestartNeeded) {
                    $restartNeeded = $true
                }
            }
            catch {
                Write-InstallWarning "Failed to enable $($feature.DisplayName): $_"
            }
        }
        elseif ($featureState -and $featureState.State -eq "Enabled") {
            Write-InstallLog "$($feature.DisplayName) already enabled"
        }
    }

    if ($restartNeeded) {
        $Global:Phase2RestartNeeded = $true
    }
}

function Install-Phase3 {
    Write-InstallLog "Installing Docker Desktop"

    # Check if Docker Desktop is already installed
    $dockerDesktop = Get-Command "docker" -ErrorAction SilentlyContinue
    if ($dockerDesktop) {
        Write-InstallLog "Docker already installed"
        return
    }

    # Download Docker Desktop
    $dockerUrl = "https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe"
    $dockerInstaller = Join-Path $env:TEMP "DockerDesktopInstaller.exe"

    Write-InstallLog "Downloading Docker Desktop..."
    try {
        Invoke-WebRequest -Uri $dockerUrl -OutFile $dockerInstaller -UseBasicParsing
    }
    catch {
        Write-InstallError "Failed to download Docker Desktop: $_"
        throw
    }

    # Install Docker Desktop
    Write-InstallLog "Installing Docker Desktop..."
    try {
        $process = Start-Process -FilePath $dockerInstaller -ArgumentList "--quiet", "--accept-license" -Wait -PassThru

        if ($process.ExitCode -eq 0) {
            Write-InstallSuccess "Docker Desktop installed successfully"
        }
        else {
            Write-InstallWarning "Docker Desktop installation returned exit code: $($process.ExitCode)"
        }
    }
    catch {
        Write-InstallError "Failed to install Docker Desktop: $_"
        throw
    }
    finally {
        Remove-Item $dockerInstaller -Force -ErrorAction SilentlyContinue
    }

    # Start Docker Desktop
    Write-InstallLog "Starting Docker Desktop..."
    Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"

    # Wait for Docker to be ready
    $maxWait = 120
    $waitTime = 0
    do {
        Start-Sleep 5
        $waitTime += 5

        try {
            $dockerVersion = docker --version 2>$null
            if ($dockerVersion) {
                Write-InstallSuccess "Docker is ready"
                break
            }
        }
        catch {
            # Docker not ready yet
        }

        if ($waitTime -ge $maxWait) {
            Write-InstallWarning "Docker startup timed out"
            break
        }
    } while ($true)
}

function Install-Phase4 {
    Write-InstallLog "Deploying MISA.AI services"

    # Create service directories
    $serviceDir = "$Global:InstallDir\services"
    $configDir = "$Global:InstallDir\config"
    $dataDir = "$Global:InstallDir\data"

    New-Item -ItemType Directory -Path $serviceDir -Force | Out-Null
    New-Item -ItemType Directory -Path $configDir -Force | Out-Null
    New-Item -ItemType Directory -Path $dataDir -Force | Out-Null

    # Download MISA.AI services
    $latestRelease = "https://api.github.com/repos/misa-ai/misa.ai/releases/latest"
    try {
        $releaseInfo = Invoke-RestMethod -Uri $latestRelease
        $downloadUrl = ($releaseInfo.assets | Where-Object { $_.name -like "*windows*.zip" }).browser_download_url

        if (-not $downloadUrl) {
            $downloadUrl = "https://download.misa.ai/misa-ai-windows-latest.zip"
        }

        $zipFile = Join-Path $env:TEMP "misa-ai-windows.zip"
        Write-InstallLog "Downloading MISA.AI services..."
        Invoke-WebRequest -Uri $downloadUrl -OutFile $zipFile -UseBasicParsing

        # Extract services
        Write-InstallLog "Extracting MISA.AI services..."
        Expand-Archive -Path $zipFile -DestinationPath $serviceDir -Force

        # Create docker-compose.yml for Windows
        $dockerCompose = @"
version: '3.8'

services:
  misa-kernel:
    image: misa-ai/kernel:latest
    container_name: misa-kernel
    ports:
      - "8080:8080"
      - "50051:50051"
    volumes:
      - $($dataDir -replace '\\', '/'):/data
      - $($configDir -replace '\\', '/'):/config
      - $($Global:LogDir -replace '\\', '/'):/logs
    environment:
      - RUST_LOG=info
      - MISA_DATA_DIR=/data
      - MISA_CONFIG_DIR=/config
    restart: unless-stopped
    healthcheck:
      test: `["CMD", "curl", "-f", "http://localhost:8080/health"]`
      interval: 30s
      timeout: 10s
      retries: 3

  ollama:
    image: ollama/ollama:latest
    container_name: misa-ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    restart: unless-stopped
    healthcheck:
      test: `["CMD", "curl", "-f", "http://localhost:11434/api/tags"]`
      interval: 30s
      timeout: 10s
      retries: 3

  postgres:
    image: postgres:15-alpine
    container_name: misa-postgres
    environment:
      POSTGRES_DB: misa_ai
      POSTGRES_USER: misa
      POSTGRES_PASSWORD: misa_secure_password_change_me
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped
    healthcheck:
      test: `["CMD-SHELL", "pg_isready -U misa"]`
      interval: 30s
      timeout: 10s
      retries: 3

  redis:
    image: redis:7-alpine
    container_name: misa-redis
    command: redis-server --appendonly yes
    volumes:
      - redis_data:/data
    restart: unless-stopped
    healthcheck:
      test: `["CMD", "redis-cli", "ping"]`
      interval: 30s
      timeout: 10s
      retries: 3

  web-app:
    image: misa-ai/web:latest
    container_name: misa-web
    ports:
      - "3000:80"
    environment:
      - REACT_APP_API_URL=http://localhost:8080
    depends_on:
      - misa-kernel
    restart: unless-stopped

volumes:
  ollama_data:
  postgres_data:
  redis_data:

networks:
  default:
    name: misa-ai-network
"@

        $dockerCompose | Out-File -FilePath "$serviceDir\docker-compose.yml" -Encoding UTF8

        # Create configuration file
        $config = @{
            Network = @{
                WebSocketPort = 8080
                GrpcPort = 50051
                TlsEnabled = $false
                BindAddress = "0.0.0.0"
            }
            Models = @{
                DefaultModel = "mixtral"
                LocalServerUrl = "http://localhost:11434"
                SwitchingPreferences = @{
                    PreferLocal = $true
                }
                AutoDownloadModels = $false
            }
            Devices = @{
                DiscoveryEnabled = $true
                RemoteDesktopEnabled = $true
                MaxDevices = 5
            }
            Security = @{
                AuthRequired = $true
                SessionTimeoutMinutes = 30
                PluginSandboxing = $true
                AuditLogging = $true
            }
            Privacy = @{
                DataCollection = $false
                CrashReports = $false
                UsageAnalytics = $false
                LocalOnlyProcessing = $true
            }
        }

        $config | ConvertTo-Json -Depth 10 | Out-File -FilePath $Global:ConfigFile -Encoding UTF8

        # Start services
        Write-InstallLog "Starting MISA.AI services..."
        Set-Location $serviceDir
        docker-compose up -d

        # Wait for services to be ready
        Start-Sleep 30

        Write-InstallSuccess "MISA.AI services deployed and started"

        Remove-Item $zipFile -Force -ErrorAction SilentlyContinue
    }
    catch {
        Write-InstallError "Failed to deploy MISA.AI services: $_"
        throw
    }
}

function Install-Phase5 {
    Write-InstallLog "Setting up mobile device coordination"

    if ($CoordinatorHost) {
        Write-InstallLog "Configuring mobile coordination with $CoordinatorHost`:$CoordinatorPort"

        # Start coordination client
        Start-CoordinationClient -Host $CoordinatorHost -Port $CoordinatorPort

        # Discover mobile devices
        $mobileDevices = Discover-MobileDevices
        if ($mobileDevices.Count -gt 0) {
            Write-InstallSuccess "Found $($mobileDevices.Count) mobile device(s)"

            foreach ($device in $mobileDevices) {
                Write-InstallLog "Coordinating with device: $($device.Name)"
                Send-CoordinationRequest -Device $device
            }
        }
        else {
            Write-InstallLog "No mobile devices found for coordination"
        }
    }
    else {
        Write-InstallWarning "No coordinator host specified, skipping mobile coordination"
    }
}

# Coordination Functions
function Start-CoordinationClient {
    param([string]$Host, [int]$Port)

    Write-InstallLog "Starting coordination client for $Host`:$Port"

    # Create coordination client script
    $clientScript = @"
# MISA.AI Coordination Client
param(
    [string]`$Host = "$Host",
    [int]`$Port = $Port
)

while (`$true) {
    try {
        `$response = Invoke-RestMethod -Uri "http://`$Host`:`$Port/misa/status" -TimeoutSec 5
        Write-Host "Coordinator status: `$($response.status)"
    }
    catch {
        Write-Warning "Cannot reach coordinator: `$(`$_)"
    }

    Start-Sleep 30
}
"@

    $scriptPath = "$Global:InstallDir\coordination-client.ps1"
    $clientScript | Out-File -FilePath $scriptPath -Encoding UTF8

    # Start in background
    Start-Process PowerShell -ArgumentList "-ExecutionPolicy Bypass", "-File `"$scriptPath`"" -WindowStyle Hidden
}

function Discover-MobileDevices {
    $devices = @()

    # Scan local network for MISA Android devices
    $localIP = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -like "192.168.*" -or $_.IPAddress -like "10.*" -or $_.IPAddress -like "172.*" }).IPAddress | Select-Object -First 1

    if ($localIP) {
        $network = $localIP.Split('.')[0..2] -join '.'

        for ($i = 1; $i -le 254; $i++) {
            $targetIP = "$network.$i"

            # Quick check for MISA Android app
            $job = Start-Job -ScriptBlock {
                param(`$IP, `$Port)
                try {
                    `$response = Invoke-WebRequest -Uri "http://`$IP`:`$Port/misa/ping" -TimeoutSec 2 -ErrorAction Stop
                    if (`$response.Content -like "*MISA*") {
                        return @{
                            IP = `$IP
                            Port = `$Port
                            Name = "MISA Device at `$IP"
                        }
                    }
                }
                catch {
                    # Device not responding or not MISA
                }
                return `$null
            } -ArgumentList $targetIP, 8082

            # Limit concurrent jobs
            if ((Get-Job -State Running).Count -ge 20) {
                Get-Job -State Running | Wait-Job -Any | Remove-Job
            }
        }

        # Wait for all jobs and collect results
        Get-Job | Wait-Job | ForEach-Object {
            $result = Receive-Job -Job $_
            if ($result) {
                $devices += $result
            }
            Remove-Job $_
        }
    }

    return $devices
}

function Send-CoordinationRequest {
    param([hashtable]$Device)

    $request = @{
        type = "installation_request"
        coordinator_host = $CoordinatorHost
        coordinator_port = $CoordinatorPort
        device_info = $Device
        timestamp = Get-Date -Format "yyyy-MM-ddTHH:mm:ss"
    } | ConvertTo-Json -Depth 10

    try {
        $response = Invoke-RestMethod -Uri "http://$($Device.IP):$($Device.Port)/misa/install" -Method Post -Body $request -ContentType "application/json" -TimeoutSec 10
        Write-InstallSuccess "Coordination request sent to $($Device.Name)"
    }
    catch {
        Write-InstallWarning "Failed to send coordination request to $($Device.Name): $_"
    }
}

# Utility Functions
function Test-RestartNeeded {
    param([string]$Phase)

    if ($Phase -eq "phase2" -and $Global:Phase2RestartNeeded) {
        return $true
    }

    return $false
}

function Install-NETFramework48 {
    Write-InstallLog "Checking .NET Framework 4.8..."

    # Check if already installed
    $net48 = Get-ChildItem "HKLM:SOFTWARE\Microsoft\NET Framework Setup\NDP\v4\Full\" -ErrorAction SilentlyContinue | Get-ItemProperty -Name Release -ErrorAction SilentlyContinue | Where-Object { $_.Release -ge 528040 }

    if (-not $net48) {
        Write-InstallLog "Installing .NET Framework 4.8..."

        $dotNet48Url = "https://download.microsoft.com/download/6/E/4/6E48E8AB-DC00-419A-B348-907570FF15D3/NDP48-x86-x64-AllOS-ENU.exe"
        $dotNet48File = Join-Path $env:TEMP "NDP48-x86-x64-AllOS-ENU.exe"

        try {
            Invoke-WebRequest -Uri $dotNet48Url -OutFile $dotNet48File -UseBasicParsing
            $process = Start-Process -FilePath $dotNet48File -ArgumentList "/quiet", "/norestart" -Wait -PassThru

            if ($process.ExitCode -eq 0) {
                Write-InstallSuccess ".NET Framework 4.8 installed successfully"
            }
        }
        finally {
            Remove-Item $dotNet48File -Force -ErrorAction SilentlyContinue
        }
    }
    else {
        Write-InstallLog ".NET Framework 4.8 already installed"
    }
}

function Install-VisualCppRedistributables {
    Write-InstallLog "Installing Visual C++ Redistributables..."

    $redistributables = @(
        @{Name="VC++ 2015-2022 x64"; Url="https://aka.ms/vs/17/release/vc_redist.x64.exe"; File="vc_redist.x64.exe"},
        @{Name="VC++ 2015-2022 x86"; Url="https://aka.ms/vs/17/release/vc_redist.x86.exe"; File="vc_redist.x86.exe"}
    )

    foreach ($redist in $redistributables) {
        Write-InstallLog "Installing $($redist.Name)..."

        try {
            $redistFile = Join-Path $env:TEMP $redist.File
            Invoke-WebRequest -Uri $redist.Url -OutFile $redistFile -UseBasicParsing

            $process = Start-Process -FilePath $redistFile -ArgumentList "/quiet", "/norestart" -Wait -PassThru

            if ($process.ExitCode -eq 0) {
                Write-InstallSuccess "$($redist.Name) installed successfully"
            }

            Remove-Item $redistFile -Force -ErrorAction SilentlyContinue
        }
        catch {
            Write-InstallWarning "Failed to install $($redist.Name): $_"
        }
    }
}

function Test-PrerequisitesValidation {
    Write-InstallLog "Validating prerequisites..."

    $issues = @()

    # Test .NET Framework
    $net48 = Get-ChildItem "HKLM:SOFTWARE\Microsoft\NET Framework Setup\NDP\v4\Full\" -ErrorAction SilentlyContinue | Get-ItemProperty -Name Release -ErrorAction SilentlyContinue | Where-Object { $_.Release -ge 528040 }
    if (-not $net48) {
        $issues += ".NET Framework 4.8 is not properly installed"
    }

    if ($issues.Count -gt 0) {
        Write-InstallError "Prerequisites validation failed:"
        $issues | ForEach-Object { Write-InstallError "  - $_" }
        throw "Prerequisites installation incomplete"
    }

    Write-InstallSuccess "All prerequisites validated successfully"
}

# Main Execution
try {
    Write-InstallLog "Starting MISA.AI Windows Installation Manager"
    Write-InstallLog "Install directory: $Global:InstallDir"
    Write-InstallLog "Coordinator: $CoordinatorHost`:$CoordinatorPort"

    # Check if running as administrator
    $currentUser = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($currentUser)
    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        Write-InstallError "Administrator privileges required"
        exit 1
    }

    # Check for resume scenario
    if ($Resume) {
        $currentState = Get-InstallState
        if ($currentState) {
            Write-InstallLog "Resuming installation from phase: $($currentState.Phase)"
            $InstallPhase = $currentState.Phase
        }
        else {
            Write-InstallWarning "No installation state found for resume"
        }
    }

    # Start phase-based installation
    $completed = Start-PhaseInstallation -StartPhase $InstallPhase

    if ($completed) {
        Write-InstallSuccess "MISA.AI installation completed successfully!"
        Write-InstallLog "Web App: http://localhost:3000"
        Write-InstallLog "Kernel API: http://localhost:8080"
        Write-InstallLog "Logs: $Global:LogDir"
    }
    else {
        Write-InstallLog "System restart required. Installation will continue automatically after restart."
        exit 3010  # Windows Installer code for restart required
    }
}
catch {
    Write-InstallError "Installation failed: $_"
    exit 1
}