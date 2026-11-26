@echo off
setlocal
cd /d "%~dp0"
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
call gradlew.bat --console=plain detekt lintDebug :lint-rules:test testDebugUnitTest assembleDebug %* || goto :fail
copy /y app\build\outputs\apk\debug\app-debug.apk ..\GrillProbe.apk >nul
echo.
echo Built ..\GrillProbe.apk   Screenshots: %~dp0screenshots
exit /b 0

:fail
echo.
echo BUILD FAILED
exit /b 1
