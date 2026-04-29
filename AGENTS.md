# AGENTS.md
## Project: KPGR3-2026 (OpenGL Graphics in Java)
Tech Stack: LWJGL 3.3.3, OpenGL 3.3+, Java 11, Maven
### Architecture
- Entry: App.main() → LwjglWindow(new Renderer())
- Window: LwjglWindow (GLFW init, GL context, main loop, callbacks)
- Base: AbstractRenderer (init, display, dispose, callback stubs)
- Concrete: Renderer (triangle + grid demo)
- Pattern: Template Method + Immutable Transforms
### Key Files
- App.java - Entry point
- LwjglWindow.java - GLFW + GL loop
- AbstractRenderer.java - Base renderer
- Renderer.java - Example renderer
- Camera.java - Immutable camera
- Mat4.java - 4x4 matrices
- Grid.java - Mesh generation
- ShaderUtils.java - Shader loading/compilation
- OGLBuffers.java - VAO/VBO/IBO
- OGLTexture2D.java - Texture loading
- pom.xml - Maven LWJGL deps
### Build & Run
mvn clean compile    # Fetch LWJGL, copy res/
mvn package          # Create shaded JAR
java -jar target/kpgr3-2026-2.0-shaded.jar
### Conventions
Shader Attributes - Java and GLSL must match:
  new OGLBuffers.Attrib("inPosition", 2)
  GLSL: in vec2 inPosition;
Uniforms - Prefix u: uniform vec3 uColor, uniform mat4 uView
Resources - Classpath root (Maven copies res/):
  ShaderUtils.loadProgram("/shaders/triangle/triangle");
  new OGLTexture2D("./textures/file.jpg");
Immutability - Always return new instances:
  Camera cam = camera.addAzimuth(0.01).forward(0.1);
  Never: camera.setAzimuth(...) — no such methods exist
### Input Handling
Override callbacks in Renderer:
  keyCallback = new GLFWKeyCallback() {
      public void invoke(...) {
          camera = camera.addAzimuth(0.01);  // NEW Camera
      }
  };
### Common Tasks
Add Renderer: Extend AbstractRenderer, override init() and display().
Add Shader: Create res/shaders/{name}/{name}.vert and .frag.
  Load: ShaderUtils.loadProgram("/shaders/{name}/{name}");
Load OBJ Model: OGLModelOBJ model = new OGLModelOBJ("/obj/file.obj");
Update Camera: camera = camera.withPosition(...).withAzimuth(...);
### Troubleshooting
- "package org.lwjgl.opengl does not exist" → mvn clean compile
- Shader compilation fails → Check res/shaders/ path, verify .vert/.frag
- Input unresponsive → Ensure callback creates NEW camera instance
- Texture missing → Check res/textures/ exists, Maven copies it
### Design Principles
- Immutable transforms → Thread-safe, no aliasing, functional composition
- Abstract renderer → Decouples GLFW from rendering
- Classpath resources → Works in JAR, IDE, cross-platform
- Shader-centric → Compile-time error checking
- Maven management → Automatic LWJGL resolution
