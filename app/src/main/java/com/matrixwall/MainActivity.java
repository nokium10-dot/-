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
import android.widget.EditText;
import android.text.InputType;
import android.widget.ImageView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int GREEN = Color.rgb(0,255,65);
    private SharedPreferences prefs;
    private LinearLayout root, body;
    private boolean settingsPage = false;
    private String lang = "ru";
    private int versionTapCount = 0;
    private boolean chatPage = false;
    private FirebaseAuth chatAuth;
    private DatabaseReference chatMessagesRef;
    private ChildEventListener chatListener;
    private ValueEventListener chatConnectionListener;
    private boolean chatHasMessages = false;
    private LinearLayout chatMessageList;
    private ScrollView chatScroll;
    private EditText chatInput;
    private boolean chatLoading = false;

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
        Button gear = button(chatPage || settingsPage ? tr("ГЛАВНАЯ","HOME") : tr("НАСТРОЙКИ","SETTINGS"));
        gear.setOnClickListener(v->{ if(chatPage){chatPage=false;settingsPage=false;}else settingsPage=!settingsPage; showScreen(); });
        top.addView(gear,new LinearLayout.LayoutParams(dp(116),dp(46)));
        root.addView(top);
        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18),dp(10),dp(18),dp(24));
        scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(settingsPage) buildSettings(); else if(chatPage) buildChat(); else buildHome();
        setContentView(root);
        animateMenu();
    }

    private void buildHome() {
        body.addView(text(tr("Живые обои MatrixWall Pro","MatrixWall Pro live wallpapers"),22,true));
        body.addView(text(tr("16 анимированных стилей • 18 палитр • 11 фигур","16 animated styles • 18 palettes • 11 shapes"),14,false));

        body.addView(label(tr("БЫСТРАЯ НАСТРОЙКА","QUICK CUSTOMIZATION")));
        body.addView(choiceButton(tr("Стиль обоев","Wallpaper style"), Settings.STYLE_NAMES, Settings.KEY_STYLE, 0));
        body.addView(choiceButton(tr("Цветовая палитра","Color palette"), Settings.COLOR_NAMES, Settings.KEY_COLOR, 0));
        body.addView(choiceButton(tr("Фигура","Shape"), shapeNames(), Settings.KEY_SHAPE, 0));
        body.addView(choiceButton(tr("Расположение","Layout"), new String[]{tr("Слева","Left"),tr("Справа","Right"),tr("По центру / обе стороны","Center / both sides")}, Settings.KEY_SIDE, 0));

        body.addView(label(tr("ГОТОВЫЕ ПРЕСЕТЫ","QUICK PRESETS")));
        body.addView(text(tr("Одно нажатие — и весь стиль обоев меняется сразу.","One tap applies a complete wallpaper look."),13,false));
        addPresetButton(tr("MATRIX CLASSIC","MATRIX CLASSIC"),0);
        addPresetButton(tr("CYBERPUNK","CYBERPUNK"),1);
        addPresetButton(tr("AURORA","AURORA"),2);
        addPresetButton(tr("NEON PURPLE","NEON PURPLE"),3);
        Button random=button(tr("СЛУЧАЙНЫЙ ДИЗАЙН","RANDOM DESIGN"));
        random.setOnClickListener(v->randomizeDesign()); body.addView(random);

        body.addView(label(tr("МОИ ПРЕСЕТЫ","MY PRESETS")));
        body.addView(text(tr("Сохраняй любимые комбинации и быстро возвращайся к ним.","Save favorite combinations and restore them anytime."),13,false));
        addCustomPresetButton(0); addCustomPresetButton(1); addCustomPresetButton(2); addCustomPresetButton(3);

        Button apply=button(tr("УСТАНОВИТЬ ОБОИ","APPLY WALLPAPER"));
        apply.setOnClickListener(v->applyWallpaper()); body.addView(apply);

        Button openChat=button(tr("ОТКРЫТЬ ОБЩИЙ ЧАТ","OPEN COMMUNITY CHAT"));
        openChat.setOnClickListener(v->{settingsPage=false;chatPage=true;showScreen();});
        body.addView(openChat);

        body.addView(label(tr("ОБМЕН ДИЗАЙНАМИ","DESIGN EXCHANGE")));
        body.addView(text(tr("Отправляй свой дизайн друзьям или вставляй код, который прислал другой пользователь.","Send your design to friends or paste a code shared by another user."),13,false));
        Button shareDesign=button(tr("ПОДЕЛИТЬСЯ МОИМ ДИЗАЙНОМ","SHARE MY DESIGN"));
        shareDesign.setOnClickListener(v->shareCurrentDesign()); body.addView(shareDesign);
        Button importDesign=button(tr("ИМПОРТИРОВАТЬ ДИЗАЙН","IMPORT DESIGN"));
        importDesign.setOnClickListener(v->showImportDesignDialog()); body.addView(importDesign);
        Button community=button(tr("ОТКРЫТЬ TELEGRAM-СООБЩЕСТВО","OPEN TELEGRAM COMMUNITY"));
        community.setOnClickListener(v->openLink("https://t.me/pirtechacc")); body.addView(community);

        body.addView(label(tr("СОЦИАЛЬНЫЕ СЕТИ","SOCIAL")));
        body.addView(text("TikTok: @pirtechacc\nTelegram: t.me/pirtechacc",14,false));
        Button tiktok=button("TikTok"); tiktok.setOnClickListener(v->openLink("https://www.tiktok.com/@pirtechacc")); body.addView(tiktok);
        Button telegram=button("Telegram"); telegram.setOnClickListener(v->openLink("https://t.me/pirtechacc")); body.addView(telegram);
        body.addView(label(tr("О ПРИЛОЖЕНИИ","ABOUT")));
        TextView version=text("MatrixWall Pro 2.7",14,true); version.setGravity(Gravity.CENTER); version.setOnClickListener(v->secretTap()); body.addView(version);
        int remaining=Math.max(0,7-prefs.getInt(Settings.KEY_SECRET_TAPS,0));
        if(!prefs.getBoolean(Settings.KEY_SECRET_UNLOCKED,false)) body.addView(text(tr("Секретная фигура: осталось нажатий — ","Secret shape: taps remaining — ")+remaining,12,false));
        else body.addView(text(tr("Секретная фигура разблокирована","Secret shape unlocked"),12,true));
    }

    private void buildSettings() {
        body.addView(text(tr("Настройки приложения","Application settings"),22,true));
        body.addView(label(tr("ИНТЕРФЕЙС","INTERFACE")));
        Button language=button(en()?"English":"Русский");
        language.setOnClickListener(v->chooseLanguage(false)); body.addView(language);

        body.addView(label(tr("ВНЕШНИЙ ВИД ОБОЕВ","WALLPAPER APPEARANCE")));
        body.addView(choiceButton(tr("Стиль обоев","Wallpaper style"), Settings.STYLE_NAMES, Settings.KEY_STYLE, 0));
        body.addView(choiceButton(tr("Цветовая палитра","Color palette"), Settings.COLOR_NAMES, Settings.KEY_COLOR, 0));
        body.addView(choiceButton(tr("Фигура","Shape"), shapeNames(), Settings.KEY_SHAPE, 0));
        body.addView(choiceButton(tr("Расположение","Layout"), new String[]{tr("Слева","Left"),tr("Справа","Right"),tr("По центру / обе стороны","Center / both sides")}, Settings.KEY_SIDE, 0));

        body.addView(label(tr("АНИМАЦИЯ","ANIMATION")));
        body.addView(label(tr("Скорость вращения","Rotation speed")));
        body.addView(speedBar(Settings.KEY_ROTATION_SPEED,10,300,100));
        body.addView(label(tr("Скорость фонового текста","Background text speed")));
        body.addView(speedBar(Settings.KEY_TEXT_SPEED,10,300,100));

        body.addView(label(tr("ЭФФЕКТЫ","EFFECTS")));
        body.addView(check(tr("Неоновое свечение фигур","Neon shape glow"),Settings.KEY_GLOW,true));
        body.addView(check(tr("Фоновый дождь символов","Background glyph rain"),Settings.KEY_RAIN,true));
        body.addView(check(tr("Звук заставки","Splash sound"),Settings.KEY_SPLASH_SOUND,true));
        body.addView(check(tr("Экономия батареи (меньше кадров)","Battery saver (lower frame rate)"),Settings.KEY_BATTERY_SAVER,false));
        Button reset=button(tr("СБРОСИТЬ НАСТРОЙКИ","RESET SETTINGS"));
        reset.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(tr("Сбросить все настройки?","Reset all settings?")).setMessage(tr("Настройки обоев вернутся к исходным значениям.","Wallpaper settings will return to defaults.")).setNegativeButton(tr("Отмена","Cancel"),null).setPositiveButton(tr("Сбросить","Reset"),(d,w)->{prefs.edit().clear().putString(Settings.KEY_LANGUAGE,lang).apply();Toast.makeText(this,tr("Настройки сброшены","Settings reset"),Toast.LENGTH_SHORT).show();showScreen();}).show());
        body.addView(reset);

        body.addView(label(tr("ИКОНКА ПРИЛОЖЕНИЯ","APP ICON")));
        String[] icons=iconNames();
        body.addView(choiceButton(tr("Выбрать иконку","Choose app icon"),icons,Settings.KEY_ICON,0));

        body.addView(text(tr("Настройки сохраняются автоматически. Выбор обоев можно изменить в любой момент — все варианты открываются по нажатию на соответствующую кнопку.","Settings save automatically. Change your wallpaper at any time; each option opens when you tap its selector."),12,false));
    }

    private String[] shapeNames() {
        boolean unlocked=prefs.getBoolean(Settings.KEY_SECRET_UNLOCKED,false);
        int count=unlocked?ShapeBank.COUNT:ShapeBank.STANDARD_COUNT;
        String[] shapes=new String[count+1]; shapes[0]=tr("Авто","Auto");
        for(int i=0;i<count;i++) shapes[i+1]=ShapeBank.NAMES[i];
        return shapes;
    }

    private String[] iconNames() {
        return new String[]{tr("Matrix Core","Matrix Core"),tr("Cyber Hex","Cyber Hex"),tr("Atom","Atom"),
            tr("Cube","Cube"),tr("Lightning","Lightning"),tr("Eye","Eye"),tr("Orbit","Orbit"),
            tr("Shield","Shield"),tr("Nova","Nova"),tr("Waveform","Waveform")};
    }

    private Button choiceButton(String title,String[] options,String key,int def) {
        Button b=button("");
        refreshChoiceButton(b,title,options,key,def);
        b.setOnClickListener(v->{
            int current=prefs.getInt(key,def);
            if(current<0||current>=options.length) current=def;
            AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(title);
            if(Settings.KEY_ICON.equals(key)) {
                final int selectedIndex=current;
                ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,0,options) {
                    @Override public View getView(int position,View convertView,ViewGroup parent) {
                        LinearLayout row=new LinearLayout(MainActivity.this);
                        row.setGravity(Gravity.CENTER_VERTICAL);
                        row.setPadding(dp(12),dp(8),dp(12),dp(8));
                        ImageView preview=new ImageView(MainActivity.this);
                        int res=getResources().getIdentifier("icon_"+(position+1),"drawable",getPackageName());
                        if(res!=0) preview.setImageResource(res);
                        row.addView(preview,new LinearLayout.LayoutParams(dp(58),dp(58)));
                        TextView name=text(options[position],16,true);
                        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);
                        lp.leftMargin=dp(14); row.addView(name,lp);
                        TextView marker=text(position==selectedIndex?"✓":"",20,true);
                        row.addView(marker);
                        return row;
                    }
                };
                builder.setAdapter(adapter,(dialog,which)->{
                    prefs.edit().putInt(key,which).apply();
                    setLauncherIcon(which); dialog.dismiss();
                    refreshChoiceButton(b,title,options,key,def);
                });
            } else {
                builder.setSingleChoiceItems(options,current,(dialog,which)->{
                    prefs.edit().putInt(key,which).apply(); dialog.dismiss();
                    refreshChoiceButton(b,title,options,key,def);
                });
            }
            builder.setNegativeButton(tr("Отмена","Cancel"),null).show();
        });
        return b;
    }

    private void refreshChoiceButton(Button b,String title,String[] options,String key,int def) {
        int value=prefs.getInt(key,def);
        if(value<0||value>=options.length)value=def;
        b.setText(title+"  •  "+options[value]);
    }

    private void addPresetButton(String title,int preset) {
        Button b=button(title);
        b.setOnClickListener(v->applyPreset(preset));
        body.addView(b);
    }

    private void applyPreset(int preset) {
        SharedPreferences.Editor e=prefs.edit();
        if(preset==0) { // classic green matrix
            e.putInt(Settings.KEY_STYLE,1).putInt(Settings.KEY_COLOR,0).putInt(Settings.KEY_SHAPE,0)
             .putInt(Settings.KEY_SIDE,0).putBoolean(Settings.KEY_RAIN,true).putBoolean(Settings.KEY_GLOW,true);
        } else if(preset==1) { // cyberpunk
            e.putInt(Settings.KEY_STYLE,7).putInt(Settings.KEY_COLOR,16).putInt(Settings.KEY_SHAPE,0)
             .putInt(Settings.KEY_SIDE,2).putBoolean(Settings.KEY_RAIN,true).putBoolean(Settings.KEY_GLOW,true);
        } else if(preset==2) { // aurora
            e.putInt(Settings.KEY_STYLE,8).putInt(Settings.KEY_COLOR,11).putInt(Settings.KEY_SHAPE,0)
             .putInt(Settings.KEY_SIDE,2).putBoolean(Settings.KEY_RAIN,false).putBoolean(Settings.KEY_GLOW,true);
        } else { // neon purple
            e.putInt(Settings.KEY_STYLE,13).putInt(Settings.KEY_COLOR,4).putInt(Settings.KEY_SHAPE,0)
             .putInt(Settings.KEY_SIDE,2).putBoolean(Settings.KEY_RAIN,false).putBoolean(Settings.KEY_GLOW,true);
        }
        e.apply();
        Toast.makeText(this,tr("Пресет применён","Preset applied"),Toast.LENGTH_SHORT).show();
        showScreen();
    }

    private String serializeCurrentDesign() {
        return "MWP1|"
            + prefs.getInt(Settings.KEY_STYLE,0) + "|"
            + prefs.getInt(Settings.KEY_COLOR,0) + "|"
            + prefs.getInt(Settings.KEY_SHAPE,0) + "|"
            + prefs.getInt(Settings.KEY_SIDE,0) + "|"
            + prefs.getInt(Settings.KEY_ROTATION_SPEED,100) + "|"
            + prefs.getInt(Settings.KEY_TEXT_SPEED,100) + "|"
            + (prefs.getBoolean(Settings.KEY_RAIN,true) ? "1" : "0") + "|"
            + (prefs.getBoolean(Settings.KEY_GLOW,true) ? "1" : "0");
    }

    private void applySerializedDesign(String code) {
        try {
            String raw=code.trim();
            int marker=raw.indexOf("MWP1|");
            if(marker>=0) raw=raw.substring(marker);
            int lineEnd=raw.indexOf('\n');
            if(lineEnd>=0) raw=raw.substring(0,lineEnd).trim();
            String[] p=raw.split("\\|");
            if(p.length!=9 || !"MWP1".equals(p[0])) throw new IllegalArgumentException();
            int style=Integer.parseInt(p[1]), color=Integer.parseInt(p[2]);
            int shape=Integer.parseInt(p[3]), side=Integer.parseInt(p[4]);
            int rotation=Integer.parseInt(p[5]), textSpeed=Integer.parseInt(p[6]);
            if(style<0||style>=Settings.STYLE_NAMES.length||color<0||color>=Settings.COLOR_NAMES.length
                ||shape<0||shape>=shapeNames().length||side<0||side>2
                ||rotation<10||rotation>300||textSpeed<10||textSpeed>300
                ||(!"0".equals(p[7])&&!"1".equals(p[7]))||(!"0".equals(p[8])&&!"1".equals(p[8])))
                throw new IllegalArgumentException();
            prefs.edit().putInt(Settings.KEY_STYLE,style).putInt(Settings.KEY_COLOR,color)
                .putInt(Settings.KEY_SHAPE,shape).putInt(Settings.KEY_SIDE,side)
                .putInt(Settings.KEY_ROTATION_SPEED,rotation).putInt(Settings.KEY_TEXT_SPEED,textSpeed)
                .putBoolean(Settings.KEY_RAIN,"1".equals(p[7])).putBoolean(Settings.KEY_GLOW,"1".equals(p[8])).apply();
            Toast.makeText(this,tr("Дизайн импортирован","Design imported"),Toast.LENGTH_SHORT).show();
            showScreen();
        } catch(Exception e) {
            Toast.makeText(this,tr("Код дизайна неверный или несовместимый","Invalid or incompatible design code"),Toast.LENGTH_LONG).show();
        }
    }

    private void shareCurrentDesign() {
        String code=serializeCurrentDesign();
        Intent send=new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT,"MatrixWall Pro design");
        send.putExtra(Intent.EXTRA_TEXT,tr("Мой дизайн MatrixWall Pro. Скопируй код и импортируй его в приложении:","My MatrixWall Pro design. Copy this code and import it in the app:")+"\n"+code);
        startActivity(Intent.createChooser(send,tr("Поделиться дизайном","Share design")));
    }

    private void showImportDesignDialog() {
        EditText input=new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("MWP1|...");
        new AlertDialog.Builder(this).setTitle(tr("Импорт дизайна","Import design"))
            .setMessage(tr("Вставь код MWP1, который прислал другой пользователь.","Paste the MWP1 code another user shared."))
            .setView(input).setNegativeButton(tr("Отмена","Cancel"),null)
            .setPositiveButton(tr("Импортировать","Import"),(d,w)->applySerializedDesign(input.getText().toString())).show();
    }

    private void addCustomPresetButton(int slot) {
        String key="custom_preset_"+slot;
        String saved=prefs.getString(key,"");
        Button b=button(tr("СЛОТ ","SLOT ")+(slot+1)+(saved.isEmpty()?tr(" — ПУСТО"," — EMPTY"):tr(" — СОХРАНЁН"," — SAVED")));
        b.setOnClickListener(v->{
            String current=prefs.getString(key,"");
            if(current.isEmpty()) {
                prefs.edit().putString(key,serializeCurrentDesign()).apply();
                Toast.makeText(this,tr("Пресет сохранён","Preset saved"),Toast.LENGTH_SHORT).show();
                showScreen();
            } else {
                new AlertDialog.Builder(this).setTitle(tr("Слот ","Slot ")+(slot+1))
                    .setItems(new String[]{tr("Применить","Apply"),tr("Перезаписать текущим дизайном","Overwrite with current design"),tr("Удалить","Delete")},(d,which)->{
                        if(which==0) applySerializedDesign(current);
                        else if(which==1) {prefs.edit().putString(key,serializeCurrentDesign()).apply();showScreen();}
                        else {prefs.edit().remove(key).apply();showScreen();}
                    }).show();
            }
        });
        body.addView(b);
    }

    private void randomizeDesign() {
        java.util.Random random=new java.util.Random();
        prefs.edit()
            .putInt(Settings.KEY_STYLE,random.nextInt(Settings.STYLE_NAMES.length))
            .putInt(Settings.KEY_COLOR,random.nextInt(Settings.COLOR_NAMES.length))
            .putInt(Settings.KEY_SHAPE,random.nextInt(shapeNames().length))
            .putInt(Settings.KEY_SIDE,random.nextInt(3))
            .putInt(Settings.KEY_ROTATION_SPEED,50+random.nextInt(201))
            .putInt(Settings.KEY_TEXT_SPEED,50+random.nextInt(201))
            .apply();
        Toast.makeText(this,tr("Новый дизайн создан","New design generated"),Toast.LENGTH_SHORT).show();
        showScreen();
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

    private void buildChat() {
        body.addView(text(tr("ОБЩИЙ ЧАТ MATRIXWALL","MATRIXWALL COMMUNITY CHAT"),20,true));
        body.addView(text(tr("Общайся с другими пользователями. Не публикуй личные данные.","Chat with other users. Do not share personal information."),12,false));
        Button changeNick=button(tr("ИЗМЕНИТЬ НИК","CHANGE NICKNAME"));
        changeNick.setOnClickListener(v->askChatNickname()); body.addView(changeNick);
        Button testFirebase=button(tr("ПРОВЕРИТЬ FIREBASE","TEST FIREBASE CONNECTION"));
        testFirebase.setOnClickListener(v->runFirebaseTest()); body.addView(testFirebase);
        chatScroll=new ScrollView(this);
        chatMessageList=new LinearLayout(this); chatMessageList.setOrientation(LinearLayout.VERTICAL);
        chatMessageList.setPadding(dp(8),dp(8),dp(8),dp(8));
        chatScroll.addView(chatMessageList);
        LinearLayout.LayoutParams scrollParams=new LinearLayout.LayoutParams(-1,0,1);
        scrollParams.topMargin=dp(10); scrollParams.bottomMargin=dp(8);
        body.addView(chatScroll,scrollParams);
        LinearLayout composer=new LinearLayout(this); composer.setGravity(Gravity.CENTER_VERTICAL);
        chatInput=new EditText(this); chatInput.setSingleLine(true);
        chatInput.setTextColor(GREEN); chatInput.setHintTextColor(Color.GRAY);
        chatInput.setHint(tr("Сообщение...","Message..."));
        chatInput.setBackgroundTintList(ColorStateList.valueOf(GREEN));
        composer.addView(chatInput,new LinearLayout.LayoutParams(0,dp(52),1));
        Button send=button(tr("ОТПРАВИТЬ","SEND"));
        send.setOnClickListener(v->sendChatMessage());
        composer.addView(send,new LinearLayout.LayoutParams(dp(112),dp(48)));
        body.addView(composer);
        startChatConnection();
    }

    private void askChatNickname() {
        EditText input=new EditText(this); input.setSingleLine(true);
        input.setMaxLines(1); input.setHint(tr("Например, MatrixFan","For example, MatrixFan"));
        input.setText(prefs.getString("chat_nickname",""));
        new AlertDialog.Builder(this).setTitle(tr("Твой ник в чате","Your chat nickname"))
            .setMessage(tr("Используй вымышленный ник. Не указывай имя, телефон или другие личные данные.","Use a nickname. Do not enter your real name, phone number, or other personal information."))
            .setView(input).setNegativeButton(tr("Отмена","Cancel"),null)
            .setPositiveButton(tr("Сохранить","Save"),(d,w)->{
                String nick=input.getText().toString().trim();
                if(nick.length()<2||nick.length()>20){Toast.makeText(this,tr("Ник должен содержать от 2 до 20 символов","Nickname must be 2-20 characters"),Toast.LENGTH_LONG).show();return;}
                nick=nick.replaceAll("[^\\p{L}\\p{N}_ -]","");
                if(nick.length()<2){Toast.makeText(this,tr("Используй буквы или цифры","Use letters or numbers"),Toast.LENGTH_LONG).show();return;}
                prefs.edit().putString("chat_nickname",nick).apply();
                showScreen();
            }).show();
    }

    private void startChatConnection() {
        if(chatLoading)return;
        if(prefs.getString("chat_nickname","").trim().length()<2){askChatNickname();return;}
        chatLoading=true;
        chatMessageList.removeAllViews();
        chatMessageList.addView(text(tr("Вход в чат...","Signing in..."),13,false));
        chatAuth=FirebaseAuth.getInstance();
        chatAuth.signInAnonymously().addOnCompleteListener(this,task->{
            chatLoading=false;
            if(!task.isSuccessful()){
                String reason = task.getException()==null ? "unknown" : task.getException().getMessage();
                showChatError(tr("Не удалось войти в Firebase: ","Firebase sign-in failed: ")+reason);
                return;
            }
            FirebaseDatabase database=FirebaseDatabase.getInstance("https://matrixwall-chat-default-rtdb.firebaseio.com/");
            chatMessagesRef=database.getReference("rooms").child("global").child("messages");
            chatHasMessages=false;
            chatMessageList.removeAllViews();
            chatMessageList.addView(text(tr("Проверяем соединение с базой...","Checking database connection..."),13,false));
            chatConnectionListener=new ValueEventListener(){
                public void onDataChange(DataSnapshot snapshot){
                    Boolean connected=snapshot.getValue(Boolean.class);
                    if(chatMessageList==null)return;
                    if(Boolean.TRUE.equals(connected)){
                        if(!chatHasMessages){chatMessageList.removeAllViews();chatMessageList.addView(text(tr("Подключено. Пока нет сообщений.","Connected. No messages yet."),13,false));}
                    } else if(!chatHasMessages){showChatError(tr("Нет соединения с Firebase Database. Проверь интернет и URL базы.","No connection to Firebase Database. Check internet and database URL."));}
                }
                public void onCancelled(DatabaseError error){showChatError(tr("Ошибка проверки соединения: ","Connection check failed: ")+error.getMessage());}
            };
            database.getReference(".info/connected").addValueEventListener(chatConnectionListener);
            chatListener=new ChildEventListener(){
                public void onChildAdded(DataSnapshot snap,String previous){
                    if(!chatHasMessages){chatHasMessages=true;chatMessageList.removeAllViews();}
                    String nick=snap.child("nickname").getValue(String.class);
                    String message=snap.child("text").getValue(String.class);
                    if(message==null||nick==null)return;
                    TextView line=text(nick+": "+message,13,false);
                    line.setPadding(dp(8),dp(7),dp(8),dp(7));
                    line.setBackgroundColor(Color.rgb(18,28,18));
                    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=dp(5);
                    chatMessageList.addView(line,lp);
                    chatScroll.post(()->chatScroll.fullScroll(View.FOCUS_DOWN));
                }
                public void onChildChanged(DataSnapshot snap,String previous){}
                public void onChildRemoved(DataSnapshot snap){}
                public void onChildMoved(DataSnapshot snap,String previous){}
                public void onCancelled(DatabaseError error){showChatError(tr("Ошибка доступа к базе: ","Database access error: ")+error.getMessage());}
            };
            chatMessagesRef.limitToLast(100).addChildEventListener(chatListener);
        });
    }

    private void showChatError(String message) {
        if(chatMessageList!=null){chatMessageList.removeAllViews();chatMessageList.addView(text(message,13,true));}
    }

    private void showFirebaseWriteError(Exception error) {
        String details;
        if (error == null) {
            details = tr("Firebase не вернул подробности ошибки.","Firebase did not provide error details.");
        } else {
            String message = error.getMessage();
            details = error.getClass().getSimpleName();
            if (message != null && !message.trim().isEmpty()) details += ": " + message;
            details += "\\n\\n" + error.toString();
        }
        new AlertDialog.Builder(this)
            .setTitle(tr("Точная ошибка отправки","Exact send error"))
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show();
    }

    private void sendChatMessage() {
        if(chatMessagesRef==null||chatAuth==null||chatAuth.getCurrentUser()==null){
            Toast.makeText(this,tr("Чат ещё подключается","Chat is still connecting"),Toast.LENGTH_SHORT).show();return;
        }
        String value=chatInput.getText().toString().trim();
        if(value.isEmpty())return;
        if(value.length()>500){Toast.makeText(this,tr("Максимум 500 символов","Maximum 500 characters"),Toast.LENGTH_SHORT).show();return;}
        Map<String,Object> message=new HashMap<>();
        message.put("uid",chatAuth.getCurrentUser().getUid());
        message.put("nickname",prefs.getString("chat_nickname","User"));
        message.put("text",value);
        message.put("timestamp",ServerValue.TIMESTAMP);
        chatMessagesRef.push().setValue(message).addOnCompleteListener(task->{
            if(task.isSuccessful())chatInput.setText("");
            else showFirebaseWriteError(task.getException());
        });
    }

    private void runFirebaseTest() {
        if(chatMessagesRef==null||chatAuth==null||chatAuth.getCurrentUser()==null) {
            Toast.makeText(this,tr("Чат ещё подключается. Попробуй через секунду.","Chat is still connecting. Try again in a moment."),Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle(tr("Проверка Firebase","Firebase connection test"))
            .setMessage(tr("Будет отправлено одно сообщение с пометкой [ТЕСТ] в общий чат. Его увидят другие пользователи.","One message marked [TEST] will be posted to the public chat and visible to other users."))
            .setNegativeButton(tr("Отмена","Cancel"),null)
            .setPositiveButton(tr("Отправить тест","Send test"),(dialog,which)->{
                Map<String,Object> message=new HashMap<>();
                message.put("uid",chatAuth.getCurrentUser().getUid());
                message.put("nickname",prefs.getString("chat_nickname","User"));
                message.put("text","[ТЕСТ] Проверка соединения MatrixWall Pro — запись в Firebase.");
                message.put("timestamp",ServerValue.TIMESTAMP);
                chatMessagesRef.push().setValue(message).addOnCompleteListener(this,task->{
                    if(task.isSuccessful()) {
                        Toast.makeText(this,tr("Успешно! Firebase сохранил тестовое сообщение.","Success! Firebase saved the test message."),Toast.LENGTH_LONG).show();
                    } else {
                        showFirebaseWriteError(task.getException());
                    }
                });
            }).show();
    }

    @Override protected void onDestroy() {
        if(chatMessagesRef!=null&&chatListener!=null)chatMessagesRef.removeEventListener(chatListener);
        if(chatConnectionListener!=null)FirebaseDatabase.getInstance("https://matrixwall-chat-default-rtdb.firebaseio.com/").getReference(".info/connected").removeEventListener(chatConnectionListener);
        super.onDestroy();
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
