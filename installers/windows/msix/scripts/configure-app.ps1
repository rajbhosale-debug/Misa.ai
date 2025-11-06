# MISA.AI Application Configuration Script
# This script configures MISA.AI after installation

param(
    [string]$InstallPath = "$env:ProgramFiles\MISA.AI",
    [string]$DataPath = "$env:ProgramData\MISA.AI",
    [switch]$Quiet
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

    # Write to application log file
    $logFile = Join-Path $DataPath "logs\install.log"
    if (-not (Test-Path (Split-Path $logFile -Parent))) {
        New-Item -ItemType Directory -Path (Split-Path $logFile -Parent) -Force | Out-Null
    }
    Add-Content -Path $logFile -Value $logMessage -ErrorAction SilentlyContinue
}

function Create-DirectoryStructure {
    Write-Log "Creating directory structure..."

    $directories = @(
        $DataPath,
        "$DataPath\config",
        "$DataPath\models",
        "$DataPath\data",
        "$DataPath\logs",
        "$DataPath\temp",
        "$DataPath\backups",
        "$env:LOCALAPPDATA\MISA.AI",
        "$env:LOCALAPPDATA\MISA.AI\cache",
        "$env:LOCALAPPDATA\MISA.AI\user",
        "$env:APPDATA\MISA.AI"
    )

    foreach ($dir in $directories) {
        try {
            if (-not (Test-Path $dir)) {
                New-Item -ItemType Directory -Path $dir -Force | Out-Null
                Write-Log "Created directory: $dir"
            }
        }
        catch {
            Write-Log "Failed to create directory: $dir`n$_" "WARN"
        }
    }
}

function Set-Permissions {
    Write-Log "Setting directory permissions..."

    try {
        # Grant current user full access to application data
        $acl = Get-Acl $DataPath
        $accessRule = New-Object System.Security.AccessControl.FileSystemAccessRule(
            [System.Security.Principal.WindowsIdentity]::GetCurrent().Name,
            "FullControl",
            "ContainerInherit,ObjectInherit",
            "None",
            "Allow"
        )
        $acl.SetAccessRule($accessRule)
        Set-Acl -Path $DataPath -AclObject $acl

        Write-Log "Permissions set successfully"
    }
    catch {
        Write-Log "Failed to set permissions`n$_" "WARN"
    }
}

function Create-Configuration {
    Write-Log "Creating default configuration..."

    $configPath = "$DataPath\config\misa.json"
    $defaultConfig = @{
        version = "1.0.0"
        installation = @{
            path = $InstallPath
            dataPath = $DataPath
            timestamp = Get-Date -Format "yyyy-MM-ddTHH:mm:ssZ"
            user = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
        }
        system = @{
            maxMemory = "4GB"
            maxCpuCores = [System.Environment]::ProcessorCount
            gpuAcceleration = $true
            autoStart = $true
            checkUpdates = $true
        }
        privacy = @{
            dataCollection = $false
            crashReports = $false
            analytics = $false
            localProcessing = $true
        }
        ai = @{
            defaultModel = "mixtral"
            maxTokens = 4096
            temperature = 0.7
            contextWindow = 8192
        }
        network = @{
            port = 8080
            sslEnabled = $true
            host = "localhost"
            maxConnections = 10
        }
        ui = @{
            theme = "auto"
            language = "en-US"
            fontSize = 14
            animations = $true
            compactMode = $false
        }
        features = @{
            voiceAssistant = $true
            screenCapture = $true
            fileAnalysis = $true
            calendarIntegration = $true
            noteTaking = $true
        }
    }

    try {
        $configJson = $defaultConfig | ConvertTo-Json -Depth 10
        Set-Content -Path $configPath -Value $configJson -Encoding UTF8
        Write-Log "Default configuration created: $configPath"
    }
    catch {
        Write-Log "Failed to create configuration file`n$_" "ERROR"
        throw
    }
}

