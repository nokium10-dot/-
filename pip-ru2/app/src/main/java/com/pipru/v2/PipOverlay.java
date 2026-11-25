package com.pipru.v2;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowInsets;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.FrameLayout;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Окно помощника «как у Gemini»: затемнение поверх любого приложения и снизу выезжает Пип, который слушает.
 * Работает и внутри сессии голосового помощника, и в прозрачной активности (кнопка «Голос» в виджете).
 */
final class PipOverlay {
    interface Host {
        /** Закрыть окно (после анимации). */
        void dismiss();

        /** Нет разрешения на микрофон: хозяин должен его запросить. */
        void requestMic();
    }

    private final Context ctx;
    private final Host host;
    private final Handler main = new Handler(Looper.getMainLooper());
    private FrameLayout root;
    private View scrim;
    private WebView web;
    private TextToSpeech tts;
    private boolean ttsReady;
    private SpeechRecognizer recognizer;
    private boolean pageReady;
    private boolean wantListen;
    private boolean closing;
    private int panelH;
    private long lastLevel;

    PipOverlay(Context ctx, Host host) {
        this.ctx = ctx;
        this.host = host;
    }

    // ---------------------------------------------------------------- вид

    View createView() {
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        panelH = Math.min((int) (dm.heightPixels * 0.62f), (int) (520 * dm.density));

        root = new FrameLayout(ctx);
        root.setBackgroundColor(Color.TRANSPARENT);

        scrim = new View(ctx);
        scrim.setBackgroundColor(0x80000000);
        scrim.setAlpha(0f);
        scrim.setOnClickListener(v -> closeAnimated());
        root.addView(scrim, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        web = PipAssets.createWebView(ctx, PipAssets.loader(ctx));
        web.addJavascriptInterface(new Bridge(), "PipOverlay");
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, panelH, Gravity.BOTTOM);
        root.addView(web, lp);
        web.setTranslationY(panelH);
        web.loadUrl(PipAssets.BASE + "face.html?mode=overlay");

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                bottom = insets.getInsets(WindowInsets.Type.systemBars()).bottom;
            } else {
                bottom = insets.getSystemWindowInsetBottom();
            }
            float dp = bottom / ctx.getResources().getDisplayMetrics().density;
            js("window.pipFace&&pipFace.inset(" + Math.round(dp) + ")");
            return insets;
        });

        tts = new TextToSpeech(ctx.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS && tts != null) {
                int r = tts.setLanguage(new Locale("ru", "RU"));
                ttsReady = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED;
                tts.setPitch(1.25f);
                tts.setSpeechRate(1.05f);
            }
        });
        return root;
    }

    /** Показывает окно: затемнение появляется, Пип выезжает снизу и начинает слушать. */
    void show() {
        if (root == null) return;
        closing = false;
        wantListen = true;
        web.setTranslationY(panelH);
        scrim.setAlpha(0f);
        scrim.animate().alpha(1f).setDuration(260).start();
        web.animate().translationY(0f).setDuration(520).setInterpolator(new OvershootInterpolator(0.85f)).start();
        if (pageReady) {
            js("window.pipFace&&pipFace.enter()");
            startListening();
        }
    }

    /** Окно скрыто системой: микрофон и речь выключаем. */
    void onHidden() {
        wantListen = false;
        stopListening();
        if (tts != null) tts.stop();
        js("window.pipFace&&pipFace.save()");
    }

    void closeAnimated() {
        if (closing || root == null) return;
        closing = true;
        stopListening();
        if (tts != null) tts.stop();
        scrim.animate().alpha(0f).setDuration(220).start();
        web.animate().translationY(panelH).setDuration(260).setInterpolator(new DecelerateInterpolator())
                .withEndAction(host::dismiss).start();
    }

    void destroy() {
        stopListening();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        if (web != null) {
            if (root != null) root.removeView(web);
            web.loadUrl("about:blank");
            web.destroy();
            web = null;
        }
    }

    /** Разрешение на микрофон получено. */
    void micGranted() {
        startListening();
    }

    void restartListening() {
        wantListen = true;
        if (pageReady) startListening();
    }

    private void js(String code) {
        if (web != null) web.evaluateJavascript("try{" + code + "}catch(e){}", null);
    }

    private static String q(String s) {
        return JSONObject.quote(s == null ? "" : s);
    }

    // ---------------------------------------------------------------- мост страницы

    private final class Bridge {
        @JavascriptInterface
        public void ready() {
            main.post(() -> {
                pageReady = true;
                js("window.pipFace&&pipFace.enter()");
                if (wantListen) startListening();
            });
        }

        @JavascriptInterface
        public void listen() {
            main.post(PipOverlay.this::startListening);
        }

        @JavascriptInterface
        public void stop() {
            main.post(PipOverlay.this::stopListening);
        }

        @JavascriptInterface
        public void speak(String text) {
            main.post(() -> speakText(text));
        }

        @JavascriptInterface
        public void close() {
            main.post(PipOverlay.this::closeAnimated);
        }

        @JavascriptInterface
        public void openApp() {
            main.post(() -> {
                Intent i = new Intent(ctx, PipActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                try {
                    ctx.startActivity(i);
                } catch (RuntimeException ignored) {
                }
                closeAnimated();
            });
        }

        @JavascriptInterface
        public void haptic() {
            main.post(() -> {
                if (web != null) web.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            });
        }
    }

    // ---------------------------------------------------------------- голос

    private void startListening() {
        if (closing || web == null) return;
        if (ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            host.requestMic();
            return;
        }
        stopListening();
        if (tts != null) tts.stop();
        SpeechRecognizer r = createRecognizer();
        if (r == null) {
            js("window.pipFace&&pipFace.state('error'," + q("На телефоне не найден сервис распознавания речи.") + ")");
            return;
        }
        recognizer = r;
        r.setRecognitionListener(new Listener());
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU");
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.getPackageName());
        js("window.pipFace&&pipFace.state('listening')");
        try {
            r.startListening(i);
        } catch (RuntimeException e) {
            stopListening();
            js("window.pipFace&&pipFace.state('error'," + q("Не получилось включить микрофон.") + ")");
        }
    }

    private SpeechRecognizer createRecognizer() {
        try {
            ComponentName other = RecognizerPicker.pick(ctx);
            if (other != null) return SpeechRecognizer.createSpeechRecognizer(ctx, other);
            if (SpeechRecognizer.isRecognitionAvailable(ctx)) return SpeechRecognizer.createSpeechRecognizer(ctx);
        } catch (RuntimeException ignored) {
        }
        return null;
    }

    private void stopListening() {
        if (recognizer != null) {
            try {
                recognizer.cancel();
                recognizer.destroy();
            } catch (RuntimeException ignored) {
            }
            recognizer = null;
        }
    }

    private void speakText(String text) {
        if (tts == null || !ttsReady || text == null || text.isEmpty()) return;
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "pip-overlay");
    }

    private final class Listener implements RecognitionListener {
        @Override public void onReadyForSpeech(Bundle params) { }

        @Override public void onBeginningOfSpeech() {
            js("window.pipFace&&pipFace.state('hearing')");
        }

        @Override public void onRmsChanged(float rmsdB) {
            long now = SystemClock.uptimeMillis();
            if (now - lastLevel < 60) return;
            lastLevel = now;
            float level = Math.max(0f, Math.min(1f, (rmsdB + 2f) / 12f));
            js("window.pipFace&&pipFace.level(" + String.format(Locale.US, "%.2f", level) + ")");
        }

        @Override public void onBufferReceived(byte[] buffer) { }

        @Override public void onEndOfSpeech() {
            js("window.pipFace&&pipFace.state('thinking')");
        }

        @Override public void onError(int error) {
            String message;
            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                message = "Не расслышал. Нажмите на микрофон и скажите ещё раз.";
            } else if (error == SpeechRecognizer.ERROR_NETWORK || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) {
                message = "Для распознавания речи нужен интернет.";
            } else if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                message = "Нет доступа к микрофону. Разрешите его в настройках приложения.";
            } else if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                message = "Распознавание занято. Попробуйте через секунду.";
            } else if (error == SpeechRecognizer.ERROR_AUDIO || error == SpeechRecognizer.ERROR_CLIENT) {
                message = "Не получилось включить микрофон.";
            } else if (error == SpeechRecognizer.ERROR_SERVER) {
                message = "Сервис распознавания речи не ответил.";
            } else {
                message = "Ошибка распознавания (код " + error + ").";
            }
            js("window.pipFace&&pipFace.state('error'," + q(message) + ")");
        }

        @Override public void onResults(Bundle results) {
            ArrayList<String> m = results == null ? null
                    : results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (m != null && !m.isEmpty()) {
                js("window.pipFace&&pipFace.heard(" + q(m.get(0)) + ")");
            } else {
                js("window.pipFace&&pipFace.state('error'," + q("Не расслышал. Скажите ещё раз.") + ")");
            }
        }

        @Override public void onPartialResults(Bundle partialResults) {
            ArrayList<String> m = partialResults == null ? null
                    : partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (m != null && !m.isEmpty()) js("window.pipFace&&pipFace.partial(" + q(m.get(0)) + ")");
        }

        @Override public void onEvent(int eventType, Bundle params) { }
    }
}
