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
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int GREEN = Color.rgb(0, 255, 65);
    private SharedPreferences prefs;
    private boolean unlocked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(Settings.FILE, Context.MODE_PRIVATE);
        unlocked = prefs.getBoolean(Settings.KEY_UNLOCKED, false);

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
        root.addView(radioGroup(Settings.STYLE_NAMES, Settings.KEY_STYLE, 0, 0,
                Settings.FIRST_EXCLUSIVE_STYLE));

        root.addView(label("Сторона"));
        root.addView(text("для стилей: Фигуры, Галактика, Тоннель", 12, false));
        root.addView(radioGroup(new String[]{"Слева", "Справа", "Обе стороны"},
                Settings.KEY_SIDE, 0, 100, 99));

        root.addView(label("Фигура"));
        root.addView(text("для стиля «Фигуры»", 12, false));
        String[] shapes = new String[ShapeBank.COUNT + 1];
        shapes[0] = "Авто (меняются сами)";
        for (int i = 0; i < ShapeBank.COUNT; i++) shapes[i + 1] = ShapeBank.NAMES[i];
        root.addView(radioGroup(shapes, Settings.KEY_SHAPE, 0, 200, 99));

        root.addView(label("Цвет"));
        root.addView(radioGroup(Settings.COLOR_NAMES, Settings.KEY_COLOR, 0, 300,
                Settings.FIRST_EXCLUSIVE_COLOR));

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

        root.addView(label("Эксклюзив"));
        if (unlocked) {
            root.addView(text("✓ Эксклюзивные обои и цвета открыты", 14, false));
        } else {
            final EditText code = new EditText(this);
            code.setInputType(InputType.TYPE_CLASS_NUMBER);
            code.setHint("Введите код");
            code.setHintTextColor(Color.rgb(0, 120, 30));
            code.setTextColor(GREEN);
            code.setTypeface(Typeface.MONOSPACE);
            code.setBackgroundTintList(ColorStateList.valueOf(GREEN));
            root.addView(code);

            Button open = greenButton("ОТКРЫТЬ ПО КОДУ", 48);
            open.setOnClickListener(v -> {
                String entered = code.getText().toString().trim();
                if (entered.equals(Settings.UNLOCK_CODE)) {
                    prefs.edit().putBoolean(Settings.KEY_UNLOCKED, true).apply();
                    Toast.makeText(this, "Эксклюзив открыт!", Toast.LENGTH_SHORT).show();
                    recreate();
                } else {
                    Toast.makeText(this, "Неверный код", Toast.LENGTH_SHORT).show();
                }
            });
            root.addView(open);
        }

        Button set = greenButton("УСТАНОВИТЬ ОБОИ", 56);
        ((LinearLayout.LayoutParams) set.getLayoutParams()).topMargin = dp(32);
        set.setOnClickListener(v -> applyWallpaper());
        root.addView(set);

        setContentView(scroll);
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
                                  final int idBase, final int firstLocked) {
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        int saved = prefs.getInt(key, def);
        if (saved < 0 || saved >= items.length) saved = def;
        if (!unlocked && saved >= firstLocked) saved = def;
        for (int i = 0; i < items.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(1000 + idBase + i);
            rb.setText((i >= firstLocked && !unlocked ? "🔒 " : "") + items[i]);
            rb.setTextColor(GREEN);
            rb.setTypeface(Typeface.MONOSPACE);
            rb.setButtonTintList(ColorStateList.valueOf(GREEN));
            group.addView(rb);
        }
        group.check(1000 + idBase + saved);

        final int[] current = {saved};
        final boolean[] reverting = {false};
        group.setOnCheckedChangeListener((g, checkedId) -> {
            if (reverting[0]) return;
            int idx = checkedId - 1000 - idBase;
            if (idx >= firstLocked && !unlocked) {
                Toast.makeText(this, "Нужен код — раздел «Эксклюзив» ниже",
                        Toast.LENGTH_SHORT).show();
                reverting[0] = true;
                g.check(1000 + idBase + current[0]);
                reverting[0] = false;
                return;
            }
            current[0] = idx;
            prefs.edit().putInt(key, idx).apply();
        });
        return group;
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