function Register-WindowsService {
    Write-Log "Registering MISA Kernel Windows service..."

    try {
        $serviceName = "MisaKernel"
        $displayName = "MISA.AI Kernel Service"
        $description = "MISA.AI Kernel - Background AI Assistant Service"
        $executablePath = Join-Path $InstallPath "MisaKernel.exe"

        if (Test-Path $executablePath) {
            # Remove existing service if it exists
            $existingService = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
            if ($existingService) {
                Write-Log "Removing existing service: $serviceName"
                Stop-Service -Name $serviceName -Force -ErrorAction SilentlyContinue
                Remove-Service -Name $serviceName -ErrorAction SilentlyContinue
                Start-Sleep 2
            }

            # Create new service
            Write-Log "Creating Windows service: $serviceName"
            New-Service -Name $serviceName -DisplayName $displayName -BinaryPathName $executablePath -Description $description -StartupType Automatic -ErrorAction Stop

            # Set service failure recovery
            Write-Log "Configuring service recovery settings..."
            $service = Get-WmiObject -Class Win32_Service -Filter "Name='$serviceName'"
            $service.Change($null, $null, $null, $null, $null, $null, "1", "60000", "1", "60000", "1", "60000")

            # Grant service logon as service rights
            $sid = New-Object System.Security.Principal.SecurityIdentifier([System.Security.Principal.WellKnownSidType]::ServiceSid, $null)
            $serviceAccount = $sid.Translate([System.Security.Principal.NTAccount]).Value

            Write-Log "Windows service registered successfully: $serviceName"
        } else {
            Write-Log "MISA Kernel executable not found: $executablePath" "WARN"
        }
    }
    catch {
        Write-Log "Failed to register Windows service`n$_" "ERROR"
        # Don't throw - allow installation to continue
    }
}

function Create-Shortcuts {
    Write-Log "Creating shortcuts..."

    try {
        $desktopPath = [Environment]::GetFolderPath("Desktop")
        $startMenuPath = [Environment]::GetFolderPath("StartMenu")
        $programsPath = Join-Path $startMenuPath "Programs"

        # Create Start Menu shortcut
        $startMenuShortcut = Join-Path $programsPath "MISA.AI.lnk"
        $executablePath = Join-Path $InstallPath "MisaAI.exe"
        $iconPath = Join-Path $InstallPath "resources\app.ico"

        if (Test-Path $executablePath) {
            $shell = New-Object -ComObject WScript.Shell

            # Start Menu shortcut
            $shortcut = $shell.CreateShortcut($startMenuShortcut)
            $shortcut.TargetPath = $executablePath
            $shortcut.WorkingDirectory = $InstallPath
            $shortcut.Description = "MISA.AI - Privacy-First AI Assistant"
            if (Test-Path $iconPath) {
                $shortcut.IconLocation = $iconPath
            }
            $shortcut.Save()

            Write-Log "Created Start Menu shortcut: $startMenuShortcut"

            # Desktop shortcut (optional - can be disabled by user)
            $desktopShortcut = Join-Path $desktopPath "MISA.AI.lnk"
            if (-not (Test-Path $desktopShortcut)) {
                $shortcut = $shell.CreateShortcut($desktopShortcut)
                $shortcut.TargetPath = $executablePath
                $shortcut.WorkingDirectory = $InstallPath
                $shortcut.Description = "MISA.AI - Privacy-First AI Assistant"
                if (Test-Path $iconPath) {
                    $shortcut.IconLocation = $iconPath
                }
                $shortcut.Save()

                Write-Log "Created Desktop shortcut: $desktopShortcut"
            }
        } else {
            Write-Log "Application executable not found: $executablePath" "WARN"
        }
    }
    catch {
        Write-Log "Failed to create shortcuts`n$_" "WARN"
    }
}

