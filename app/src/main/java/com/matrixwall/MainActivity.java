package com.matrixwall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int GREEN = Color.rgb(0,255,65);
    private SharedPreferences prefs;
    private LinearLayout root, body;
    private boolean settingsPage = false;
    private String lang = "ru";
    private int versionTapCount = 0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(Settings.FILE, Context.MODE_PRIVATE);
        lang = prefs.getString(Settings.KEY_LANGUAGE, "");
        showScreen();
        if (lang.isEmpty()) chooseLanguage(true);
        showSplash();
    }

    private boolean en() { return "en".equals(lang); }
    private String tr(String ru, String english) { return en() ? english : ru; }

    private void showScreen() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(16),dp(12),dp(12),dp(8));
        TextView brand = text("MATRIXWALL PRO",20,true);
        top.addView(brand,new LinearLayout.LayoutParams(0,dp(48),1));
        Button gear = button(settingsPage ? tr("ГЛАВНАЯ","HOME") : tr("НАСТРОЙКИ","SETTINGS"));
        gear.setOnClickListener(v->{ settingsPage=!settingsPage; showScreen(); });
        top.addView(gear,new LinearLayout.LayoutParams(dp(116),dp(46)));
        root.addView(top);
        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18),dp(10),dp(18),dp(24));
        scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(settingsPage) buildSettings(); else buildHome();
        setContentView(root);
        animateMenu();
    }

    private void buildHome() {
        body.addView(text(tr("Живые обои MatrixWall Pro","MatrixWall Pro live wallpapers"),22,true));
        body.addView(text(tr("16 стилей • 18 цветов • 11 фигур + секрет","16 styles • 18 colors • 11 shapes + secret"),14,false));
        body.addView(label(tr("Выбранный стиль","Selected wallpaper")));
        int style=prefs.getInt(Settings.KEY_STYLE,0);
        body.addView(text(Settings.STYLE_NAMES[Math.max(0,Math.min(style,Settings.STYLE_NAMES.length-1))],17,true));
        Button apply=button(tr("УСТАНОВИТЬ ОБОИ","APPLY WALLPAPER"));
        apply.setOnClickListener(v->applyWallpaper());
        body.addView(apply);
        body.addView(label(tr("СТУДИЯ ОБОЕВ — ЦВЕТ И ФИГУРА","WALLPAPER STUDIO — COLOR & SHAPE")));
        body.addView(text(tr("Выбирай цвет и форму в одном месте. Изменения сохраняются сразу.","Choose color and shape together. Changes are saved instantly."),13,false));
        body.addView(label(tr("СТУДИЯ ОБОЕВ — ЦВЕТ И ФИГУРА","WALLPAPER STUDIO — COLOR & SHAPE")));
        body.addView(label(tr("Цветовая палитра","Color palette")));
        body.addView(radio(Settings.COLOR_NAMES,Settings.KEY_COLOR,0));
        body.addView(label(tr("Фигура","Shape")));
        body.addView(shapeSelector());
        body.addView(label("MATRIXWALL PRO"));
        body.addView(text("TikTok: @pirtechacc\nTelegram: t.me/pirtechacc",14,false));
        Button tiktok=button("TikTok"); tiktok.setOnClickListener(v->openLink("https://www.tiktok.com/@pirtechacc")); body.addView(tiktok);
        Button telegram=button("Telegram"); telegram.setOnClickListener(v->openLink("https://t.me/pirtechacc")); body.addView(telegram);
        body.addView(label(tr("О приложении","About")));
        TextView version=text("MatrixWall Pro 2.0",14,true); version.setGravity(Gravity.CENTER); version.setOnClickListener(v->secretTap()); body.addView(version);
        int remaining=Math.max(0,7-prefs.getInt(Settings.KEY_SECRET_TAPS,0));
        if(!prefs.getBoolean(Settings.KEY_SECRET_UNLOCKED,false)) body.addView(text(tr("Секретная фигура: осталось нажатий — ","Secret shape: taps remaining — ")+remaining,12,false));
        else body.addView(text(tr("Секретная фигура разблокирована","Secret shape unlocked"),12,true));
    }

    private void buildSettings() {
        body.addView(text(tr("Настройки приложения","Application settings"),22,true));
        body.addView(label(tr("Язык / Language","Language / Язык")));
        Button language=button(en()?"English":"Русский");
        language.setOnClickListener(v->chooseLanguage(false)); body.addView(language);
        body.addView(label(tr("Живые обои","Live wallpapers")));
        body.addView(radio(Settings.STYLE_NAMES,Settings.KEY_STYLE,0));
        body.addView(label(tr("Цветовая палитра","Color palette")));
        body.addView(radio(Settings.COLOR_NAMES,Settings.KEY_COLOR,0));
        body.addView(label(tr("Скорость вращения","Rotation speed")));
        body.addView(speedBar(Settings.KEY_ROTATION_SPEED,10,300,100));
        body.addView(label(tr("Скорость фонового текста","Background text speed")));
        body.addView(speedBar(Settings.KEY_TEXT_SPEED,10,300,100));
        body.addView(label(tr("Дополнительные эффекты","Extra effects")));
        CheckBox glow=check(tr("Неоновое свечение фигур","Neon shape glow"),Settings.KEY_GLOW,true); body.addView(glow);
        CheckBox rain=check(tr("Фоновый дождь символов","Background glyph rain"),Settings.KEY_RAIN,true);
        body.addView(rain);
        CheckBox sound=check(tr("Звук заставки","Splash sound"),Settings.KEY_SPLASH_SOUND,true);
        body.addView(sound);
        body.addView(label(tr("Иконка приложения","App icon")));
        body.addView(text(tr("Выберите один из 10 вариантов. Иконка изменится в списке приложений и на рабочем столе.","Choose one of 10 designs. The launcher icon changes on your home screen and in the app list."),13,false));
        String[] icons={"Matrix PRO 1","Matrix PRO 2","Matrix PRO 3","Matrix PRO 4","Matrix PRO 5","Matrix PRO 6","Matrix PRO 7","Matrix PRO 8","Matrix PRO 9","Matrix PRO 10"};
        body.addView(radio(icons,Settings.KEY_ICON,0));
        body.addView(label(tr("Фигура","Shape")));
        body.addView(shapeSelector());
        body.addView(label(tr("Расположение","Layout")));
        body.addView(radio(new String[]{tr("Слева","Left"),tr("Справа","Right"),tr("По центру / обе стороны","Center / both sides")},Settings.KEY_SIDE,0));
        body.addView(text(tr("Изменения скорости и цвета применяются к живым обоям автоматически. Для смены иконки может потребоваться обновить рабочий стол.","Speed and color changes apply to live wallpaper automatically. Your launcher may take a moment to refresh the icon."),12,false));
    }


    private RadioGroup shapeSelector() {
        boolean unlocked=prefs.getBoolean(Settings.KEY_SECRET_UNLOCKED,false);
        int count=unlocked?ShapeBank.COUNT:ShapeBank.STANDARD_COUNT;
        String[] shapes=new String[count+1]; shapes[0]=tr("Авто","Auto");
        for(int i=0;i<count;i++) shapes[i+1]=ShapeBank.NAMES[i];
        return radio(shapes,Settings.KEY_SHAPE,0);
    }

    private void secretTap() {
        if(prefs.getBoolean(Settings.KEY_SECRET_UNLOCKED,false)) {
            Toast.makeText(this,tr("Секрет уже открыт","Secret already unlocked"),Toast.LENGTH_SHORT).show(); return;
        }
        int taps=prefs.getInt(Settings.KEY_SECRET_TAPS,0)+1;
        prefs.edit().putInt(Settings.KEY_SECRET_TAPS,taps).apply();
        int left=Math.max(0,7-taps);
        if(left==0) {
            prefs.edit().putBoolean(Settings.KEY_SECRET_UNLOCKED,true).putInt(Settings.KEY_SHAPE,ShapeBank.COUNT).apply();
            new AlertDialog.Builder(this).setTitle(tr("СЕКРЕТНАЯ ФИГУРА ОТКРЫТА!","SECRET SHAPE UNLOCKED!"))
                .setMessage(tr("Открыта фигура «Феникс». Она уже выбрана в студии обоев.","The Phoenix shape is unlocked and selected in Wallpaper Studio."))
                .setPositiveButton("OK",(d,w)->showScreen()).show();
        } else { Toast.makeText(this,tr("Ещё нажатий до секрета: ","Taps left: ")+left,Toast.LENGTH_SHORT).show(); showScreen(); }
    }

    private void animateMenu() {
        for(int i=0;i<body.getChildCount();i++) {
            View child=body.getChildAt(i); child.setAlpha(0f); child.setTranslationY(dp(10));
            child.animate().alpha(1f).translationY(0f).setStartDelay(Math.min(i*12,180)).setDuration(220).start();
        }
    }

    private RadioGroup radio(String[] values,String key,int def) {
        RadioGroup group=new RadioGroup(this); group.setOrientation(RadioGroup.VERTICAL);
        int selected=prefs.getInt(key,def); if(selected<0||selected>=values.length)selected=def;
        for(int i=0;i<values.length;i++){
            final int index=i; RadioButton rb=new RadioButton(this); rb.setId(3000+i);
            rb.setText(values[i]); rb.setTextColor(GREEN); rb.setTypeface(Typeface.MONOSPACE);
            rb.setButtonTintList(ColorStateList.valueOf(GREEN)); group.addView(rb);
            if(i==selected)rb.setChecked(true);
        }
        group.setOnCheckedChangeListener((g,id)->{
            int index=id-3000; if(index<0||index>=values.length)return;
            prefs.edit().putInt(key,index).apply();
            if(Settings.KEY_ICON.equals(key)) setLauncherIcon(index);
        });
        return group;
    }

    private LinearLayout speedBar(String key,int min,int max,int def) {
        LinearLayout wrap=new LinearLayout(this); wrap.setOrientation(LinearLayout.VERTICAL);
        TextView value=text("",13,false); SeekBar bar=new SeekBar(this);
        bar.setMax(max-min); int saved=prefs.getInt(key,def); saved=Math.max(min,Math.min(max,saved));
        bar.setProgress(saved-min); bar.setProgressTintList(ColorStateList.valueOf(GREEN));
        bar.setThumbTintList(ColorStateList.valueOf(GREEN));
        value.setText(String.format(java.util.Locale.US,"%.1fx",saved/100f));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean from){int v=p+min;value.setText(String.format(java.util.Locale.US,"%.1fx",v/100f));if(from)prefs.edit().putInt(key,v).apply();}
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        wrap.addView(value); wrap.addView(bar); return wrap;
    }

    private CheckBox check(String caption,String key,boolean def) {
        CheckBox c=new CheckBox(this); c.setText(caption); c.setTextColor(GREEN);
        c.setButtonTintList(ColorStateList.valueOf(GREEN)); c.setTypeface(Typeface.MONOSPACE);
        c.setChecked(prefs.getBoolean(key,def)); c.setOnCheckedChangeListener((b,v)->prefs.edit().putBoolean(key,v).apply()); return c;
    }

    private void chooseLanguage(boolean first) {
        String[] labels={"Русский","English"};
        new AlertDialog.Builder(this).setTitle(first?"Выберите язык / Choose language":tr("Язык приложения","App language"))
            .setItems(labels,(d,which)->{lang=which==1?"en":"ru";prefs.edit().putString(Settings.KEY_LANGUAGE,lang).apply();showScreen();})
            .setCancelable(!first).show();
    }

    private void setLauncherIcon(int selected) {
        PackageManager pm=getPackageManager();
        for(int i=0;i<10;i++){
            ComponentName cn=new ComponentName(this,"com.matrixwall.Icon"+(i+1));
            pm.setComponentEnabledSetting(cn, i==selected?PackageManager.COMPONENT_ENABLED_STATE_ENABLED:PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);
        }
        Toast.makeText(this,tr("Иконка обновлена","App icon updated"),Toast.LENGTH_SHORT).show();
    }

    private void showSplash() {
        FrameLayout frame = new FrameLayout(this);
        // showScreen() has already attached root with setContentView(); detach it
        // before placing it inside the splash container to avoid an already-parented View crash.
        if (root.getParent() instanceof ViewGroup) {
            ((ViewGroup) root.getParent()).removeView(root);
        }
        frame.addView(root, new FrameLayout.LayoutParams(-1, -1));
        TextView logo = text("MATRIXWALL\\nPRO", 30, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackgroundColor(Color.BLACK);
        frame.addView(logo, new FrameLayout.LayoutParams(-1, -1));
        setContentView(frame);
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(700);
        logo.startAnimation(fadeIn);
        if (prefs.getBoolean(Settings.KEY_SPLASH_SOUND, true)) {
            try {
                ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 45);
                tone.startTone(ToneGenerator.TONE_PROP_ACK, 220);
                new Handler(Looper.getMainLooper()).postDelayed(tone::release, 450);
            } catch (Exception ignored) {}
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
            fadeOut.setDuration(400);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation a) {}
                public void onAnimationRepeat(Animation a) {}
                public void onAnimationEnd(Animation a) { frame.removeView(logo); }
            });
            logo.startAnimation(fadeOut);
        }, 1000);
    }
    private void applyWallpaper() {
        try { Intent i=new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(this,MatrixWallpaperService.class));startActivity(i);
        } catch(Exception e){try{startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));}catch(Exception ignored){}}
    }
    private void openLink(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));}catch(Exception e){Toast.makeText(this,"Unable to open link",Toast.LENGTH_SHORT).show();}}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextColor(Color.BLACK);b.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);b.setBackgroundColor(GREEN);return b;}
    private TextView text(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(GREEN);t.setTypeface(Typeface.MONOSPACE,bold?Typeface.BOLD:Typeface.NORMAL);t.setPadding(0,dp(5),0,dp(5));return t;}
    private TextView label(String s){TextView t=text("> "+s,17,true);t.setPadding(0,dp(22),0,dp(5));return t;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
}
