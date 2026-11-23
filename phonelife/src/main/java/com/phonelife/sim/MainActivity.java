package com.phonelife.sim;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.opengl.GLSurfaceView;
import android.opengl.GLES20;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Locale;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.opengl.GLSurfaceView.Renderer;

public class MainActivity extends Activity {
    GameView game;
    TextView money, status, story;
    int cash=12500, stock=0, parts=0, phones=0, day=1, rep=0;
    String phase="Мастерская";
    void refresh(){
        money.setText("₽ "+cash+"   |   День "+day+"   |   Репутация "+rep);
        status.setText("Склад: телефоны "+phones+"  •  детали "+parts+"  •  заказы "+stock+"     "+phase);
    }
    void toast(String s){ Toast.makeText(this,s,Toast.LENGTH_SHORT).show(); }
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(5894|1024|512);
        FrameLayout root=new FrameLayout(this);
        game=new GameView(this); root.addView(game);
        LinearLayout hud=new LinearLayout(this); hud.setOrientation(LinearLayout.VERTICAL);
        hud.setPadding(22,14,22,10); hud.setBackgroundColor(0xAA101A2A);
        money=new TextView(this); money.setTextColor(Color.WHITE); money.setTextSize(17); money.setTypeface(null,1);
        status=new TextView(this); status.setTextColor(0xFFD4E4F5); status.setTextSize(12);
        story=new TextView(this); story.setTextColor(0xFFFFD98A); story.setTextSize(13);
        hud.addView(money); hud.addView(status); hud.addView(story);
        FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP); root.addView(hud,hp);
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER); bar.setPadding(8,7,8,7); bar.setBackgroundColor(0xDD101A2A);
        add(bar,"Детали −₽2 000",()->{if(cash>=2000){cash-=2000;parts+=3;toast("Куплено 3 набора деталей");}else toast("Не хватает рублей");});
        add(bar,"Б/У телефон −₽4 500",()->{if(cash>=4500){cash-=4500;phones++;game.setMode(1);toast("Телефон добавлен на склад");}else toast("Не хватает рублей");});
        add(bar,"Собрать +₽3 200",()->{if(parts>0&&phones>0){parts--;phones--;cash+=3200;rep++;game.setMode(2);toast("Телефон собран и проверен! +₽3 200");}else toast("Нужны телефон и детали");});
        add(bar,"Продать +₽5 500",()->{if(phones>0){phones--;cash+=5500;rep++;game.setMode(3);toast("Покупатель забрал телефон");}else toast("На складе нет телефонов");});
        add(bar,"Самокат",()->{phase="Поездка по району";day++;game.setMode(4);story.setText("Знакомься с жителями района и ищи выгодные обмены.");toast("Ты приехал на точку обмена");});
        add(bar,"Обмен",()->{if(phones>0){phones--;cash+=6200;rep+=2;game.setMode(5);toast("Удачный обмен! Получено ₽6 200");}else toast("Нужен телефон для обмена");});
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM); root.addView(bar,bp);
        setContentView(root);
        story.setText("Глава 1. Старт: дядя оставил тебе маленькую мастерскую. Заработай первые ₽50 000!");
        refresh();
    }
    void add(LinearLayout l,String label,Runnable r){
        Button b=new Button(this); b.setText(label); b.setTextSize(10); b.setAllCaps(false);
        b.setTextColor(Color.WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF246B83));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,58,1); p.setMargins(3,0,3,0); l.addView(b,p);
        b.setOnClickListener(v->{r.run();refresh();if(cash>=50000)story.setText("Глава 2. Ты накопил ₽50 000! Пора открыть вторую точку.");});
    }
    static class GameView extends GLSurfaceView {
        Scene scene;
        GameView(Activity a){super(a);setEGLContextClientVersion(2);scene=new Scene();setRenderer(scene);setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);}
        void setMode(int m){scene.mode=m;}
    }
    static class Scene implements Renderer {
        int program, pos, color, mvp; FloatBuffer cube;
        float[] proj=new float[16], view=new float[16], model=new float[16], vp=new float[16], out=new float[16];
        long start=System.currentTimeMillis(); int mode=0;
        final String vs="attribute vec4 aPos; uniform mat4 uMvp; void main(){gl_Position=uMvp*aPos;}";
        final String fs="precision mediump float; uniform vec4 uColor; void main(){gl_FragColor=uColor;}";
        float[] vertices={
        -1,-1,-1, 1,-1,-1, 1,1,-1, -1,-1,-1,1,1,-1,-1,1,-1,
        -1,-1,1,1,1,1,1,-1,1,-1,-1,1,-1,1,1,1,1,1,
        -1,-1,-1,-1,1,-1,-1,1,1,-1,-1,-1,-1,1,1,-1,-1,1,
        1,-1,-1,1,-1,1,1,1,1,1,-1,-1,1,1,1,1,1,-1,
        -1,1,-1,1,1,-1,1,1,1,-1,1,-1,1,1,1,-1,1,1,
        -1,-1,-1,-1,-1,1,1,-1,1,-1,-1,-1,1,-1,1,1,-1,-1};
        @Override public void onSurfaceCreated(GL10 gl,EGLConfig c){
            GLES20.glClearColor(.035f,.075f,.12f,1); GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            int v=shader(GLES20.GL_VERTEX_SHADER,vs), f=shader(GLES20.GL_FRAGMENT_SHADER,fs);
            program=GLES20.glCreateProgram();GLES20.glAttachShader(program,v);GLES20.glAttachShader(program,f);GLES20.glLinkProgram(program);
            pos=GLES20.glGetAttribLocation(program,"aPos");color=GLES20.glGetUniformLocation(program,"uColor");mvp=GLES20.glGetUniformLocation(program,"uMvp");
            ByteBuffer bb=ByteBuffer.allocateDirect(vertices.length*4).order(ByteOrder.nativeOrder());cube=bb.asFloatBuffer();cube.put(vertices).position(0);
        }
        int shader(int type,String src){int s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);return s;}
        @Override public void onSurfaceChanged(GL10 gl,int w,int h){GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(proj,0,45,(float)w/h,.1f,100f);Matrix.setLookAtM(view,0,0,7,10,0,0,0,0,1,0);Matrix.multiplyMM(vp,0,proj,0,view,0);}
        void box(float x,float y,float z,float sx,float sy,float sz,float rot,float r,float g,float b){
            Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);Matrix.rotateM(model,0,rot,0,1,0);Matrix.scaleM(model,0,sx,sy,sz);Matrix.multiplyMM(out,0,vp,0,model,0);
            GLES20.glUniformMatrix4fv(mvp,1,false,out,0);GLES20.glUniform4f(color,r,g,b,1);cube.position(0);GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,0,cube);GLES20.glEnableVertexAttribArray(pos);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,36);
        }
        @Override public void onDrawFrame(GL10 gl){
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);GLES20.glUseProgram(program);
            float t=(System.currentTimeMillis()-start)/1000f;
            box(0,-1.35f,0,7,.12f,5,0,.12f,.2f,.25f);
            for(int i=-3;i<=3;i++) box(i*.9f,-1.18f,-2,.025f,.01f,4,0,.25f,.35f,.4f);
            // workshop bench
            box(-1.8f,-.55f,-.2f,1.7f,.16f,.85f,0,.35f,.22f,.13f);
            box(-3,-1,-.2f,.12f,.55f,.12f,0,.22f,.14f,.1f);box(-.6f,-1,-.2f,.12f,.55f,.12f,0,.22f,.14f,.1f);
            // phone chassis + display + camera island
            float bob=(float)Math.sin(t*2)*.035f;
            box(-1.7f,.2f,.0f,.48f,.82f,.09f,mode==2?(float)Math.sin(t*5)*8:0,.08f,.1f,.13f);
            box(-1.7f,.2f,.105f,.43f,.75f,.018f,0,.1f,.55f,.72f);
            box(-1.7f,.9f,.13f,.12f,.025f,.02f,0,.02f,.03f,.04f);
            if(mode==2){box(-.55f,.35f,.3f,.18f,.06f,.18f,(t*90)%360,.9f,.65f,.12f);box(-.1f,.55f,.3f,.15f,.06f,.15f,(t*120)%360,.2f,.8f,.65f);}
            // stylized NPC: head, torso, arms, legs
            float nx=2.0f+(mode==4?(float)Math.sin(t)*.7f:0);
            box(nx,.65f,-.5f,.24f,.24f,.22f,0,.78f,.55f,.38f);
            box(nx,.05f,-.5f,.32f,.48f,.22f,0,.15f,.48f,.72f);
            box(nx-.38f,.08f,-.5f,.12f,.42f,.12f,(float)Math.sin(t*3)*12,.78f,.55f,.38f);
            box(nx+.38f,.08f,-.5f,.12f,.42f,.12f,-(float)Math.sin(t*3)*12,.78f,.55f,.38f);
            box(nx-.14f,-.65f,-.5f,.13f,.28f,.14f,0,.12f,.14f,.2f);box(nx+.14f,-.65f,-.5f,.13f,.28f,.14f,0,.12f,.14f,.2f);
            // electric scooter prop
            box(3.6f,-.85f,.8f,.75f,.07f,.16f,0,.12f,.18f,.22f);
            box(3.1f,-.95f,.8f,.18f,.18f,.18f,0,.04f,.05f,.06f);box(4.1f,-.95f,.8f,.18f,.18f,.18f,0,.04f,.05f,.06f);
            box(3.65f,-.25f,.8f,.06f,.62f,.06f,0,.15f,.7f,.55f);box(3.65f,.08f,.8f,.4f,.05f,.06f,0,.1f,.12f,.15f);
            // floating parts when assembling
            if(mode==2){float lift=(float)Math.abs(Math.sin(t*4))*.35f;box(-1.7f,1.2f+lift,.0f,.26f,.08f,.04f,(t*90)%360,.9f,.7f,.2f);box(-1.7f,-.65f-lift,.0f,.28f,.07f,.04f,(t*90)%360,.2f,.8f,.7f);}
        }
    }
}
