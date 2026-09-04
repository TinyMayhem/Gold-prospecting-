package com.tinymayhem.goldsolver;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

public class CalibrationView extends View {
    private final Bitmap bitmap;
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF imageRect = new RectF();
    private RectF board;
    private final RectF[] slots = new RectF[3];

    private int activeKind = 0; // 1 board, 2 slot
    private int activeSlot = -1;
    private int activeHandle = -1; // 0 TL,1 TR,2 BL,3 BR,4 move
    private float lastX, lastY;

    public CalibrationView(Context c, Bitmap b, SharedPreferences prefs) {
        super(c);
        bitmap = b;
        board = CalibrationPrefs.getBoard(prefs);
        for (int i=0;i<3;i++) slots[i] = CalibrationPrefs.getSlot(prefs, i);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(2));
        gridPaint.setColor(Color.rgb(78, 235, 186));
        gridPaint.setShadowLayer(dp(3), 0, 0, Color.BLACK);

        slotPaint.setStyle(Paint.Style.STROKE);
        slotPaint.setStrokeWidth(dp(2));
        slotPaint.setColor(Color.rgb(255, 207, 92));
        slotPaint.setShadowLayer(dp(3), 0, 0, Color.BLACK);

        handlePaint.setStyle(Paint.Style.FILL);
        handlePaint.setColor(Color.WHITE);
        handlePaint.setShadowLayer(dp(3), 0, dp(1), Color.BLACK);

        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(dp(13));
        labelPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setShadowLayer(dp(3), 0, dp(1), Color.BLACK);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        fitImage();
        c.drawColor(Color.BLACK);
        c.drawBitmap(bitmap, null, imageRect, bitmapPaint);

        RectF br = toScreen(board);
        c.drawRect(br, gridPaint);
        for (int i=1;i<8;i++) {
            float x = br.left + br.width()*i/8f;
            float y = br.top + br.height()*i/8f;
            c.drawLine(x, br.top, x, br.bottom, gridPaint);
            c.drawLine(br.left, y, br.right, y, gridPaint);
        }
        drawHandles(c, br, gridPaint.getColor());
        c.drawText("8 x 8 BOARD", br.centerX(), br.top-dp(7), labelPaint);

        String[] names={"LEFT PIECE","MIDDLE PIECE","RIGHT PIECE"};
        for (int i=0;i<3;i++) {
            RectF sr=toScreen(slots[i]);
            c.drawRoundRect(sr, dp(8), dp(8), slotPaint);
            drawCorner(c, sr.left, sr.top, slotPaint.getColor());
            drawCorner(c, sr.right, sr.bottom, slotPaint.getColor());
            c.drawText(names[i], sr.centerX(), sr.top-dp(7), labelPaint);
        }
    }

    private void fitImage() {
        float vw=getWidth(), vh=getHeight();
        if (vw<=0 || vh<=0) return;
        float scale=Math.min(vw/bitmap.getWidth(), vh/bitmap.getHeight());
        float w=bitmap.getWidth()*scale, h=bitmap.getHeight()*scale;
        imageRect.set((vw-w)/2f, (vh-h)/2f, (vw+w)/2f, (vh+h)/2f);
    }

    private RectF toScreen(RectF n) {
        return new RectF(
                imageRect.left+n.left*imageRect.width(),
                imageRect.top+n.top*imageRect.height(),
                imageRect.left+n.right*imageRect.width(),
                imageRect.top+n.bottom*imageRect.height());
    }

    private void drawHandles(Canvas c, RectF r, int color) {
        drawCorner(c,r.left,r.top,color); drawCorner(c,r.right,r.top,color);
        drawCorner(c,r.left,r.bottom,color); drawCorner(c,r.right,r.bottom,color);
    }

    private void drawCorner(Canvas c,float x,float y,int color) {
        handlePaint.setColor(color);
        c.drawCircle(x,y,dp(9),handlePaint);
        handlePaint.setColor(Color.WHITE);
        c.drawCircle(x,y,dp(4),handlePaint);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        fitImage();
        if (!imageRect.contains(e.getX(),e.getY())) return true;
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX=e.getX(); lastY=e.getY();
                chooseTarget(lastX,lastY);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (activeKind!=0) {
                    float dx=(e.getX()-lastX)/Math.max(1f,imageRect.width());
                    float dy=(e.getY()-lastY)/Math.max(1f,imageRect.height());
                    if (activeKind==1) updateRect(board,activeHandle,dx,dy,0.38f,0.26f);
                    else updateRect(slots[activeSlot],activeHandle,dx,dy,0.10f,0.07f);
                    lastX=e.getX(); lastY=e.getY();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activeKind=0; activeSlot=-1; activeHandle=-1;
                return true;
        }
        return true;
    }

    private void chooseTarget(float x,float y) {
        float hit=dp(28), best=Float.MAX_VALUE;
        RectF br=toScreen(board);
        float[][] bh={{br.left,br.top},{br.right,br.top},{br.left,br.bottom},{br.right,br.bottom}};
        for(int i=0;i<4;i++){
            float d=dist(x,y,bh[i][0],bh[i][1]);
            if(d<hit && d<best){best=d;activeKind=1;activeHandle=i;}
        }
        if(activeKind==0 && br.contains(x,y)){activeKind=1;activeHandle=4;best=0;}

        for(int s=0;s<3;s++){
            RectF sr=toScreen(slots[s]);
            float d0=dist(x,y,sr.left,sr.top), d3=dist(x,y,sr.right,sr.bottom);
            if(d0<hit && d0<best){best=d0;activeKind=2;activeSlot=s;activeHandle=0;}
            if(d3<hit && d3<best){best=d3;activeKind=2;activeSlot=s;activeHandle=3;}
            if(activeKind==0 && sr.contains(x,y)){activeKind=2;activeSlot=s;activeHandle=4;}
        }
    }

    private void updateRect(RectF r,int h,float dx,float dy,float minW,float minH){
        if(h==4){
            float nx=clampDelta(dx,-r.left,1f-r.right);
            float ny=clampDelta(dy,-r.top,1f-r.bottom);
            r.offset(nx,ny); return;
        }
        if(h==0 || h==2) r.left=clamp(r.left+dx,0f,r.right-minW);
        if(h==1 || h==3) r.right=clamp(r.right+dx,r.left+minW,1f);
        if(h==0 || h==1) r.top=clamp(r.top+dy,0f,r.bottom-minH);
        if(h==2 || h==3) r.bottom=clamp(r.bottom+dy,r.top+minH,1f);
    }

    private float clampDelta(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
    private float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
    private float dist(float x,float y,float a,float b){return (float)Math.hypot(x-a,y-b);}

    public void resetDefaults(){
        board=CalibrationPrefs.defaultBoard();
        for(int i=0;i<3;i++) slots[i]=CalibrationPrefs.defaultSlot(i);
        invalidate();
    }

    public void saveCalibration(SharedPreferences prefs){
        CalibrationPrefs.save(prefs,new RectF(board),new RectF[]{new RectF(slots[0]),new RectF(slots[1]),new RectF(slots[2])});
    }

    private float dp(float x){return x*getResources().getDisplayMetrics().density;}
}
