package com.pipru.v2;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/** Общие помощники для лёгких страниц Пипа (виджет и оверлей помощника): отдача файлов из assets и настройка WebView. */
final class PipAssets {
    static final String HOST = "appassets.androidplatform.net";
    static final String BASE = "https://" + HOST + "/app/";

    private PipAssets() {}

    static WebViewAssetLoader loader(Context context) {
        final Context app = context.getApplicationContext();
        return new WebViewAssetLoader.Builder()
                .setDomain(HOST)
                .addPathHandler("/app/", path -> open(app, path))
                .build();
    }

    private static WebResourceResponse open(Context context, String path) {
        if (path == null || path.contains("..")) return null;
        if (path.isEmpty() || path.endsWith("/")) path += "index.html";
        // face.html лежит в assets/face/ (в репозитории), остальное собирает GitHub Actions в assets/pip/.
        String file = path.equals("face.html") ? "face/face.html" : "pip/" + path;
        try {
            InputStream in = context.getAssets().open(file);
            String mime = mimeOf(path);
            boolean text = mime.startsWith("text/") || mime.equals("application/json");
            return new WebResourceResponse(mime, text ? "utf-8" : null, in);
        } catch (IOException e) {
            return null;
        }
    }

    private static String mimeOf(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        if (p.endsWith(".html")) return "text/html";
        if (p.endsWith(".js") || p.endsWith(".mjs")) return "text/javascript";
        if (p.endsWith(".wasm")) return "application/wasm";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".json")) return "application/json";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }

    /** WebView для страницы лица: прозрачный, с JS и хранилищем (общим с основным приложением). */
    static WebView createWebView(Context context, final WebViewAssetLoader loader) {
        WebView web = new WebView(context);
        web.setBackgroundColor(Color.TRANSPARENT);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setLongClickable(false);
        web.setHapticFeedbackEnabled(false);
        web.setOnLongClickListener(v -> true);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(false);
        s.setTextZoom(100);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !HOST.equals(request.getUrl().getHost());
            }
        });
        return web;
    }
}
