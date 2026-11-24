
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
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;

public class OverlayService extends Service {
    private WindowManager wm;
    private TrajectoryView drawing;
    private TextView handle1, handle2, bubble;
    private TextView[] frameHandles = new TextView[4];
    private WindowManager.LayoutParams h1Params, h2Params, bubbleParams;
    private WindowManager.LayoutParams[] frameParams = new WindowManager.LayoutParams[4];
    private PopupWindow menu;
    private SharedPreferences prefs;

    private int tableW = 254, tableH = 127, segments = 7;
    private float p1x=.22f, p1y=.68f, p2x=.43f, p2y=.32f;

    // Limites conectados do retângulo
    private float left=.08f, top=.20f, right=.92f, bottom=.80f;

    @Override
    public void onCreate() {
        super.onCreate();

        prefs = getSharedPreferences("pool_geometry", MODE_PRIVATE);

        tableW = prefs.getInt("tableW",254);
        tableH = prefs.getInt("tableH",127);
        segments = prefs.getInt("segments",7);

        p1x = prefs.getFloat("p1x",.22f);
        p1y = prefs.getFloat("p1y",.68f);
        p2x = prefs.getFloat("p2x",.43f);
        p2y = prefs.getFloat("p2y",.32f);

        left = prefs.getFloat("rectLeft",.08f);
        top = prefs.getFloat("rectTop",.20f);
        right = prefs.getFloat("rectRight",.92f);
        bottom = prefs.getFloat("rectBottom",.80f);

        wm = (WindowManager)getSystemService(WINDOW_SERVICE);

        startAsForeground();
        createDrawingLayer();
        createPointHandles();
        createFrameHandles();
        createBubble();
        updateAll();
    }

    private void startAsForeground() {
        String id = "pool_geometry_overlay";

        NotificationManager nm =
            (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                new NotificationChannel(
                    id,
                    "Pool Geometry",
                    NotificationManager.IMPORTANCE_LOW
                )
            );
        }

