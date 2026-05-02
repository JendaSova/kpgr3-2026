import lwjglutils.OGLBuffers;
import lwjglutils.OGLModelOBJ;
import lwjglutils.OGLRenderTarget;
import lwjglutils.OGLTexture2D;
import lwjglutils.OGLUtils;
import lwjglutils.ShaderUtils;
import org.lwjgl.glfw.*;
import solid.Grid;
import solid.Solid;
import transforms.*;

import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renderer - KPGR3 2026
 * Prostorová scéna s parametrickými tělesy, osvětlením (Blinn-Phong),
 * reflektorem, kamerou WSAD+myš, přepínáním projekce a debug módy.
 */
public class Renderer extends AbstractRenderer {

    // ── Shader programy ──────────────────────────────────────────────────────
    private int shaderGrid;      // hlavní shader pro grid
    private int shaderObj;       // shader pro OBJ model (slon)
    private int shaderLight;     // malý shader pro znázornění pozice světla

    // ── Geometrie ────────────────────────────────────────────────────────────
    private Solid grid;          // rovinný grid jako základ pro parametrická tělesa
    private Solid lightMarker;   // malý grid jako značka světla
    private OGLModelOBJ elephant;// načtený OBJ model

    // ── Textury ──────────────────────────────────────────────────────────────
    private OGLTexture2D textureBricks;
    private OGLTexture2D textureGlobe;

    // ── Kamera a projekce ────────────────────────────────────────────────────
    private Camera camera;
    private Mat4 proj;
    private boolean perspectiveProj = true;  // true = perspektiva, false = orto
    private double mouseX, mouseY;
    private boolean mousePressed = false;

    // ── Stav aplikace ────────────────────────────────────────────────────────
    private int  currentFunction = 0;   // 0-5: která parametrická funkce
    private int  renderMode = 0;        // 0=plochy, 1=hrany, 2=body
    private int  fragMode = 0;          // 0=textura+osvětlení, 1=normála, 2=pozice, 3=hloubka, 4=UV, 5=difúzní
    private boolean ambientOn  = true;
    private boolean diffuseOn  = true;
    private boolean specularOn = true;
    private boolean spotOn     = true;
    private boolean animOn     = true;   // animace uniform proměnné (čas)
    private float   time       = 0.0f;

    // Pozice světla — pohybuje se klávesami IJKL
    private float lightX =  1.5f;
    private float lightY =  1.5f;
    private float lightZ =  2.0f;

    // Pohyb
    private boolean moveW, moveS, moveA, moveD;

    // ── Init ─────────────────────────────────────────────────────────────────
    @Override
    public void init() {
        super.init();
        glEnable(GL_DEPTH_TEST);
        glClearColor(0.1f, 0.1f, 0.15f, 1.0f);

        // Kamera
        camera = new Camera()
                .withPosition(new Vec3D(0, -3, 1.5))
                .withAzimuth(Math.toRadians(90))
                .withZenith(Math.toRadians(-20))
                .withFirstPerson(true);

        proj = new Mat4PerspRH(Math.toRadians(60), height / (double) width, 0.1, 100);

        // Shadery
        shaderGrid   = ShaderUtils.loadProgram("/shaders/grid/grid");
        shaderObj    = ShaderUtils.loadProgram("/shaders/obj/obj");
        shaderLight  = ShaderUtils.loadProgram("/shaders/light/light");

        // Geometrie
        grid        = new Grid(60, 60);   // jemný grid pro pěkný výsledek
        lightMarker = new Grid(4, 4);

        try {
            elephant = new OGLModelOBJ("/obj/ElephantBody.obj");
        } catch (Exception e) {
            System.err.println("OBJ nenačteno: " + e.getMessage());
        }

        // Textury
        try {
            textureBricks = new OGLTexture2D("./textures/bricks.jpg");
            textureGlobe  = new OGLTexture2D("./textures/globe.jpg");
        } catch (IOException e) {
            System.err.println("Textura nenačtena: " + e.getMessage());
        }
    }

