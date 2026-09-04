package com.tinymayhem.goldsolver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

public class SolutionOverlayView extends View {
    private final Paint card = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint coord = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint note = new Paint(Paint.ANTI_ALIAS_FLAG);
    private JSONArray moves;
    private String banner = "";

    public SolutionOverlayView(Context c) {
        super(c);
        setBackgroundColor(Color.TRANSPARENT);
        card.setColor(Color.argb(220, 38, 27, 22));
        title.setColor(Color.WHITE);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setTextAlign(Paint.Align.CENTER);
        title.setTextSize(dp(12));
        coord.setColor(Color.WHITE);
        coord.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        coord.setTextAlign(Paint.Align.CENTER);
        coord.setTextSize(dp(16));
        coord.setShadowLayer(dp(3), 0, dp(1), Color.BLACK);
        note.setColor(Color.WHITE);
        note.setTextAlign(Paint.Align.CENTER);
        note.setTextSize(dp(10));
    }

    public void setSolution(JSONArray m, String b) {
        moves = m;
        banner = b == null ? "" : b;
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (moves == null) return;
        int w=getWidth(), h=getHeight();

        // V4 intentionally does NOT draw shapes on the board. Pixel-perfect board
        // alignment varies slightly with Android capture/insets, while grid
        // coordinates do not. Show each answer directly beneath its piece slot.
        final float[] centers={0.242f,0.500f,0.758f};
        final String[] names={"LEFT","MIDDLE","RIGHT"};
        float cardY=h*0.812f;
        float cardW=w*0.285f;
        float cardH=dp(54);

        try {
            for(int slot=0;slot<3;slot++){
                JSONObject found=null;
                int order=0;
                for(int i=0;i<moves.length();i++){
                    JSONObject m=moves.getJSONObject(i);
                    if(m.optInt("piece",-1)==slot){ found=m; order=i+1; break; }
                }
                float cx=w*centers[slot];
                if(found==null) continue;
                RectF r=new RectF(cx-cardW/2,cardY,cx+cardW/2,cardY+cardH);
                c.drawRoundRect(r,dp(10),dp(10),card);
                c.drawText(order+"  "+names[slot],cx,cardY+dp(18),title);
                String xy="R"+(found.getInt("row")+1)+"  C"+(found.getInt("col")+1);
                c.drawText(xy,cx,cardY+dp(42),coord);
            }
        } catch(Exception ignored) {}

        // Small coordinate reminder, far enough below the pieces not to cover them.
        c.drawText("R = top to bottom   •   C = left to right",w/2f,h*0.868f,note);
    }

    private float dp(float x){ return x*getResources().getDisplayMetrics().density; }
}
