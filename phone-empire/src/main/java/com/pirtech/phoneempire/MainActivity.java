package com.pirtech.phoneempire;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.content.*;
import android.media.*;
import android.speech.tts.TextToSpeech;
import java.util.*;

public class MainActivity extends Activity {
    private Adventure game;
    private TextToSpeech tts;
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(8,10,18));
        getWindow().setNavigationBarColor(Color.rgb(8,10,18));
        tts=new TextToSpeech(this,s->{if(s==TextToSpeech.SUCCESS)tts.setLanguage(new Locale("ru","RU"));});
        game=new Adventure(this); setContentView(game);
    }
    @Override protected void onPause(){super.onPause();if(game!=null)game.save();}
    @Override protected void onDestroy(){if(tts!=null)tts.shutdown();super.onDestroy();}

    private class Adventure extends View {
        Canvas c; Paint p=new Paint(); Bitmap[] rooms=new Bitmap[8];
        float px=480,py=510,joyX=0,joyY=0,scaleX,scaleY;
        int room=0,chapter=0,dialogue=0,heart=3,coins=0;
        boolean talking=false,showHelp=true;
        String speaker="",line="";
        long last=System.nanoTime();
        final int ink=Color.rgb(10,10,18),paper=Color.WHITE,red=Color.rgb(255,65,85),gold=Color.rgb(255,211,89);
        final String[][] talks={
            {"СТАРЫЙ СМОТРИТЕЛЬ","Ты наконец проснулся. Это место давно ждёт гостя.","Восемь комнат соединены старыми переходами.","Найди три осколка памяти. Тогда откроется северная дверь."},
            {"МИРА","Не бойся темноты. Она просто не умеет говорить.","Я потеряла ключ в мастерской. Поможешь найти?","Спасибо. Возьми эту монету."},
            {"МЕХАНИК","Этот механизм питается воспоминаниями.","Нажми на кнопку рядом с панелью, когда найдёшь осколок."},
            {"ЛЕСНОЙ ДУХ","Здесь каждый звук оставляет след.","Ты можешь идти дальше. Только не забывай слушать."},
            {"АРХИВАРИУС","В старых книгах написано: выход всегда там, где ты решишь его искать.","Ты собрал достаточно воспоминаний."},
            {"СТРАННИК","Я видел свет за восточной дверью.","Не все встречи случайны."},
            {"ХРАНИТЕЛЬ","Ты прошёл длинный путь.","Комнаты меняются, но выбор остаётся за тобой."},
            {"ГОЛОС","Спасибо, что дошёл сюда.","Это только начало новой истории."}
        };
        final String[] names={"ЗАЛ ПРОБУЖДЕНИЯ","СТАРАЯ МАСТЕРСКАЯ","ЗАТОНУВШАЯ БИБЛИОТЕКА","ТИХИЙ ЛЕС","ЗАЛ ПАМЯТИ","ЗАБРОШЕННАЯ СТАНЦИЯ","СЕВЕРНЫЙ КОРИДОР","ПОСЛЕДНЯЯ КОМНАТА"};
        Adventure(Context ctx){super(ctx);setLayerType(View.LAYER_TYPE_SOFTWARE,null);load();}
        void load(){android.content.SharedPreferences s=getSharedPreferences("soulstory",0);room=s.getInt("room",0);px=s.getFloat("px",480);py=s.getFloat("py",510);chapter=s.getInt("chapter",0);coins=s.getInt("coins",0);}
        void save(){getSharedPreferences("soulstory",0).edit().putInt("room",room).putFloat("px",px).putFloat("py",py).putInt("chapter",chapter).putInt("coins",coins).apply();}
        int col(String v){return Color.parseColor(v);}
        void fill(int color){p.setColor(color);p.setStyle(Paint.Style.FILL);}
        void box(float x,float y,float w,float h,int color){fill(color);c.drawRect(x,y,x+w,y+h,p);}
        void stroke(float x,float y,float w,float h,int color,float sw){p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(sw);c.drawRect(x,y,x+w,y+h,p);p.setStyle(Paint.Style.FILL);}
        void text(String s,float x,float y,int size,int color){fill(color);p.setTypeface(Typeface.create("monospace",Typeface.BOLD));p.setTextSize(size);c.drawText(s,x,y,p);}
        void center(String s,float x,float y,int size,int color){p.setTypeface(Typeface.create("monospace",Typeface.BOLD));p.setTextSize(size);text(s,x-p.measureText(s)/2,y,size,color);}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);c=canvas;scaleX=getWidth()/960f;scaleY=getHeight()/540f;c.save();c.scale(scaleX,scaleY);
            long now=System.nanoTime();float dt=Math.min(.04f,(now-last)/1e9f);last=now;
            if(!talking){px+=joyX*180*dt;py+=joyY*180*dt;px=Math.max(55,Math.min(905,px));py=Math.max(90,Math.min(500,py));}
            drawRoom();drawObjects();drawCharacter(px,py,1.0f,Color.rgb(235,221,194),false);
            hud();if(talking)drawDialogue();if(showHelp&&!talking)drawHelp();
            c.restore();postInvalidateDelayed(16);
        }
        void drawRoom(){
            if(rooms[room]==null){
                for(int i=0;i<rooms.length;i++)if(i!=room&&rooms[i]!=null){rooms[i].recycle();rooms[i]=null;}
                try{rooms[room]=BitmapFactory.decodeStream(getAssets().open("rooms/room"+room+".png"));}catch(Exception ignored){}
            }
            if(rooms[room]!=null){p.setFilterBitmap(false);c.drawBitmap(rooms[room],null,new Rect(0,0,960,540),p);}
            else {box(0,0,960,540,Color.rgb(22,25,37));box(30,55,900,440,Color.rgb(44,48,62));}
            stroke(28,50,904,450,Color.rgb(150,142,128),5);
            // Doors: each room has a clear, walkable exit.
            box(430,48,100,15,Color.rgb(16,17,26));stroke(430,48,100,40,gold,3);
            box(25,245,15,85,Color.rgb(16,17,26));box(920,245,15,85,Color.rgb(16,17,26));
            text(names[room],34,34,18,Color.WHITE);
        }
        void drawObjects(){
            // Furnishings and landmarks are intentionally chunky pixel shapes.
            switch(room){
                case 0: drawBed(180,165);drawLamp(760,150);drawNpc(680,300,0);break;
                case 1: drawDesk(160,180);drawCrate(690,350);drawNpc(770,260,1);break;
                case 2: drawBooks(170,170);drawBooks(730,170);drawNpc(480,260,2);break;
                case 3: drawTree(210,170);drawTree(730,175);drawNpc(500,270,3);break;
                case 4: drawPillar(210,160);drawPillar(730,160);drawNpc(480,270,4);break;
                case 5: drawDesk(170,180);drawCrate(700,340);drawNpc(480,260,5);break;
                case 6: drawPillar(220,160);drawPillar(720,160);drawNpc(480,270,6);break;
                default: drawLamp(220,160);drawLamp(740,160);drawNpc(480,260,7);break;
            }
            // Collectible memory shards.
            if(chapter<3){float[] xs={300,600,480},ys={300,370,160};for(int i=0;i<3;i++)if((chapter&(1<<i))==0)drawShard(xs[i],ys[i]);}
        }
        void drawCharacter(float x,float y,float z,int skin,boolean npc){
            int ix=(int)x,iy=(int)y;
            box(ix-11,iy-29,22,25,skin);box(ix-15,iy-8,30,22,Color.rgb(76,91,143));
            box(ix-14,iy+12,10,10,Color.rgb(35,37,52));box(ix+4,iy+12,10,10,Color.rgb(35,37,52));
            box(ix-7,iy-21,4,4,ink);box(ix+4,iy-21,4,4,ink);
            box(ix-5,iy-13,10,2,Color.rgb(135,66,76));
            if(!npc){box(ix-15,iy-33,30,4,Color.rgb(20,20,28));box(ix-14,iy-32,28,2,red);}
        }
        void drawNpc(float x,float y,int id){drawCharacter(x,y,1,Color.rgb(214,180,140),true);box(x-15,y-31,30,6,Color.rgb(55+id*12,90,120));}
        void drawBed(float x,float y){box(x,y,160,85,Color.rgb(98,78,100));box(x+8,y+8,144,25,Color.rgb(211,199,190));box(x+8,y+38,144,39,Color.rgb(89,105,148));}
        void drawLamp(float x,float y){box(x-5,y,10,100,Color.rgb(100,83,64));box(x-25,y-14,50,18,gold);box(x-40,y+86,80,12,Color.rgb(83,67,55));}
        void drawDesk(float x,float y){box(x,y,160,55,Color.rgb(110,77,54));box(x+12,y+55,14,60,Color.rgb(73,55,48));box(x+134,y+55,14,60,Color.rgb(73,55,48));}
        void drawCrate(float x,float y){box(x,y,70,65,Color.rgb(128,88,52));stroke(x,y,70,65,Color.rgb(205,160,91),4);stroke(x+8,y+8,54,49,Color.rgb(205,160,91),3);}
        void drawBooks(float x,float y){for(int i=0;i<5;i++){box(x,y+i*22,105,17,new int[]{0xffa45c52,0xff64816f,0xff6e719d,0xffc49a55,0xff7c5c83}[i]);}}
        void drawTree(float x,float y){box(x-8,y,16,95,Color.rgb(91,63,48));box(x-45,y-35,90,55,Color.rgb(47,104,76));box(x-32,y-55,64,35,Color.rgb(62,126,83));}
        void drawPillar(float x,float y){box(x,y,42,160,Color.rgb(117,119,134));box(x-8,y,58,12,Color.rgb(174,173,179));box(x-8,y+148,58,12,Color.rgb(174,173,179));}
        void drawShard(float x,float y){fill(gold);Path a=new Path();a.moveTo(x,y-15);a.lineTo(x+12,y);a.lineTo(x,y+17);a.lineTo(x-12,y);a.close();c.drawPath(a,p);stroke(x-5,y-7,10,14,Color.WHITE,2);}
        void hud(){box(0,0,960,52,0xdd080a12);text("SOULBOUND",28,32,19,Color.WHITE);text(names[room],340,31,16,gold);text("ОСКОЛКИ "+Integer.bitCount(chapter&7)+"/3",760,31,14,Color.WHITE);
            box(20,468,920,58,0xcc080a12);text("ПЕРЕМЕЩЕНИЕ",38,493,12,Color.LTGRAY);text("ИССЛЕДУЙ КОМНАТУ  •  ПОДОЙДИ К ПЕРСОНАЖУ И НАЖМИ «E»",38,514,13,Color.WHITE);
        }
        void drawHelp(){box(270,170,420,105,0xee10121d);stroke(270,170,420,105,gold,3);center("ТЫ ПРОСНУЛСЯ В НЕЗНАКОМОМ МЕСТЕ",480,205,15,Color.WHITE);center("Веди пальцем по левой части экрана,",480,232,14,Color.LTGRAY);center("чтобы ходить. Коснись NPC рядом для диалога.",480,254,13,Color.LTGRAY);}
        void drawDialogue(){box(30,350,900,150,0xf0080a12);stroke(30,350,900,150,Color.WHITE,4);text(speaker,55,385,19,gold);wrap(line,55,420,830,23,16,Color.WHITE);text("КОСНИСЬ, ЧТОБЫ ПРОДОЛЖИТЬ  ▸",590,480,12,Color.LTGRAY);}
        void wrap(String s,float x,float y,float width,int step,int size,int color){String cur="";int row=0;for(String word:s.split(" ")){String test=cur.isEmpty()?word:cur+" "+word;p.setTextSize(size);p.setTypeface(Typeface.MONOSPACE);if(p.measureText(test)>width){text(cur,x,y+row++*step,size,color);cur=word;}else cur=test;}text(cur,x,y+row*step,size,color);}
        void interact(){
            if(talking){dialogue++;String[] a=talks[room];if(dialogue>=a.length){talking=false;dialogue=0;if(room==1&&coins==0)coins++;invalidate();return;}line=a[dialogue];invalidate();return;}
            if(showHelp){showHelp=false;invalidate();return;}
            if(Math.abs(px-480)<105&&Math.abs(py-260)<110 || Math.abs(px-680)<110&&Math.abs(py-300)<100 || Math.abs(px-770)<100&&Math.abs(py-260)<100){
                talking=true;dialogue=0;speaker=talks[room][0];line=talks[room].length>1?talks[room][1]:"Здесь тихо.";invalidate();return;
            }
            float[] xs={300,600,480},ys={300,370,160};for(int i=0;i<3;i++)if((chapter&(1<<i))==0&&Math.hypot(px-xs[i],py-ys[i])<55){chapter|=1<<i;coins+=5;line="Ты нашёл осколок памяти.";speaker="СИСТЕМА";talking=true;dialogue=0;invalidate();return;}
            if(py<100){room=(room+7)%8;px=480;py=420;save();}
            else if(px<65){room=(room+7)%8;px=875;save();}
            else if(px>895){room=(room+1)%8;px=85;save();}
            else if(py>450){room=(room+1)%8;px=480;py=105;save();}
            invalidate();
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            float x=e.getX()/scaleX,y=e.getY()/scaleY;
            if(e.getAction()==MotionEvent.ACTION_DOWN){if(talking)return true;if(showHelp){showHelp=false;return true;}if(x>700&&y>390)return true;joyX=(x-180)/130f;joyY=(y-340)/130f;float len=(float)Math.hypot(joyX,joyY);if(len>1){joyX/=len;joyY/=len;}return true;}
            if(e.getAction()==MotionEvent.ACTION_MOVE){if(x<560){joyX=Math.max(-1,Math.min(1,(x-180)/130f));joyY=Math.max(-1,Math.min(1,(y-340)/130f));}return true;}
            if(e.getAction()==MotionEvent.ACTION_UP){joyX=joyY=0;if(x>700&&y>390)interact();else if(y<80&&x>850)save();return true;}return true;
        }
    }
}