    // ── Display (každý snímek) ────────────────────────────────────────────────
    @Override
    public void display() {
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (animOn) time += 0.01f;

        // --- Nastavení polygon módu ---
        if (renderMode == 0)      glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        else if (renderMode == 1) glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        else                      glPolygonMode(GL_FRONT_AND_BACK, GL_POINT);

        float[] viewArr = camera.getViewMatrix().floatArray();
        float[] projArr = proj.floatArray();
        float[] lightPos = {lightX, lightY, lightZ};
        float[] eyePos = {
            (float) camera.getPosition().getX(),
            (float) camera.getPosition().getY(),
            (float) camera.getPosition().getZ()
        };

        // ══ Render gridu (parametrická tělesa) ══
        glUseProgram(shaderGrid);

        setUniformMat4(shaderGrid, "uView", viewArr);
        setUniformMat4(shaderGrid, "uProj", projArr);
        setUniform1i(shaderGrid, "uFunction", currentFunction);
        setUniform1f(shaderGrid, "uTime",     time);
        setUniform1i(shaderGrid, "uFragMode", fragMode);
        setUniform3f(shaderGrid, "uLightPos", lightPos);
        setUniform3f(shaderGrid, "uEyePos",   eyePos);
        setUniform1i(shaderGrid, "uAmbient",  ambientOn  ? 1 : 0);
        setUniform1i(shaderGrid, "uDiffuse",  diffuseOn  ? 1 : 0);
        setUniform1i(shaderGrid, "uSpecular", specularOn ? 1 : 0);
        setUniform1i(shaderGrid, "uSpot",     spotOn     ? 1 : 0);

        // Model matice — první těleso (identita)
        float[] modelId = new Mat4Identity().floatArray();
        setUniformMat4(shaderGrid, "uModel", modelId);

        if (textureBricks != null) textureBricks.bind(shaderGrid, "uTexture");

        grid.getBuffers().draw(GL_TRIANGLES, shaderGrid);

        // ── Druhé těleso: funkce 1 (sférická) posunutá o +3 na ose X ──
        float[] modelShift = new Mat4Transl(3, 0, 0).floatArray();
        setUniformMat4(shaderGrid, "uModel", modelShift);
        int prevFn = currentFunction;
        setUniform1i(shaderGrid, "uFunction", (currentFunction + 1) % 6);

        if (textureGlobe != null) textureGlobe.bind(shaderGrid, "uTexture");
        grid.getBuffers().draw(GL_TRIANGLES, shaderGrid);

        setUniform1i(shaderGrid, "uFunction", prevFn);

        // ══ Render OBJ modelu (slon) ══
        if (elephant != null) {
            glUseProgram(shaderObj);
            float[] modelElephant = new Mat4Transl(-3, 0, 0)
                    .mul(new Mat4RotX(Math.toRadians(-180)))
                    .mul(new Mat4Scale(0.01))
                    .floatArray();
            setUniformMat4(shaderObj, "uModel",  modelElephant);
            setUniformMat4(shaderObj, "uView",   viewArr);
            setUniformMat4(shaderObj, "uProj",   projArr);
            setUniform3f(shaderObj,   "uLightPos", lightPos);
            setUniform3f(shaderObj,   "uEyePos",   eyePos);
            setUniform1i(shaderObj,   "uFragMode", fragMode);
            if (textureBricks != null) textureBricks.bind(shaderObj, "uTexture");
            elephant.getBuffers().draw(GL_TRIANGLES, shaderObj);
        }

        // ══ Render značky světla ══
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glUseProgram(shaderLight);
        float[] modelLight = new Mat4Transl(lightX, lightY, lightZ)
                .mul(new Mat4Scale(0.1))
                .floatArray();
        setUniformMat4(shaderLight, "uModel", modelLight);
        setUniformMat4(shaderLight, "uView",  viewArr);
        setUniformMat4(shaderLight, "uProj",  projArr);
        lightMarker.getBuffers().draw(GL_TRIANGLES, shaderLight);

        // ── WSAD pohyb ──
        double speed = 0.05;
        if (moveW) camera = camera.forward(speed);
        if (moveS) camera = camera.backward(speed);
        if (moveA) camera = camera.left(speed);
        if (moveD) camera = camera.right(speed);

        // ── HUD text ──
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        textRenderer.clear();
        textRenderer.addStr2D(5, 20, "Funkce: " + getFunctionName() + "  [F1-F6]");
        textRenderer.addStr2D(5, 40, "Mod zobrazeni: " + getRenderModeName() + "  [M]");
        textRenderer.addStr2D(5, 60, "Frag mód: " + getFragModeName() + "  [N]");
        textRenderer.addStr2D(5, 80, "Projekce: " + (perspectiveProj ? "perspektivní" : "ortogonální") + "  [P]");
        textRenderer.addStr2D(5, 100,"Ambient[1] Diffuse[2] Specular[3] Spot[4]: "
                + b(ambientOn) + " " + b(diffuseOn) + " " + b(specularOn) + " " + b(spotOn));
        textRenderer.addStr2D(5, 120,"Animace [T]: " + b(animOn) + "  Světlo IJKL+UO");
        textRenderer.addStr2D(width - 110, height - 5, "(c) PGRF UHK");
        textRenderer.draw();
    }

