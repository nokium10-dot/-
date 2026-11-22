package com.matrixwall;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.Random;

public class MatrixWallpaperService extends WallpaperService {

    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ$#%&*<>/|=+";
    private static final String[] WORDS = {
            "scan", "ping 127.0.0.1", "dir /s", "decrypt", "connect", "tracert",
            "netstat -an", "access granted", "download", "inject", "bypass", "mount"
    };
    private static final int TRAIL = 18;

    @Override
    public Engine onCreateEngine() {
        return new MatrixEngine();
    }

    private class MatrixEngine extends Engine {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Runnable frame = new Runnable() {
            @Override
            public void run() {
                drawFrame();
            }
        };
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random rnd = new Random();
        private final char[] one = new char[1];
        private final char[] glyphs = new char[ShapeBank.N];
        private final ArrayList<String> lines = new ArrayList<>();

        private SharedPreferences prefs;
        private boolean visible;
        private int w = 1, h = 1;
        private long last;
        private int tick;
        private float time;

        // settings
        private int style = 0;
        private int side = 0;
        private int shapeMode = 0;
        private boolean rain = true;
        private int cr = 0, cg = 255, cb = 65, hr = 190, hg = 255, hb = 200;

        // shapes animation
        private float yaw, wobble;
        private int curShape = 0, nextShape = 1;
        private float shapeTimer, morph;
        private boolean morphing;

        // terminal
        private float termTimer;

        // rain
        private int cols;
        private float cell;
        private float[] colY, colSpeed;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            prefs = getSharedPreferences(Settings.FILE, Context.MODE_PRIVATE);
            paint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            paint.setTextAlign(Paint.Align.CENTER);
            for (int i = 0; i < glyphs.length; i++) glyphs[i] = randomChar();
            loadPrefs();
        }

        private char randomChar() {
            return CHARS.charAt(rnd.nextInt(CHARS.length()));
        }

        private int main(int alpha) {
            return Color.argb(clamp(alpha), cr, cg, cb);
        }

        private int hi(int alpha) {
            return Color.argb(clamp(alpha), hr, hg, hb);
        }

        private int clamp(int a) {
            return a < 0 ? 0 : (a > 255 ? 255 : a);
        }

        private void loadPrefs() {
            boolean unlocked = prefs.getBoolean(Settings.KEY_UNLOCKED, false);

            style = prefs.getInt(Settings.KEY_STYLE, 0);
            if (style < 0 || style >= Settings.STYLE_NAMES.length) style = 0;
            if (!unlocked && style >= Settings.FIRST_EXCLUSIVE_STYLE) style = 0;

            int color = prefs.getInt(Settings.KEY_COLOR, 0);
            if (color < 0 || color >= Settings.COLORS.length) color = 0;
            if (!unlocked && color >= Settings.FIRST_EXCLUSIVE_COLOR) color = 0;
            int[] t = Settings.COLORS[color];
            cr = t[0];
            cg = t[1];
            cb = t[2];
            hr = t[3];
            hg = t[4];
            hb = t[5];

            side = prefs.getInt(Settings.KEY_SIDE, 0);
            shapeMode = prefs.getInt(Settings.KEY_SHAPE, 0);
            rain = prefs.getBoolean(Settings.KEY_RAIN, true);
            if (shapeMode > 0 && shapeMode <= ShapeBank.COUNT) {
                curShape = shapeMode - 1;
                morphing = false;
            }
        }

