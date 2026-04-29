import lwjglutils.OGLBuffers;
import lwjglutils.OGLTexture2D;
import lwjglutils.OGLUtils;
import lwjglutils.ShaderUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import solid.Grid;
import solid.Solid;
import transforms.Camera;
import transforms.Mat4PerspRH;
import transforms.Vec3D;

import java.io.IOException;

import static org.lwjgl.opengl.GL11C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL20.*;

/**
 * @author PGRF FIM UHK
 * @version 2.0
 * @since 2019-09-02
 */
public class Renderer extends AbstractRenderer {

    private OGLBuffers triangleBuffers;
    private int shaderProgramTriangle, shaderProgramGrid;

    private Solid grid;

    private Camera camera;
    private Mat4PerspRH proj;

    // Textures
    private OGLTexture2D textureBricks;

    @Override
    public void init() {
//      *************  Kamera a projekce *************
        camera = new Camera()
                .withPosition(new Vec3D(0, -2.5, 1))
                .withAzimuth(Math.toRadians(90))
                .withZenith(Math.toRadians(-25))
                .withFirstPerson(true);

        proj = new Mat4PerspRH(Math.toRadians(90), height / (double) width, 0.1, 100);

//      *************  Triangle  *************
        float[] vb = new float[] {
          0.5f, 1,  1, 0, 0,
          -1, 0,    0, 1, 0,
           1, -1,   0, 0, 1
        };

        int[] ib = new int[] {
                0, 1, 2
        };

        OGLBuffers.Attrib[] attributes = new OGLBuffers.Attrib[] {
          new OGLBuffers.Attrib("inPosition", 2),
                new OGLBuffers.Attrib("inColor", 3)
        };

        triangleBuffers = new OGLBuffers(vb, attributes, ib);

        shaderProgramTriangle = ShaderUtils.loadProgram("/shaders/triangle/triangle");
        glUseProgram(shaderProgramTriangle);

        int uColorLoc = glGetUniformLocation(shaderProgramTriangle, "uColor");
        glUniform3f(uColorLoc, 0f, 0f, 1f);

//      *************  Grid *************
        grid = new Grid(20, 20);

        shaderProgramGrid = ShaderUtils.loadProgram("/shaders/grid/grid");
        glUseProgram(shaderProgramGrid);

//      *************  Textures *************
        try {
            textureBricks = new OGLTexture2D("textures/bricks.jpg");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        //glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
    }

    @Override
    public void display() {
//      ************* Render grid *************
        glUseProgram(shaderProgramGrid);
        textureBricks.bind(shaderProgramGrid, "textureBricks");

        int uViewLoc = glGetUniformLocation(shaderProgramGrid, "uView");
        glUniformMatrix4fv(uViewLoc, false, camera.getViewMatrix().floatArray());

        int uProjLoc = glGetUniformLocation(shaderProgramGrid, "uProj");
        glUniformMatrix4fv(uProjLoc, false, proj.floatArray());

        grid.getBuffers().draw(GL_TRIANGLES, shaderProgramGrid);
    }

    private GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            int uColorLoc = glGetUniformLocation(shaderProgramTriangle, "uColor");
            glUniform3f(uColorLoc, 0f, 1f, 1f);
        }
    };

    private GLFWWindowSizeCallback wsCallback = new GLFWWindowSizeCallback() {
        @Override
        public void invoke(long window, int w, int h) {
        }
    };

    private GLFWMouseButtonCallback mbCallback = new GLFWMouseButtonCallback() {
        @Override
        public void invoke(long window, int button, int action, int mods) {

        }

    };

    private GLFWCursorPosCallback cpCallbacknew = new GLFWCursorPosCallback() {
        @Override
        public void invoke(long window, double x, double y) {
        }
    };

    private GLFWScrollCallback scrollCallback = new GLFWScrollCallback() {
        @Override
        public void invoke(long window, double dx, double dy) {
        }
    };


    @Override
    public GLFWKeyCallback getKeyCallback() {
        return keyCallback;
    }

    @Override
    public GLFWWindowSizeCallback getWsCallback() {
        return wsCallback;
    }

    @Override
    public GLFWMouseButtonCallback getMouseCallback() {
        return mbCallback;
    }

    @Override
    public GLFWCursorPosCallback getCursorCallback() {
        return cpCallbacknew;
    }

    @Override
    public GLFWScrollCallback getScrollCallback() {
        return scrollCallback;
    }
}