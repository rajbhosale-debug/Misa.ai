# MISA.AI Windows Installation Coordinator Script
# Phase-based installation with restart management and mobile device coordination

param(
    [string]$CoordinatorHost = "",
    [int]$CoordinatorPort = 8081,
    [string]$InstallPhase = "auto",
    [switch]$Resume,
    [switch]$Force,
    [switch]$Quiet
)

# Global Variables
$Global:InstallDir = "$env:ProgramFiles\MISA.AI"
$Global:UserDataDir = "$env:LOCALAPPDATA\MISA.AI"
$Global:LogDir = "$env:LOCALAPPDATA\MISA.AI\logs"
$Global:StateFile = "$Global:UserDataDir\.install_state"
$Global:RestartMarker = "$Global:UserDataDir\.restart_marker"
$Global:CoordinationPort = $CoordinatorPort

# Installation Phases Configuration
$Global:InstallationPhases = @{
    "prerequisites" = @{
        Name = "System Prerequisites"
        Description = "Installing .NET Framework and Visual C++ redistributables"
        EstimatedMinutes = 5
        RestartRequired = $false
        RequiredFeatures = @("NETFramework", "VCRedistributables")
    }
    "windows_features" = @{
        Name = "Windows Features"
        Description = "Enabling Hyper-V, Containers, and WSL"
        EstimatedMinutes = 15
        RestartRequired = $true
        RequiredFeatures = @("Microsoft-Hyper-V-All", "Containers", "Microsoft-Windows-Subsystem-Linux", "VirtualMachinePlatform")
    }
    "docker_setup" = @{
        Name = "Docker Desktop"
        Description = "Installing and configuring Docker Desktop"
        EstimatedMinutes = 10
        RestartRequired = $false
        RequiredFeatures = @("DockerDesktop")
    }
    "misa_services" = @{
        Name = "MISA.AI Services"
        Description = "Deploying and configuring MISA.AI services"
        EstimatedMinutes = 5
        RestartRequired = $false
        RequiredFeatures = @("MisaKernel", "MisaWeb", "MisaDatabase")
    }
    "coordination_setup" = @{
        Name = "Device Coordination"
        Description = "Setting up mobile device coordination and pairing"
        EstimatedMinutes = 3
        RestartRequired = $false
        RequiredFeatures = @("CoordinationServer", "DeviceDiscovery")
    }
}

# Logging Functions
function Write-CoordinationLog {
    param(
        [string]$Message,
        [ValidateSet("INFO", "SUCCESS", "WARNING", "ERROR", "DEBUG")]
        [string]$Level = "INFO"
    )

    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $logMessage = "[$timestamp] [$Level] $Message"

    # Console output
    if (-not $Quiet) {
        switch ($Level) {
            "ERROR" { Write-Host $logMessage -ForegroundColor Red }
            "WARNING" { Write-Host $logMessage -ForegroundColor Yellow }
            "SUCCESS" { Write-Host $logMessage -ForegroundColor Green }
            "DEBUG" { Write-Host $logMessage -ForegroundColor Gray }
            default { Write-Host $logMessage -ForegroundColor Cyan }
        }
    }

    # File logging
    if (-not (Test-Path $Global:LogDir)) {
        New-Item -ItemType Directory -Path $Global:LogDir -Force | Out-Null
    }
    $logFile = Join-Path $Global:LogDir "coordination-install-$(Get-Date -Format 'yyyyMMdd').log"
    Add-Content -Path $logFile -Value $logMessage -ErrorAction SilentlyContinue

    # Event logging for errors
    if ($Level -eq "ERROR") {
        try {
            Write-EventLog -LogName Application -Source "MISA.AI Coordinator" -EventId 1002 -EntryType Error -Message $Message -ErrorAction SilentlyContinue
        } catch {
            # Event log might not be available
        }
    }
}