    // ── Pomocné metody ────────────────────────────────────────────────────────
    private String getFunctionName() {
        if (currentFunction == 0) return "0: Kartezska - vlna cos";
        if (currentFunction == 1) return "1: Kartezska - sedlo";
        if (currentFunction == 2) return "2: Sfericka - koule";
        if (currentFunction == 3) return "3: Sfericka - torus";
        if (currentFunction == 4) return "4: Cylindricka - valec";
        if (currentFunction == 5) return "5: Cylindricka - sroubovice";
        return "?";
    }
    private String getRenderModeName() {
        if (renderMode == 0) return "Plochy";
        if (renderMode == 1) return "Hrany";
        return "Body";
    }
    private String getFragModeName() {
        if (fragMode == 0) return "Textura+Osvetleni";
        if (fragMode == 1) return "Normala";
        if (fragMode == 2) return "Pozice XYZ";
        if (fragMode == 3) return "Hloubka Z";
        if (fragMode == 4) return "UV souradnice";
        if (fragMode == 5) return "Difuzni slozka";
        return "?";
    }
    private String b(boolean v) { return v ? "ON" : "off"; }

    private void setUniformMat4(int prog, String name, float[] val) {
        int loc = glGetUniformLocation(prog, name);
        if (loc >= 0) glUniformMatrix4fv(loc, false, val);
    }
    private void setUniform1i(int prog, String name, int val) {
        int loc = glGetUniformLocation(prog, name);
        if (loc >= 0) glUniform1i(loc, val);
    }
    private void setUniform1f(int prog, String name, float val) {
        int loc = glGetUniformLocation(prog, name);
        if (loc >= 0) glUniform1f(loc, val);
    }
    private void setUniform3f(int prog, String name, float[] val) {
        int loc = glGetUniformLocation(prog, name);
        if (loc >= 0) glUniform3f(loc, val[0], val[1], val[2]);
    }

