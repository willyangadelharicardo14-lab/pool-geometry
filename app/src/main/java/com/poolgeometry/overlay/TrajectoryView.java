package com.poolgeometry.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.graphics.Color;
import android.graphics.RectF;

public class TrajectoryView extends View {
    private final Paint line=new Paint(3),glow=new Paint(3);
    private int tableW=254,tableH=127,segments=7;
    private float p1x=.22f,p1y=.68f,p2x=.43f,p2y=.32f;
    public TrajectoryView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    public void setConfig(int w,int h,int n,float ax,float ay,float bx,float by){
        tableW=w;tableH=h;segments=n;p1x=ax;p1y=ay;p2x=bx;p2y=by;postInvalidate();
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float sw=getWidth(),sh=getHeight();
        if(sw<=0||sh<=0)return;
        // Retângulo proporcional centralizado; será alinhado à mesa real na etapa de calibração.
        float margin=sw*.055f,availW=sw-2*margin,availH=sh*.82f;
        float ratio=(float)tableW/(float)tableH;
        float tw=availW,th=tw/ratio;
        if(th>availH){th=availH;tw=th*ratio;}
        float left=(sw-tw)/2f,top=(sh-th)/2f;
        RectF bounds=new RectF(left,top,left+tw,top+th);
        float x1=clamp(p1x*sw,left,bounds.right),y1=clamp(p1y*sh,top,bounds.bottom);
        float x2=clamp(p2x*sw,left,bounds.right),y2=clamp(p2y*sh,top,bounds.bottom);
        float dx=x2-x1,dy=y2-y1;
        if(Math.hypot(dx,dy)<1){dx=1;dy=0;}
        Path path=new Path();path.moveTo(x1,y1);
        float x=x1,y=y1;
        for(int i=0;i<segments;i++){
            float tx=Float.POSITIVE_INFINITY,ty=Float.POSITIVE_INFINITY;
            if(dx>0)tx=(bounds.right-x)/dx;else if(dx<0)tx=(bounds.left-x)/dx;
            if(dy>0)ty=(bounds.bottom-y)/dy;else if(dy<0)ty=(bounds.top-y)/dy;
            float t=Math.min(tx>0.0001f?tx:Float.POSITIVE_INFINITY,ty>0.0001f?ty:Float.POSITIVE_INFINITY);
            if(!Float.isFinite(t))break;
            x+=dx*t;y+=dy*t;x=clamp(x,bounds.left,bounds.right);y=clamp(y,bounds.top,bounds.bottom);
            path.lineTo(x,y);
            boolean hitX=Math.abs(tx-t)<0.001f,hitY=Math.abs(ty-t)<0.001f;
            if(hitX)dx=-dx;if(hitY)dy=-dy;
        }
        glow.setColor(0x667C3AED);glow.setStyle(Paint.Style.STROKE);glow.setStrokeWidth(8);glow.setStrokeCap(Paint.Cap.ROUND);glow.setStrokeJoin(Paint.Join.ROUND);
        line.setColor(0xFFF0ABFC);line.setStyle(Paint.Style.STROKE);line.setStrokeWidth(3.2f);line.setStrokeCap(Paint.Cap.ROUND);line.setStrokeJoin(Paint.Join.ROUND);
        c.drawPath(path,glow);c.drawPath(path,line);
    }
    private float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}
}