function Write-ProgressInfo {
    param(
        [string]$Activity,
        [string]$Status,
        [int]$PercentComplete = -1,
        [int]$SecondsRemaining = -1
    )

    if (-not $Quiet) {
        $progressParams = @{
            Activity = $Activity
            Status = $Status
        }

        if ($PercentComplete -ge 0) {
            $progressParams.PercentComplete = $PercentComplete
        }

        if ($SecondsRemaining -ge 0) {
            $progressParams.SecondsRemaining = $SecondsRemaining
        }

        Write-Progress @progressParams
    }
}

# State Management
function Save-InstallationState {
    param(
        [string]$Phase,
        [hashtable]$Progress = @{},
        [string]$Status = "running"
    )

    $stateData = @{
        Phase = $Phase
        Status = $Status
        StartTime = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
        CoordinatorHost = $CoordinatorHost
        CoordinatorPort = $CoordinatorPort
        Progress = $Progress
        RestartCount = (Get-RestartCount) + 1
    }

    try {
        $stateJson = $stateData | ConvertTo-Json -Depth 10
        $stateJson | Out-File -FilePath $Global:StateFile -Encoding UTF8 -Force
        Write-CoordinationLog "Installation state saved: $Phase ($Status)" "DEBUG"
    } catch {
        Write-CoordinationLog "Failed to save installation state: $_" "ERROR"
    }
}

function Get-InstallationState {
    if (Test-Path $Global:StateFile) {
        try {
            $stateJson = Get-Content -Path $Global:StateFile -Raw
            return $stateJson | ConvertFrom-Json
        } catch {
            Write-CoordinationLog "Failed to read installation state: $_" "WARNING"
            return $null
        }
    }
    return $null
}

function Get-RestartCount {
    $state = Get-InstallationState
    if ($state -and $state.RestartCount) {
        return [int]$state.RestartCount
    }
    return 0
}

function Create-RestartMarker {
    param([string]$Reason = "installation")

    $restartData = @{
        Reason = $Reason
        Timestamp = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ss")
        CurrentPhase = (Get-InstallationState).Phase
        ResumeOnRestart = $true
    }

    try {
        $restartJson = $restartData | ConvertTo-Json -Depth 5
        $restartJson | Out-File -FilePath $Global:RestartMarker -Encoding UTF8 -Force
        Write-CoordinationLog "Restart marker created: $Reason" "INFO"
    } catch {
        Write-CoordinationLog "Failed to create restart marker: $_" "ERROR"
    }
}

function Test-ResumeRequired {
    if (Test-Path $Global:RestartMarker) {
        try {
            $restartJson = Get-Content -Path $Global:RestartMarker -Raw
            $restartData = $restartJson | ConvertFrom-Json

            if ($restartData.ResumeOnRestart -and $restartData.CurrentPhase) {
                $restartTime = [DateTime]::ParseExact($restartData.Timestamp, "yyyy-MM-ddTHH:mm:ss", $null)
                $elapsed = (Get-Date) - $restartTime

                # Resume if restart was within last 30 minutes
                if ($elapsed.TotalMinutes -lt 30) {
                    Write-CoordinationLog "Resume required from phase: $($restartData.CurrentPhase)" "INFO"
                    return @{
                        Resume = $true
                        Phase = $restartData.CurrentPhase
                        Reason = $restartData.Reason
                    }
                }
            }
        } catch {
            Write-CoordinationLog "Invalid restart marker format" "WARNING"
        }
    }

    # Clean up old restart marker
    if (Test-Path $Global:RestartMarker) {
        Remove-Item $Global:RestartMarker -Force -ErrorAction SilentlyContinue
    }

    return @{ Resume = $false }
}

