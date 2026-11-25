package com.pirtech.phoneempire;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.content.*;
import android.speech.tts.TextToSpeech;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.SystemClock;
import java.util.*;

public class MainActivity extends Activity {
    private GameView game;
    private TextToSpeech tts;
    private Music music;
    private boolean voiceReady=false;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(10,16,29));
        getWindow().setNavigationBarColor(Color.rgb(10,16,29));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        tts=new TextToSpeech(this, status -> {
            if(status==TextToSpeech.SUCCESS) {
                int r=tts.setLanguage(new Locale("ru","RU"));
                voiceReady=r!=TextToSpeech.LANG_MISSING_DATA && r!=TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });
        game=new GameView(this);
        setContentView(game);
        music=new Music();
        music.start();
    }
    @Override protected void onPause(){super.onPause(); if(game!=null)game.save(); if(music!=null)music.pause();}
    @Override protected void onResume(){super.onResume(); if(music!=null)music.resume();}
    @Override protected void onDestroy(){if(music!=null)music.stop();if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
    private void speak(String s){if(voiceReady && tts!=null)tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"story");}

    private class Music {
        volatile boolean running=true, playing=true;
        Thread thread; AudioTrack track;
        void start(){thread=new Thread(()->{
            int rate=22050, n=rate*4;
            short[] loop=new short[n];
            double[] notes={261.63,329.63,392.00,329.63,220.00,293.66,349.23,293.66};
            for(int i=0;i<n;i++){
                double t=i/(double)rate, beat=(t%2.0)/2.0;
                double f=notes[(int)(t/0.5)%notes.length];
                double env=Math.min(1,beat*12)*Math.min(1,(1-beat)*5);
                double v=(Math.sin(2*Math.PI*f*t)*0.12+Math.sin(2*Math.PI*f*0.5*t)*0.045+Math.sin(2*Math.PI*65*t)*0.025)*env;
                loop[i]=(short)(Math.max(-1,Math.min(1,v))*32767);
            }
            try{
                track=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(n*2).setTransferMode(AudioTrack.MODE_STREAM).build();
                track.play();
                while(running){if(playing){track.write(loop,0,loop.length);}else SystemClock.sleep(100);}
            }catch(Exception ignored){} finally{if(track!=null){try{track.stop();track.release();}catch(Exception ignored){}}}
        },"PhoneEmpireMusic");thread.setDaemon(true);thread.start();}
        void pause(){playing=false;} void resume(){playing=true;} void stop(){running=false;playing=false;if(track!=null)try{track.pause();}catch(Exception ignored){}}
    }

    private class GameView extends View {
        Canvas c; Paint p=new Paint(3); float sx,sy; int tab=0; long last=0;
        int bg=Color.rgb(10,16,29), panel=Color.rgb(22,31,49), panel2=Color.rgb(30,43,64), white=Color.rgb(240,245,255), muted=Color.rgb(151,169,193), cyan=Color.rgb(66,211,219), green=Color.rgb(88,214,151), gold=Color.rgb(255,194,92), red=Color.rgb(255,112,119);
        long money=150000; int day=1, reputation=5, phones=0, sold=0, staff=0, shopLevel=1, research=0, marketing=0;
        int parts=0, story=0, xp=0; boolean musicOn=true, voiceOn=true;
        String company="NOVA MOBILE", model="NOVA One"; String notice="Добро пожаловать в собственную компанию!";
        String[] tabs={"БИЗНЕС","РАЗРАБОТКА","ЗАДАНИЯ","КОМАНДА"};
        String[] chapters={
            "Глава 1. Маленькое начало. Вы открыли мастерскую в старом торговом центре. Ваша цель — доказать, что даже маленькая команда может создать отличный телефон.",
            "Глава 2. Первый тираж. Поставщик готов продать вам комплектующие. Соберите первую партию и найдите своих покупателей.",
            "Глава 3. Имя на коробке. Покупатели начинают узнавать NOVA. Улучшайте качество и не забывайте о репутации.",
            "Глава 4. Большая камера. Инженеры предлагают новую систему камер. Вложитесь в исследования, чтобы обойти конкурентов.",
            "Глава 5. Свой цех. Спрос растёт. Пора расширить производство и нанять первых сотрудников.",
            "Глава 6. На витрине. Откройте фирменную точку продаж и запустите рекламную кампанию.",
            "Глава 7. Сложный выбор. Конкурент снижает цены. Решите, будете ли вы бороться стоимостью или качеством.",
            "Глава 8. Новый процессор. Разработка собственного чипа потребует времени и серьёзных вложений.",
            "Глава 9. За пределами города. Выходите в новые регионы и увеличивайте производство.",
            "Глава 10. Флагман. Создайте телефон, который станет лицом компании.",
            "Глава 11. Большая лига. Удерживайте качество, обучайте команду и развивайте сеть магазинов.",
            "Глава 12. Империя. Ваша компания стала заметным игроком рынка. Но впереди ещё много технологий."
        };
        String[] tasks={"Продайте первые 3 телефона","Соберите партию из 5 смартфонов","Достигните репутации 15","Исследуйте улучшенную камеру","Наймите 2 сотрудников","Улучшите мастерскую до 2 уровня","Продайте 20 смартфонов","Изучите новый процессор","Накопите 500 000 ₽","Создайте флагманскую модель","Нанимайте и расширяйте бизнес","Станьте лидером рынка"};
        GameView(Context ctx){super(ctx);setLayerType(View.LAYER_TYPE_SOFTWARE,null);load();}
        void load(){android.content.SharedPreferences s=getSharedPreferences("empire",0);money=s.getLong("money",150000);day=s.getInt("day",1);reputation=s.getInt("rep",5);phones=s.getInt("phones",0);sold=s.getInt("sold",0);staff=s.getInt("staff",0);shopLevel=s.getInt("level",1);research=s.getInt("research",0);marketing=s.getInt("marketing",0);parts=s.getInt("parts",0);story=s.getInt("story",0);xp=s.getInt("xp",0);}
        void save(){getSharedPreferences("empire",0).edit().putLong("money",money).putInt("day",day).putInt("rep",reputation).putInt("phones",phones).putInt("sold",sold).putInt("staff",staff).putInt("level",shopLevel).putInt("research",research).putInt("marketing",marketing).putInt("parts",parts).putInt("story",story).putInt("xp",xp).apply();}
        void txt(String s,float x,float y,float size,int color){p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));c.drawText(s,x,y,p);}
        void bold(String s,float x,float y,float size,int color){p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));c.drawText(s,x,y,p);}
        void rect(float x,float y,float w,float h,float r,int color){p.setColor(color);c.drawRoundRect(x,y,x+w,y+h,r,r,p);}
        void line(float x,float y,float x2,float y2,int color,float sw){p.setColor(color);p.setStrokeWidth(sw);c.drawLine(x,y,x2,y2,p);}
        void card(float x,float y,float w,float h){rect(x,y,w,h,18,panel);p.setColor(Color.argb(24,255,255,255));c.drawRoundRect(x,y,x+w,y+h,18,18,p);}
        void button(String label,float x,float y,float w,float h,int color){rect(x,y,w,h,13,color);p.setColor(Color.WHITE);p.setTextSize(13);p.setTypeface(Typeface.DEFAULT_BOLD);float tw=p.measureText(label);c.drawText(label,x+(w-tw)/2,y+h/2+5,p);}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);c=canvas;sx=getWidth()/390f;sy=getHeight()/844f;c.save();c.scale(sx,sy);c.drawColor(bg);
            bold("PHONE EMPIRE",20,34,19,white);txt("NOVA MOBILE  •  ДЕНЬ "+day,20,56,11,muted);
            rect(18,70,354,72,16,panel);txt("КАПИТАЛ",32,91,10,muted);bold(String.format(Locale.US,"%,d ₽",money),32,121,23,green);txt("РЕПУТАЦИЯ",220,91,10,muted);bold(reputation+"/100",220,121,21,cyan);
            if(tab==0)drawBusiness();else if(tab==1)drawResearch();else if(tab==2)drawTasks();else drawTeam();
            float y=780;rect(12,y,366,52,18,panel2);
            for(int i=0;i<4;i++){float x=17+i*91; if(tab==i)rect(x,y+5,86,42,12,Color.rgb(47,78,105));txt(tabs[i],x+10,y+31,10,tab==i?cyan:muted);}
            if(notice.length()>0){rect(18,728,354,38,12,Color.rgb(33,52,70));txt(clip(notice,46),28,752,11,white);}
            c.restore();
        }
        String clip(String s,int n){return s.length()>n?s.substring(0,n-1)+"…":s;}
        void drawBusiness(){
            bold("ВАША КОМПАНИЯ",20,174,16,white);
            card(18,190,354,145);drawPhone(42,207,72);bold(model,132,224,17,white);txt("Ваш первый смартфон",132,246,11,muted);txt("Собрано: "+phones+"    Продано: "+sold,132,268,11,cyan);txt("Уровень мастерской: "+shopLevel,132,290,11,muted);
            button("СОБРАТЬ 1  •  18 000 ₽",30,302,330,27,Color.rgb(34,139,125));
            bold("УПРАВЛЕНИЕ",20,371,15,white);
            card(18,386,170,91);txt("Комплектующие",30,409,11,muted);bold(parts+" комплектов",30,435,19,white);button("Купить 5 • 45 000 ₽",28,443,150,25,Color.rgb(53,92,130));
            card(202,386,170,91);txt("Продажи",214,409,11,muted);bold("+"+String.format(Locale.US,"%,d",salePrice())+" ₽",214,435,19,green);button("Продать 1 телефон",212,443,150,25,Color.rgb(34,139,125));
            card(18,490,354,94);txt("СЛЕДУЮЩИЙ ШАГ",32,514,10,gold);bold(tasks[Math.min(story,11)],32,540,15,white);txt("Глава "+(story+1)+" из 12",32,560,11,muted);
            button("Завершить рабочий день",20,603,350,43,Color.rgb(55,89,132));
        }
        void drawPhone(float x,float y,float size){rect(x,y,size*.57f,size,13,Color.rgb(5,9,16));rect(x+3,y+4,size*.57f-6,size-8,10,Color.rgb(65,154,183));rect(x+8,y+13,size*.57f-16,size*.55f,5,Color.rgb(29,61,94));rect(x+size*.2f,y+size*.83f,size*.17f,3,3,Color.WHITE);}
        int salePrice(){return 30000+research*2500+shopLevel*1000;}
        void drawResearch(){
            bold("ЛАБОРАТОРИЯ",20,174,17,white);txt("Инвестируйте в технологии и улучшайте устройства.",20,196,11,muted);
            String[] names={"Камера Pro","OLED-дисплей","Быстрый процессор","Батарея высокой ёмкости","Автоматизация цеха"};
            String[] desc={"Больше деталей на снимках","Яркие цвета и контраст","Быстрее приложения и игры","Больше времени без зарядки","Снижение расходов на сборку"};
            int[] costs={45000,65000,90000,55000,120000};
            for(int i=0;i<5;i++){int y=218+i*91;card(18,y,354,79);bold(names[i],31,y+25,14,white);txt(desc[i],31,y+45,10,muted);button(research>i?"ИЗУЧЕНО":String.format(Locale.US,"%,d ₽",costs[i]),245,y+19,112,38,research>i?Color.rgb(47,112,91):Color.rgb(53,92,130));}
            card(18,690,354,36);txt("Уровень технологий: "+research+" / 5",30,713,12,cyan);
        }
        void drawTasks(){
            bold("ИСТОРИЯ КОМПАНИИ",20,174,17,white);txt("Каждый шаг приближает NOVA к вершине.",20,196,11,muted);
            card(18,213,354,180);txt("ТЕКУЩАЯ ГЛАВА",32,239,10,gold);bold("Глава "+(story+1),32,267,19,white);wrap(chapters[Math.min(story,11)],32,292,320,14,11,muted);
            button("ОЗВУЧИТЬ ИСТОРИЮ",30,355,330,27,Color.rgb(53,92,130));
            bold("ЦЕЛИ КАМПАНИИ",20,426,15,white);
            for(int i=0;i<6;i++){int k=Math.min(story+i,11), y=444+i*45;card(18,y,354,38);rect(29,y+11,16,16,5,k<story?green:panel2);txt((k+1)+". "+tasks[k],56,y+25,11,k<story?green:white);}
        }
        void drawTeam(){
            bold("КОМАНДА И РОСТ",20,174,17,white);txt("Люди, реклама и развитие мастерской.",20,196,11,muted);
            card(18,215,354,88);txt("СОТРУДНИКИ",32,240,10,muted);bold(""+staff+" человек",32,273,23,white);button("Нанять • 35 000 ₽",211,241,145,40,Color.rgb(53,92,130));
            card(18,319,354,88);txt("МАСТЕРСКАЯ",32,344,10,muted);bold("Уровень "+shopLevel,32,377,23,white);button("Улучшить • "+(shopLevel*80000)+" ₽",211,345,145,40,Color.rgb(53,92,130));
            card(18,423,354,88);txt("РЕКЛАМНАЯ КАМПАНИЯ",32,448,10,muted);bold("Уровень "+marketing,32,481,23,white);button("Запустить • 25 000 ₽",211,449,145,40,Color.rgb(53,92,130));
            card(18,527,354,95);txt("СТАТИСТИКА",32,552,10,muted);txt("Продано устройств: "+sold,32,578,12,white);txt("Опыт компании: "+xp+"    Репутация: "+reputation,32,600,11,cyan);
            button(musicOn?"МУЗЫКА: ВКЛ":"МУЗЫКА: ВЫКЛ",20,646,168,38,Color.rgb(53,92,130));button(voiceOn?"ОЗВУЧКА: ВКЛ":"ОЗВУЧКА: ВЫКЛ",202,646,168,38,Color.rgb(53,92,130));
            button("СОХРАНИТЬ ИГРУ",20,694,350,31,Color.rgb(34,139,125));
        }
        void wrap(String text,float x,float y,float width,float lineH,float size,int color){p.setTextSize(size);String[] words=text.split(" ");String line="";int rows=0;for(String w:words){String t=line.isEmpty()?w:line+" "+w;if(p.measureText(t)>width){txt(line,x,y+rows*lineH,size,color);line=w;rows++;}else line=t;}if(!line.isEmpty())txt(line,x,y+rows*lineH,size,color);}
        void msg(String s){notice=s;invalidate();}
        void buyParts(){if(money>=45000){money-=45000;parts+=5;msg("Закуплено 5 комплектов комплектующих.");}else msg("Недостаточно средств для закупки.");}
        void assemble(){if(parts<1){msg("Сначала купите комплектующие.");return;}if(money<18000){msg("Не хватает денег на сборку.");return;}parts--;money-=18000;phones++;xp+=10;msg("Смартфон собран. Проверьте качество и цену!");advance();}
        void sell(){if(phones<1){msg("На складе пока нет готовых телефонов.");return;}phones--;sold++;money+=salePrice();reputation=Math.min(100,reputation+1+(marketing>0?1:0));xp+=15;msg("Продажа завершена! Покупатель доволен.");advance();}
        void advance(){if(story==0&&sold>=3)story=1;if(story==1&&phones+sold>=5)story=2;if(story==2&&reputation>=15)story=3;if(story==3&&research>=1)story=4;if(story==4&&staff>=2)story=5;if(story==5&&shopLevel>=2)story=6;if(story==6&&sold>=20)story=7;if(story==7&&research>=3)story=8;if(story==8&&money>=500000)story=9;if(story==9&&research>=5)story=10;if(story==10&&staff>=5)story=11;}
        void endDay(){day++;long upkeep=staff*2500L+shopLevel*1800L;money=Math.max(0,money-upkeep);long bonus=phones*1200L;money+=bonus;reputation=Math.max(0,Math.min(100,reputation+(money>0?1:0)));advance();save();msg("День "+day+". Расходы: "+upkeep+" ₽. Доход со склада: "+bonus+" ₽.");}
        @Override public boolean onTouchEvent(android.view.MotionEvent e){float x=e.getX()/sx,y=e.getY()/sy;if(e.getAction()!=MotionEvent.ACTION_UP)return true;
            if(y>780){tab=Math.min(3,Math.max(0,(int)((x-17)/91)));invalidate();return true;}
            if(tab==0){if(y>=302&&y<338)assemble();else if(y>=443&&y<478){if(x<195)buyParts();else sell();}else if(y>=603&&y<650)endDay();}
            else if(tab==1){int idx=(int)((y-218)/91);int[] costs={45000,65000,90000,55000,120000};if(idx>=0&&idx<5&&y>=218){if(research>idx)msg("Эта технология уже изучена.");else if(money>=costs[idx]){money-=costs[idx];research++;xp+=35;msg("Исследование завершено: "+new String[]{"Камера Pro","OLED-дисплей","Быстрый процессор","Новая батарея","Автоматизация"}[idx]);advance();}else msg("Недостаточно средств для исследования.");}}
            else if(tab==2){if(y>=355&&y<393){if(voiceOn)speak(chapters[Math.min(story,11)]);else msg("Озвучка отключена.");}}
            else if(tab==3){if(y>=241&&y<300){if(money>=35000){money-=35000;staff++;msg("Новый сотрудник присоединился к команде.");advance();}else msg("Не хватает денег на зарплату и найм.");}
                else if(y>=345&&y<407){long cost=shopLevel*80000L;if(money>=cost){money-=cost;shopLevel++;msg("Мастерская улучшена до уровня "+shopLevel+".");advance();}else msg("Не хватает средств на улучшение.");}
                else if(y>=449&&y<512){if(money>=25000){money-=25000;marketing++;reputation=Math.min(100,reputation+3);msg("Реклама запущена. Репутация растёт.");}else msg("Недостаточно денег на рекламу.");}
                else if(y>=646&&y<686){musicOn=!musicOn;if(musicOn)music.resume();else music.pause();msg("Настройка музыки изменена.");}
                else if(y>=694&&y<730){voiceOn=!voiceOn;msg("Настройка озвучки изменена.");}
                else if(y>=694){save();msg("Прогресс сохранён.");}
            }
            save();invalidate();return true;
        }
    }
}
