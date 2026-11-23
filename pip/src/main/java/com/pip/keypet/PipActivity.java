package com.pip.keypet;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import java.util.ArrayList;
import java.util.Locale;

public final class PipActivity extends Activity {
    private static final int REQUEST_SPEECH = 701;
    private static final int REQUEST_ASSISTANT_ROLE = 702;
    private WebView webView;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("ru", "RU"));
        });

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(8, 9, 13));

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(8, 9, 13));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(true);
        webView.getSettings().setSupportZoom(false);
        webView.addJavascriptInterface(new PipBridge(), "PipAndroid");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(
                    WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                if (Intent.ACTION_ASSIST.equals(getIntent().getAction())) {
                    Toast.makeText(PipActivity.this, "Пип готов", Toast.LENGTH_SHORT).show();
                }
            }
        });
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        TextView mic = new TextView(this);
        mic.setText("🎙");
        mic.setTextSize(21);
        mic.setTextColor(Color.WHITE);
        mic.setGravity(Gravity.CENTER);
        mic.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(28, 29, 39)));
        mic.setBackground(getDrawable(android.R.drawable.btn_default));
        FrameLayout.LayoutParams micParams = new FrameLayout.LayoutParams(dp(48), dp(48),
                Gravity.TOP | Gravity.END);
        micParams.setMargins(0, dp(12), dp(12), 0);
        mic.setOnClickListener(v -> startVoiceInput());
        root.addView(mic, micParams);

        setContentView(root);
        webView.loadUrl("https://appassets.androidplatform.net/assets/pip/index.html");
    }

    private final class PipBridge {
        @JavascriptInterface
        public void chooseAssistant() {
            runOnUiThread(PipActivity.this::requestAssistantRole);
        }
    }

    private void requestAssistantRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roles = getSystemService(RoleManager.class);
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                if (roles.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                    Toast.makeText(this, "Пип уже выбран помощником", Toast.LENGTH_SHORT).show();
                } else {
                    startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT),
                            REQUEST_ASSISTANT_ROLE);
                }
                return;
            }
        }
        try {
            startActivity(new Intent("android.settings.VOICE_INPUT_SETTINGS"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "В этой версии Android нет отдельного выбора помощника",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажи команду для Пипа");
        try {
            startActivityForResult(intent, REQUEST_SPEECH);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Не найден сервис распознавания речи", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) handleVoiceCommand(matches.get(0));
        } else if (requestCode == REQUEST_ASSISTANT_ROLE && resultCode == RESULT_OK) {
            Toast.makeText(this, "Пип выбран помощником", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleVoiceCommand(String raw) {
        String command = raw.toLowerCase(new Locale("ru", "RU"));
        String reply;
        if (command.contains("покорми") || command.contains("дай еду")) {
            runInSimulator("if(window.sim&&window.sim.M)window.sim.M._sim_action(0);");
            reply = "Спасибо! Пип поел.";
        } else if (command.contains("поиграй") || command.contains("играть")) {
            runInSimulator("if(window.sim&&window.sim.M)window.sim.M._sim_action(1);");
            reply = "Давай играть!";
        } else if (command.contains("погладь") || command.contains("погладить")) {
            runInSimulator("if(window.sim&&window.sim.M){window.sim.M._sim_touch(233,233,1);setTimeout(()=>window.sim.M._sim_touch(233,233,0),450);}");
            reply = "Мне приятно!";
        } else if (command.contains("открой меню") || command.equals("меню")) {
            runInSimulator("if(window.sim&&window.sim.M)window.sim.M._sim_button(0);");
            reply = "Открываю меню.";
        } else if (command.contains("который час") || command.contains("время")) {
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());
            reply = "Сейчас " + fmt.format(new java.util.Date());
        } else if (command.contains("привет")) {
            reply = "Привет! Я Пип.";
        } else {
            reply = "Команды: покорми, поиграй, погладь, открой меню, который час.";
        }
        if (tts != null) tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "pip-voice-reply");
    }

    private void runInSimulator(String javascript) {
        if (webView != null) webView.evaluateJavascript(javascript, null);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.destroy();
            webView = null;
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
