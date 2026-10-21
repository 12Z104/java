@echo off
setlocal EnableExtensions
set "BOOT=%~dp0mvnw.ps1"
if not exist "%BOOT%" (
  echo [OHCP] Bootstrap script not found: %BOOT%
  exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -File "%BOOT%" %*
exit /b %ERRORLEVEL%