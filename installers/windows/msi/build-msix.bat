@echo off
echo Building MISA.AI Windows MSIX Package...

REM Set variables
set SOLUTION_DIR=%~dp0..\..\
set MSIX_DIR=%~dp0\msix
set BUILD_DIR=%MSIX_DIR%build
set DIST_DIR=%SOLUTION_DIR%dist\windows
set SOURCE_DIR=%SOLUTION_DIR%desktop\target\x86_64-pc-windows-msvc\release

REM Create build directory
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"

echo Checking prerequisites...

REM Check for MSBuild
msbuild /? >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo MSBuild not found. Please install Visual Studio 2019+ or Build Tools.
    exit /b 1
)

REM Check for Windows SDK
set WINDOWS_SDK=
for %%i in (10.0 8.1) do (
    if exist "C:\Program Files (x86)\Windows Kits\%%i\bin\**\makeappx.exe" (
        set WINDOWS_SDK=%%i
        goto :found_sdk
    )
)

:not_found_sdk
echo Windows SDK not found. Please install Windows 10 SDK.
exit /b 1

:found_sdk
echo Found Windows SDK: %WINDOWS_SDK%

REM Check if source executables exist
if not exist "%SOURCE_DIR%\misa-desktop.exe" (
    echo Tauri desktop executable not found.
    echo Please build the desktop application first:
    echo   cd desktop
    echo   npm run tauri build
    exit /b 1
)

if not exist "%SOLUTION_DIR%core\target\x86_64-pc-windows-msvc\release\misa-kernel.exe" (
    echo MISA Kernel executable not found.
    echo Please build the core components first:
    echo   cd core
    echo   cargo build --release
    exit /b 1
)

echo Building MSIX project...

REM Build the MSIX project
cd "%MSIX_DIR%"
msbuild MisaPackage.wxsproj /p:Configuration=Release /p:Platform=x64 /p:UapAppxPackageBuildMode=StoreUpload

if %ERRORLEVEL% neq 0 (
    echo MSIX build failed.
    exit /b 1
)

echo Preparing MSIX content...

REM Create MSIX content directory
set MSIX_CONTENT_DIR=%BUILD_DIR%\MSIX
if not exist "%MSIX_CONTENT_DIR%" mkdir "%MSIX_CONTENT_DIR%"

REM Copy application files
copy "%SOURCE_DIR%\misa-desktop.exe" "%MSIX_CONTENT_DIR%\MisaAI.exe"
copy "%SOLUTION_DIR%core\target\x86_64-pc-windows-msvc\release\misa-kernel.exe" "%MSIX_CONTENT_DIR%\"

REM Copy dependencies
copy "%SOURCE_DIR%\*.dll" "%MSIX_CONTENT_DIR%\" 2>nul

REM Create required directories for MSIX
if not exist "%MSIX_CONTENT_DIR%\platforms" mkdir "%MSIX_CONTENT_DIR%\platforms"
if not exist "%MSIX_CONTENT_DIR%\resources" mkdir "%MSIX_CONTENT_DIR%\resources"
if not exist "%MSIX_CONTENT_DIR%\scripts" mkdir "%MSIX_CONTENT_DIR%\scripts"
if not exist "%MSIX_CONTENT_DIR%\Assets" mkdir "%MSIX_CONTENT_DIR%\Assets"

REM Copy scripts
xcopy "%MSIX_DIR%\scripts\*" "%MSIX_CONTENT_DIR%\scripts\" /E /I /Y

REM Copy assets
copy "%MSIX_DIR%\Assets\*" "%MSIX_CONTENT_DIR%\Assets\" 2>nul

REM Create appxmanifest
copy "%MSIX_DIR%\MisaPackage.appxmanifest" "%MSIX_CONTENT_DIR%\"

REM Copy license and documentation
copy "%SOLUTION_DIR%LICENSE" "%MSIX_CONTENT_DIR%\"
copy "%SOLUTION_DIR%README.md" "%MSIX_CONTENT_DIR%\"

echo Creating MSIX package...

REM Find makeappx.exe
set MAKEAPPX=
for /r "C:\Program Files (x86)\Windows Kits\%WINDOWS_SDK%\bin" %%i in (makeappx.exe) do (
    if exist "%%i" (
        set MAKEAPPX=%%i
        goto :found_makeappx
    )
)