# Phase Implementation Functions
function Invoke-PhaseBasedInstallation {
    param([string]$StartPhase = "auto")

    Write-CoordinationLog "Starting phase-based installation from $StartPhase" "INFO"

    # Check for resume scenario
    $resumeInfo = Test-ResumeRequired
    if ($Resume -or $resumeInfo.Resume) {
        $currentState = Get-InstallationState
        if ($currentState -and $currentState.Phase) {
            $StartPhase = $currentState.Phase
            Write-CoordinationLog "Resuming installation from phase: $StartPhase" "INFO"
        }
    }

    # Create installation directories
    if (-not (Test-Path $Global:InstallDir)) {
        New-Item -ItemType Directory -Path $Global:InstallDir -Force | Out-Null
    }
    if (-not (Test-Path $Global:UserDataDir)) {
        New-Item -ItemType Directory -Path $Global:UserDataDir -Force | Out-Null
    }

    # Execute installation phases
    $phases = @("prerequisites", "windows_features", "docker_setup", "misa_services", "coordination_setup")
    $startIndex = if ($StartPhase -eq "auto") { 0 } else { [Math]::Max(0, $phases.IndexOf($StartPhase)) }

    for ($i = $startIndex; $i -lt $phases.Count; $i++) {
        $phase = $phases[$i]
        $phaseConfig = $Global:InstallationPhases[$phase]

        Write-ProgressInfo -Activity "MISA.AI Installation" -Status "Installing: $($phaseConfig.Name)" -PercentComplete (($i / $phases.Count) * 100)

        try {
            Save-InstallationState -Phase $phase -Status "starting"

            Write-CoordinationLog "=== Starting Phase: $($phaseConfig.Name) ===" "INFO"
            Write-CoordinationLog "Description: $($phaseConfig.Description)" "INFO"

            # Execute phase-specific installation
            $phaseResult = & "Invoke-$($Phase.Replace('_', ''))Phase"

            if ($phaseResult.Success) {
                Save-InstallationState -Phase $phase -Status "completed" -Progress $phaseResult.Progress
                Write-CoordinationLog "=== Completed Phase: $($phaseConfig.Name) ===" "SUCCESS"

                # Check if restart is needed
                if ($phaseConfig.RestartRequired) {
                    Write-CoordinationLog "System restart required after $($phaseConfig.Name)" "WARNING"
                    Create-RestartMarker -Reason "phase_complete_$phase"

                    # Schedule restart
                    Schedule-SystemRestart -Reason "MISA.AI installation $($phaseConfig.Name) completed" -Delay 30
                    return $false
                }
            } else {
                Write-CoordinationLog "Phase failed: $($phaseConfig.Name) - $($phaseResult.Error)" "ERROR"
                throw "Installation failed at phase: $phase"
            }

        } catch {
            Save-InstallationState -Phase $phase -Status "failed" -Progress @{ Error = $_.Exception.Message }
            Write-CoordinationLog "Phase execution failed: $_" "ERROR"
            throw
        }
    }

    # Installation completed successfully
    Save-InstallationState -Phase "completed" -Status "success"
    Write-CoordinationLog "=== Installation Completed Successfully ===" "SUCCESS"

    # Clean up state files
    Remove-Item $Global:StateFile -ErrorAction SilentlyContinue
    Remove-Item $Global:RestartMarker -ErrorAction SilentlyContinue

    return $true
}

