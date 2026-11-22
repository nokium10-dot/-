package com.matrixwall;

/** Наборы 3D-точек для фигур. Каждая фигура ровно из N точек (для плавного перехода). */
final class ShapeBank {
    static final int N = 300;
    static final int COUNT = 5;
    static final String[] NAMES = {"Куб", "Сфера", "Тор", "Пирамида", "ДНК"};

    private static final float[][] CACHE = new float[COUNT][];

    private ShapeBank() {}

    /** Плоский массив x,y,z,x,y,z... длиной N*3, координаты примерно в [-1;1]. */
    static synchronized float[] get(int shape) {
        if (CACHE[shape] == null) CACHE[shape] = build(shape);
        return CACHE[shape];
    }

    private static float[] build(int shape) {
        switch (shape) {
            case 0: return cube();
            case 1: return sphere();
            case 2: return torus();
            case 3: return pyramid();
            default: return helix();
        }
    }

    private static void put(float[] p, int i, float x, float y, float z) {
        p[i * 3] = x;
        p[i * 3 + 1] = y;
        p[i * 3 + 2] = z;
    }

    private static float[] cube() {
        float[] p = new float[N * 3];
        float s = 0.75f;
        int[][] edges = new int[12][2];
        int e = 0;
        for (int v = 0; v < 8; v++) {
            for (int b = 0; b < 3; b++) {
                if ((v & (1 << b)) == 0) {
                    edges[e][0] = v;
                    edges[e][1] = v | (1 << b);
                    e++;
                }
            }
        }
        for (int i = 0; i < N; i++) {
            int[] ed = edges[i % 12];
            float t = ((i / 12) + 0.5f) / 25f;
            float[] a = cubeVertex(ed[0], s);
            float[] b = cubeVertex(ed[1], s);
            put(p, i, a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t);
        }
        return p;
    }

    private static float[] cubeVertex(int v, float s) {
        return new float[]{(v & 1) == 0 ? -s : s, (v & 2) == 0 ? -s : s, (v & 4) == 0 ? -s : s};
    }

    private static float[] sphere() {
        float[] p = new float[N * 3];
        for (int i = 0; i < N; i++) {
            float y = 1f - 2f * (i + 0.5f) / N;
            float r = (float) Math.sqrt(1f - y * y);
            float phi = i * 2.3999632f;
            put(p, i, (float) Math.cos(phi) * r * 0.9f, y * 0.9f, (float) Math.sin(phi) * r * 0.9f);
        }
        return p;
    }

    private static float[] torus() {
        float[] p = new float[N * 3];
        int cols = 30;
        int rows = N / cols;
        float big = 0.65f;
        float small = 0.3f;
        for (int i = 0; i < N; i++) {
            double u = 2 * Math.PI * (i % cols) / cols;
            double v = 2 * Math.PI * (i / cols) / rows;
            float rr = big + small * (float) Math.cos(v);
            put(p, i, rr * (float) Math.cos(u), small * (float) Math.sin(v), rr * (float) Math.sin(u));
        }
        return p;
    }

    private static float[] pyramid() {
        float[] p = new float[N * 3];
        float[][] v = {
                {-0.8f, 0.7f, -0.8f},
                {0.8f, 0.7f, -0.8f},
                {0.8f, 0.7f, 0.8f},
                {-0.8f, 0.7f, 0.8f},
                {0f, -0.9f, 0f}
        };
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {0, 4}, {1, 4}, {2, 4}, {3, 4}};
        for (int i = 0; i < N; i++) {
            int[] ed = edges[i % 8];
            float t = ((i / 8) + 0.5f) / 38f;
            float[] a = v[ed[0]];
            float[] b = v[ed[1]];
            put(p, i, a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t);
        }
        return p;
    }

    private static float[] helix() {
        float[] p = new float[N * 3];
        for (int i = 0; i < N; i++) {
            int m = i % 3;
            int j = i / 3;
            float t = j / 99f;
            double a = t * 6 * Math.PI;
            float y = (t * 2f - 1f) * 0.95f;
            float ax = (float) Math.cos(a) * 0.55f;
            float az = (float) Math.sin(a) * 0.55f;
            float bx = (float) Math.cos(a + Math.PI) * 0.55f;
            float bz = (float) Math.sin(a + Math.PI) * 0.55f;
            if (m == 0) {
                put(p, i, ax, y, az);
            } else if (m == 1) {
                put(p, i, bx, y, bz);
            } else {
                float f = ((j * 37) % 100) / 100f;
                put(p, i, ax + (bx - ax) * f, y, az + (bz - az) * f);
            }
        }
        return p;
    }
}
