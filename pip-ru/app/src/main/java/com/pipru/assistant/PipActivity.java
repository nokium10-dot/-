package com.pipru.assistant;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class PipActivity extends Activity {
    public static final String EXTRA_AUTO_LISTEN = "com.pipru.assistant.AUTO_LISTEN";
    public static final String EXTRA_OPEN_SETTINGS = "com.pipru.assistant.OPEN_SETTINGS";
    private static final int REQ_AUDIO = 71;
    private static final int REQ_SPEECH = 72;

    private TextView status;
    private PipFace face;
    private TextToSpeech tts;
    private boolean ttsReady;
    private boolean autoListenPending;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(13, 17, 29));
        getWindow().setNavigationBarColor(Color.rgb(13, 17, 29));
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildInterface();
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                ttsReady = true;
                tts.setLanguage(new Locale("ru", "RU"));
            }
        });
        handleIntent(getIntent());
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(22), dp(18), dp(22), dp(22));
        root.setBackgroundColor(Color.rgb(13, 17, 29));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = new TextView(this);
        name.setText("ПИП");
        name.setTextColor(Color.WHITE);
        name.setTextSize(22);
        name.setLetterSpacing(.16f);
        name.setTypeface(null, 1);
        top.addView(name, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button settings = makeButton("⚙");
        settings.setOnClickListener(v -> showSettings());
        top.addView(settings, new LinearLayout.LayoutParams(dp(52), dp(48)));
        root.addView(top, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView subtitle = new TextView(this);
        subtitle.setText("ТВОЙ КАРМАННЫЙ ПОМОЩНИК");
        subtitle.setTextColor(Color.rgb(142, 157, 183));
        subtitle.setTextSize(11);
        subtitle.setLetterSpacing(.12f);
        root.addView(subtitle);

        face = new PipFace(this);
        LinearLayout.LayoutParams faceParams = new LinearLayout.LayoutParams(-1, 0, 1);
        faceParams.topMargin = dp(10);
        root.addView(face, faceParams);

        status = new TextView(this);
        status.setText("Привет! Я Пип. Нажми на микрофон, чтобы поговорить.");
        status.setTextColor(Color.WHITE);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(12), dp(14), dp(12), dp(14));
        status.setBackground(roundRect(Color.rgb(28, 36, 54), dp(18)));
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(88)));

        Button mic = makeButton("🎙   Поговорить с Пипом");
        mic.setTextSize(16);
        mic.setOnClickListener(v -> beginListening());
        LinearLayout.LayoutParams micParams = new LinearLayout.LayoutParams(-1, dp(58));
        micParams.topMargin = dp(16);
        root.addView(mic, micParams);

        Button say = makeButton("Привет, Пип!");
        say.setOnClickListener(v -> respond("Привет! Рад тебя слышать. Чем займёмся?"));
        LinearLayout.LayoutParams sayParams = new LinearLayout.LayoutParams(-1, dp(48));
        sayParams.topMargin = dp(9);
        root.addView(say, sayParams);

        setContentView(root);
    }

    private android.graphics.drawable.GradientDrawable roundRect(int color, int radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color); d.setCornerRadius(radius); return d;
    }

    private Button makeButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackground(roundRect(Color.rgb(48, 91, 160), dp(16)));
        b.setPadding(dp(10), 0, dp(10), 0);
        return b;
    }

    private void beginListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showMessage("На устройстве нет доступного сервиса распознавания речи.");
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
            return;
        }
        try {
            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажи что-нибудь Пипу");
            startActivityForResult(i, REQ_SPEECH);
            status.setText("Слушаю тебя…");
            face.setMood(1);
        } catch (Exception e) {
            showMessage("Не удалось запустить распознавание речи.");
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_AUDIO && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) beginListening();
        else if (requestCode == REQ_AUDIO) showMessage("Для голосового общения нужен доступ к микрофону.");
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_SPEECH) return;
        if (resultCode == RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) processPhrase(matches.get(0));
            else respond("Я не расслышал. Попробуй ещё раз.");
        } else {
            status.setText("Я здесь, когда захочешь поговорить.");
            face.setMood(0);
        }
    }

    private void processPhrase(String phrase) {
        String p = phrase.toLowerCase(Locale.ROOT);
        if (p.contains("привет") || p.contains("здравств")) respond("Привет! Как настроение?");
        else if (p.contains("как дела")) respond("У меня всё хорошо! Готов помочь.");
        else if (p.contains("время")) {
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("HH:mm", new Locale("ru"));
            respond("Сейчас " + f.format(new java.util.Date()) + ".");
        } else if (p.contains("спасибо")) respond("Всегда пожалуйста!");
        else if (p.contains("пока")) respond("До встречи! Я буду здесь.");
        else if (p.contains("кто ты") || p.contains("как тебя зовут")) respond("Я Пип, твой карманный помощник.");
        else respond("Ты сказал: " + phrase + ". Я пока учусь понимать такие просьбы.");
    }

    private void respond(String message) {
        status.setText(message);
        face.setMood(2);
        if (ttsReady) tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "pip-reply");
    }

    private void showMessage(String message) {
        status.setText(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showSettings() {
        String[] items = {"Выбрать Пипа цифровым помощником", "Проверить голосовой ввод", "О приложении"};
        new AlertDialog.Builder(this).setTitle("Настройки Пипа").setItems(items, (d, which) -> {
            if (which == 0) {
                try { startActivity(new Intent("android.settings.VOICE_INPUT_SETTINGS")); }
                catch (Exception e) { showMessage("Открой настройки телефона → Приложения → Помощник."); }
            } else if (which == 1) beginListening();
            else new AlertDialog.Builder(this).setTitle("Пип").setMessage("Пип — русскоязычный голосовой помощник. Голосовой ввод выполняется системным сервисом распознавания речи, ответы озвучиваются системным синтезатором речи.").setPositiveButton("Понятно", null).show();
        }).setNegativeButton("Закрыть", null).show();
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        if (intent.getBooleanExtra(EXTRA_OPEN_SETTINGS, false)) showSettings();
        if (intent.getBooleanExtra(EXTRA_AUTO_LISTEN, false) || Intent.ACTION_ASSIST.equals(intent.getAction())) {
            autoListenPending = true;
            face.postDelayed(() -> { if (autoListenPending) beginListening(); autoListenPending = false; }, 450);
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override protected void onDestroy() {
        autoListenPending = false;
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }

    private static class PipFace extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int mood = 0;
        private long started = System.currentTimeMillis();
        PipFace(Activity a) { super(a); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
        void setMood(int m) { mood = m; invalidate(); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w=getWidth(), h=getHeight(), cx=w/2f, cy=h/2f;
            float s=Math.min(w,h)*.37f;
            float bob=(float)Math.sin((System.currentTimeMillis()-started)/650.0)*s*.025f;
            c.save(); c.translate(0,bob);
            p.setColor(Color.rgb(63,139,224)); p.setShadowLayer(s*.10f,0,s*.06f,0x66000000);
            c.drawCircle(cx,cy,s,p); p.clearShadowLayer();
            p.setColor(Color.rgb(255,255,255));
            c.drawOval(new RectF(cx-s*.57f,cy-s*.34f,cx-s*.08f,cy+s*.20f),p);
            c.drawOval(new RectF(cx+s*.08f,cy-s*.34f,cx+s*.57f,cy+s*.20f),p);
            p.setColor(Color.rgb(24,35,55));
            float look=mood==1?s*.09f:0;
            c.drawCircle(cx-s*.31f+look,cy-s*.06f,s*.105f,p);
            c.drawCircle(cx+s*.31f+look,cy-s*.06f,s*.105f,p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Math.max(4,s*.055f)); p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(Color.rgb(255,255,255));
            if(mood==1) c.drawArc(new RectF(cx-s*.20f,cy+s*.16f,cx+s*.20f,cy+s*.48f),200,140,false,p);
            else c.drawArc(new RectF(cx-s*.22f,cy+s*.13f,cx+s*.22f,cy+s*(mood==2?.58f:.43f)),15,150,false,p);
            p.setStyle(Paint.Style.FILL);
            c.restore();
            if(mood!=1) postInvalidateDelayed(60);
        }
    }
}