function Register-Protocols {
    Write-Log "Registering URL protocols..."

    try {
        # Register misa:// protocol
        $protocolPath = "HKCU:\SOFTWARE\Classes\misa"
        New-Item -Path $protocolPath -Force | Out-Null
        Set-ItemProperty -Path $protocolPath -Name "(Default)" -Value "MISA.AI Protocol" -Force
        Set-ItemProperty -Path $protocolPath -Name "URL Protocol" -Value "" -Force

        New-Item -Path "$protocolPath\shell\open\command" -Force | Out-Null
        $command = "`"$InstallPath\MisaAI.exe`" `"%1`""
        Set-ItemProperty -Path "$protocolPath\shell\open\command" -Name "(Default)" -Value $command -Force

        Write-Log "Registered misa:// protocol"
    }
    catch {
        Write-Log "Failed to register protocols`n$_" "WARN"
    }
}

function Initialize-Database {
    Write-Log "Initializing application database..."

    try {
        # Create SQLite database file
        $databasePath = Join-Path $DataPath "data\misa.db"

        if (-not (Test-Path $databasePath)) {
            # The actual database initialization will be handled by the application
            # Just create the directory structure
            $dataDir = Split-Path $databasePath -Parent
            if (-not (Test-Path $dataDir)) {
                New-Item -ItemType Directory -Path $dataDir -Force | Out-Null
            }

            Write-Log "Database directory created: $dataDir"
        } else {
            Write-Log "Database already exists: $databasePath"
        }
    }
    catch {
        Write-Log "Failed to initialize database`n$_" "WARN"
    }
}

function Test-Configuration {
    Write-Log "Testing application configuration..."

    $issues = @()

    # Check if main executable exists
    $executablePath = Join-Path $InstallPath "MisaAI.exe"
    if (-not (Test-Path $executablePath)) {
        $issues += "Main application executable not found: $executablePath"
    }

    # Check if configuration file exists
    $configPath = Join-Path $DataPath "config\misa.json"
    if (-not (Test-Path $configPath)) {
        $issues += "Configuration file not found: $configPath"
    }

    # Check if data directory exists and is writable
    if (-not (Test-Path $DataPath)) {
        $issues += "Data directory not found: $DataPath"
    } else {
        try {
            $testFile = Join-Path $DataPath "test.tmp"
            "test" | Out-File -FilePath $testFile -Encoding UTF8
            Remove-Item $testFile -Force
        }
        catch {
            $issues += "Data directory is not writable: $DataPath"
        }
    }

    if ($issues.Count -gt 0) {
        Write-Log "Configuration test failed:" "ERROR"
        $issues | ForEach-Object { Write-Log "  - $_" "ERROR" }
        throw "Application configuration incomplete"
    }

    Write-Log "Application configuration validated successfully"
}

# Main execution
try {
    Write-Log "Starting MISA.AI application configuration..."
    Write-Log "Install path: $InstallPath"
    Write-Log "Data path: $DataPath"

    # Create directory structure
    Create-DirectoryStructure

    # Set permissions
    Set-Permissions

    # Create default configuration
    Create-Configuration

    # Register Windows service
    Register-WindowsService

    # Create shortcuts
    Create-Shortcuts

    # Register protocols
    Register-Protocols

    # Initialize database
    Initialize-Database

    # Test configuration
    Test-Configuration

    Write-Log "Application configuration completed successfully!"

    if (-not $Quiet) {
        Write-Log ""
        Write-Log "MISA.AI has been successfully installed and configured!"
        Write-Log ""
        Write-Log "Launch options:"
        Write-Log "  • Start Menu: Programs > MISA.AI"
        Write-Log "  • Desktop: Double-click MISA.AI shortcut"
        Write-Log "  • Command Line: MisaAI.exe"
        Write-Log ""
        Write-Log "Configuration: $DataPath\config\misa.json"
        Write-Log "Logs: $DataPath\logs\"
        Write-Log ""
    }
}
catch {
    Write-Log "Application configuration failed:`n$_" "ERROR"
    exit 1
}

exit 0