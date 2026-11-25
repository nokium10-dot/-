package com.pipru.v2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Base64;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.RemoteViews;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * «Живой» виджет: скрытая страница лица Пипа (тот же симулятор) крутится в сервисе и присылает кадры,
 * а виджет рисует их. Пока открыто окно Пипа или погашен экран, сервис отдыхает.
 */
public class PipWidgetService extends Service {
    static final String ACTION_POKE = "com.pipru.v2.widget.POKE";
    static final String ACTION_FEED = "com.pipru.v2.widget.FEED";
    static final String ACTION_PLAY = "com.pipru.v2.widget.PLAY";
    static final String ACTION_PET = "com.pipru.v2.widget.PET";

    private static final String CHANNEL = "pip_widget";
    private static final int NOTE_ID = 4101;
    private static final int FW = 233;   // кадр от страницы: 233×233, RGB565
    private static final int OUT = 360;  // картинка в виджете

    private static volatile PipWidgetService instance;
    private static int holders; // сколько окон Пипа открыто (только главный поток)

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ArrayList<String> queued = new ArrayList<>();
    private final Object frameLock = new Object();
    private WebView web;
    private boolean ready;
    private boolean paused;
    private boolean screenOn = true;
    private BroadcastReceiver screenReceiver;

    private Bitmap src;
    private Bitmap out;
    private Canvas outCanvas;
    private Paint paint;
    private int[] ids = new int[0];
    private long idsAt;
    private long savedAt;

    /** Окно Пипа открылось (on=true) или закрылось (on=false). */
    static void hold(boolean on) {
        holders = Math.max(0, holders + (on ? 1 : -1));
        PipWidgetService s = instance;
        if (s != null) s.applyHold();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        paused = holders > 0;
        try {
            goForeground();
        } catch (RuntimeException e) {
            stopSelf();
            return;
        }
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        screenOn = pm == null || pm.isInteractive();
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                screenOn = !Intent.ACTION_SCREEN_OFF.equals(intent.getAction());
                applyRun();
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(screenReceiver, f);
        createWeb();
    }

    private void goForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Пип на рабочем столе",
                NotificationManager.IMPORTANCE_MIN));
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, PipActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_pip)
                .setContentTitle("Пип живёт на рабочем столе")
                .setContentText("Удалите виджет, чтобы остановить")
                .setContentIntent(open)
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTE_ID, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTE_ID, n);
        }
    }

    private void createWeb() {
        web = PipAssets.createWebView(getApplicationContext(), PipAssets.loader(this));
        web.addJavascriptInterface(new Bridge(), "PipWidget");
        // Страница без окна: даём ей размер и «видимость», чтобы таймеры не засыпали.
        web.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY));
        web.layout(0, 0, 400, 400);
        try {
            web.dispatchWindowVisibilityChanged(View.VISIBLE);
        } catch (RuntimeException ignored) {
        }
        web.onResume();
        web.resumeTimers();
        web.loadUrl(PipAssets.BASE + "face.html?mode=widget");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (action != null) {
            String act = null;
            if (ACTION_POKE.equals(action)) act = "poke";
            else if (ACTION_FEED.equals(action)) act = "feed";
            else if (ACTION_PLAY.equals(action)) act = "play";
            else if (ACTION_PET.equals(action)) act = "pet";
            if (act != null) run("window.pipFace&&pipFace.act('" + act + "')");
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        instance = null;
        if (screenReceiver != null) {
            try {
                unregisterReceiver(screenReceiver);
            } catch (RuntimeException ignored) {
            }
            screenReceiver = null;
        }
        if (web != null) {
            web.loadUrl("about:blank");
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    private void run(String code) {
        if (web == null) return;
        if (!ready) {
            queued.add(code);
            return;
        }
        web.evaluateJavascript("try{" + code + "}catch(e){}", null);
    }

    private void applyRun() {
        if (web == null || !ready) return;
        boolean go = screenOn && !paused;
        web.evaluateJavascript(go ? "try{pipFace.resume()}catch(e){}" : "try{pipFace.pause()}catch(e){}", null);
    }

    private void applyHold() {
        boolean now = holders > 0;
        if (now == paused || web == null) return;
        paused = now;
        if (paused) {
            applyRun();
        } else {
            // Окно закрылось: питомец мог измениться, поэтому страница читает его заново.
            ready = false;
            web.reload();
        }
    }

    private final class Bridge {
        @JavascriptInterface
        public void ready() {
            main.post(() -> {
                ready = true;
                for (String code : queued) run(code);
                queued.clear();
                applyRun();
            });
        }

        @JavascriptInterface
        public void frame(String base64) {
            handleFrame(base64);
        }
    }

    private void handleFrame(String base64) {
        if (base64 == null || base64.isEmpty()) return;
        byte[] raw;
        try {
            raw = Base64.decode(base64, Base64.DEFAULT);
        } catch (IllegalArgumentException e) {
            return;
        }
        if (raw.length < FW * FW * 2) return;
        synchronized (frameLock) {
            if (src == null) {
                src = Bitmap.createBitmap(FW, FW, Bitmap.Config.RGB_565);
                out = Bitmap.createBitmap(OUT, OUT, Bitmap.Config.ARGB_8888);
                outCanvas = new Canvas(out);
                BitmapShader shader = new BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                Matrix m = new Matrix();
                m.setScale(OUT / (float) FW, OUT / (float) FW);
                shader.setLocalMatrix(m);
                paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
                paint.setShader(shader);
            }
            src.copyPixelsFromBuffer(ByteBuffer.wrap(raw));
            out.eraseColor(0);
            outCanvas.drawCircle(OUT / 2f, OUT / 2f, OUT / 2f - 1f, paint); // круглый экран Пипа
            push();
        }
    }

    private void push() {
        long now = System.currentTimeMillis();
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(this);
            if (now - idsAt > 2000) {
                idsAt = now;
                ids = mgr.getAppWidgetIds(new ComponentName(this, PipWidgetProvider.class));
                if (ids == null || ids.length == 0) {
                    main.post(this::stopSelf); // виджет удалили
                    return;
                }
            }
            RemoteViews rv = new RemoteViews(getPackageName(), R.layout.widget_pip);
            rv.setImageViewBitmap(R.id.pip_face, out);
            mgr.partiallyUpdateAppWidget(ids, rv);
            if (now - savedAt > 8000) {
                savedAt = now;
                saveLast();
            }
        } catch (RuntimeException ignored) {
        }
    }

    /** Последний кадр хранится в файле: после перезапуска виджет сразу показывает Пипа, а не пустое место. */
    private void saveLast() {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(PipWidgetProvider.lastFrameFile(this));
            out.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (IOException ignored) {
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
