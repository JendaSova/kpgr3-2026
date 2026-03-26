package solid;

import lwjglutils.OGLBuffers;

public class Grid extends Solid {
    public Grid(int m, int n) {
        float[] vb = new float[2 * m * n];
        int[] ib = new int[3 * 2 * (m - 1) * (n - 1)];

        int index = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                vb[index++] = (float) j / (n - 1);
                vb[index++] = (float) i / (m - 1);
//                System.out.println("x: " + (float) j / (n - 1));
//                System.out.println("y: " + (float) i / (m - 1));
//                System.out.println("----");
            }
//            System.out.println("-----------------------------------");
        }

        index = 0;
        for (int i = 0; i < m - 1; i++) {
            int offset = i * n;
            for (int j = 0; j < n - 1; j++) {
                ib[index++] = j + offset;
                ib[index++] = j + n + offset;
                ib[index++] = j + 1 + offset;

                ib[index++] = j + 1 + offset;
                ib[index++] = j + n + offset;
                ib[index++] = j + n + 1 + offset;
//                System.out.println(j + offset);
//                System.out.println(j + n + offset);
//                System.out.println(j + 1 + offset);
//
//                System.out.println("-");
//
//                System.out.println(j + 1 + offset);
//                System.out.println(j + n + offset);
//                System.out.println(j + n + 1 + offset);
//
//                System.out.println("----");
            }
//            System.out.println("-----------------------------------");
        }

        OGLBuffers.Attrib[] attributes = new OGLBuffers.Attrib[] {
                new OGLBuffers.Attrib("inPosition", 2)
        };

        buffers = new OGLBuffers(vb, attributes ,ib);
    }
}