# Phase 1: Prerequisites Installation
function Invoke-PrerequisitesPhase {
    Write-CoordinationLog "Installing system prerequisites" "INFO"

    $progress = @{}
    $success = $true
    $error = ""

    try {
        # .NET Framework check and installation
        Write-CoordinationLog "Checking .NET Framework 4.8..." "INFO"
        $netResult = Install-NETFramework48
        $progress.NETFramework = $netResult.Success

        if (-not $netResult.Success) {
            Write-CoordinationLog ".NET Framework installation failed: $($netResult.Error)" "ERROR"
            $success = $false
            $error = $netResult.Error
        }

        # Visual C++ redistributables check and installation
        Write-CoordinationLog "Installing Visual C++ redistributables..." "INFO"
        $vcResult = Install-VisualCppRedistributables
        $progress.VCRedistributables = $vcResult.Success

        if (-not $vcResult.Success) {
            Write-CoordinationLog "Visual C++ redistributables installation failed: $($vcResult.Error)" "ERROR"
            $success = $false
            $error = if ($error) { "$error; $($vcResult.Error)" } else { $vcResult.Error }
        }

        # Validate prerequisites
        if ($success) {
            Write-CoordinationLog "Validating prerequisite installation..." "INFO"
            $validation = Test-PrerequisitesInstallation
            $progress.Validation = $validation.Success

            if (-not $validation.Success) {
                Write-CoordinationLog "Prerequisite validation failed: $($validation.Error)" "ERROR"
                $success = $false
                $error = if ($error) { "$error; $($validation.Error)" } else { $validation.Error }
            }
        }

    } catch {
        Write-CoordinationLog "Prerequisites installation failed: $_" "ERROR"
        $success = $false
        $error = $_.Exception.Message
    }

    return @{ Success = $success; Progress = $progress; Error = $error }
}

# Phase 2: Windows Features
function Invoke-WindowsFeaturesPhase {
    Write-CoordinationLog "Installing Windows features" "INFO"

    $progress = @{}
    $success = $true
    $error = ""
    $restartNeeded = $false

    try {
        $features = $Global:InstallationPhases.windows_features.RequiredFeatures

        foreach ($feature in $features) {
            Write-CoordinationLog "Enabling Windows feature: $feature" "INFO"

            $featureResult = Install-WindowsFeature -FeatureName $feature
            $progress[$feature] = $featureResult.Success

            if ($featureResult.RestartNeeded) {
                $restartNeeded = $true
                Write-CoordinationLog "Feature $feature requires system restart" "WARNING"
            }

            if (-not $featureResult.Success) {
                Write-CoordinationLog "Failed to enable feature $feature: $($featureResult.Error)" "ERROR"
                $success = $false
                $error = if ($error) { "$error; $($featureResult.Error)" } else { $featureResult.Error }
            }
        }

        $progress.RestartNeeded = $restartNeeded

    } catch {
        Write-CoordinationLog "Windows features installation failed: $_" "ERROR"
        $success = $false
        $error = $_.Exception.Message
    }

    return @{ Success = $success; Progress = $progress; Error = $error }
}

# Phase 3: Docker Desktop Setup
function Invoke-DockerSetupPhase {
    Write-CoordinationLog "Setting up Docker Desktop" "INFO"

    $progress = @{}
    $success = $true
    $error = ""

    try {
        # Check if Docker is already installed
        $dockerCheck = Test-DockerInstallation
        $progress.DockerInstalled = $dockerCheck.Installed

        if ($dockerCheck.Installed) {
            Write-CoordinationLog "Docker Desktop is already installed" "INFO"
        } else {
            Write-CoordinationLog "Installing Docker Desktop..." "INFO"
            $installResult = Install-DockerDesktop
            $progress.DockerInstallation = $installResult.Success

            if (-not $installResult.Success) {
                Write-CoordinationLog "Docker Desktop installation failed: $($installResult.Error)" "ERROR"
                $success = $false
                $error = $installResult.Error
            }
        }

        # Configure Docker if installation was successful
        if ($success) {
            Write-CoordinationLog "Configuring Docker Desktop..." "INFO"
            $configResult = Configure-DockerDesktop
            $progress.DockerConfiguration = $configResult.Success

            if (-not $configResult.Success) {
                Write-CoordinationLog "Docker Desktop configuration failed: $($configResult.Error)" "ERROR"
                $success = $false
                $error = if ($error) { "$error; $($configResult.Error)" } else { $configResult.Error }
            }
        }

    } catch {
        Write-CoordinationLog "Docker Desktop setup failed: $_" "ERROR"
        $success = $false
        $error = $_.Exception.Message
    }

    return @{ Success = $success; Progress = $progress; Error = $error }
}