        @Override
        public void onVisibilityChanged(boolean isVisible) {
            visible = isVisible;
            handler.removeCallbacks(frame);
            if (isVisible) {
                loadPrefs();
                last = SystemClock.uptimeMillis();
                handler.post(frame);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            w = Math.max(1, width);
            h = Math.max(1, height);
            initRain();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            visible = false;
            handler.removeCallbacks(frame);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            visible = false;
            handler.removeCallbacks(frame);
        }

        private void initRain() {
            cell = Math.max(20f, w / 32f);
            cols = (int) (w / cell) + 1;
            colY = new float[cols];
            colSpeed = new float[cols];
            for (int i = 0; i < cols; i++) {
                colY[i] = rnd.nextFloat() * h;
                colSpeed[i] = cell * (4f + rnd.nextFloat() * 8f);
            }
        }

        private void drawFrame() {
            if (!visible) return;
            long now = SystemClock.uptimeMillis();
            float dt = Math.min(0.1f, (now - last) / 1000f);
            last = now;

            SurfaceHolder holder = getSurfaceHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    update(dt);
                    render(c);
                }
            } catch (Exception ignored) {
            } finally {
                if (c != null) {
                    try {
                        holder.unlockCanvasAndPost(c);
                    } catch (Exception ignored) {
                    }
                }
            }
            handler.removeCallbacks(frame);
            if (visible) handler.postDelayed(frame, 25);
        }

        private void update(float dt) {
            if (++tick % 60 == 0) loadPrefs();

            time += dt;
            yaw += dt * 0.9f;
            wobble += dt * 0.5f;

            if (style == 0 && shapeMode == 0) {
                if (!morphing) {
                    shapeTimer += dt;
                    if (shapeTimer > 7f) {
                        shapeTimer = 0f;
                        nextShape = (curShape + 1) % ShapeBank.COUNT;
                        morph = 0f;
                        morphing = true;
                    }
                } else {
                    morph += dt / 1.6f;
                    if (morph >= 1f) {
                        morph = 1f;
                        morphing = false;
                        curShape = nextShape;
                    }
                }
            }

            for (int i = 0; i < glyphs.length; i++) {
                if (rnd.nextInt(45) == 0) glyphs[i] = randomChar();
            }

            if (colY != null) {
                for (int i = 0; i < cols; i++) {
                    colY[i] += colSpeed[i] * dt;
                    if (colY[i] - TRAIL * cell > h) {
                        colY[i] = -rnd.nextFloat() * h * 0.5f;
                        colSpeed[i] = cell * (4f + rnd.nextFloat() * 8f);
                    }
                }
            }

            if (style == 2) {
                termTimer += dt;
                while (termTimer > 0.12f) {
                    termTimer -= 0.12f;
                    lines.add(makeLine());
                }
                while (lines.size() > 80) lines.remove(0);
            }
        }

        private String hex(int n) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) sb.append("0123456789ABCDEF".charAt(rnd.nextInt(16)));
            return sb.toString();
        }

        private String makeLine() {
            switch (rnd.nextInt(4)) {
                case 0:
                    return "C:\\> " + WORDS[rnd.nextInt(WORDS.length)] + " " + hex(4);
                case 1:
                    return "0x" + hex(4) + "  " + hex(2) + " " + hex(2) + " " + hex(2) + " "
                            + hex(2) + "  " + hex(6);
                case 2:
                    return "[ OK ] " + WORDS[rnd.nextInt(WORDS.length)];
                default:
                    StringBuilder sb = new StringBuilder();
                    int n = 12 + rnd.nextInt(20);
                    for (int i = 0; i < n; i++) sb.append(rnd.nextBoolean() ? '1' : '0');
                    return sb.toString();
            }
        }

        private void render(Canvas c) {
            c.drawColor(Color.BLACK);
            switch (style) {
                case 1:
                    drawRain(c, 1f);
                    break;
                case 2:
                    drawTerminal(c);
                    break;
                case 3:
                    if (rain) drawRain(c, 0.45f);
                    drawGalaxy(c);
                    break;
                case 4:
                    if (rain) drawRain(c, 0.35f);
                    drawTunnel(c);
                    break;
                case 5:
                    drawWave(c);
                    break;
                default:
                    if (rain) drawRain(c, 0.55f);
                    drawShapes(c);
                    break;
            }
        }

        // ---------- rain ----------
        private void drawRain(Canvas c, float intensity) {
            if (colY == null) return;
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(cell * 0.9f);
            for (int col = 0; col < cols; col++) {
                float x = col * cell + cell / 2f;
                for (int k = 0; k < TRAIL; k++) {
                    float y = colY[col] - k * cell;
                    if (y < 0 || y > h + cell) continue;
                    int row = (int) (y / cell);
                    int idx = Math.abs(col * 131 + row * 17 + tick / 8) % CHARS.length();
                    if (k == 0) {
                        paint.setColor(hi((int) (255 * intensity)));
                    } else {
                        paint.setColor(main((int) (210 * intensity * (1f - k / (float) TRAIL))));
                    }
                    one[0] = CHARS.charAt(idx);
                    c.drawText(one, 0, 1, x, y, paint);
                }
            }
        }

        // ---------- terminal ----------
        private void drawTerminal(Canvas c) {
            float size = w / 36f;
            float lh = size * 1.35f;
            paint.setTextSize(size);
            paint.setTextAlign(Paint.Align.LEFT);
            int rows = (int) (h / lh) - 4;
            int maxChars = (int) (w / (size * 0.6f)) - 1;
            int start = Math.max(0, lines.size() - rows);
            float y = lh * 2.5f;
            for (int i = start; i < lines.size(); i++) {
                String s = lines.get(i);
                if (s.length() > maxChars) s = s.substring(0, maxChars);
                int age = lines.size() - 1 - i;
                paint.setColor(main(230 - Math.min(150, age * 4)));
                c.drawText(s, size * 0.6f, y, paint);
                y += lh;
            }
            String prompt = "C:\\> " + (((tick / 12) % 2 == 0) ? "_" : " ");
            paint.setColor(hi(255));
            c.drawText(prompt, size * 0.6f, y, paint);
            paint.setTextAlign(Paint.Align.CENTER);
        }

        // ---------- galaxy ----------
        private void drawGalaxy(Canvas c) {
            float radius = Math.min(w, h) * 0.30f;
            float cy = h * 0.45f;
            if (side == 0 || side == 2) galaxyAt(c, w * 0.27f, cy, radius, 1f);
            if (side == 1 || side == 2) galaxyAt(c, w * 0.73f, cy, radius, side == 2 ? -1f : 1f);
        }

        private void galaxyAt(Canvas c, float cx, float cy, float radius, float dir) {
            paint.setTextAlign(Paint.Align.CENTER);
            float tilt = 0.40f + 0.12f * (float) Math.sin(wobble);
            for (int i = 0; i < 360; i++) {
                int arm = i % 3;
                float t = ((i / 3) + 0.5f) / 120f;
                float ang = arm * 2.0944f + t * 5.5f + dir * yaw * 0.6f;
                float x = (float) Math.cos(ang) * t;
                float z = (float) Math.sin(ang) * t;
                float sx = cx + x * radius;
                float sy = cy + z * radius * tilt;
                float near = 1f - t;
                float size = radius * 0.09f * (0.6f + 0.8f * near);
                paint.setTextSize(size);
                paint.setColor(t < 0.15f ? hi(255) : main((int) (60 + 195 * near)));
                one[0] = glyphs[i % glyphs.length];
                c.drawText(one, 0, 1, sx, sy + size * 0.35f, paint);
            }
        }

        // ---------- tunnel (exclusive) ----------
        private void drawTunnel(Canvas c) {
            float base = Math.min(w, h);
            float cx = side == 0 ? w * 0.27f : (side == 1 ? w * 0.73f : w * 0.5f);
            float cy = h * 0.45f;
            int rings = 14;
            int per = 14;
            paint.setTextAlign(Paint.Align.CENTER);
            for (int r = 0; r < rings; r++) {
                float d = ((r + time * 2.5f) % rings) / rings;
                float rad = base * (0.02f + 0.55f * d * d);
                float size = base * (0.012f + 0.05f * d);
                float fa = Math.min(1f, d * 1.6f);
                if (d > 0.92f) fa *= (1f - d) / 0.08f;
                int alpha = (int) (255 * fa);
                float twist = yaw * 0.5f + d * 2f;
                paint.setTextSize(size);
                paint.setColor(d > 0.8f ? hi(alpha) : main(alpha));
                for (int k = 0; k < per; k++) {
                    float ang = twist + k * (6.2831855f / per);
                    float x = cx + (float) Math.cos(ang) * rad;
                    float y = cy + (float) Math.sin(ang) * rad;
                    one[0] = glyphs[(r * per + k) % glyphs.length];
                    c.drawText(one, 0, 1, x, y + size * 0.35f, paint);
                }
            }
        }

        // ---------- wave (exclusive) ----------
        private void drawWave(Canvas c) {
            float cellW = w / 18f;
            float cellH = cellW * 1.25f;
            int colsN = 19;
            int rowsN = (int) (h / cellH) + 2;
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(cellW * 0.8f);
            for (int row = 0; row < rowsN; row++) {
                for (int col = 0; col < colsN; col++) {
                    float x = col * cellW + cellW / 2f;
                    float baseY = row * cellH + cellH;
                    float ph = col * 0.45f + row * 0.22f - time * 2.2f;
                    float s = (float) Math.sin(ph);
                    float v = s * 0.5f + 0.5f;
                    int alpha = (int) (40 + 215 * v);
                    paint.setColor(v > 0.9f ? hi(alpha) : main(alpha));
                    int idx = Math.abs(col * 37 + row * 11 + tick / 10) % CHARS.length();
                    one[0] = CHARS.charAt(idx);
                    c.drawText(one, 0, 1, x, baseY + s * cellH * 0.8f, paint);
                }
            }
        }

        // ---------- shapes ----------
        private void drawShapes(Canvas c) {
            float[] a = ShapeBank.get(curShape);
            float[] b = morphing ? ShapeBank.get(nextShape) : null;
            float t = 0f;
            if (morphing) t = morph * morph * (3f - 2f * morph);

            float radius = Math.min(w, h) * 0.20f;
            float cy = h * 0.45f;

            if (side == 0 || side == 2) drawShape(c, a, b, t, w * 0.27f, cy, radius, yaw);
            if (side == 1 || side == 2) {
                drawShape(c, a, b, t, w * 0.73f, cy, radius, side == 2 ? -yaw + 1.5f : yaw);
            }
        }

        private void drawShape(Canvas c, float[] a, float[] b, float t,
                               float cx, float cy, float radius, float angle) {
            paint.setTextAlign(Paint.Align.CENTER);
            float cosY = (float) Math.cos(angle);
            float sinY = (float) Math.sin(angle);
            float pitch = 0.45f + 0.25f * (float) Math.sin(wobble);
            float cosP = (float) Math.cos(pitch);
            float sinP = (float) Math.sin(pitch);
            float baseSize = radius * 0.14f;

            for (int i = 0; i < ShapeBank.N; i++) {
                float x = a[i * 3];
                float y = a[i * 3 + 1];
                float z = a[i * 3 + 2];
                if (b != null) {
                    x += (b[i * 3] - x) * t;
                    y += (b[i * 3 + 1] - y) * t;
                    z += (b[i * 3 + 2] - z) * t;
                }
                float x1 = x * cosY + z * sinY;
                float z1 = -x * sinY + z * cosY;
                float y2 = y * cosP - z1 * sinP;
                float z2 = y * sinP + z1 * cosP;

                float persp = 3.2f / (3.2f + z2);
                float sx = cx + x1 * radius * persp;
                float sy = cy + y2 * radius * persp;

                float near = (1.2f - z2) / 2.4f;
                if (near < 0f) near = 0f;
                if (near > 1f) near = 1f;

                int alpha = (int) (70 + 185 * near);
                paint.setColor(near > 0.88f ? hi(alpha) : main(alpha));
                float size = baseSize * (0.7f + 0.7f * near);
                paint.setTextSize(size);
                one[0] = glyphs[i];
                c.drawText(one, 0, 1, sx, sy + size * 0.35f, paint);
            }
        }
    }
}
