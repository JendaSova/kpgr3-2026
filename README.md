# KPGR3-2026 Graphics Project

OpenGL Graphics Application using LWJGL 3.3.3

## Quick Start

### Option 1: Run Script (Recommended)
Double-click `run_app.bat` or run `run_app.ps1` in PowerShell.

### Option 2: Command Line
```bash
# Set environment (run once per session)
set JAVA_HOME=C:\Users\PC\Desktop\KPPRO\jdk-11.0.28+6
set M2_HOME=C:\apache-maven-3.8.1
set PATH=%JAVA_HOME%\bin;%M2_HOME%\bin;%PATH%

# Run application
mvn exec:java -Dexec.mainClass="App" -q
```

### Option 3: JAR File
```bash
mvn package
java -jar target/kpgr3-2026-2.0.jar
```

## Project Structure

- `src/` - Java source files
- `res/` - Resources (shaders, textures, models)
- `lib/` - LWJGL libraries
- `run_app.bat` - Windows batch launcher
- `run_app.ps1` - PowerShell launcher

## Controls

- **WASD** - Move camera
- **Mouse** - Look around
- **ESC** - Exit

## Build Commands

```bash
mvn clean compile    # Compile only
mvn package         # Create JAR with dependencies
mvn exec:java -Dexec.mainClass="App" -q  # Run directly
```

## Requirements

- Java 11 (Eclipse Temurin)
- Maven 3.8.1
- Windows 10/11 with OpenGL support