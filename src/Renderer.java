import lwjglutils.OGLModelOBJ;
import lwjglutils.OGLRenderTarget;
import lwjglutils.OGLTexture2D;
import lwjglutils.OGLTexImageFloat;
import lwjglutils.ShaderUtils;
import org.lwjgl.glfw.*;
import solid.Grid;
import solid.Solid;
import transforms.*;

import java.io.IOException;
import java.util.Random;
import java.util.function.IntConsumer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renderer v7 - KPGR3 2026
 * SSAO: 4-průchodový deferred shading
 * 1. geometry pass -> G-buffer (pozice, normála, albedo)
 * 2. SSAO výpočet (64 vzorků v polokouli)
 * 3. Blur (4x4 box filter)
 * 4. Finální kompozice (Blinn-Phong + AO)
 * Klávesa Q = zapnout/vypnout SSAO, E = zobrazit SSAO mapu
 */
public class Renderer extends AbstractRenderer {

    private int shaderGrid, shaderObj, shaderLight;
    private int shaderGeometry, shaderGeometryObj, shaderSSAO, shaderBlur, shaderLighting;

    private Solid grid, lightMarker, fullscreenQuad;
    private OGLModelOBJ elephant;
    private OGLTexture2D textureBricks, textureGlobe;

    // SSAO render targety
    private OGLRenderTarget gBuffer;
    private OGLRenderTarget ssaoTarget;
    private OGLRenderTarget blurTarget;
    private OGLTexture2D noiseTex;
    private float[] ssaoKernel;

    // Kamera
    private Camera camera;
    private Mat4 proj;
    private boolean perspectiveProj = true;
    private double mouseX, mouseY;
    private boolean mousePressed = false;

    // Stav
    private int currentFunction = 0;
    private int renderMode = 0;
    private int fragMode = 0;
    private boolean ambientOn = true, diffuseOn = true, specularOn = true, spotOn = true;
    private boolean animOn = true;
    private boolean ssaoOn = false;
    private boolean showSSAO = false;
    private float time = 0.0f, orbitAngle = 0.0f;

    private float lightX = 1.5f, lightY = 1.5f, lightZ = 2.0f;
    private float spotDirX = 0.0f, spotDirY = 0.0f, spotDirZ = -1.0f;
    private float spotAngle = 30.0f;
    private boolean moveW, moveS, moveA, moveD;

    @Override
    public void init() {
        super.init();
        glEnable(GL_DEPTH_TEST);
        glClearColor(0.1f, 0.1f, 0.15f, 1.0f);

        camera = new Camera()
                .withPosition(new Vec3D(0, -3, 1.5))
                .withAzimuth(Math.toRadians(90))
                .withZenith(Math.toRadians(-20))
                .withFirstPerson(true);
        proj = new Mat4PerspRH(Math.toRadians(60), height / (double) width, 0.1, 100);

        shaderGrid      = ShaderUtils.loadProgram("/shaders/grid/grid");
        shaderObj       = ShaderUtils.loadProgram("/shaders/obj/obj");
        shaderLight     = ShaderUtils.loadProgram("/shaders/light/light");
        shaderGeometry    = ShaderUtils.loadProgram("/shaders/ssao/geometry");
        shaderGeometryObj = ShaderUtils.loadProgram("/shaders/ssao/geometry_obj", "/shaders/ssao/geometry", null, null, null, null);
        shaderSSAO      = ShaderUtils.loadProgram("/shaders/ssao/quad", "/shaders/ssao/ssao", null, null, null, null, (p) -> {});
        shaderBlur      = ShaderUtils.loadProgram("/shaders/ssao/quad", "/shaders/ssao/blur", null, null, null, null, (p) -> {});
        shaderLighting  = ShaderUtils.loadProgram("/shaders/ssao/quad", "/shaders/ssao/lighting", null, null, null, null, (p) -> {});
        System.out.println("=== SSAO SHADER IDs ===");
        System.out.println("shaderGeometry: " + shaderGeometry);
        System.out.println("shaderSSAO:     " + shaderSSAO);
        System.out.println("shaderBlur:     " + shaderBlur);
        System.out.println("shaderLighting: " + shaderLighting);
        System.out.println("======================");

        grid           = new Grid(60, 60);
        lightMarker    = new Grid(4, 4);
        fullscreenQuad = new Grid(2, 2);

        try { elephant = new OGLModelOBJ("/obj/ElephantBody.obj"); }
        catch (Exception e) { System.err.println("OBJ nenacten: " + e.getMessage()); }

        try {
            textureBricks = new OGLTexture2D("./textures/bricks.jpg");
            textureGlobe  = new OGLTexture2D("./textures/globe.jpg");
        } catch (IOException e) { System.err.println("Textura nenactena"); }

        initSSAO();
        initGBuffer();
    }

