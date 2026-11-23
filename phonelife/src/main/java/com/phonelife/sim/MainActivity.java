package com.phonelife.sim;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import android.content.res.ColorStateList;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.opengl.GLES20;
import android.opengl.Matrix;
import java.nio.*;
import java.util.Locale;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends Activity {
    private GameView game;
    private TextView top, quest, prompt;
    private int money=18000, parts=0, usedPhones=0, screens=0, batteries=0, finishedPhones=0, reputation=0, day=1;
    private int storyStep=0, upgrades=0, orders=0, totalSales=0, completedRepairs=0;
    private boolean outside=false, scooterRiding=false;
    private SharedPreferences save;
    private final String[] quests={
        "ЗАДАНИЕ 1/5 · Осмотри мастерскую и купи первые детали",
        "ЗАДАНИЕ 2/5 · Купи подержанный телефон и собери его на верстаке",
        "ЗАДАНИЕ 3/5 · Продай готовый телефон первому клиенту",
        "ЗАДАНИЕ 4/5 · Съезди на самокате к торговцу и найди редкую деталь",
        "ЗАДАНИЕ 5/5 · Выполни заказ и открой собственную мастерскую"
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);
        FrameLayout root=new FrameLayout(this);
        game=new GameView(this);
        root.addView(game);
        LinearLayout hud=new LinearLayout(this);
        hud.setOrientation(LinearLayout.VERTICAL);
        hud.setPadding(18,10,18,10);
        hud.setBackgroundColor(0xB8101928);
        top=new TextView(this); top.setTextColor(Color.WHITE); top.setTextSize(16); top.setTypeface(null,1);
        quest=new TextView(this); quest.setTextColor(0xFFFFD58A); quest.setTextSize(12);
        prompt=new TextView(this); prompt.setTextColor(0xFFE3F5FF); prompt.setTextSize(12);
        hud.addView(top); hud.addView(quest); hud.addView(prompt);
        FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);
        root.addView(hud,hp);

        LinearLayout left=new LinearLayout(this); left.setOrientation(LinearLayout.VERTICAL); left.setPadding(10,8,10,8);
        left.setGravity(Gravity.CENTER);
        MovePad pad=new MovePad(this,(x,y)->game.setMove(x,y));
        left.addView(pad,new LinearLayout.LayoutParams(150,130));
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(170,150,Gravity.BOTTOM|Gravity.LEFT);
        lp.setMargins(12,0,0,12); root.addView(left,lp);

        LinearLayout right=new LinearLayout(this); right.setOrientation(LinearLayout.VERTICAL); right.setGravity(Gravity.BOTTOM|Gravity.RIGHT);
        Button act=makeButton("ДЕЙСТВИЕ",0xFFDB9B45); right.addView(act);
        act.setOnClickListener(v->interact());
        Button shop=makeButton("МАГАЗИН",0xFF247B91); right.addView(shop);
        shop.setOnClickListener(v->openShop());
        Button bag=makeButton("РЮКЗАК",0xFF315D83); right.addView(bag);
        bag.setOnClickListener(v->openInventory());
        Button tasks=makeButton("ИСТОРИЯ",0xFF46556F); right.addView(tasks);
        tasks.setOnClickListener(v->showStory());
        FrameLayout.LayoutParams rp=new FrameLayout.LayoutParams(160,-2,Gravity.BOTTOM|Gravity.RIGHT);
        rp.setMargins(0,0,14,12); root.addView(right,rp);

        TextView hint=new TextView(this); hint.setText("ЛЕВАЯ СТОРОНА — ХОДЬБА   •   ПРОВЕДИ ПО ЭКРАНУ — ОБЗОР");
        hint.setTextColor(0xAAFFFFFF); hint.setTextSize(10); hint.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams htp=new FrameLayout.LayoutParams(-1,28,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        htp.bottomMargin=8; root.addView(hint,htp);
        game.setOnTouchListener((v,e)->{
            if(e.getX()<getResources().getDisplayMetrics().widthPixels*.38f) return false;
            if(e.getAction()==MotionEvent.ACTION_DOWN){game.lookStart(e.getX(),e.getY());return true;}
            if(e.getAction()==MotionEvent.ACTION_MOVE){game.lookMove(e.getX(),e.getY());return true;}
            if(e.getAction()==MotionEvent.ACTION_UP){game.lookEnd();return true;}
            return true;
        });
        setContentView(root);
        save=getSharedPreferences("phone_life_save",MODE_PRIVATE);
        loadGame();
        updateHud();
        Toast.makeText(this,"Добро пожаловать в PHONE LIFE. Осмотрись в мастерской.",Toast.LENGTH_LONG).show();
    }

    @Override protected void onPause(){saveGame();super.onPause();}

    private Button makeButton(String text,int color){
        Button b=new Button(this); b.setText(text); b.setTextSize(11); b.setAllCaps(false); b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(ColorStateList.valueOf(color));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,48); p.setMargins(3,3,3,3); b.setLayoutParams(p);
        return b;
    }
    private void updateHud(){
        top.setText(String.format(Locale.US,"₽ %,d     ДЕНЬ %d     РЕПУТАЦИЯ %d     ЗАКАЗЫ %d",money,day,reputation,orders));
        quest.setText(storyStep>=5?"ИСТОРИЯ ЗАВЕРШЕНА · Мастерская открыта. Продолжай развивать бизнес.":quests[Math.min(storyStep,4)]);
        String near=game==null?"":game.getTargetHint();
        prompt.setText(near);
    }
    private void spend(int amount){money-=amount;}
    private void saveGame(){if(save==null)return;save.edit().putInt("money",money).putInt("parts",parts).putInt("usedPhones",usedPhones).putInt("screens",screens).putInt("batteries",batteries).putInt("finishedPhones",finishedPhones).putInt("reputation",reputation).putInt("day",day).putInt("storyStep",storyStep).putInt("upgrades",upgrades).putInt("orders",orders).putInt("totalSales",totalSales).putInt("completedRepairs",completedRepairs).putBoolean("outside",outside).apply();}
    private void loadGame(){money=save.getInt("money",18000);parts=save.getInt("parts",0);usedPhones=save.getInt("usedPhones",0);screens=save.getInt("screens",0);batteries=save.getInt("batteries",0);finishedPhones=save.getInt("finishedPhones",0);reputation=save.getInt("reputation",0);day=save.getInt("day",1);storyStep=Math.max(0,Math.min(5,save.getInt("storyStep",0)));upgrades=save.getInt("upgrades",0);orders=save.getInt("orders",0);totalSales=save.getInt("totalSales",0);completedRepairs=save.getInt("completedRepairs",0);outside=save.getBoolean("outside",false);game.setOutside(outside);}
    private void interact(){
        String target=game.getTarget();
        if("bench".equals(target)){
            if(usedPhones>0 && parts>0){
                usedPhones--; parts--; finishedPhones++; money+=900; reputation++;
                game.setAction(1);
                Toast.makeText(this,"Телефон собран и прошёл базовую проверку. +₽900",Toast.LENGTH_SHORT).show();
                if(storyStep<2) storyStep=2;
            } else if(orders>0 && parts>0){
                orders--; parts--; money+=1600; reputation+=2; day++;
                game.setAction(1);
                Toast.makeText(this,"Заказ на ремонт выполнен. Клиент оплатил ₽1 600.",Toast.LENGTH_LONG).show();
            } else if(usedPhones>0) Toast.makeText(this,"Для сборки нужны набор деталей, экран AMOLED и аккумулятор. Проверь магазин и рюкзак.",Toast.LENGTH_LONG).show();
            else if(orders>0) Toast.makeText(this,"Для ремонта нужен набор деталей. Купи его в магазине.",Toast.LENGTH_SHORT).show();
            else Toast.makeText(this,"Сначала купи подержанный телефон или возьми заказ на ремонт.",Toast.LENGTH_SHORT).show();
        } else if("customer".equals(target)){
            if(finishedPhones>0){finishedPhones--;money+=6500;reputation+=2;totalSales++;game.setAction(2);Toast.makeText(this,"Клиент доволен. +₽6 500",Toast.LENGTH_SHORT).show();storyStep=Math.max(storyStep,3);}
            else Toast.makeText(this,"Клиент: «Ищу хороший восстановленный смартфон». Сначала собери один.",Toast.LENGTH_LONG).show();
        } else if("shelf".equals(target)){
            openShop();
        } else if("door".equals(target)){
            outside=!outside; day++; game.setOutside(outside);
            if(outside){storyStep=Math.max(storyStep,3);Toast.makeText(this,"Ты вышел на улицу. Найди торговца у остановки.",Toast.LENGTH_LONG).show();}
            else Toast.makeText(this,"Ты вернулся в мастерскую.",Toast.LENGTH_SHORT).show();
        } else if("scooter".equals(target)){
            scooterRiding=!scooterRiding;game.setRiding(scooterRiding);
            Toast.makeText(this,scooterRiding?"Самокат включён. Используй джойстик для поездки.":"Ты слез с самоката.",Toast.LENGTH_SHORT).show();
        } else if("vendor".equals(target)){
            if(outside){if(money>=3000){spend(3000);parts++;batteries++;screens++;reputation++;storyStep=4;Toast.makeText(this,"Торговец продал редкую батарею и набор деталей.",Toast.LENGTH_LONG).show();}else Toast.makeText(this,"Торговец: «Нужно ₽3 000 за комплект».",Toast.LENGTH_SHORT).show();}
        } else {
            Toast.makeText(this,"Подойди ближе к верстаку, клиенту, стеллажу или двери.",Toast.LENGTH_SHORT).show();
        }
        saveGame();
        updateHud();
    }
    private void openShop(){
        String[] items={"Набор деталей ×3 — ₽1 500","Подержанный смартфон — ₽4 500","Экран AMOLED — ₽2 500","Аккумулятор — ₽1 200","Улучшить мастерскую — ₽8 000","Заказ на срочный ремонт — ₽900"};
        new AlertDialog.Builder(this).setTitle("МАГАЗИН КОМПЛЕКТУЮЩИХ").setItems(items,(d,w)->{
            int cost=w==0?1500:w==1?4500:w==2?2500:w==3?1200:w==4?8000:900;
            if(money<cost){Toast.makeText(this,"Не хватает рублей.",Toast.LENGTH_SHORT).show();return;}
            spend(cost);
            if(w==0)parts+=3; if(w==1)usedPhones++; if(w==2)screens++; if(w==3)batteries++; if(w==4){upgrades++;reputation++;if(storyStep>=4){storyStep=5;Toast.makeText(this,"Мастерская улучшена! Ты открыл собственный сервис. История пройдена.",Toast.LENGTH_LONG).show();}} if(w==5){orders++;parts++;}\n            if(w==0 && storyStep==0) storyStep=1;
            if(storyStep==0)storyStep=1;
            saveGame();updateHud();
        }).setNegativeButton("ЗАКРЫТЬ",null).show();
    }
    private void openInventory(){
        String s="Подержанные телефоны: "+usedPhones+"\nГотовые телефоны: "+finishedPhones+
            "\nНаборы деталей: "+parts+"\nЭкраны: "+screens+"\nАккумуляторы: "+batteries+
            "\nУлучшения мастерской: "+upgrades+"\nАктивные заказы: "+orders+"\nПродано телефонов: "+totalSales+"\nРемонтов выполнено: "+completedRepairs+"\nРепутация: "+reputation+"\n\nПрогресс сохраняется автоматически.";
        new AlertDialog.Builder(this).setTitle("РЮКЗАК И СКЛАД").setMessage(s).setPositiveButton("ПОНЯТНО",null).show();
    }
    private void showStory(){
        String s="Глава 1. НАСЛЕДСТВО\nДядя оставил тебе старую мастерскую и записку: «Телефоны можно чинить, а доверие клиентов нужно заслужить».\n\n"+
        "Глава 2. ПЕРВЫЙ ЗАКАЗ\nКупи подержанный смартфон, детали и восстанови его на верстаке.\n\n"+
        "Глава 3. СЛОВО ПО РАЙОНУ\nПродай готовый телефон клиенту и заработай репутацию.\n\n"+
        "Глава 4. ПОЕЗДКА\nВыйди на улицу, найди торговца и купи редкую батарею.\n\n"+
        "Глава 5. СВОЁ ДЕЛО\nВыполняй заказы, копи деньги и улучши мастерскую за ₽8 000.\n\n"+(storyStep>=5?"ФИНАЛ: Ты открыл собственный сервис! Теперь можно продолжать свободную игру.":"История ещё продолжается.");
        new AlertDialog.Builder(this).setTitle("ИСТОРИЯ PHONE LIFE").setMessage(s).setPositiveButton("ПРОДОЛЖИТЬ",null).show();
    }

    interface MoveListener { void move(float x,float y); }
    static class MovePad extends View {
        Paint p=new Paint(3); MoveListener listener; float cx,cy,dx,dy; boolean active;
        MovePad(Activity a,MoveListener l){super(a);listener=l;}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float x=getWidth()/2f,y=getHeight()/2f,r=52;
            p.setColor(0x55334455);c.drawCircle(x,y,r,p);p.setColor(0xAA7FB8D2);c.drawCircle(active?x+dx:x,active?y+dy:y,24,p);
            p.setColor(0x99FFFFFF);p.setTextSize(10);c.drawText("MOVE",x-15,y+r+16,p);}
        @Override public boolean onTouchEvent(MotionEvent e){float x=e.getX()-getWidth()/2f,y=e.getY()-getHeight()/2f;
            if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE){active=true;float n=(float)Math.sqrt(x*x+y*y);float max=48;if(n>max){x=x/n*max;y=y/n*max;}dx=x;dy=y;listener.move(x/max,-y/max);invalidate();return true;}
            active=false;dx=dy=0;listener.move(0,0);invalidate();return true;}
    }

    static class GameView extends GLSurfaceView {
        Scene scene;
        float lastX,lastY;
        GameView(Activity a){super(a);setEGLContextClientVersion(2);scene=new Scene();setRenderer(scene);setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);}
        void setMove(float x,float y){scene.moveX=x;scene.moveY=y;}
        void lookStart(float x,float y){lastX=x;lastY=y;}
        void lookMove(float x,float y){float dx=x-lastX,dy=y-lastY;lastX=x;lastY=y;scene.turn(dx*.22f,dy*.18f);}
        void lookEnd(){}
        String getTarget(){return scene.target();}
        String getTargetHint(){return scene.targetHint();}
        void setAction(int a){scene.action=a;}
        void setOutside(boolean b){scene.outside=b;scene.riding=false;scene.x=0;scene.z=b?10.5f:3.5f;scene.yaw=b?180:0;}
        void setRiding(boolean b){scene.riding=b;}
    }

    static class Scene implements GLSurfaceView.Renderer {
        int program,pos,color,mvp; FloatBuffer cube;
        float[] proj=new float[16],view=new float[16],model=new float[16],vp=new float[16],out=new float[16];
        volatile float moveX=0,moveY=0; volatile int action=0; volatile boolean outside=false, riding=false;
        float x=0,y=1.62f,z=3.5f,yaw=0,pitch=0; long last=System.nanoTime(); float anim=0;
        final String vs="attribute vec4 aPos; uniform mat4 uMvp; void main(){gl_Position=uMvp*aPos;}";
        final String fs="precision mediump float; uniform vec4 uColor; void main(){gl_FragColor=uColor;}";
        final float[] vertices={
        -1,-1,-1,1,-1,-1,1,1,-1,-1,-1,-1,1,1,-1,-1,1,-1,
        -1,-1,1,1,1,1,1,-1,1,-1,-1,1,-1,1,1,1,1,1,
        -1,-1,-1,-1,1,-1,-1,1,1,-1,-1,-1,-1,1,1,-1,-1,1,
        1,-1,-1,1,-1,1,1,1,1,1,-1,-1,1,1,1,1,1,-1,
        -1,1,-1,1,1,-1,1,1,1,-1,1,-1,1,1,1,-1,1,1,
        -1,-1,-1,-1,-1,1,1,-1,1,-1,-1,-1,1,-1,1,1,-1,-1};
        @Override public void onSurfaceCreated(GL10 gl,EGLConfig c){
            GLES20.glClearColor(.035f,.065f,.095f,1);GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            int v=shader(GLES20.GL_VERTEX_SHADER,vs),f=shader(GLES20.GL_FRAGMENT_SHADER,fs);
            program=GLES20.glCreateProgram();GLES20.glAttachShader(program,v);GLES20.glAttachShader(program,f);GLES20.glLinkProgram(program);
            pos=GLES20.glGetAttribLocation(program,"aPos");color=GLES20.glGetUniformLocation(program,"uColor");mvp=GLES20.glGetUniformLocation(program,"uMvp");
            ByteBuffer bb=ByteBuffer.allocateDirect(vertices.length*4).order(ByteOrder.nativeOrder());cube=bb.asFloatBuffer();cube.put(vertices).position(0);
        }
        int shader(int type,String src){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);return s;}
        @Override public void onSurfaceChanged(GL10 gl,int w,int h){GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(proj,0,70,(float)w/Math.max(1,h),.08f,100f);}
        void turn(float dx,float dy){yaw+=dx;pitch=Math.max(-55,Math.min(55,pitch+dy));}
        String target(){
            String best="";float bd=99;
            String[] names=outside?new String[]{"scooter","vendor","door"}:new String[]{"bench","shelf","customer","door"};
            float[][] pts=outside?new float[][]{{0,0,12},{0,0,15},{0,0,5}}:new float[][]{{0,0,-2.3f},{-3.6f,0,-2.5f},{2.7f,0,-2.4f},{4.1f,0,1.4f}};
            float dx=(float)Math.sin(Math.toRadians(yaw)),dz=-(float)Math.cos(Math.toRadians(yaw));
            for(int i=0;i<names.length;i++){float vx=pts[i][0]-x,vz=pts[i][2]-z;float d=(float)Math.sqrt(vx*vx+vz*vz);float dot=(vx*dx+vz*dz)/Math.max(.001f,d);
                if(d<3.0f&&dot>.25f&&d<bd){bd=d;best=names[i];}}
            return best;
        }
        String targetHint(){String t=target();if("bench".equals(t))return "ВЕРСТАК · собрать или проверить смартфон";if("shelf".equals(t))return "СТЕЛЛАЖ · купить телефоны и комплектующие";if("customer".equals(t))return "КЛИЕНТ · поговорить и продать готовый телефон";if("door".equals(t))return outside?"ВОЗВРАЩЕНИЕ · мастерская":"ВЫХОД · улица и поездка";if("scooter".equals(t))return "САМОКАТ · сесть и поехать";if("vendor".equals(t))return "ТОРГОВЕЦ · редкие детали";return "Осмотрись: найди верстак, стеллаж и клиента.";}
        void box(float bx,float by,float bz,float sx,float sy,float sz,float rot,float r,float g,float b){
            Matrix.setIdentityM(model,0);Matrix.translateM(model,0,bx,by,bz);Matrix.rotateM(model,0,rot,0,1,0);Matrix.scaleM(model,0,sx,sy,sz);Matrix.multiplyMM(out,0,vp,0,model,0);
            GLES20.glUniformMatrix4fv(mvp,1,false,out,0);GLES20.glUniform4f(color,r,g,b,1);cube.position(0);GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,0,cube);GLES20.glEnableVertexAttribArray(pos);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,36);
        }
        void phone(float px,float py,float pz,float scale,float tint){
            box(px,py,pz,.32f*scale,.62f*scale,.055f*scale,0,.035f,.045f,.055f);
            box(px,py,pz+.058f*scale,.285f*scale,.57f*scale,.012f*scale,0,.10f,.48f*tint,.68f*tint);
            box(px,py+.49f*scale,pz+.073f*scale,.07f*scale,.018f*scale,.01f,0,.01f,.02f,.025f);
            box(px-.22f*scale,py+.47f*scale,pz+.03f*scale,.045f*scale,.06f*scale,.035f*scale,0,.12f,.13f,.15f);
        }
        @Override public void onDrawFrame(GL10 gl){
            long now=System.nanoTime();float dt=Math.min(.05f,(now-last)/1e9f);last=now;anim+=dt;
            float sy=(float)Math.sin(Math.toRadians(yaw)),cy=(float)Math.cos(Math.toRadians(yaw));
            float speed=riding?7.0f:3.2f;x+=(sy*moveY+cy*moveX)*dt*speed;z+=(-cy*moveY+sy*moveX)*dt*speed;
            if(outside){x=Math.max(-7,Math.min(7,x));z=Math.max(6,Math.min(32,z));}else{x=Math.max(-4.45f,Math.min(4.45f,x));z=Math.max(-4.45f,Math.min(4.45f,z));}
            float py=(float)Math.toRadians(pitch);float lookX=(float)(Math.sin(Math.toRadians(yaw))*Math.cos(py)),lookY=(float)Math.sin(py),lookZ=(float)(-Math.cos(Math.toRadians(yaw))*Math.cos(py));
            Matrix.setLookAtM(view,0,x,y,z,x+lookX,y+lookY,z+lookZ,0,1,0);Matrix.multiplyMM(vp,0,proj,0,view,0);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);GLES20.glUseProgram(program);
            if(!outside) drawWorkshop(); else drawStreet();
        }
        void drawWorkshop(){
            // floor and walls
            box(0,-.14f,0,5,.14f,5,0,.22f,.25f,.28f);
            box(0,2.7f,-5,5,2.7f,.12f,0,.16f,.21f,.27f);
            box(-5,2.7f,0,.12f,2.7f,5,0,.14f,.19f,.24f);
            box(5,2.7f,-2.7f,.12f,2.7f,2.3f,0,.14f,.19f,.24f);
            box(5,2.7f,4.2f,.12f,2.7f,.8f,0,.14f,.19f,.24f);
            // ceiling lights
            box(-2,5,-2,.8f,.04f,.18f,0,.9f,.86f,.68f);box(2,5,-2,.8f,.04f,.18f,0,.9f,.86f,.68f);
            // workbench
            box(0,.92f,-2.35f,1.65f,.12f,.72f,0,.42f,.25f,.14f);
            box(-1.45f,.42f,-2.35f,.12f,.5f,.12f,0,.24f,.15f,.1f);box(1.45f,.42f,-2.35f,.12f,.5f,.12f,0,.24f,.15f,.1f);
            phone(0,1.12f,-2.25f, .8f,1);
            box(-.7f,1.1f,-2.0f,.22f,.035f,.12f,0,.8f,.72f,.5f);
            // shelves with boxed devices
            box(-3.75f,1.5f,-2.5f,.18f,1.5f,.55f,0,.32f,.2f,.12f);
            for(int i=0;i<3;i++){float yy=.55f+i*.9f;box(-3.48f,yy,-2.5f,.45f,.05f,.62f,0,.42f,.29f,.18f);phone(-3.5f,yy+.36f,-2.5f,.35f,i==1?.7f:1);}
            // customer
            person(2.65f,0,-2.5f,.85f,.26f,.58f);
            // chair and posters
            box(2.5f,.55f,-.5f,.45f,.06f,.4f,0,.2f,.28f,.3f);box(2.5f,.25f,-.8f,.06f,.3f,.06f,0,.12f,.16f,.18f);
            box(-1.8f,3.1f,-4.82f,.9f,.55f,.035f,0,.08f,.34f,.4f);
            box(-1.8f,3.1f,-4.78f,.68f,.07f,.02f,0,.85f,.88f,.8f);
            // door
            box(4.25f,1.35f,1.35f,.08f,1.35f,.9f,0,.28f,.16f,.09f);
            box(4.15f,1.4f,1.35f,.04f,1.25f,.75f,0,.12f,.33f,.38f);
            // tool drawers and small parts
            box(-2.4f,.45f,-.2f,.75f,.42f,.5f,0,.22f,.25f,.29f);
            box(-2.4f,.9f,-.2f,.78f,.06f,.53f,0,.42f,.26f,.14f);
            for(int i=0;i<4;i++)box(-2.65f+i*.16f,.98f,-.2f,.045f,.03f,.08f,anim*20,.9f,.7f,.2f);
            // window on right wall
            box(4.91f,2.8f,-1.4f,.025f,.75f,1.15f,0,.1f,.35f,.55f);
            box(4.88f,2.8f,-1.4f,.04f,.04f,1.2f,0,.72f,.8f,.82f);
            box(4.88f,2.8f,-1.4f,.04f,.75f,.04f,0,.72f,.8f,.82f);
            if(action==1){float a=(float)Math.abs(Math.sin(anim*5));box(0,1.8f+a*.4f,-2.2f,.25f,.08f,.12f,anim*80,.2f,.9f,.8f);box(.45f,1.6f+a*.3f,-2.2f,.2f,.06f,.1f,-anim*80,.9f,.7f,.25f);}
        }
        void person(float px,float ground,float pz,float sc,float r,float b){
            float sway=(float)Math.sin(anim*2)*.035f;
            box(px,ground+1.55f*sc,pz,.26f*sc,.27f*sc,.22f*sc,0,.76f,.55f,.38f);
            box(px,ground+.95f*sc,pz,.38f*sc,.52f*sc,.25f*sc,0,r,.22f,b);
            box(px-.43f*sc,ground+.98f*sc,pz,.12f*sc,.42f*sc,.12f*sc,sway*100,.76f,.55f,.38f);
            box(px+.43f*sc,ground+.98f*sc,pz,.12f*sc,.42f*sc,.12f*sc,-sway*100,.76f,.55f,.38f);
            box(px-.14f*sc,ground+.35f*sc,pz,.13f*sc,.35f*sc,.14f*sc,0,.12f,.15f,.2f);
            box(px+.14f*sc,ground+.35f*sc,pz,.13f*sc,.35f*sc,.14f*sc,0,.12f,.15f,.2f);
        }
        void drawStreet(){
            box(0,-.16f,11,8,.16f,9,0,.23f,.27f,.29f);
            box(0,0,7,8,.02f,2,0,.28f,.3f,.31f);
            for(int i=-6;i<=6;i+=3){box(i,.02f,11,.045f,.015f,8,0,.85f,.75f,.3f);}
            // buildings
            box(-6,2.4f,11,1.4f,2.4f,5,0,.34f,.38f,.43f);
            box(6,2.8f,13,1.4f,2.8f,4,0,.42f,.3f,.25f);
            for(int i=0;i<3;i++){box(-6,2+i*.9f,8.95f,.5f,.25f,.04f,0,.2f,.55f,.68f);box(6,2+i*.9f,10.95f,.5f,.25f,.04f,0,.2f,.55f,.68f);}
            // scooter and vendor
            box(0,.48f,12f,.75f,.07f,.18f,0,.1f,.13f,.16f);
            box(-.65f,.25f,12f,.22f,.22f,.22f,0,.035f,.04f,.05f);box(.65f,.25f,12f,.22f,.22f,.22f,0,.035f,.04f,.05f);
            box(0,.95f,12f,.06f,.52f,.06f,0,.1f,.65f,.52f);box(0,1.22f,12f,.42f,.05f,.06f,0,.1f,.12f,.13f);
            if(riding){box(0,1.05f,.55f,.42f,.06f,.12f,0,.08f,.12f,.14f);box(-.32f,.9f,.55f,.035f,.25f,.035f,0,.12f,.16f,.18f);box(.32f,.9f,.55f,.035f,.25f,.035f,0,.12f,.16f,.18f);}
            person(0,0,15,.9f,.18f,.62f);
            box(0,.9f,14.5f,.9f,.08f,.45f,0,.4f,.23f,.12f);
            phone(0,1.08f,14.5f,.55f,.8f);
            // street lamps
            for(int i=-1;i<=1;i+=2){box(i*4,2,10,.07f,2,.07f,0,.16f,.18f,.2f);box(i*4,4,10,.35f,.06f,.25f,0,.9f,.82f,.6f);}
        }
    }
}