@echo off
REM KPGR3-2026 Application Launcher
REM This script sets up the environment and runs the application

echo ========================================
echo   KPGR3-2026 Graphics Application
echo ========================================
echo.

echo Setting up environment...
set JAVA_HOME=C:\Users\PC\Desktop\KPPRO\jdk-11.0.28+6
set M2_HOME=C:\apache-maven-3.8.1
set PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%

echo.
echo Starting application...
echo Press Ctrl+C to stop the application
echo.

cd /d "C:\Users\PC\Desktop\KPPRO\kpgr3-2026"
mvn exec:java -Dexec.mainClass="App" -q

echo.
echo Application finished.
pause
