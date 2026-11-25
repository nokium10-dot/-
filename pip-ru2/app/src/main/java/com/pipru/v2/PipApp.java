package com.pipru.v2;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/** Пока открыто любое окно Пипа, «живой» виджет отдыхает: так питомец в виджете и в приложении не расходятся. */
public class PipApp extends Application {
    private int started;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity a, Bundle b) { }

            @Override public void onActivityStarted(Activity a) {
                if (started++ == 0) PipWidgetService.hold(true);
            }

            @Override public void onActivityResumed(Activity a) { }

            @Override public void onActivityPaused(Activity a) { }

            @Override public void onActivityStopped(Activity a) {
                if (started > 0 && --started == 0) PipWidgetService.hold(false);
            }

            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }

            @Override public void onActivityDestroyed(Activity a) { }
        });
    }
}