    // ── Callbacks ────────────────────────────────────────────────────────────
    private final GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true);

            // WSAD
            boolean pressed = (action == GLFW_PRESS || action == GLFW_REPEAT);
            if (key == GLFW_KEY_W) moveW = pressed;
            if (key == GLFW_KEY_S) moveS = pressed;
            if (key == GLFW_KEY_A) moveA = pressed;
            if (key == GLFW_KEY_D) moveD = pressed;

            if (action == GLFW_PRESS) {
                // Výběr funkce
                if (key == GLFW_KEY_F1) currentFunction = 0;
                if (key == GLFW_KEY_F2) currentFunction = 1;
                if (key == GLFW_KEY_F3) currentFunction = 2;
                if (key == GLFW_KEY_F4) currentFunction = 3;
                if (key == GLFW_KEY_F5) currentFunction = 4;
                if (key == GLFW_KEY_F6) currentFunction = 5;

                // Mód zobrazení
                if (key == GLFW_KEY_M) renderMode = (renderMode + 1) % 3;
                // Frag mód
                if (key == GLFW_KEY_N) fragMode = (fragMode + 1) % 6;
                // Projekce
                if (key == GLFW_KEY_P) {
                    perspectiveProj = !perspectiveProj;
                    if (perspectiveProj)
                        proj = new Mat4PerspRH(Math.toRadians(60), height / (double) width, 0.1, 100);
                    else
                        proj = new Mat4OrthoRH(6, 6 * height / (double) width, 0.1, 100);
                }
                // Složky osvětlení
                if (key == GLFW_KEY_1) ambientOn  = !ambientOn;
                if (key == GLFW_KEY_2) diffuseOn  = !diffuseOn;
                if (key == GLFW_KEY_3) specularOn = !specularOn;
                if (key == GLFW_KEY_4) spotOn     = !spotOn;
                // Animace
                if (key == GLFW_KEY_T) animOn = !animOn;

                // Pohyb světla
                if (key == GLFW_KEY_I) lightY += 0.2f;
                if (key == GLFW_KEY_K) lightY -= 0.2f;
                if (key == GLFW_KEY_J) lightX -= 0.2f;
                if (key == GLFW_KEY_L) lightX += 0.2f;
                if (key == GLFW_KEY_U) lightZ += 0.2f;
                if (key == GLFW_KEY_O) lightZ -= 0.2f;
            }
        }
    };

    private final GLFWWindowSizeCallback wsCallback = new GLFWWindowSizeCallback() {
        @Override
        public void invoke(long window, int w, int h) {
            if (w > 0 && h > 0) {
                width = w; height = h;
                if (textRenderer != null) textRenderer.resize(width, height);
                if (perspectiveProj)
                    proj = new Mat4PerspRH(Math.toRadians(60), h / (double) w, 0.1, 100);
                else
                    proj = new Mat4OrthoRH(6, 6 * h / (double) w, 0.1, 100);
            }
        }
    };

    private final GLFWMouseButtonCallback mbCallback = new GLFWMouseButtonCallback() {
        @Override
        public void invoke(long window, int button, int action, int mods) {
            if (button == GLFW_MOUSE_BUTTON_1) {
                mousePressed = (action == GLFW_PRESS);
                if (mousePressed) {
                    double[] xb = new double[1], yb = new double[1];
                    glfwGetCursorPos(window, xb, yb);
                    mouseX = xb[0]; mouseY = yb[0];
                }
            }
        }
    };

    private final GLFWCursorPosCallback cpCallbacknew = new GLFWCursorPosCallback() {
        @Override
        public void invoke(long window, double x, double y) {
            if (mousePressed) {
                double dx = x - mouseX;
                double dy = y - mouseY;
                mouseX = x; mouseY = y;
                camera = camera
                        .addAzimuth(Math.toRadians(-dx * 0.3))
                        .addZenith(Math.toRadians(-dy * 0.3));
            }
        }
    };

    private final GLFWScrollCallback scrollCallback = new GLFWScrollCallback() {
        @Override
        public void invoke(long window, double dx, double dy) {
            camera = dy > 0 ? camera.forward(0.3) : camera.backward(0.3);
        }
    };

    @Override public GLFWKeyCallback getKeyCallback()          { return keyCallback;    }
    @Override public GLFWWindowSizeCallback getWsCallback()    { return wsCallback;     }
    @Override public GLFWMouseButtonCallback getMouseCallback(){ return mbCallback;     }
    @Override public GLFWCursorPosCallback getCursorCallback() { return cpCallbacknew; }
    @Override public GLFWScrollCallback getScrollCallback()    { return scrollCallback; }
}
