# MISA.AI Prerequisites Installation Script
# This script installs system prerequisites for MISA.AI

param(
    [switch]$Force,
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

    # Also write to event log for system administrators
    if ($Level -eq "ERROR") {
        Write-EventLog -LogName Application -Source "MISA.AI Installer" -EventId 1001 -EntryType Error -Message $Message -ErrorAction SilentlyContinue
    }
}

function Test-AdminPrivileges {
    $currentUser = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($currentUser)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Test-WindowsVersion {
    $version = [System.Environment]::OSVersion.Version
    $build = $version.Build

    Write-Log "Windows version: $($version.Major).$($version.Minor).$build"

    # Windows 10/11 required (Build 19041+)
    if ($build -lt 19041) {
        throw "MISA.AI requires Windows 10 (Build 19041) or later. Current build: $build"
    }

    return $true
}

function Test-HardwareRequirements {
    Write-Log "Checking hardware requirements..."

    # Check RAM (minimum 4GB, recommended 8GB)
    $ram = Get-WmiObject -Class Win32_ComputerSystem
    $totalRAM = [math]::Round($ram.TotalPhysicalMemory / 1GB, 1)
    Write-Log "Total RAM: $totalRAM GB"

    if ($totalRAM -lt 4) {
        throw "MISA.AI requires at least 4GB RAM. Found: $totalRAM GB"
    }

    if ($totalRAM -lt 8) {
        Write-Log "WARNING: Less than 8GB RAM detected. Performance may be degraded." "WARN"
    }

    # Check disk space (minimum 10GB free)
    $systemDrive = Get-WmiObject -Class Win32_LogicalDisk | Where-Object { $_.DeviceID -eq "C:" }
    $freeSpace = [math]::Round($systemDrive.FreeSpace / 1GB, 1)
    Write-Log "Free disk space: $freeSpace GB"

    if ($freeSpace -lt 10) {
        throw "MISA.AI requires at least 10GB free disk space. Found: $freeSpace GB"
    }

    # Check if 64-bit OS
    if (-not [Environment]::Is64BitOperatingSystem) {
        throw "MISA.AI requires 64-bit Windows"
    }

    return $true
}

function Install-WindowsFeatures {
    Write-Log "Installing required Windows features..."

    $features = @(
        "Microsoft-Hyper-V-All",
        "Containers",
        "Microsoft-Windows-Subsystem-Linux",
        "VirtualMachinePlatform"
    )

    foreach ($feature in $features) {
        $featureState = Get-WindowsOptionalFeature -Online -FeatureName $feature -ErrorAction SilentlyContinue

        if ($featureState -and $featureState.State -eq "Disabled") {
            Write-Log "Enabling Windows feature: $feature"
            try {
                Enable-WindowsOptionalFeature -Online -FeatureName $feature -NoRestart -WarningAction SilentlyContinue
                Write-Log "Successfully enabled feature: $feature"
            }
            catch {
                Write-Log "Failed to enable feature $feature`n$_" "WARN"
            }
        }
        elseif ($featureState -and $featureState.State -eq "Enabled") {
            Write-Log "Windows feature already enabled: $feature"
        }
    }
}

function Install-NETFramework {
    Write-Log "Checking .NET Framework installation..."

    # Check if .NET Framework 4.8 is installed
    $net48 = Get-ChildItem "HKLM:SOFTWARE\Microsoft\NET Framework Setup\NDP\v4\Full\" -ErrorAction SilentlyContinue | Get-ItemProperty -Name Release -ErrorAction SilentlyContinue | Where-Object { $_.Release -ge 528040 }

    if (-not $net48) {
        Write-Log "Installing .NET Framework 4.8..."

        $dotNet48Url = "https://download.microsoft.com/download/6/E/4/6E48E8AB-DC00-419A-B348-907570FF15D3/NDP48-x86-x64-AllOS-ENU.exe"
        $dotNet48File = Join-Path $env:TEMP "NDP48-x86-x64-AllOS-ENU.exe"

        try {
            Write-Log "Downloading .NET Framework 4.8 installer..."
            Invoke-WebRequest -Uri $dotNet48Url -OutFile $dotNet48File -UseBasicParsing

            Write-Log "Installing .NET Framework 4.8 (this may take several minutes)..."
            $process = Start-Process -FilePath $dotNet48File -ArgumentList "/quiet", "/norestart" -Wait -PassThru

            if ($process.ExitCode -eq 0) {
                Write-Log ".NET Framework 4.8 installed successfully"
            } else {
                Write-Log ".NET Framework 4.8 installation failed with exit code: $($process.ExitCode)" "WARN"
            }

            Remove-Item $dotNet48File -Force -ErrorAction SilentlyContinue
        }
        catch {
            Write-Log "Failed to install .NET Framework 4.8`n$_" "WARN"
        }
    }
    else {
        Write-Log ".NET Framework 4.8 is already installed"
    }
}

function Install-VisualCppRedistributables {
    Write-Log "Installing Visual C++ Redistributables..."

    $redistributables = @(
        @{Name="VC++ 2015-2022 x64"; Url="https://aka.ms/vs/17/release/vc_redist.x64.exe"; File="vc_redist.x64.exe"},
        @{Name="VC++ 2015-2022 x86"; Url="https://aka.ms/vs/17/release/vc_redist.x86.exe"; File="vc_redist.x86.exe"}
    )

    foreach ($redist in $redistributables) {
        Write-Log "Installing $($redist.Name)..."

        try {
            $redistFile = Join-Path $env:TEMP $redist.File
            Write-Log "Downloading $($redist.Name)..."
            Invoke-WebRequest -Uri $redist.Url -OutFile $redistFile -UseBasicParsing

            $process = Start-Process -FilePath $redistFile -ArgumentList "/quiet", "/norestart" -Wait -PassThru

            if ($process.ExitCode -eq 0) {
                Write-Log "$($redist.Name) installed successfully"
            } else {
                Write-Log "$($redist.Name) installation returned exit code: $($process.ExitCode)" "WARN"
            }

            Remove-Item $redistFile -Force -ErrorAction SilentlyContinue
        }
        catch {
            Write-Log "Failed to install $($redist.Name)`n$_" "WARN"
        }
    }
}

function Test-Prerequisites {
    Write-Log "Validating prerequisites installation..."

    $issues = @()

    # Test .NET Framework
    $net48 = Get-ChildItem "HKLM:SOFTWARE\Microsoft\NET Framework Setup\NDP\v4\Full\" -ErrorAction SilentlyContinue | Get-ItemProperty -Name Release -ErrorAction SilentlyContinue | Where-Object { $_.Release -ge 528040 }
    if (-not $net48) {
        $issues += ".NET Framework 4.8 is not properly installed"
    }

    # Test Windows features
    $requiredFeatures = @("Microsoft-Hyper-V-All", "Containers")
    foreach ($feature in $requiredFeatures) {
        $featureState = Get-WindowsOptionalFeature -Online -FeatureName $feature -ErrorAction SilentlyContinue
        if ($featureState -and $featureState.State -ne "Enabled") {
            $issues += "Windows feature '$feature' is not enabled"
        }
    }

    if ($issues.Count -gt 0) {
        Write-Log "Prerequisites validation failed:" "ERROR"
        $issues | ForEach-Object { Write-Log "  - $_" "ERROR" }
        throw "Prerequisites installation incomplete"
    }

    Write-Log "All prerequisites validated successfully"
}

# Main execution
try {
    Write-Log "Starting MISA.AI prerequisites installation..."
    Write-Log "Installer version: 1.0.0"
    Write-Log "Running as: $([Environment]::UserName)"

    # Check for administrator privileges
    if (-not (Test-AdminPrivileges)) {
        throw "Administrator privileges required for prerequisites installation"
    }

    # Check Windows version
    Test-WindowsVersion

    # Check hardware requirements
    Test-HardwareRequirements

    # Install Windows features
    Install-WindowsFeatures

    # Install .NET Framework
    Install-NETFramework

    # Install Visual C++ redistributables
    Install-VisualCppRedistributables

    # Validate all prerequisites
    Test-Prerequisites

    Write-Log "Prerequisites installation completed successfully!"

    # Recommend restart if Windows features were enabled
    $restartNeeded = $false
    $features = @("Microsoft-Hyper-V-All", "Containers", "Microsoft-Windows-Subsystem-Linux")
    foreach ($feature in $features) {
        $featureState = Get-WindowsOptionalFeature -Online -FeatureName $feature -ErrorAction SilentlyContinue
        if ($featureState -and $featureState.RestartNeeded) {
            $restartNeeded = $true
            break
        }
    }

    if ($restartNeeded -and -not $Quiet) {
        Write-Log "System restart may be required for some features to take effect." "WARN"
    }
}
catch {
    Write-Log "Prerequisites installation failed:`n$_" "ERROR"
    exit 1
}

exit 0