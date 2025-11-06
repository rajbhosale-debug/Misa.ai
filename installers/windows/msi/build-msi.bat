@echo off
echo Building MISA.AI Windows MSI Installer...

REM Set variables
set SOLUTION_DIR=%~dp0..\..\
set INSTALLER_DIR=%~dp0
set BUILD_DIR=%INSTALLER_DIR%build
set DIST_DIR=%SOLUTION_DIR%dist\windows
set SOURCE_DIR=%SOLUTION_DIR%desktop\target\x86_64-pc-windows-msvc\release

REM Create build directory
if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"

echo Checking prerequisites...

REM Check for WiX Toolset
candle.exe /? >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo WiX Toolset not found. Please install WiX Toolset v4+.
    echo Download from: https://wixtoolset.org/releases/
    exit /b 1
)

REM Check for Visual Studio Build Tools
msbuild /? >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo MSBuild not found. Please install Visual Studio Build Tools.
    exit /b 1
)

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

echo Building Custom Actions...

REM Build custom actions
cd "%INSTALLER_DIR%"
msbuild CustomActions.csproj /p:Configuration=Release /p:Platform=x86

if %ERRORLEVEL% neq 0 (
    echo Custom Actions build failed.
    exit /b 1
)

echo Compiling WiX source files...

REM Compile WiX source files
candle.exe -out "%BUILD_DIR%\" -arch x64 ^
    -dSourceDir="%SOURCE_DIR%" ^
    -dSolutionDir="%SOLUTION_DIR%" ^
    Product.wxs Directory.wxs UI.wxs

if %ERRORLEVEL% neq 0 (
    echo WiX compilation failed.
    exit /b 1
)

echo Linking MSI package...

REM Link the MSI
light.exe -out "%BUILD_DIR%\Misa.ai.msi" ^
    "%BUILD_DIR%\Product.wixobj" ^
    "%BUILD_DIR%\Directory.wixobj" ^
    "%BUILD_DIR%\UI.wixobj" ^
    -ext WixUIExtension ^
    -ext WixUtilExtension ^
    -cultures:en-US

if %ERRORLEVEL% neq 0 (
    echo MSI linking failed.
    exit /b 1
)

echo Copying application files...

REM Create temporary directory for MSI content
set MSI_CONTENT_DIR=%BUILD_DIR%\msi-content
if not exist "%MSI_CONTENT_DIR%" mkdir "%MSI_CONTENT_DIR%"

REM Copy main executable
copy "%SOURCE_DIR%\misa-desktop.exe" "%MSI_CONTENT_DIR%\MisaAI.exe"
copy "%SOLUTION_DIR%core\target\x86_64-pc-windows-msvc\release\misa-kernel.exe" "%MSI_CONTENT_DIR%\"

REM Copy DLL dependencies
copy "%SOURCE_DIR%\*.dll" "%MSI_CONTENT_DIR%\" 2>nul

REM Create subdirectories
if not exist "%MSI_CONTENT_DIR%\platforms" mkdir "%MSI_CONTENT_DIR%\platforms"
if not exist "%MSI_CONTENT_DIR%\resources" mkdir "%MSI_CONTENT_DIR%\resources"
if not exist "%MSI_CONTENT_DIR%\config" mkdir "%MSI_CONTENT_DIR%\config"
if not exist "%MSI_CONTENT_DIR%\docs" mkdir "%MSI_CONTENT_DIR%\docs"
if not exist "%MSI_CONTENT_DIR%\scripts" mkdir "%MSI_CONTENT_DIR%\scripts"
if not exist "%MSI_CONTENT_DIR%\bundles" mkdir "%MSI_CONTENT_DIR%\bundles"
if not exist "%MSI_CONTENT_DIR%\models" mkdir "%MSI_CONTENT_DIR%\models"

REM Copy resources
copy "%SOLUTION_DIR%LICENSE" "%MSI_CONTENT_DIR%\"
copy "%SOLUTION_DIR%README.md" "%MSI_CONTENT_DIR%\docs\"

REM Create configuration files
echo {> "%MSI_CONTENT_DIR%\config\default.json"
echo   "version": "1.0.0",>> "%MSI_CONTENT_DIR%\config\default.json"
echo   "installation": {>> "%MSI_CONTENT_DIR%\config\default.json"
echo     "path": "[INSTALLFOLDER]",>> "%MSI_CONTENT_DIR%\config\default.json"
echo     "dataPath": "[DATADIR]">> "%MSI_CONTENT_DIR%\config\default.json"
echo   }>> "%MSI_CONTENT_DIR%\config\default.json"
echo }>> "%MSI_CONTENT_DIR%\config\default.json"

REM Copy dependency installers if they exist
if exist "%SOLUTION_DIR%installers\windows\bundles\*" copy "%SOLUTION_DIR%installers\windows\bundles\*" "%MSI_CONTENT_DIR%\bundles\"

echo Creating final MSI package...

REM Use WiX to create final MSI with embedded files
candle.exe -out "%BUILD_DIR%\final-" -arch x64 ^
    -dSourceDir="%MSI_CONTENT_DIR%" ^
    -dSolutionDir="%SOLUTION_DIR%" ^
    Product.wxs Directory.wxs UI.wxs

light.exe -out "%DIST_DIR%\Misa.ai.msi" ^
    "%BUILD_DIR%\final-Product.wixobj" ^
    "%BUILD_DIR%\final-Directory.wixobj" ^
    "%BUILD_DIR%\final-UI.wixobj" ^
    -ext WixUIExtension ^
    -ext WixUtilExtension ^
    -cultures:en-US ^
    -b "%MSI_CONTENT_DIR%" ^
    -reusecab

if %ERRORLEVEL% neq 0 (
    echo Final MSI creation failed.
    exit /b 1
)

echo MSI build completed successfully!
echo Output: %DIST_DIR%\Misa.ai.msi

REM Get MSI size
for %%I in ("%DIST_DIR%\Misa.ai.msi") do echo Size: %%~zI bytes

echo.
echo To sign the MSI (if you have a code signing certificate):
echo signtool sign /f certificate.pfx /p password /t http://timestamp.digicert.com "%DIST_DIR%\Misa.ai.msi"

echo.
echo To test the installation:
echo msiexec /i "%DIST_DIR%\Misa.ai.msi" /l*v install.log

echo.
echo Build completed at %date% %time%

REM Cleanup temporary files
if exist "%BUILD_DIR%" rmdir /s /q "%BUILD_DIR%"
if exist "%MSI_CONTENT_DIR%" rmdir /s /q "%MSI_CONTENT_DIR%"

exit /b 0