    private void initSSAO() {
        Random rand = new Random(42);
        ssaoKernel = new float[64 * 3];
        for (int i = 0; i < 64; i++) {
            float x = rand.nextFloat() * 2.0f - 1.0f;
            float y = rand.nextFloat() * 2.0f - 1.0f;
            float z = rand.nextFloat();
            float len = (float) Math.sqrt(x*x + y*y + z*z);
            x /= len; y /= len; z /= len;
            float scale = 0.1f + (i / 64.0f) * (i / 64.0f) * 0.9f;
            ssaoKernel[i*3] = x*scale; ssaoKernel[i*3+1] = y*scale; ssaoKernel[i*3+2] = z*scale;
        }
        float[] noiseData = new float[16 * 3];
        for (int i = 0; i < 16; i++) {
            noiseData[i*3]   = rand.nextFloat() * 2.0f - 1.0f;
            noiseData[i*3+1] = rand.nextFloat() * 2.0f - 1.0f;
            noiseData[i*3+2] = 0.0f;
        }
        try {
            noiseTex = new OGLTexture2D(new OGLTexImageFloat(4, 4,
                    new OGLTexImageFloat.Format(3), noiseData));
        } catch (Exception e) { System.err.println("Noise tex error: " + e.getMessage()); }
    }

    private void initGBuffer() {
        gBuffer    = new OGLRenderTarget(width, height, 3, new OGLTexImageFloat.Format(3));
        ssaoTarget = new OGLRenderTarget(width, height, 1, new OGLTexImageFloat.Format(1));
        blurTarget = new OGLRenderTarget(width, height, 1, new OGLTexImageFloat.Format(1));
    }

    @Override
    public void display() {
        if (animOn) { time += 0.01f; orbitAngle += 0.02f; }

        float[] viewArr  = camera.getViewMatrix().floatArray();
        float[] projArr  = proj.floatArray();
        float[] lightPos = {lightX, lightY, lightZ};
        float[] eyePos   = {(float)camera.getPosition().getX(),
                             (float)camera.getPosition().getY(),
                             (float)camera.getPosition().getZ()};

        // Světlo ve view space pro deferred pass
        Point3D lp = new Point3D(lightX, lightY, lightZ).mul(camera.getViewMatrix());
        float[] lightPosView = {(float)lp.getX(), (float)lp.getY(), (float)lp.getZ()};

        if (ssaoOn) {
            renderGeometryPass(viewArr, projArr);
            renderSSAOPass(projArr);
            renderBlurPass();
            renderLightingPass(lightPos, lightPosView);
        } else {
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            glViewport(0, 0, width, height);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            renderStandard(viewArr, projArr, lightPos, eyePos);
        }

        double speed = 0.05;
        if (moveW) camera = camera.forward(speed);
        if (moveS) camera = camera.backward(speed);
        if (moveA) camera = camera.left(speed);
        if (moveD) camera = camera.right(speed);

        renderHUD();
    }

