package com.pipru.v2;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.RemoteViews;

import java.io.File;

/** Виджет 3×3: живое лицо Пипа на рабочем столе. Касание лица — тык, кнопки — кормить, играть, гладить, голос. */
public class PipWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        RemoteViews views = build(context);
        for (int id : ids) manager.updateAppWidget(id, views);
        start(context);
    }

    @Override
    public void onEnabled(Context context) {
        start(context);
    }

    @Override
    public void onDisabled(Context context) {
        try {
            context.stopService(new Intent(context, PipWidgetService.class));
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            if (hasWidgets(context)) start(context);
            return;
        }
        super.onReceive(context, intent);
    }

    static boolean hasWidgets(Context context) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(context);
            int[] ids = m.getAppWidgetIds(new ComponentName(context, PipWidgetProvider.class));
            return ids != null && ids.length > 0;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Запускает «живой» режим. Если система не разрешает (фон), виджет просто показывает последний кадр до первого касания. */
    static void start(Context context) {
        try {
            context.startForegroundService(new Intent(context, PipWidgetService.class));
        } catch (RuntimeException ignored) {
        }
    }

    static File lastFrameFile(Context context) {
        return new File(context.getFilesDir(), "widget_last.png");
    }

    static RemoteViews build(Context context) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_pip);
        try {
            File f = lastFrameFile(context);
            if (f.exists()) {
                Bitmap last = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (last != null) rv.setImageViewBitmap(R.id.pip_face, last);
            }
        } catch (RuntimeException ignored) {
        }
        rv.setOnClickPendingIntent(R.id.pip_face, service(context, PipWidgetService.ACTION_POKE, 11));
        rv.setOnClickPendingIntent(R.id.btn_feed, service(context, PipWidgetService.ACTION_FEED, 12));
        rv.setOnClickPendingIntent(R.id.btn_play, service(context, PipWidgetService.ACTION_PLAY, 13));
        rv.setOnClickPendingIntent(R.id.btn_pet, service(context, PipWidgetService.ACTION_PET, 14));
        Intent voice = new Intent(context, PipOverlayActivity.class)
                .setAction(Intent.ACTION_ASSIST)
                .putExtra(PipActivity.EXTRA_AUTO_LISTEN, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        rv.setOnClickPendingIntent(R.id.btn_voice, PendingIntent.getActivity(context, 15, voice,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        return rv;
    }

    private static PendingIntent service(Context context, String action, int code) {
        Intent i = new Intent(context, PipWidgetService.class).setAction(action);
        return PendingIntent.getForegroundService(context, code, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
