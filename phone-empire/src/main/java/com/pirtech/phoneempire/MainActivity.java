package com.pirtech.phoneempire;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.content.*;
import android.speech.tts.TextToSpeech;
import java.util.*;

public class MainActivity extends Activity {
    private Game game; private TextToSpeech tts;
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(12,15,24));getWindow().setNavigationBarColor(Color.rgb(12,15,24));tts=new TextToSpeech(this,s->{if(s==TextToSpeech.SUCCESS)tts.setLanguage(new Locale("ru","RU"));});game=new Game(this);setContentView(game);}
    @Override protected void onPause(){if(game!=null)game.save();super.onPause();}
    @Override protected void onDestroy(){if(tts!=null)tts.shutdown();super.onDestroy();}
    class Game extends View {
        Canvas c; Paint p=new Paint(); float sx,sy,playerX=150,playerY=0,vy=0,camera=0; boolean left,right,jump,ground=true,menu=false,dialog=false;
        int zone=0,money=150000,rep=0,level=1,phones=0,day=1,selected=0; String message="Добро пожаловать в PHONE EMPIRE!";
        long last=System.nanoTime(); final int ink=0xff10131d,white=0xfff5f0df,gold=0xffffcc65,cyan=0xff76d7dc;
        String[] zones={"МАСТЕРСКАЯ","УЛИЦА","МАГАЗИН","ЗАВОД"};
        String[] people={"МИРА — дизайнер","ПОКУПАТЕЛЬ","ПРОДАВЕЦ","ИНЖЕНЕР"};
        Game(Context x){super(x);setLayerType(View.LAYER_TYPE_SOFTWARE,null);android.content.SharedPreferences s=getSharedPreferences("phoneempire",0);money=s.getInt("money",150000);rep=s.getInt("rep",0);level=s.getInt("level",1);phones=s.getInt("phones",0);zone=s.getInt("zone",0);playerX=s.getFloat("x",150);playerY=0;}
        void save(){getSharedPreferences("phoneempire",0).edit().putInt("money",money).putInt("rep",rep).putInt("level",level).putInt("phones",phones).putInt("zone",zone).putFloat("x",playerX).apply();}
        void rect(float x,float y,float w,float h,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRect(x,y,x+w,y+h,p);}
        void outline(float x,float y,float w,float h,int color,int sw){p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(sw);c.drawRect(x,y,x+w,y+h,p);p.setStyle(Paint.Style.FILL);}
        void txt(String s,float x,float y,int size,int color){p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.create("monospace",Typeface.BOLD));c.drawText(s,x,y,p);}
        void center(String s,float x,float y,int size,int color){p.setTypeface(Typeface.create("monospace",Typeface.BOLD));p.setTextSize(size);txt(s,x-p.measureText(s)/2,y,size,color);}
        @Override protected void onDraw(Canvas cv){super.onDraw(cv);c=cv;sx=getWidth()/960f;sy=getHeight()/540f;c.save();c.scale(sx,sy);long now=System.nanoTime();float dt=Math.min(.04f,(now-last)/1e9f);last=now;
            if(!menu&&!dialog){if(left)playerX-=190*dt;if(right)playerX+=190*dt;if(jump&&ground){vy=-430;ground=false;}vy+=1000*dt;playerY+=vy*dt;if(playerY>0){playerY=0;vy=0;ground=true;}if(playerX<24){if(zone>0){zone--;playerX=900;}else playerX=24;}if(playerX>936){if(zone<3){zone++;playerX=55;}else playerX=936;}}
            camera=Math.max(0,Math.min(760,playerX-400));drawWorld();drawPlayer(400,430+playerY);drawHud();drawControls();if(menu)drawMenu();if(dialog)drawDialog();c.restore();postInvalidateDelayed(16);}
        void drawWorld(){int sky=zone==1?0xff52677d:zone==2?0xff323b50:zone==3?0xff303744:0xff242a3b;rect(0,0,960,540,sky);
            // distant skyline and pixel clouds
            for(int i=0;i<12;i++){float x=i*115-camera*.28f;rect(x,180+(i%3)*24,75,250-(i%3)*30,zone==1?0xff38495c:0xff34394b);rect(x+12,200+(i%3)*24,12,18,0xffd4b77a);rect(x+38,200+(i%3)*24,12,18,0xffd4b77a);}
            rect(0,400,960,140,0xff34343d);rect(0,430,960,110,0xff55515a);for(int i=0;i<20;i++)rect(i*55-camera%55,465,28,5,0xffd5bd82);
            if(zone==0){building(70-camera,205,290,225,"PIRTECH LAB",0xff79604e);rect(145-camera,300,130,100,0xff262d3b);rect(160-camera,312,100,70,0xff75aab1);bench(520-camera,350);}
            if(zone==1){building(120-camera,220,230,210,"ГОРОД",0xff5d6479);building(510-camera,245,260,185,"АРЕНДА",0xff6a584e);rect(650-camera,300,75,100,0xff242b3c);}
            if(zone==2){building(110-camera,205,300,225,"PIRTECH STORE",0xff596b73);rect(150-camera,315,220,85,0xff243b48);for(int i=0;i<3;i++)phone(180+i*65-camera,340);}
            if(zone==3){building(80-camera,190,440,240,"PIRTECH FACTORY",0xff60616b);for(int i=0;i<3;i++){rect(120+i*125-camera,315,75,45,0xff35434a);phone(140+i*125-camera,320);}}
            // props, people and interaction marker
            if(zone==0){npc(650-camera,430,0xffd8ad83);txt("МИРА",630-camera,365,12,white);}
            if(zone==1){npc(440-camera,430,0xffb8c4d3);txt("КЛИЕНТ",415-camera,365,12,white);}
            if(zone==2){npc(480-camera,430,0xffc79b79);txt("ПРОДАВЕЦ",450-camera,365,12,white);}
            if(zone==3){npc(650-camera,430,0xffa5b5a8);txt("ИНЖЕНЕР",620-camera,365,12,white);}
            rect(0,510,960,30,0xff171a25);txt("← НАЗАД",20,530,12,white);txt("ПЕРЕХОДЫ МЕЖДУ ЛОКАЦИЯМИ",650,530,11,0xffc8c2b5);
        }
        void building(float x,float y,float w,float h,String label,int col){rect(x,y,w,h,col);rect(x-8,y-12,w+16,18,0xff292d3a);for(int i=0;i<4;i++)for(int j=0;j<2;j++){rect(x+18+i*65,y+35+j*65,35,32,0xffa9c6c4);outline(x+18+i*65,y+35+j*65,35,32,0xff283542,3);}rect(x+w/2-48,y+h-75,96,75,0xff30303b);center(label,x+w/2,y+22,13,white);}
        void bench(float x,float y){rect(x,y,170,18,0xffa9784f);rect(x+12,y+18,12,55,0xff59483c);rect(x+145,y+18,12,55,0xff59483c);phone(x+60,y-20);}
        void phone(float x,float y){rect(x,y,26,42,0xff111723);outline(x,y,26,42,0xffd2d6d2,2);rect(x+4,y+5,18,27,0xff66aab5);rect(x+10,y+35,6,3,white);}
        void npc(float x,float y,int shirt){rect(x-12,y-62,24,25,0xffd7a982);rect(x-15,y-37,30,31,shirt);rect(x-13,y-6,10,8,0xff252936);rect(x+3,y-6,10,8,0xff252936);rect(x-9,y-52,4,4,ink);rect(x+5,y-52,4,4,ink);}
        void drawPlayer(float x,float y){int step=(int)(System.currentTimeMillis()/150%2);rect(x-13,y-62,26,25,0xffd9b18b);rect(x-15,y-37,30,32,0xff426c9b);rect(x-12,y-5,10,10,0xff202431);rect(x+2,y-5,10,10,0xff202431);rect(x-9,y-52,4,4,ink);rect(x+5,y-52,4,4,ink);rect(x-15,y-65,30,6,0xff25212a);rect(x-13,y-64,26,4,0xffd7bd75);if(step==1){rect(x-15,y-3,10,4,0xffe0c4a0);}}
        void drawHud(){rect(0,0,960,55,0xe9111420);txt("PHONE EMPIRE",22,25,17,white);txt(zones[zone],22,45,11,gold);txt("₽ "+money,510,25,16,0xff9de0a9);txt("РЕПУТАЦИЯ "+rep,690,25,12,white);txt("ТЕЛЕФОНЫ "+phones+"  •  УР. "+level,690,44,11,cyan);}
        void drawControls(){rect(24,442,75,75,0x55333344);rect(110,442,75,75,0x55333344);rect(795,442,65,75,0x55333344);rect(872,442,65,75,0x884e7180);center("◀",61,490,28,white);center("▶",147,490,28,white);center("↑",827,490,26,white);center("E",904,490,26,white);}
        void drawMenu(){rect(100,75,760,365,0xf0161b29);outline(100,75,760,365,gold,4);txt("PIRTECH — ПАНЕЛЬ КОМПАНИИ",130,112,20,white);txt("ДЕНЬ "+day+"     КАПИТАЛ: ₽ "+money,130,143,14,cyan);
            String[] opts={"1. Разработать новую модель смартфона  (- ₽ 25 000)","2. Выпустить партию из 10 телефонов  (- ₽ 40 000)","3. Продать партию и получить прибыль  (+ ₽ 75 000)","4. Улучшить мастерскую / завод  (- ₽ 60 000)","5. Реклама бренда  (- ₽ 15 000)","6. Завершить рабочий день"};
            for(int i=0;i<opts.length;i++){rect(125,165+i*38,710,31,i==selected?0xff46546a:0xff262d3c);txt(opts[i],142,186+i*38,13,white);}txt("Нажми на строку, чтобы выполнить действие.  E — закрыть",130,414,12,gold);}
        void drawDialog(){rect(35,350,890,145,0xf0161b29);outline(35,350,890,145,white,3);txt(people[zone],60,382,17,gold);txt(message,60,421,15,white);txt("НАЖМИ E, ЧТОБЫ ЗАКРЫТЬ",650,470,11,cyan);}
        void interact(){if(dialog){dialog=false;return;}if(menu){int cost=0;switch(selected){case 0:cost=25000;if(money>=cost){money-=cost;level++;rep+=2;message="Новая модель PIRTECH Nova готова к производству."; }break;case 1:cost=40000;if(money>=cost){money-=cost;phones+=10;message="Партия из 10 смартфонов собрана."; }break;case 2:if(phones>0){money+=phones*7500;rep+=phones;phones=0;message="Продажи прошли успешно! Репутация растёт.";}else message="Сначала выпусти партию смартфонов.";break;case 3:cost=60000;if(money>=cost){money-=cost;level++;message="Оборудование улучшено. Производство стало эффективнее.";}break;case 4:cost=15000;if(money>=cost){money-=cost;rep+=8;message="Рекламная кампания повысила известность бренда.";}break;case 5:day++;money+=level*5000;message="День завершён. Доход от бизнеса: ₽ "+(level*5000);menu=false;save();return;}if(cost>0&&money<cost)message="Недостаточно денег для этого действия.";save();dialog=true;menu=false;return;}
            if(zone==0){menu=true;selected=0;}else if(zone==1){message="Покупатель: «Когда появится ваш новый смартфон? Я слежу за PIRTECH!»";dialog=true;rep++;}else if(zone==2){menu=true;selected=2;}else{menu=true;selected=3;}invalidate();}
        @Override public boolean onTouchEvent(android.view.MotionEvent e){float x=e.getX()/sx,y=e.getY()/sy;int a=e.getActionMasked();if(a==MotionEvent.ACTION_DOWN){if(menu){if(y>160&&y<400){selected=Math.max(0,Math.min(5,(int)((y-165)/38)));invalidate();}else if(y>405){menu=false;}return true;}if(dialog){dialog=false;invalidate();return true;}if(y>435){if(x<100)left=true;else if(x<200)right=true;else if(x>860)interact();else if(x>780)jump=true;}else if(x>300&&y>300){interact();}invalidate();return true;}if(a==MotionEvent.ACTION_MOVE){return true;}if(a==MotionEvent.ACTION_UP){left=right=jump=false;return true;}return true;}
    }
}