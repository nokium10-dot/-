package com.pip.keypet;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
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
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("ru", "RU"));
        });

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(9, 9, 10));

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(14), dp(6), dp(10), dp(6));
        bar.setBackgroundColor(Color.rgb(19, 19, 21));

        TextView title = new TextView(this);
        title.setText("ПИП  /  ВИРТУАЛЬНЫЙ ПИТОМЕЦ");
        title.setTextColor(Color.WHITE);
        title.setTextSize(14);
        title.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, dp(44), 1));

        Button voice = new Button(this);
        voice.setText("🎙");
        voice.setTextSize(18);
        voice.setOnClickListener(v -> startVoiceInput());
        bar.addView(voice, new LinearLayout.LayoutParams(dp(54), dp(44)));

        Button assistant = new Button(this);
        assistant.setText("Помощник");
        assistant.setTextSize(11);
        assistant.setOnClickListener(v -> requestAssistantRole());
        bar.addView(assistant, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(44)));

        root.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        status = new TextView(this);
        status.setText("Пип готов");
        status.setTextColor(Color.rgb(180, 180, 180));
        status.setTextSize(11);
        status.setPadding(dp(14), dp(3), dp(14), dp(3));
        status.setBackgroundColor(Color.rgb(19, 19, 21));
        root.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(24)));

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(19, 20, 23));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(true);
        webView.getSettings().setSupportZoom(false);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(
                    WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                status.setText("Пип загружен · касайся экрана, чтобы играть");
            }
        });
        root.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);

        webView.loadUrl("https://appassets.androidplatform.net/assets/pip/index.html");

        if (Intent.ACTION_ASSIST.equals(getIntent().getAction())) {
            status.setText("Пип вызван как помощник · нажми 🎙, чтобы сказать команду");
        }
    }

    private void requestAssistantRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roles = getSystemService(RoleManager.class);
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_ASSISTANT)) {
                if (roles.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                    Toast.makeText(this, "Пип уже выбран помощником", Toast.LENGTH_SHORT).show();
                } else {
                    startActivityForResult(
                            roles.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT),
                            REQUEST_ASSISTANT_ROLE);
                }
                return;
            }
        }
        try {
            startActivity(new Intent("android.settings.VOICE_INPUT_SETTINGS"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "В этой версии Android нет отдельного выбора помощника", Toast.LENGTH_LONG).show();
        }
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажи команду для Пипа");
        try {
            startActivityForResult(intent, REQUEST_SPEECH);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "На устройстве не найден сервис распознавания речи", Toast.LENGTH_LONG).show();
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
            reply = "Спа-сибо! Пип поел.";
        } else if (command.contains("поиграй") || command.contains("играть")) {
            runInSimulator("if(window.sim&&window.sim.M)window.sim.M._sim_action(1);");
            reply = "Давай играть!";
        } else if (command.contains("погладь") || command.contains("погладить")) {
            runInSimulator("if(window.sim&&window.sim.M){window.sim.M._sim_touch(233,233,1);setTimeout(()=>window.sim.M._sim_touch(233,233,0),450);}");
            reply = "Мне приятно!";
        } else if (command.contains("открой меню") || command.contains("меню")) {
            runInSimulator("if(window.sim&&window.sim.M)window.sim.M._sim_button(0);");
            reply = "Открываю меню.";
        } else if (command.contains("который час") || command.contains("время")) {
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());
            reply = "Сейчас " + fmt.format(new java.util.Date());
        } else if (command.contains("привет")) {
            reply = "Привет! Я Пип.";
        } else {
            reply = "Я пока понимаю команды: покорми, поиграй, погладь, открой меню и который час.";
        }

        status.setText("Ты сказал: " + raw);
        speak(reply);
    }

    private void runInSimulator(String javascript) {
        if (webView != null) webView.evaluateJavascript(javascript, null);
    }

    private void speak(String text) {
        if (tts != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "pip-voice-reply");
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
