# KPGR3-2026 Application Launcher
# This script sets up the environment and runs the application

Write-Host "Setting up environment..." -ForegroundColor Green

# Set environment variables
$env:JAVA_HOME = "C:\Users\PC\Desktop\KPPRO\jdk-11.0.28+6"
$env:M2_HOME = "C:\apache-maven-3.8.1"
$env:PATH = "$env:JAVA_HOME\bin;$env:M2_HOME\bin;" + $env:PATH

Write-Host ""
Write-Host "Starting KPGR3-2026 Application..." -ForegroundColor Green
Write-Host ""

# Change to project directory and run
Set-Location "C:\Users\PC\Desktop\KPPRO\kpgr3-2026"
& mvn exec:java -Dexec.mainClass="App" -q

Write-Host ""
Write-Host "Application finished." -ForegroundColor Green
Read-Host "Press Enter to exit"
