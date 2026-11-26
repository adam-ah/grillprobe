@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"

call build.bat || exit /b 1

set "SERIAL="
for /f "skip=1 tokens=1,2" %%a in ('"%ADB%" devices') do (
    if "%%b"=="device" if not defined SERIAL (
        for /f %%s in ('"%ADB%" -s %%a shell getprop ro.build.version.sdk') do (
            if %%s GEQ 31 set "SERIAL=%%a"
        )
    )
)
if not defined SERIAL (
    echo No Android 12+ phone connected with USB debugging enabled.
    exit /b 1
)

echo Installing on !SERIAL!
"%ADB%" -s !SERIAL! install -r ..\GrillProbe.apk || exit /b 1
ping -n 4 127.0.0.1 >nul
"%ADB%" -s !SERIAL! shell am start -n app.grillprobe/.MainActivity
