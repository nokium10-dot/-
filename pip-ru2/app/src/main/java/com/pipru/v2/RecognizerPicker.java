package com.pipru.v2;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.speech.RecognitionService;

import java.util.List;

/**
 * Выбирает настоящий сервис распознавания речи (Google и т.п.), исключая сам Пип.
 * Когда Пип назначен помощником, система может сделать его распознавателем по умолчанию,
 * поэтому свой микрофон и «прокси»-сервис Пипа всегда обращаются напрямую к другому сервису.
 */
final class RecognizerPicker {
    private RecognizerPicker() {}

    static ComponentName pick(Context context) {
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> services;
        try {
            services = pm.queryIntentServices(new Intent(RecognitionService.SERVICE_INTERFACE), 0);
        } catch (RuntimeException e) {
            return null;
        }
        ComponentName best = null;
        int bestRank = Integer.MAX_VALUE;
        for (ResolveInfo info : services) {
            if (info.serviceInfo == null || !info.serviceInfo.exported) continue;
            String pkg = info.serviceInfo.packageName;
            if (context.getPackageName().equals(pkg)) continue;
            int rank = rank(pkg);
            if (rank < bestRank) {
                bestRank = rank;
                best = new ComponentName(pkg, info.serviceInfo.name);
            }
        }
        return best;
    }

    private static int rank(String pkg) {
        if ("com.google.android.googlequicksearchbox".equals(pkg)) return 0;
        if ("com.google.android.tts".equals(pkg)) return 1;
        if (pkg.startsWith("com.google.")) return 2;
        if (pkg.startsWith("com.samsung.")) return 5;
        return 10;
    }
}