:not_found_makeappx
echo makeappx.exe not found.
exit /b 1

:found_makeappx
echo Using makeappx.exe: %MAKEAPPX%

REM Create mapping file for MSIX
echo [Files] > "%BUILD_DIR%\mappfile.txt"
setlocal enabledelayedexpansion
for /r "%MSIX_CONTENT_DIR%" %%f in (*) do (
    set "relpath=%%f"
    set "relpath=!relpath:%MSIX_CONTENT_DIR%\=!"
    echo "!relpath!" "%%f" >> "%BUILD_DIR%\mappfile.txt"
)

REM Create MSIX package
"%MAKEAPPX%" pack /o /h SHA256 /f "%BUILD_DIR%\mappfile.txt" /p "%DIST_DIR%\Misa.ai.msix"

if %ERRORLEVEL% neq 0 (
    echo MSIX package creation failed.
    exit /b 1
)

echo Signing MSIX package...

REM Check for certificate
set CERT_FILE=%MSIX_DIR%\MisaAI_TemporaryKey.pfx
if not exist "%CERT_FILE%" (
    echo Creating temporary certificate for signing...

    REM Find powershell
    set POWERSHELL=powershell
    if exist "C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe" (
        set POWERSHELL=C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe
    )

    %POWERSHELL% -Command "& {$cert = New-SelfSignedCertificate -DnsName 'localhost' -CertStoreLocation 'cert:\LocalMachine\My' -KeyUsage DigitalSignature -KeyAlgorithm RSA -KeyLength 2048; $password = ConvertTo-SecureString -String 'MisaAI2024' -Force -AsPlainText; Export-PfxCertificate -Cert $cert -FilePath '%CERT_FILE%' -Password $password}"
)

if exist "%CERT_FILE%" (
    REM Find signtool
    set SIGNTOOL=
    for /r "C:\Program Files (x86)\Windows Kits\%WINDOWS_SDK%\bin" %%i in (signtool.exe) do (
        if exist "%%i" (
            set SIGNTOOL=%%i
            goto :found_signtool
        )
    )

    :found_signtool
    if exist "%SIGNTOOL%" (
        echo Signing MSIX with certificate...
        "%SIGNTOOL%" sign /f "%CERT_FILE%" /p MisaAI2024 /fd SHA256 /a "%DIST_DIR%\Misa.ai.msix"

        if %ERRORLEVEL% neq 0 (
            echo MSIX signing failed (but package was created).
        ) else (
            echo MSIX signed successfully.
        )
    )
)

echo Creating App Installer file for sideloading...

REM Create appinstaller file
set APPINSTALLER=%DIST_DIR%\Misa.ai.appinstaller
echo ^<?xml version="1.0" encoding="utf-8"?^> > "%APPINSTALLER%"
echo ^<AppInstaller xmlns="http://schemas.microsoft.com/appx/appinstaller/2018"^> >> "%APPINSTALLER%"
echo   ^<Name Name="MisaAI" Version="1.0.0.0" Publisher="CN=MisaAI Team, O=MisaAI, L=San Francisco, S=California, C=US"/^> >> "%APPINSTALLER%"
echo   ^<MainApp Name="MisaAI" Package="MisaAI" Version="1.0.0.0" Publisher="CN=MisaAI Team, O=MisaAI, L=San Francisco, S=California, C=US"/^> >> "%APPINSTALLER%"
echo   ^<UpdateSettings URIs="https://misa.ai/updates"/^> >> "%APPINSTALLER%"
echo ^</AppInstaller^> >> "%APPINSTALLER%"

echo MSIX build completed successfully!
echo Output: %DIST_DIR%\Misa.ai.msix
echo App Installer: %APPINSTALLER%

REM Get MSIX size
for %%I in ("%DIST_DIR%\Misa.ai.msix") do echo Size: %%~zI bytes

echo.
echo To install the MSIX package:
echo   Double-click: %DIST_DIR%\Misa.ai.msix
echo   PowerShell: Add-AppxPackage -Path "%DIST_DIR%\Misa.ai.msix"
echo   Command line: %DIST_DIR%\Misa.ai.msix

echo.
echo To create an enterprise deployment:
echo   1. Sign with your organization's certificate
echo   2. Upload to your distribution server
echo   3. Update the appinstaller file with your server URL

echo.
echo Build completed at %date% %time%

REM Cleanup temporary files
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"

exit /b 0