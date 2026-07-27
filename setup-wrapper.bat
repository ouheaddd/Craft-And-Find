@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-wrapper.ps1"
if errorlevel 1 (
  echo.
  echo Failed to download gradle-wrapper.jar.
  echo Copy it manually from the official NeoForge 1.21.1 ModDevGradle MDK.
  exit /b 1
)
echo.
echo Wrapper is ready. You can now run: gradlew.bat runClient
endlocal
