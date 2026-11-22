package com.matrixwall;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int GREEN = Color.rgb(0, 255, 65);
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(Settings.FILE, Context.MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(40), dp(24), dp(24));
        scroll.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(text("C:\\> MATRIX_WALL", 26, true));
        root.addView(text("Живые обои из зелёных символов", 14, false));

        root.addView(label("Стиль"));
        root.addView(radioGroup(Settings.STYLE_NAMES, Settings.KEY_STYLE, 0, 0));

        root.addView(label("Сторона"));
        root.addView(text("для стилей: Фигуры, Галактика, Тоннель", 12, false));
        root.addView(radioGroup(new String[]{"Слева", "Справа", "Обе стороны"},
                Settings.KEY_SIDE, 0, 100));

        root.addView(label("Фигура"));
        root.addView(text("для стиля «Фигуры»", 12, false));
        String[] shapes = new String[ShapeBank.COUNT + 1];
        shapes[0] = "Авто (меняются сами)";
        for (int i = 0; i < ShapeBank.COUNT; i++) shapes[i + 1] = ShapeBank.NAMES[i];
        root.addView(radioGroup(shapes, Settings.KEY_SHAPE, 0, 200));

        root.addView(label("Цвет"));
        root.addView(radioGroup(Settings.COLOR_NAMES, Settings.KEY_COLOR, 0, 300));

        root.addView(label("Фон"));
        CheckBox rain = new CheckBox(this);
        rain.setText("Падающие символы на фоне");
        rain.setTextColor(GREEN);
        rain.setTypeface(Typeface.MONOSPACE);
        rain.setButtonTintList(ColorStateList.valueOf(GREEN));
        rain.setChecked(prefs.getBoolean(Settings.KEY_RAIN, true));
        rain.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(Settings.KEY_RAIN, isChecked).apply());
        root.addView(rain);

        CheckBox splashSound = new CheckBox(this);
        splashSound.setText("Звук заставки");
        splashSound.setTextColor(GREEN);
        splashSound.setTypeface(Typeface.MONOSPACE);
        splashSound.setButtonTintList(ColorStateList.valueOf(GREEN));
        splashSound.setChecked(prefs.getBoolean(Settings.KEY_SPLASH_SOUND, true));
        splashSound.setOnCheckedChangeListener((buttonView, checked) ->
                prefs.edit().putBoolean(Settings.KEY_SPLASH_SOUND, checked).apply());
        root.addView(splashSound);

        root.addView(label("MATRIXWALL PRO"));
        root.addView(text("Все стили, фигуры и цветовые темы доступны без кода.", 13, false));

        Button set = greenButton("УСТАНОВИТЬ ОБОИ", 56);
        ((LinearLayout.LayoutParams) set.getLayoutParams()).topMargin = dp(32);
        set.setOnClickListener(v -> applyWallpaper());
        root.addView(set);

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.HORIZONTAL);
        shell.setBackgroundColor(Color.BLACK);
        shell.addView(scroll, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setGravity(Gravity.CENTER_HORIZONTAL);
        rail.setPadding(dp(5), dp(18), dp(5), dp(12));
        rail.setBackgroundColor(Color.rgb(0, 28, 8));
        LinearLayout.LayoutParams railLp = new LinearLayout.LayoutParams(dp(92),
                ViewGroup.LayoutParams.MATCH_PARENT);
        shell.addView(rail, railLp);
        TextView railTitle = text("SOCIAL", 12, true);
        railTitle.setGravity(Gravity.CENTER);
        rail.addView(railTitle);
        Button tiktok = greenButton("TIK\nTOK", 64);
        tiktok.setTextSize(11);
        tiktok.setOnClickListener(v -> openLink("https://www.tiktok.com/@pirtechacc"));
        rail.addView(tiktok);
        Button telegram = greenButton("TELE\nGRAM", 64);
        telegram.setTextSize(11);
        telegram.setOnClickListener(v -> openLink("https://t.me/pirtechacc"));
        rail.addView(telegram);

        FrameLayout screen = new FrameLayout(this);
        screen.addView(shell, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(screen);
        showSplash(screen);
    }

    private void showSplash(FrameLayout screen) {
        FrameLayout splash = new FrameLayout(this);
        splash.setBackgroundColor(Color.BLACK);
        TextView logo = text("MATRIXWALL\nPRO", 30, true);
        logo.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        splash.addView(logo, lp);
        screen.addView(splash, lp);
        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(850);
        logo.startAnimation(fade);
        if (prefs.getBoolean(Settings.KEY_SPLASH_SOUND, true)) {
            try {
                ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 45);
                tone.startTone(ToneGenerator.TONE_PROP_ACK, 220);
                new Handler(Looper.getMainLooper()).postDelayed(tone::release, 500);
            } catch (Exception ignored) {
            }
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AlphaAnimation out = new AlphaAnimation(1f, 0f);
            out.setDuration(350);
            out.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {}
                @Override public void onAnimationRepeat(Animation animation) {}
                @Override public void onAnimationEnd(Animation animation) {
                    screen.removeView(splash);
                }
            });
            splash.startAnimation(out);
        }, 1250);
    }

    private Button greenButton(String label, int heightDp) {
        Button b = new Button(this);
        b.setText(label);
        b.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        b.setTextColor(Color.BLACK);
        b.setBackgroundColor(GREEN);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(heightDp));
        lp.topMargin = dp(12);
        b.setLayoutParams(lp);
        return b;
    }

    private RadioGroup radioGroup(String[] items, final String key, int def,
                                  final int idBase) {
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        int saved = prefs.getInt(key, def);
        if (saved < 0 || saved >= items.length) saved = def;
        for (int i = 0; i < items.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(1000 + idBase + i);
            rb.setText(items[i]);
            rb.setTextColor(GREEN);
            rb.setTypeface(Typeface.MONOSPACE);
            rb.setButtonTintList(ColorStateList.valueOf(GREEN));
            group.addView(rb);
        }
        group.check(1000 + idBase + saved);

        final int[] current = {saved};
        group.setOnCheckedChangeListener((g, checkedId) -> {
            int idx = checkedId - 1000 - idBase;
            current[0] = idx;
            prefs.edit().putInt(key, idx).apply();
        });
        return group;
    }

    private void openLink(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyWallpaper() {
        try {
            Intent i = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new ComponentName(this, MatrixWallpaperService.class));
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));
            } catch (Exception ignored) {
            }
        }
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(sp);
        tv.setTextColor(GREEN);
        tv.setTypeface(Typeface.MONOSPACE, bold ? Typeface.BOLD : Typeface.NORMAL);
        tv.setGravity(Gravity.START);
        return tv;
    }

    private TextView label(String s) {
        TextView tv = text("> " + s, 18, true);
        tv.setPadding(0, dp(28), 0, dp(6));
        return tv;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
