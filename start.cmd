@echo off
setlocal EnableExtensions
cd /d "%~dp0"
set "JAR=target\ohcp-deficiency-agent-1.0.0.jar"
if not exist "%JAR%" (
  echo [OHCP] First run: building project, please wait...
  call mvnw.cmd -q -DskipTests package
  if errorlevel 1 (
    echo [OHCP] Build failed. See errors above.
    pause
    exit /b 1
  )
)
echo [OHCP] Starting server: http://localhost:8080
start "OHCP Server" cmd /c "java -jar %JAR%"
timeout /t 4 /nobreak >nul
start "" "http://localhost:8080"
echo [OHCP] Server is running in a separate window. Close that window to stop.
endlocal