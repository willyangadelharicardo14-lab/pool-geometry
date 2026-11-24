package com.poolgeometry.overlay;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;

public class OverlayService extends Service {
    private WindowManager wm;
    private TrajectoryView drawing;
    private WindowManager.LayoutParams drawingParams;
    private TextView handle1, handle2, bubble;
    private WindowManager.LayoutParams h1Params, h2Params, bubbleParams;
    private PopupWindow menu;
    private SharedPreferences prefs;
    private int tableW=254, tableH=127, segments=7;
    private float p1x=.22f,p1y=.68f,p2x=.43f,p2y=.32f;
    private boolean menuVisible=false;

    @Override public void onCreate() {
        super.onCreate();
        prefs=getSharedPreferences("pool_geometry",MODE_PRIVATE);
        tableW=prefs.getInt("tableW",254);tableH=prefs.getInt("tableH",127);segments=prefs.getInt("segments",7);
        p1x=prefs.getFloat("p1x",.22f);p1y=prefs.getFloat("p1y",.68f);
        p2x=prefs.getFloat("p2x",.43f);p2y=prefs.getFloat("p2y",.32f);
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        startAsForeground();
        createDrawingLayer();
        createPointHandles();
        createBubble();
        updateAll();
    }
    private void startAsForeground(){
        String id="pool_geometry_overlay";
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(id,"Pool Geometry",NotificationManager.IMPORTANCE_LOW));
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,id):new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_menu_compass).setContentTitle("Pool Geometry ativo")
                .setContentText("Toque no botão PG para abrir as configurações.")
                .setOngoing(true);
        if(Build.VERSION.SDK_INT>=34)startForeground(7,b.build(),ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(7,b.build());
    }
    private WindowManager.LayoutParams overlayParams(int w,int h,int flags){
        return new WindowManager.LayoutParams(w,h,
                Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE,
                flags,PixelFormat.TRANSLUCENT);
    }
    private void createDrawingLayer(){
        drawing=new TrajectoryView(this);
        drawingParams=overlayParams(-1,-1,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        drawingParams.gravity=Gravity.TOP|Gravity.START;
        wm.addView(drawing,drawingParams);
    }
    private TextView point(String label){
        TextView v=new TextView(this);v.setText(label);v.setTextColor(Color.WHITE);v.setTextSize(13);
        v.setGravity(Gravity.CENTER);v.setBackground(circle(0xFF9333EA));
        v.setOnTouchListener(new View.OnTouchListener(){
            float sx,sy;int ox,oy;boolean first;
            @Override public boolean onTouch(View view,MotionEvent e){
                if(e.getAction()==MotionEvent.ACTION_DOWN){sx=e.getRawX();sy=e.getRawY();WindowManager.LayoutParams lp=(WindowManager.LayoutParams)view.getLayoutParams();ox=lp.x;oy=lp.y;first=true;return true;}
                if(e.getAction()==MotionEvent.ACTION_MOVE){WindowManager.LayoutParams lp=(WindowManager.LayoutParams)view.getLayoutParams();lp.x=ox+(int)(e.getRawX()-sx);lp.y=oy+(int)(e.getRawY()-sy);wm.updateViewLayout(view,lp);updatePointFromHandle(view);updateDrawing();return true;}
                if(e.getAction()==MotionEvent.ACTION_UP){save();return true;}return true;
            }
        });
        return v;
    }
    private void createPointHandles(){
        handle1=point("1");handle2=point("2");
        int flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        h1Params=overlayParams(dp(36),dp(36),flags);h2Params=overlayParams(dp(36),dp(36),flags);
        h1Params.gravity=Gravity.TOP|Gravity.START;h2Params.gravity=Gravity.TOP|Gravity.START;
        wm.addView(handle1,h1Params);wm.addView(handle2,h2Params);
    }
    private void createBubble(){
        bubble=new TextView(this);bubble.setText("PG");bubble.setTextColor(Color.WHITE);bubble.setTextSize(17);
        bubble.setGravity(Gravity.CENTER);bubble.setBackground(circle(0xFF7E22CE));bubble.setElevation(dp(8));
        bubble.setOnClickListener(v->showMenu());
        bubble.setOnTouchListener(new View.OnTouchListener(){
            float sx,sy;int ox,oy;boolean moved;
            @Override public boolean onTouch(View v,MotionEvent e){
                if(e.getAction()==MotionEvent.ACTION_DOWN){sx=e.getRawX();sy=e.getRawY();ox=bubbleParams.x;oy=bubbleParams.y;moved=false;return true;}
                if(e.getAction()==MotionEvent.ACTION_MOVE){float dx=e.getRawX()-sx,dy=e.getRawY()-sy;if(Math.hypot(dx,dy)>8)moved=true;if(moved){bubbleParams.x=ox+(int)dx;bubbleParams.y=oy+(int)dy;wm.updateViewLayout(bubble,bubbleParams);}return true;}
                if(e.getAction()==MotionEvent.ACTION_UP){if(!moved)showMenu();return true;}return true;
            }
        });
        bubbleParams=overlayParams(dp(50),dp(50),WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        bubbleParams.gravity=Gravity.TOP|Gravity.START;bubbleParams.x=dp(18);bubbleParams.y=dp(110);
        wm.addView(bubble,bubbleParams);
    }
    private GradientDrawable circle(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setShape(GradientDrawable.OVAL);d.setStroke(dp(1),0x99FFFFFF);return d;}
    private GradientDrawable panelBg(){GradientDrawable d=new GradientDrawable();d.setColor(0xF21B1424);d.setCornerRadius(dp(16));d.setStroke(dp(1),0x665C466E);return d;}
    private void showMenu(){
        if(menu!=null&&menu.isShowing()){menu.dismiss();menu=null;return;}
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(12),dp(14),dp(12));box.setBackground(panelBg());
        TextView title=new TextView(this);title.setText("POOL GEOMETRY");title.setTextColor(Color.WHITE);title.setTextSize(14);title.setPadding(0,0,0,dp(8));box.addView(title);
        EditText w=field(String.valueOf(tableW));EditText h=field(String.valueOf(tableH));
        box.addView(label("Largura da mesa (cm)"));box.addView(w);
        box.addView(label("Altura da mesa (cm)"));box.addView(h);
        TextView count=label("Quantidade de linhas: "+segments);box.addView(count);
        SeekBar seek=new SeekBar(this);seek.setMax(29);seek.setProgress(segments-1);seek.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFC084FC));box.addView(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean u){segments=p+1;count.setText("Quantidade de linhas: "+segments);updateDrawing();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){save();}});
        Button apply=new Button(this);apply.setText("Aplicar");apply.setAllCaps(false);apply.setOnClickListener(v->{
            try{tableW=Math.max(50,Math.min(1000,Integer.parseInt(w.getText().toString())));tableH=Math.max(50,Math.min(1000,Integer.parseInt(h.getText().toString())));save();updateDrawing();}catch(Exception ignored){}
            if(menu!=null){menu.dismiss();menu=null;}
        });box.addView(apply);
        menu=new PopupWindow(box,dp(238),-2,true);menu.setBackgroundDrawable(panelBg());menu.setOutsideTouchable(true);
        menu.setElevation(dp(12));menu.showAtLocation(bubble,Gravity.TOP|Gravity.START,bubbleParams.x,bubbleParams.y+dp(58));
    }
    private EditText field(String value){EditText e=new EditText(this);e.setSingleLine(true);e.setText(value);e.setTextColor(Color.WHITE);e.setTextSize(14);e.setPadding(dp(9),dp(5),dp(9),dp(5));e.setBackgroundColor(0xFF30263A);return e;}
    private TextView label(String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(0xFFE9DFF0);t.setTextSize(12);t.setPadding(0,dp(7),0,dp(3));return t;}
    private int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private void updatePointFromHandle(View v){
        int[] pos=new int[2];v.getLocationOnScreen(pos);
        float x=pos[0]+v.getWidth()/2f,y=pos[1]+v.getHeight()/2f;
        float sw=getResources().getDisplayMetrics().widthPixels,sh=getResources().getDisplayMetrics().heightPixels;
        if(v==handle1){p1x=x/sw;p1y=y/sh;}else{p2x=x/sw;p2y=y/sh;}
        save();
    }
    private void updateAll(){
        int sw=getResources().getDisplayMetrics().widthPixels,sh=getResources().getDisplayMetrics().heightPixels;
        h1Params.x=(int)(p1x*sw)-dp(18);h1Params.y=(int)(p1y*sh)-dp(18);
        h2Params.x=(int)(p2x*sw)-dp(18);h2Params.y=(int)(p2y*sh)-dp(18);
        wm.updateViewLayout(handle1,h1Params);wm.updateViewLayout(handle2,h2Params);updateDrawing();
    }
    private void updateDrawing(){if(drawing!=null){drawing.setConfig(tableW,tableH,segments,p1x,p1y,p2x,p2y);}}
    private void save(){prefs.edit().putInt("tableW",tableW).putInt("tableH",tableH).putInt("segments",segments)
            .putFloat("p1x",p1x).putFloat("p1y",p1y).putFloat("p2x",p2x).putFloat("p2y",p2y).apply();}
    @Override public int onStartCommand(Intent i,int f,int id){return START_STICKY;}
    @Override public void onDestroy(){
        if(menu!=null&&menu.isShowing())menu.dismiss();
        try{if(drawing!=null)wm.removeView(drawing);if(handle1!=null)wm.removeView(handle1);if(handle2!=null)wm.removeView(handle2);if(bubble!=null)wm.removeView(bubble);}catch(Exception ignored){}
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i){return null;}
}