# Phase 4: MISA.AI Services
function Invoke-MisaServicesPhase {
    Write-CoordinationLog "Deploying MISA.AI services" "INFO"

    $progress = @{}
    $success = $true
    $error = ""

    try {
        # Create service directories
        $serviceDirs = @("services", "config", "data", "logs")
        foreach ($dir in $serviceDirs) {
            $dirPath = Join-Path $Global:InstallDir $dir
            if (-not (Test-Path $dirPath)) {
                New-Item -ItemType Directory -Path $dirPath -Force | Out-Null
            }
        }

        # Download and extract MISA.AI services
        Write-CoordinationLog "Downloading MISA.AI services..." "INFO"
        $downloadResult = Get-MisaServicesPackage
        $progress.PackageDownload = $downloadResult.Success

        if ($downloadResult.Success) {
            Write-CoordinationLog "Extracting MISA.AI services..." "INFO"
            $extractResult = Expand-MisaServicesPackage -PackagePath $downloadResult.PackagePath
            $progress.PackageExtraction = $extractResult.Success

            if (-not $extractResult.Success) {
                Write-CoordinationLog "Services extraction failed: $($extractResult.Error)" "ERROR"
                $success = $false
                $error = $extractResult.Error
            }
        } else {
            Write-CoordinationLog "Services download failed: $($downloadResult.Error)" "ERROR"
            $success = $false
            $error = $downloadResult.Error
        }

        # Deploy Docker configuration
        if ($success) {
            Write-CoordinationLog "Creating Docker configuration..." "INFO"
            $dockerConfig = New-MisaDockerConfig
            $progress.DockerConfig = $dockerConfig.Success

            if (-not $dockerConfig.Success) {
                Write-CoordinationLog "Docker configuration failed: $($dockerConfig.Error)" "ERROR"
                $success = $false
                $error = if ($error) { "$error; $($dockerConfig.Error)" } else { $dockerConfig.Error }
            }
        }

        # Start MISA.AI services
        if ($success) {
            Write-CoordinationLog "Starting MISA.AI services..." "INFO"
            $startResult = Start-MisaServices
            $progress.ServicesStarted = $startResult.Success

            if (-not $startResult.Success) {
                Write-CoordinationLog "Services startup failed: $($startResult.Error)" "ERROR"
                $success = $false
                $error = if ($error) { "$error; $($startResult.Error)" } else { $startResult.Error }
            }
        }

    } catch {
        Write-CoordinationLog "MISA.AI services deployment failed: $_" "ERROR"
        $success = $false
        $error = $_.Exception.Message
    }

    return @{ Success = $success; Progress = $progress; Error = $error }
}