    private void renderGeometryPass(float[] viewArr, float[] projArr) {
        gBuffer.bind();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glPolygonMode(GL_FRONT_AND_BACK, renderMode == 0 ? GL_FILL : renderMode == 1 ? GL_LINE : GL_POINT);

        glUseProgram(shaderGeometry);
        setUniformMat4(shaderGeometry, "uView",  viewArr);
        setUniformMat4(shaderGeometry, "uProj",  projArr);
        setUniform1f(shaderGeometry,   "uTime",  time);

        setUniformMat4(shaderGeometry, "uModel", new Mat4Identity().floatArray());
        setUniform1i(shaderGeometry,   "uFunction", currentFunction);
        if (textureBricks != null) textureBricks.bind(shaderGeometry, "uTexture");
        grid.getBuffers().draw(GL_TRIANGLES, shaderGeometry);

        setUniformMat4(shaderGeometry, "uModel", new Mat4Transl(3, 0, 0).floatArray());
        setUniform1i(shaderGeometry,   "uFunction", (currentFunction + 1) % 6);
        if (textureGlobe != null) textureGlobe.bind(shaderGeometry, "uTexture");
        grid.getBuffers().draw(GL_TRIANGLES, shaderGeometry);

        if (elephant != null) {
            double oY = Math.sin(orbitAngle) * 150, oZ = Math.cos(orbitAngle) * 150;
            glUseProgram(shaderGeometryObj);
            setUniformMat4(shaderGeometryObj, "uView",  viewArr);
            setUniformMat4(shaderGeometryObj, "uProj",  projArr);
            setUniformMat4(shaderGeometryObj, "uModel",
                new Mat4Transl(0, oY, oZ).mul(new Mat4RotX(Math.toRadians(180))).mul(new Mat4Scale(0.01)).floatArray());
            if (textureBricks != null) textureBricks.bind(shaderGeometryObj, "uTexture");
            elephant.getBuffers().draw(GL_TRIANGLES, shaderGeometryObj);
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void renderSSAOPass(float[] projArr) {
        ssaoTarget.bind();
        glClear(GL_COLOR_BUFFER_BIT);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glUseProgram(shaderSSAO);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getColorTexture(0).getTextureId());
        setUniform1i(shaderSSAO, "gPosition", 0);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getColorTexture(1).getTextureId());
        setUniform1i(shaderSSAO, "gNormal", 1);
        if (noiseTex != null) {
            glActiveTexture(GL_TEXTURE2);
            glBindTexture(GL_TEXTURE_2D, noiseTex.getTextureId());
            setUniform1i(shaderSSAO, "uNoiseTex", 2);
        }
        for (int i = 0; i < 64; i++) {
            int loc = glGetUniformLocation(shaderSSAO, "uSamples[" + i + "]");
            if (loc >= 0) glUniform3f(loc, ssaoKernel[i*3], ssaoKernel[i*3+1], ssaoKernel[i*3+2]);
        }
        setUniformMat4(shaderSSAO, "uProj", projArr);
        setUniform2f(shaderSSAO, "uNoiseScale", new float[]{width / 4.0f, height / 4.0f});
        fullscreenQuad.getBuffers().draw(GL_TRIANGLES, shaderSSAO);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void renderBlurPass() {
        blurTarget.bind();
        glClear(GL_COLOR_BUFFER_BIT);
        glUseProgram(shaderBlur);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, ssaoTarget.getColorTexture(0).getTextureId());
        setUniform1i(shaderBlur, "uSSAOInput", 0);
        fullscreenQuad.getBuffers().draw(GL_TRIANGLES, shaderBlur);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private void renderLightingPass(float[] lightPos, float[] lightPosView) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glUseProgram(shaderLighting);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getColorTexture(0).getTextureId());
        setUniform1i(shaderLighting, "gPosition", 0);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getColorTexture(1).getTextureId());
        setUniform1i(shaderLighting, "gNormal", 1);
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, gBuffer.getColorTexture(2).getTextureId());
        setUniform1i(shaderLighting, "gAlbedo", 2);
        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, blurTarget.getColorTexture(0).getTextureId());
        setUniform1i(shaderLighting, "uSSAOBlur", 3);

        setUniform3f(shaderLighting, "uLightPos",     lightPos);
        setUniform3f(shaderLighting, "uLightPosView", lightPosView);
        setUniform1i(shaderLighting, "uShowSSAO",  showSSAO  ? 1 : 0);
        setUniform1i(shaderLighting, "uAmbient",   ambientOn  ? 1 : 0);
        setUniform1i(shaderLighting, "uDiffuse",   diffuseOn  ? 1 : 0);
        setUniform1i(shaderLighting, "uSpecular",  specularOn ? 1 : 0);

        fullscreenQuad.getBuffers().draw(GL_TRIANGLES, shaderLighting);
    }

    private void renderStandard(float[] viewArr, float[] projArr, float[] lightPos, float[] eyePos) {
        glPolygonMode(GL_FRONT_AND_BACK, renderMode == 0 ? GL_FILL : renderMode == 1 ? GL_LINE : GL_POINT);
        float[] spotDir = {spotDirX, spotDirY, spotDirZ};

        glUseProgram(shaderGrid);
        setUniformMat4(shaderGrid, "uView", viewArr); setUniformMat4(shaderGrid, "uProj", projArr);
        setUniform1i(shaderGrid, "uFunction", currentFunction); setUniform1f(shaderGrid, "uTime", time);
        setUniform1i(shaderGrid, "uFragMode", fragMode);
        setUniform3f(shaderGrid, "uLightPos", lightPos); setUniform3f(shaderGrid, "uEyePos", eyePos);
        setUniform1i(shaderGrid, "uAmbient", ambientOn?1:0); setUniform1i(shaderGrid, "uDiffuse", diffuseOn?1:0);
        setUniform1i(shaderGrid, "uSpecular", specularOn?1:0); setUniform1i(shaderGrid, "uSpot", spotOn?1:0);
        setUniform3f(shaderGrid, "uSpotDir", spotDir); setUniform1f(shaderGrid, "uSpotAngle", spotAngle);
        setUniformMat4(shaderGrid, "uModel", new Mat4Identity().floatArray());
        if (textureBricks != null) textureBricks.bind(shaderGrid, "uTexture");
        grid.getBuffers().draw(GL_TRIANGLES, shaderGrid);
        setUniformMat4(shaderGrid, "uModel", new Mat4Transl(3, 0, 0).floatArray());
        setUniform1i(shaderGrid, "uFunction", (currentFunction + 1) % 6);
        if (textureGlobe != null) textureGlobe.bind(shaderGrid, "uTexture");
        grid.getBuffers().draw(GL_TRIANGLES, shaderGrid);
        setUniform1i(shaderGrid, "uFunction", currentFunction);

        if (elephant != null) {
            glUseProgram(shaderObj);
            double oY = Math.sin(orbitAngle)*150, oZ = Math.cos(orbitAngle)*150;
            setUniformMat4(shaderObj, "uModel",
                new Mat4Transl(0,oY,oZ).mul(new Mat4RotX(Math.toRadians(180))).mul(new Mat4Scale(0.01)).floatArray());
            setUniformMat4(shaderObj, "uView", viewArr); setUniformMat4(shaderObj, "uProj", projArr);
            setUniform3f(shaderObj, "uLightPos", lightPos); setUniform3f(shaderObj, "uEyePos", eyePos);
            setUniform1i(shaderObj, "uFragMode", fragMode);
            if (textureBricks != null) textureBricks.bind(shaderObj, "uTexture");
            elephant.getBuffers().draw(GL_TRIANGLES, shaderObj);
        }

        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glUseProgram(shaderLight);
        float[] ml = new Mat4Transl(lightX,lightY,lightZ).mul(new Mat4Scale(0.1)).floatArray();
        setUniformMat4(shaderLight, "uModel", ml);
        setUniformMat4(shaderLight, "uView", viewArr); setUniformMat4(shaderLight, "uProj", projArr);
        lightMarker.getBuffers().draw(GL_TRIANGLES, shaderLight);
    }

    private void renderHUD() {
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        textRenderer.clear();
        textRenderer.addStr2D(5,  20, "Funkce: " + getFunctionName() + "  [F1-F6]");
        textRenderer.addStr2D(5,  40, "Mod zobrazeni: " + getRenderModeName() + "  [M]");
        textRenderer.addStr2D(5,  60, "Frag mod [N]: " + getFragModeName() + " (jen bez SSAO)");
        textRenderer.addStr2D(5,  80, "Projekce [P]: " + (perspectiveProj ? "perspektivni" : "ortogonalni"));
        textRenderer.addStr2D(5, 100, "Ambient[1] Diffuse[2] Specular[3] Spot[4]: "
                + b(ambientOn) + " " + b(diffuseOn) + " " + b(specularOn) + " " + b(spotOn));
        textRenderer.addStr2D(5, 120, "Animace [T]: " + b(animOn) + "  Svetlo IJKL+UO");
        textRenderer.addStr2D(5, 140, "Spot Y/H/G/B uhel Z/X: ("
                + String.format("%.1f", spotDirX) + ","
                + String.format("%.1f", spotDirY) + ","
                + String.format("%.1f", spotDirZ) + ") "
                + String.format("%.0f", spotAngle) + "deg");
        textRenderer.addStr2D(5, 160, "SSAO [Q]: " + b(ssaoOn) + "  Zobraz SSAO [E]: " + b(showSSAO));
        textRenderer.addStr2D(width - 110, height - 5, "(c) PGRF UHK");
        textRenderer.draw();
    }

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
        if (renderMode == 0) return "Plochy"; if (renderMode == 1) return "Hrany"; return "Body";
    }
    private String getFragModeName() {
        if (fragMode == 0) return "0: Osvetleni+Textura"; if (fragMode == 1) return "1: Normala";
        if (fragMode == 2) return "2: Pozice"; if (fragMode == 3) return "3: Hloubka";
        if (fragMode == 4) return "4: UV"; if (fragMode == 5) return "5: Bez textury";
        if (fragMode == 6) return "6: Vzdalenost"; if (fragMode == 7) return "7: Textura RGBA";
        return "?";
    }
    private String b(boolean v) { return v ? "ON" : "off"; }

    private void setUniformMat4(int p, String n, float[] v) { int l=glGetUniformLocation(p,n); if(l>=0) glUniformMatrix4fv(l,false,v); }
    private void setUniform1i(int p, String n, int v)       { int l=glGetUniformLocation(p,n); if(l>=0) glUniform1i(l,v); }
    private void setUniform1f(int p, String n, float v)     { int l=glGetUniformLocation(p,n); if(l>=0) glUniform1f(l,v); }
    private void setUniform3f(int p, String n, float[] v)   { int l=glGetUniformLocation(p,n); if(l>=0) glUniform3f(l,v[0],v[1],v[2]); }
    private void setUniform2f(int p, String n, float[] v)   { int l=glGetUniformLocation(p,n); if(l>=0) glUniform2f(l,v[0],v[1]); }

    private final GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) glfwSetWindowShouldClose(window, true);
            boolean pressed = (action == GLFW_PRESS || action == GLFW_REPEAT);
            if (key == GLFW_KEY_W) moveW = pressed; if (key == GLFW_KEY_S) moveS = pressed;
            if (key == GLFW_KEY_A) moveA = pressed; if (key == GLFW_KEY_D) moveD = pressed;
            if (action == GLFW_PRESS) {
                if (key == GLFW_KEY_F1) currentFunction = 0; if (key == GLFW_KEY_F2) currentFunction = 1;
                if (key == GLFW_KEY_F3) currentFunction = 2; if (key == GLFW_KEY_F4) currentFunction = 3;
                if (key == GLFW_KEY_F5) currentFunction = 4; if (key == GLFW_KEY_F6) currentFunction = 5;
                if (key == GLFW_KEY_M) renderMode = (renderMode + 1) % 3;
                if (key == GLFW_KEY_N) fragMode = (fragMode + 1) % 8;
                if (key == GLFW_KEY_P) {
                    perspectiveProj = !perspectiveProj;
                    proj = perspectiveProj
                        ? new Mat4PerspRH(Math.toRadians(60), height/(double)width, 0.1, 100)
                        : new Mat4OrthoRH(6, 6*height/(double)width, 0.1, 100);
                }
                if (key == GLFW_KEY_1) ambientOn  = !ambientOn;
                if (key == GLFW_KEY_2) diffuseOn  = !diffuseOn;
                if (key == GLFW_KEY_3) specularOn = !specularOn;
                if (key == GLFW_KEY_4) spotOn     = !spotOn;
                if (key == GLFW_KEY_T) animOn     = !animOn;
                if (key == GLFW_KEY_Q) ssaoOn     = !ssaoOn;
                if (key == GLFW_KEY_E) { showSSAO = !showSSAO; if (showSSAO) ssaoOn = true; }
                if (key == GLFW_KEY_I) lightY += 0.2f; if (key == GLFW_KEY_K) lightY -= 0.2f;
                if (key == GLFW_KEY_J) lightX -= 0.2f; if (key == GLFW_KEY_L) lightX += 0.2f;
                if (key == GLFW_KEY_U) lightZ += 0.2f; if (key == GLFW_KEY_O) lightZ -= 0.2f;
                if (key == GLFW_KEY_Y) spotDirX += 0.1f; if (key == GLFW_KEY_H) spotDirX -= 0.1f;
                if (key == GLFW_KEY_G) spotDirY += 0.1f; if (key == GLFW_KEY_B) spotDirY -= 0.1f;
                if (key == GLFW_KEY_Z) spotAngle = Math.min(spotAngle + 2.0f, 89.0f);
                if (key == GLFW_KEY_X) spotAngle = Math.max(spotAngle - 2.0f,  5.0f);
            }
        }
    };

    private final GLFWWindowSizeCallback wsCallback = new GLFWWindowSizeCallback() {
        @Override
        public void invoke(long window, int w, int h) {
            if (w > 0 && h > 0) {
                width = w; height = h;
                if (textRenderer != null) textRenderer.resize(width, height);
                proj = perspectiveProj
                    ? new Mat4PerspRH(Math.toRadians(60), h/(double)w, 0.1, 100)
                    : new Mat4OrthoRH(6, 6*h/(double)w, 0.1, 100);
                initGBuffer();
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
            if (button == GLFW_MOUSE_BUTTON_MIDDLE && action == GLFW_PRESS) {
                camera = new Camera().withPosition(new Vec3D(0,-3,1.5))
                        .withAzimuth(Math.toRadians(90)).withZenith(Math.toRadians(-20)).withFirstPerson(true);
            }
        }
    };

    private final GLFWCursorPosCallback cpCallbacknew = new GLFWCursorPosCallback() {
        @Override
        public void invoke(long window, double x, double y) {
            if (mousePressed) {
                double dx = x-mouseX, dy = y-mouseY; mouseX = x; mouseY = y;
                camera = camera.addAzimuth(Math.toRadians(-dx*0.3)).addZenith(Math.toRadians(-dy*0.3));
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
