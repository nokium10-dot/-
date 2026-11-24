package com.pipru.assistant;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Пип на Android: оригинальный симулятор Pip-KeyPet (LVGL → WebAssembly + Three.js),
 * запущенный в WebView, с русским мобильным интерфейсом и голосовым управлением.
 */
public final class PipActivity extends Activity {
    static final String EXTRA_AUTO_LISTEN = "com.pipru.assistant.AUTO_LISTEN";
    static final String EXTRA_OPEN_SETTINGS = "com.pipru.assistant.OPEN_SETTINGS";

    private static final String HOST = "appassets.androidplatform.net";
    private static final String HOME_URL = "https://" + HOST + "/app/index.html";
    private static final int REQ_AUDIO = 701;
    private static final int REQ_SPEECH_UI = 702;
    private static final int REQ_ROLE = 703;

    private FrameLayout root;
    private WebView webView;
    private TextToSpeech tts;
    private boolean ttsReady;
    private SpeechRecognizer recognizer;
    private boolean pageReady;
    private boolean pendingListen;
    private boolean pendingSettings;
    private long lastLevelPush;

    // ---------------------------------------------------------------- жизненный цикл

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#050609"));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int l, t, r, b;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets i = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                l = i.left;
                t = i.top;
                r = i.right;
                b = i.bottom;
            } else {
                l = insets.getSystemWindowInsetLeft();
                t = insets.getSystemWindowInsetTop();
                r = insets.getSystemWindowInsetRight();
                b = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(l, t, r, b);
            return insets;
        });
        setContentView(root);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS && tts != null) {
                int r = tts.setLanguage(new Locale("ru", "RU"));
                ttsReady = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED;
                tts.setPitch(1.25f);
                tts.setSpeechRate(1.05f);
            }
        });

        createWebView();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideStatusBar();
        if (webView != null) {
            webView.onResume();
            js("window.pipRefreshAssistant&&pipRefreshAssistant()");
        }
    }

    @Override
    protected void onPause() {
        if (webView != null) {
            js("window.pipApi&&pipApi.save()");
            webView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        stopListening();
        if (tts != null) tts.stop();
        super.onStop();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideStatusBar();
    }

    @Override
    protected void onDestroy() {
        stopListening();
        if (webView != null) {
            root.removeView(webView);
            webView.loadUrl("about:blank");
            webView.destroy();
            webView = null;
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (webView == null) {
            super.onBackPressed();
            return;
        }
        // Сначала закрываем окна внутри страницы (меню, подтверждение), и только потом выходим.
        webView.evaluateJavascript("(window.pipUi&&pipUi.back&&pipUi.back())?1:0", value -> {
            if (!"1".equals(value)) finish();
        });
    }

    private void hideStatusBar() {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        boolean listen = intent.getBooleanExtra(EXTRA_AUTO_LISTEN, false)
                || Intent.ACTION_ASSIST.equals(action)
                || Intent.ACTION_VOICE_COMMAND.equals(action)
                || Intent.ACTION_SEARCH_LONG_PRESS.equals(action);
        boolean settings = intent.getBooleanExtra(EXTRA_OPEN_SETTINGS, false);
        if (listen) {
            if (Build.VERSION.SDK_INT >= 27) setShowWhenLocked(true);
            if (pageReady) startVoice();
            else pendingListen = true;
        }
        if (settings) {
            if (pageReady) js("window.pipUi&&pipUi.open('look')");
            else pendingSettings = true;
        }
        // Чтобы повторный вход в приложение не включал микрофон снова.
        intent.removeExtra(EXTRA_AUTO_LISTEN);
        intent.removeExtra(EXTRA_OPEN_SETTINGS);
    }

    // ---------------------------------------------------------------- WebView

    private void createWebView() {
        WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
                .setDomain(HOST)
                .addPathHandler("/app/", this::loadAsset)
                .build();

        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor("#050609"));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setLongClickable(false);
        webView.setHapticFeedbackEnabled(false);
        webView.setOnLongClickListener(v -> true);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setTextZoom(100);

        webView.addJavascriptInterface(new Bridge(), "PipAndroid");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Внутри приложения только локальные страницы Пипа.
                return !HOST.equals(request.getUrl().getHost());
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                runOnUiThread(PipActivity.this::recreate);
                return true;
            }
        });

        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        webView.loadUrl(HOME_URL);
    }

    /** Отдаёт файлы из assets/pip/ с правильными MIME-типами (особенно .js и .wasm). */
    private WebResourceResponse loadAsset(String path) {
        if (path == null || path.contains("..")) return null;
        if (path.isEmpty() || path.endsWith("/")) path += "index.html";
        try {
            InputStream in = getAssets().open("pip/" + path);
            String mime = mimeOf(path);
            boolean text = mime.startsWith("text/") || mime.equals("application/json")
                    || mime.equals("image/svg+xml");
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

    private void js(String code) {
        if (webView != null) webView.evaluateJavascript("try{" + code + "}catch(e){}", null);
    }

    private static String q(String s) {
        return JSONObject.quote(s == null ? "" : s);
    }

    /** То, что страница может вызвать из JavaScript (window.PipAndroid). */
    private final class Bridge {
        @JavascriptInterface
        public void onReady() {
            runOnUiThread(() -> {
                pageReady = true;
                if (pendingSettings) {
                    pendingSettings = false;
                    js("window.pipUi&&pipUi.open('look')");
                }
                if (pendingListen) {
                    pendingListen = false;
                    startVoice();
                }
                js("window.pipRefreshAssistant&&pipRefreshAssistant()");
            });
        }

        @JavascriptInterface
        public void startVoice() {
            runOnUiThread(PipActivity.this::startVoice);
        }

        @JavascriptInterface
        public void stopVoice() {
            runOnUiThread(PipActivity.this::stopListening);
        }

        @JavascriptInterface
        public void speak(String text) {
            runOnUiThread(() -> PipActivity.this.speak(text));
        }

        @JavascriptInterface
        public boolean isAssistant() {
            return PipActivity.this.isAssistant();
        }

        @JavascriptInterface
        public void chooseAssistant() {
            runOnUiThread(PipActivity.this::requestAssistantRole);
        }

        @JavascriptInterface
        public void openAssistantSettings() {
            runOnUiThread(PipActivity.this::openAssistantSettings);
        }

        @JavascriptInterface
        public void haptic() {
            runOnUiThread(() -> {
                if (webView != null) webView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            });
        }

        @JavascriptInterface
        public String version() {
            try {
                PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
                return info.versionName == null ? "" : info.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                return "";
            }
        }
    }

    // ---------------------------------------------------------------- голос: речь → текст

    private void startVoice() {
        if (!pageReady) {
            pendingListen = true;
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        beginListening();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_AUDIO) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            beginListening();
        } else {
            js("window.pipVoice&&pipVoice.state('error'," + q("Без доступа к микрофону Пип не услышит команду. "
                    + "Разрешите микрофон в настройках приложения.") + ")");
            launchSpeechUi();
        }
    }

    private void beginListening() {
        stopListening();
        if (tts != null) tts.stop();
        SpeechRecognizer r = createRecognizer();
        if (r == null) {
            launchSpeechUi();
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
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        js("window.pipVoice&&pipVoice.state('listening')");
        try {
            r.startListening(i);
        } catch (RuntimeException e) {
            stopListening();
            launchSpeechUi();
        }
    }

    private SpeechRecognizer createRecognizer() {
        try {
            android.content.ComponentName other = RecognizerPicker.pick(this);
            if (other != null) return SpeechRecognizer.createSpeechRecognizer(this, other);
            if (SpeechRecognizer.isRecognitionAvailable(this)) {
                return SpeechRecognizer.createSpeechRecognizer(this);
            }
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

    /** Запасной путь: системное окно распознавания речи. */
    @SuppressWarnings("deprecation")
    private void launchSpeechUi() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажите команду Пипу");
        try {
            startActivityForResult(i, REQ_SPEECH_UI);
        } catch (ActivityNotFoundException e) {
            js("window.pipVoice&&pipVoice.state('error'," + q("На телефоне не найден сервис распознавания речи.") + ")");
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SPEECH_UI) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> m = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (m != null && !m.isEmpty()) {
                    js("window.pipVoice&&pipVoice.heard(" + q(m.get(0)) + ")");
                }
            }
        } else if (requestCode == REQ_ROLE) {
            js("window.pipRefreshAssistant&&pipRefreshAssistant()");
            if (!isAssistant()) {
                Toast.makeText(this,
                        "Если окно выбора не появилось, нажмите «Настройки помощника»",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private final class Listener implements RecognitionListener {
        @Override public void onReadyForSpeech(Bundle params) { }

        @Override public void onBeginningOfSpeech() {
            js("window.pipVoice&&pipVoice.state('hearing')");
        }

        @Override public void onRmsChanged(float rmsdB) {
            long now = SystemClock.uptimeMillis();
            if (now - lastLevelPush < 70) return;
            lastLevelPush = now;
            float level = Math.max(0f, Math.min(1f, (rmsdB + 2f) / 12f));
            js("window.pipVoice&&pipVoice.level(" + String.format(Locale.US, "%.2f", level) + ")");
        }

        @Override public void onBufferReceived(byte[] buffer) { }

        @Override public void onEndOfSpeech() {
            js("window.pipVoice&&pipVoice.state('thinking')");
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
            js("window.pipVoice&&pipVoice.state('error'," + q(message) + ")");
        }

        @Override public void onResults(Bundle results) {
            ArrayList<String> m = results == null ? null
                    : results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (m != null && !m.isEmpty()) {
                js("window.pipVoice&&pipVoice.heard(" + q(m.get(0)) + ")");
            } else {
                js("window.pipVoice&&pipVoice.state('error'," + q("Не расслышал. Скажите ещё раз.") + ")");
            }
        }

        @Override public void onPartialResults(Bundle partialResults) {
            ArrayList<String> m = partialResults == null ? null
                    : partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (m != null && !m.isEmpty()) {
                js("window.pipVoice&&pipVoice.partial(" + q(m.get(0)) + ")");
            }
        }

        @Override public void onEvent(int eventType, Bundle params) { }
    }

    // ---------------------------------------------------------------- голос: текст → речь

    private void speak(String text) {
        if (tts == null || !ttsReady || text == null || text.isEmpty()) return;
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "pip-reply");
    }

    // ---------------------------------------------------------------- роль «цифровой помощник»

    private boolean isAssistant() {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                RoleManager roles = getSystemService(RoleManager.class);
                if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                    return roles.isRoleHeld(RoleManager.ROLE_ASSISTANT);
                }
            }
            String current = Settings.Secure.getString(getContentResolver(), "assistant");
            return current != null && current.startsWith(getPackageName() + "/");
        } catch (RuntimeException e) {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private void requestAssistantRole() {
        if (isAssistant()) {
            Toast.makeText(this, "Пип уже ваш голосовой помощник", Toast.LENGTH_SHORT).show();
            js("window.pipRefreshAssistant&&pipRefreshAssistant()");
            return;
        }
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                RoleManager roles = getSystemService(RoleManager.class);
                if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                    startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT), REQ_ROLE);
                    return;
                }
            } catch (RuntimeException ignored) {
            }
        }
        openAssistantSettings();
    }

    private void openAssistantSettings() {
        String[] actions = {
                Settings.ACTION_VOICE_INPUT_SETTINGS,
                Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS,
                Settings.ACTION_SETTINGS
        };
        for (String action : actions) {
            try {
                startActivity(new Intent(action));
                Toast.makeText(this, "Выберите «Пип — голосовой помощник»", Toast.LENGTH_LONG).show();
                return;
            } catch (RuntimeException ignored) {
            }
        }
        Toast.makeText(this, "Откройте Настройки → Приложения → Приложения по умолчанию → Цифровой помощник",
                Toast.LENGTH_LONG).show();
    }
}