# Phase 5: Coordination Setup
function Invoke-CoordinationSetupPhase {
    Write-CoordinationLog "Setting up device coordination" "INFO"

    $progress = @{}
    $success = $true
    $error = ""

    try {
        # Start coordination server
        Write-CoordinationLog "Starting coordination server on port $Global:CoordinationPort..." "INFO"
        $serverResult = Start-CoordinationServer -Port $Global:CoordinationPort
        $progress.CoordinationServer = $serverResult.Success

        if (-not $serverResult.Success) {
            Write-CoordinationLog "Coordination server failed to start: $($serverResult.Error)" "ERROR"
            $success = $false
            $error = $serverResult.Error
        }

        # Discover mobile devices
        if ($success -and $CoordinatorHost) {
            Write-CoordinationLog "Discovering mobile devices..." "INFO"
            $discoveryResult = Start-MobileDeviceDiscovery -CoordinatorHost $CoordinatorHost -CoordinatorPort $Global:CoordinationPort
            $progress.MobileDiscovery = $discoveryResult.Success
            $progress.DiscoveredDevices = $discoveryResult.Devices.Count

            if (-not $discoveryResult.Success) {
                Write-CoordinationLog "Mobile device discovery failed: $($discoveryResult.Error)" "WARNING"
                # Not critical - continue installation
            }
        }

        # Register Windows service
        Write-CoordinationLog "Registering MISA.AI coordination service..." "INFO"
        $serviceResult = Register-CoordinationService
        $progress.ServiceRegistration = $serviceResult.Success

        if (-not $serviceResult.Success) {
            Write-CoordinationLog "Service registration failed: $($serviceResult.Error)" "WARNING"
            # Not critical - can be registered manually
        }

        # Configure firewall rules
        Write-CoordinationLog "Configuring firewall rules..." "INFO"
        $firewallResult = Configure-FirewallRules -Port $Global:CoordinationPort
        $progress.FirewallConfig = $firewallResult.Success

        if (-not $firewallResult.Success) {
            Write-CoordinationLog "Firewall configuration failed: $($firewallResult.Error)" "WARNING"
            # Not critical - can be configured manually
        }

    } catch {
        Write-CoordinationLog "Coordination setup failed: $_" "ERROR"
        $success = $false
        $error = $_.Exception.Message
    }

    return @{ Success = $success; Progress = $progress; Error = $error }
}

# Utility Functions (placeholder implementations - would be implemented in actual deployment)
function Install-NETFramework48 { return @{ Success = $true } }
function Install-VisualCppRedistributables { return @{ Success = $true } }
function Test-PrerequisitesInstallation { return @{ Success = $true } }
function Install-WindowsFeature { param($FeatureName); return @{ Success = $true; RestartNeeded = $false } }
function Test-DockerInstallation { return @{ Installed = $false } }
function Install-DockerDesktop { return @{ Success = $true } }
function Configure-DockerDesktop { return @{ Success = $true } }
function Get-MisaServicesPackage { return @{ Success = $true; PackagePath = "C:\temp\misa-services.zip" } }
function Expand-MisaServicesPackage { param($PackagePath); return @{ Success = $true } }
function New-MisaDockerConfig { return @{ Success = $true } }
function Start-MisaServices { return @{ Success = $true } }
function Start-CoordinationServer { param($Port); return @{ Success = $true } }
function Start-MobileDeviceDiscovery { param($CoordinatorHost, $CoordinatorPort); return @{ Success = $true; Devices = @() } }
function Register-CoordinationService { return @{ Success = $true } }
function Configure-FirewallRules { param($Port); return @{ Success = $true } }
function Schedule-SystemRestart { param($Reason, $Delay); return $true }

# Main Execution
try {
    Write-CoordinationLog "MISA.AI Installation Coordinator Started" "INFO"
    Write-CoordinationLog "Coordinator: $CoordinatorHost`:$CoordinatorPort" "INFO"
    Write-CoordinationLog "Install Phase: $InstallPhase" "INFO"

    # Check for administrator privileges
    $currentUser = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($currentUser)
    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        throw "Administrator privileges required for MISA.AI installation"
    }

    # Start phase-based installation
    $installationCompleted = Invoke-PhaseBasedInstallation -StartPhase $InstallPhase

    if ($installationCompleted) {
        Write-CoordinationLog "=== MISA.AI Installation Completed Successfully ===" "SUCCESS"
        Write-CoordinationLog "Web Application: http://localhost:3000" "INFO"
        Write-CoordinationLog "Kernel API: http://localhost:8080" "INFO"
        Write-CoordinationLog "Coordinator Port: $Global:CoordinationPort" "INFO"
        Write-CoordinationLog "Log Directory: $Global:LogDir" "INFO"

        # Launch application
        Start-Process -FilePath (Join-Path $Global:InstallDir "MisaAI.exe")
    }

} catch {
    Write-CoordinationLog "Installation failed: $_" "ERROR"
    exit 1
} finally {
    Write-ProgressInfo -Activity "MISA.AI Installation" -Completed
}