        Notification.Builder b =
            Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this,id)
            : new Notification.Builder(this);

        b.setSmallIcon(android.R.drawable.ic_menu_compass)
         .setContentTitle("Pool Geometry ativo")
         .setContentText("PG abre as configurações. Arraste os cantos para calibrar.")
         .setOngoing(true);

        if(Build.VERSION.SDK_INT >= 34) {
            startForeground(
                7,
                b.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            );
        } else {
            startForeground(7,b.build());
        }
    }

    private WindowManager.LayoutParams overlayParams(int w,int h,int flags) {
        return new WindowManager.LayoutParams(
            w,
            h,
            Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE,
            flags,
            PixelFormat.TRANSLUCENT
        );
    }

    private void createDrawingLayer() {
        drawing = new TrajectoryView(this);

        WindowManager.LayoutParams p = overlayParams(
            -1,
            -1,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        p.gravity = Gravity.TOP | Gravity.START;
        wm.addView(drawing,p);
    }

    private TextView point(String label,int color) {
        TextView v = new TextView(this);
        v.setText(label);
        v.setTextColor(Color.WHITE);
        v.setTextSize(13);
        v.setGravity(Gravity.CENTER);
        v.setBackground(circle(color));

        v.setOnTouchListener(new View.OnTouchListener() {
            float sx,sy;
            int ox,oy;

            @Override
            public boolean onTouch(View view,MotionEvent e) {
                if(e.getAction() == MotionEvent.ACTION_DOWN) {
                    sx=e.getRawX();
                    sy=e.getRawY();

                    WindowManager.LayoutParams lp =
                        (WindowManager.LayoutParams)view.getLayoutParams();

                    ox=lp.x;
                    oy=lp.y;
                    return true;
                }

                if(e.getAction() == MotionEvent.ACTION_MOVE) {
                    WindowManager.LayoutParams lp =
                        (WindowManager.LayoutParams)view.getLayoutParams();

                    lp.x=ox+(int)(e.getRawX()-sx);
                    lp.y=oy+(int)(e.getRawY()-sy);

                    wm.updateViewLayout(view,lp);
                    updatePointFromHandle(view);
                    updateDrawing();
                    return true;
                }

                if(e.getAction() == MotionEvent.ACTION_UP) {
                    save();
                    return true;
                }

                return true;
            }
        });

        return v;
    }

    private void createPointHandles() {
        handle1 = point("1",0xFF9333EA);
        handle2 = point("2",0xFF9333EA);

        int flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        h1Params=overlayParams(dp(36),dp(36),flags);
        h2Params=overlayParams(dp(36),dp(36),flags);

        h1Params.gravity=Gravity.TOP|Gravity.START;
        h2Params.gravity=Gravity.TOP|Gravity.START;

        wm.addView(handle1,h1Params);
        wm.addView(handle2,h2Params);
    }

    // Quatro cantos conectados
    private void createFrameHandles() {
        String[] labels={"┏","┓","┗","┛"};

        for(int i=0;i<4;i++) {
            final int corner=i;

            TextView v=new TextView(this);
            v.setText(labels[i]);
            v.setTextColor(0xFF20E6A0);
            v.setTextSize(30);
            v.setGravity(Gravity.CENTER);
            v.setShadowLayer(dp(3),0,0,0xFF000000);

            frameHandles[i]=v;

            frameParams[i]=overlayParams(
                dp(30),
                dp(30),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            );

            frameParams[i].gravity=Gravity.TOP|Gravity.START;
            wm.addView(v,frameParams[i]);

            v.setOnTouchListener(new View.OnTouchListener() {
                float sx,sy;
                int ox,oy;

                @Override
                public boolean onTouch(View view,MotionEvent e) {

                    if(e.getAction()==MotionEvent.ACTION_DOWN) {
                        sx=e.getRawX();
                        sy=e.getRawY();

                        ox=frameParams[corner].x;
                        oy=frameParams[corner].y;
                        return true;
                    }

                    if(e.getAction()==MotionEvent.ACTION_MOVE) {

                        int sw=getResources().getDisplayMetrics().widthPixels;
                        int sh=getResources().getDisplayMetrics().heightPixels;

                        float nx=(ox+e.getRawX()-sx+dp(15))/sw;
                        float ny=(oy+e.getRawY()-sy+dp(15))/sh;

                        if(corner==0) {
                            left=clamp(nx,.02f,right-.12f);
                            top=clamp(ny,.05f,bottom-.12f);
                        }

                        if(corner==1) {
                            right=clamp(nx,left+.12f,.98f);
                            top=clamp(ny,.05f,bottom-.12f);
                        }

                        if(corner==2) {
                            left=clamp(nx,.02f,right-.12f);
                            bottom=clamp(ny,top+.12f,.95f);
                        }

                        if(corner==3) {
                            right=clamp(nx,left+.12f,.98f);
                            bottom=clamp(ny,top+.12f,.95f);
                        }

                        updateFrameHandles();
                        updateDrawing();
                        return true;
                    }

                    if(e.getAction()==MotionEvent.ACTION_UP) {
                        updateApproxDimensions();
                        save();
                        return true;
                    }

                    return true;
                }
            });
        }
    }

    private void createBubble() {
        bubble=new TextView(this);
        bubble.setText("PG");
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(17);
        bubble.setGravity(Gravity.CENTER);
        bubble.setBackground(circle(0xFF7E22CE));
        bubble.setElevation(dp(8));

        bubble.setOnTouchListener(new View.OnTouchListener() {
            float sx,sy;
            int ox,oy;
            boolean moved;

            @Override
            public boolean onTouch(View v,MotionEvent e) {
                if(e.getAction()==MotionEvent.ACTION_DOWN) {
                    sx=e.getRawX();
                    sy=e.getRawY();
                    ox=bubbleParams.x;
                    oy=bubbleParams.y;
                    moved=false;
                    return true;
                }

                if(e.getAction()==MotionEvent.ACTION_MOVE) {
                    float dx=e.getRawX()-sx;
                    float dy=e.getRawY()-sy;

                    if(Math.hypot(dx,dy)>8) moved=true;

                    if(moved) {
                        bubbleParams.x=ox+(int)dx;
                        bubbleParams.y=oy+(int)dy;
                        wm.updateViewLayout(bubble,bubbleParams);
                    }
                    return true;
                }

                if(e.getAction()==MotionEvent.ACTION_UP) {
                    if(!moved) showMenu();
                    return true;
                }

                return true;
            }
        });

        bubbleParams=overlayParams(
            dp(50),
            dp(50),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        );

        bubbleParams.gravity=Gravity.TOP|Gravity.START;
        bubbleParams.x=dp(18);
        bubbleParams.y=dp(110);

        wm.addView(bubble,bubbleParams);
    }

    private GradientDrawable circle(int color) {
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);
        d.setShape(GradientDrawable.OVAL);
        d.setStroke(dp(1),0xCCFFFFFF);
        return d;
    }

    private GradientDrawable panelBg() {
        GradientDrawable d=new GradientDrawable();
        d.setColor(0xF21B1424);
        d.setCornerRadius(dp(16));
        d.setStroke(dp(1),0x665C466E);
        return d;
    }

    private void showMenu() {
        if(menu!=null && menu.isShowing()) {
            menu.dismiss();
            menu=null;
            return;
        }

        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14),dp(12),dp(14),dp(12));
        box.setBackground(panelBg());

        TextView title=new TextView(this);
        title.setText("POOL GEOMETRY");
        title.setTextColor(Color.WHITE);
        title.setTextSize(14);
        title.setPadding(0,0,0,dp(8));
        box.addView(title);

        TextView help=label("CALIBRAR MESA");
        box.addView(help);

        TextView instruction=label(
            "Arraste qualquer canto em L. O retângulo permanece conectado e os outros cantos acompanham o redimensionamento."
        );

        instruction.setPadding(0,dp(4),0,dp(10));
        box.addView(instruction);

        TextView size=label(
            "Proporção atual: "+tableW+" × "+tableH+" cm"
        );
        box.addView(size);

        TextView count=label("Quantidade de linhas: "+segments);
        box.addView(count);

        SeekBar seek=new SeekBar(this);
        seek.setMax(29);
        seek.setProgress(segments-1);
        box.addView(seek);

        seek.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar s,int p,boolean u) {
                    segments=p+1;
                    count.setText("Quantidade de linhas: "+segments);
                    updateDrawing();
                }

                public void onStartTrackingTouch(SeekBar s) {}

                public void onStopTrackingTouch(SeekBar s) {
                    save();
                }
            }
        );

        Button apply=new Button(this);
        apply.setText("Concluir calibração");
        apply.setAllCaps(false);

        apply.setOnClickListener(v -> {
            save();
            if(menu!=null) {
                menu.dismiss();
                menu=null;
            }
        });

        box.addView(apply);

        menu=new PopupWindow(box,dp(250),-2,true);
        menu.setBackgroundDrawable(panelBg());
        menu.setOutsideTouchable(true);
        menu.setElevation(dp(12));

        menu.showAtLocation(
            bubble,
            Gravity.TOP|Gravity.START,
            bubbleParams.x,
            bubbleParams.y+dp(58)
        );
    }

    private TextView label(String s) {
        TextView t=new TextView(this);
        t.setText(s);
        t.setTextColor(0xFFE9DFF0);
        t.setTextSize(12);
        t.setPadding(0,dp(7),0,dp(3));
        return t;
    }

    private int dp(float v) {
        return (int)(v*getResources().getDisplayMetrics().density+.5f);
    }

    private float clamp(float v,float min,float max) {
        return Math.max(min,Math.min(max,v));
    }

    private void updatePointFromHandle(View v) {
        int[] pos=new int[2];
        v.getLocationOnScreen(pos);

        float x=pos[0]+v.getWidth()/2f;
        float y=pos[1]+v.getHeight()/2f;

        float sw=getResources().getDisplayMetrics().widthPixels;
        float sh=getResources().getDisplayMetrics().heightPixels;

        if(v==handle1) {
            p1x=x/sw;
            p1y=y/sh;
        } else {
            p2x=x/sw;
            p2y=y/sh;
        }

        save();
    }

    private void updateApproxDimensions() {
        tableW=Math.max(50,Math.round((right-left)*1000));
        tableH=Math.max(50,Math.round((bottom-top)*1000));
    }

    private void updateFrameHandles() {
        int sw=getResources().getDisplayMetrics().widthPixels;
        int sh=getResources().getDisplayMetrics().heightPixels;
        int sz=dp(30);

        float[] xs={left,right,left,right};
        float[] ys={top,top,bottom,bottom};

        for(int i=0;i<4;i++) {
            frameParams[i].x=(int)(xs[i]*sw)-sz/2;
            frameParams[i].y=(int)(ys[i]*sh)-sz/2;

            wm.updateViewLayout(frameHandles[i],frameParams[i]);
        }
    }

    private void updateAll() {
        int sw=getResources().getDisplayMetrics().widthPixels;
        int sh=getResources().getDisplayMetrics().heightPixels;

        h1Params.x=(int)(p1x*sw)-dp(18);
        h1Params.y=(int)(p1y*sh)-dp(18);

        h2Params.x=(int)(p2x*sw)-dp(18);
        h2Params.y=(int)(p2y*sh)-dp(18);

        wm.updateViewLayout(handle1,h1Params);
        wm.updateViewLayout(handle2,h2Params);

        updateFrameHandles();
        updateDrawing();
    }

    private void updateDrawing() {
        if(drawing!=null) {
            drawing.setConfig(
                segments,
                p1x,p1y,
                p2x,p2y,
                left,top,right,bottom
            );
        }
    }

    private void save() {
        prefs.edit()
            .putInt("tableW",tableW)
            .putInt("tableH",tableH)
            .putInt("segments",segments)
            .putFloat("p1x",p1x)
            .putFloat("p1y",p1y)
            .putFloat("p2x",p2x)
            .putFloat("p2y",p2y)
            .putFloat("rectLeft",left)
            .putFloat("rectTop",top)
            .putFloat("rectRight",right)
            .putFloat("rectBottom",bottom)
            .apply();
    }

    @Override
    public int onStartCommand(Intent i,int f,int id) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if(menu!=null && menu.isShowing()) menu.dismiss();

        try {
            if(drawing!=null) wm.removeView(drawing);
            if(handle1!=null) wm.removeView(handle1);
            if(handle2!=null) wm.removeView(handle2);
            if(bubble!=null) wm.removeView(bubble);

            for(TextView v:frameHandles) {
                if(v!=null) wm.removeView(v);
            }
        } catch(Exception ignored) {}

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }
}
