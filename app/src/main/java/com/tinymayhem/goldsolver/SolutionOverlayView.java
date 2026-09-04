package com.tinymayhem.goldsolver;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

public class SolutionOverlayView extends View {
    private final SharedPreferences prefs;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint orderText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint card = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cardTitle = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint coord = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint preview = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint previewStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint note = new Paint(Paint.ANTI_ALIAS_FLAG);
    private JSONArray moves;
    private String banner = "";

    private static final int[] MOVE_COLORS = new int[]{
            Color.rgb(80, 235, 177),
            Color.rgb(255, 204, 84),
            Color.rgb(104, 193, 255)
    };

    public SolutionOverlayView(Context c, SharedPreferences p) {
        super(c);
        prefs = p;
        setBackgroundColor(Color.TRANSPARENT);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(3));
        stroke.setShadowLayer(dp(3), 0, dp(1), Color.BLACK);

        orderText.setColor(Color.WHITE);
        orderText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        orderText.setTextAlign(Paint.Align.CENTER);
        orderText.setTextSize(dp(18));
        orderText.setShadowLayer(dp(4), 0, dp(1), Color.BLACK);

        card.setColor(Color.argb(225, 38, 27, 22));
        cardTitle.setColor(Color.WHITE);
        cardTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        cardTitle.setTextAlign(Paint.Align.CENTER);
        cardTitle.setTextSize(dp(11));
        coord.setColor(Color.WHITE);
        coord.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        coord.setTextAlign(Paint.Align.CENTER);
        coord.setTextSize(dp(15));
        coord.setShadowLayer(dp(3), 0, dp(1), Color.BLACK);

        preview.setStyle(Paint.Style.FILL);
        previewStroke.setStyle(Paint.Style.STROKE);
        previewStroke.setStrokeWidth(dp(1));
        previewStroke.setColor(Color.WHITE);

        note.setColor(Color.WHITE);
        note.setTextAlign(Paint.Align.CENTER);
        note.setTextSize(dp(9));
        note.setShadowLayer(dp(3), 0, dp(1), Color.BLACK);
    }

    public void setSolution(JSONArray m, String b) {
        moves = m;
        banner = b == null ? "" : b;
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (moves == null) return;
        int w = getWidth(), h = getHeight();
        RectF board = CalibrationPrefs.getBoard(prefs);
        float bl = board.left*w, bt = board.top*h, br = board.right*w, bb = board.bottom*h;
        float cellW = (br-bl)/8f, cellH=(bb-bt)/8f;

        try {
            // Main calibrated overlay. Each move uses the exact detected piece shape.
            for (int i=0;i<moves.length();i++) {
                JSONObject m = moves.getJSONObject(i);
                int color = MOVE_COLORS[Math.min(i, MOVE_COLORS.length-1)];
                fill.setColor(Color.argb(78, Color.red(color), Color.green(color), Color.blue(color)));
                stroke.setColor(color);
                JSONArray cells = m.optJSONArray("cells");
                if (cells == null) continue;
                float firstCx=0, firstCy=0;
                for (int j=0;j<cells.length();j++) {
                    JSONArray z = cells.getJSONArray(j);
                    int rr = m.getInt("row") + z.getInt(0);
                    int cc = m.getInt("col") + z.getInt(1);
                    float l = bl + cc*cellW, t=bt+rr*cellH;
                    RectF r = new RectF(l+dp(1.5f), t+dp(1.5f), l+cellW-dp(1.5f), t+cellH-dp(1.5f));
                    c.drawRoundRect(r, dp(5), dp(5), fill);
                    c.drawRoundRect(r, dp(5), dp(5), stroke);
                    if (j==0) { firstCx=r.centerX(); firstCy=r.centerY(); }
                }
                c.drawText(String.valueOf(i+1), firstCx, firstCy-(orderText.ascent()+orderText.descent())/2f, orderText);
            }

            // Backup coordinate cards under each real piece slot. The mini preview shows
            // exactly what shape and gem colors the detector thinks it saw.
            String[] names={"LEFT","MIDDLE","RIGHT"};
            float maxCardBottom = h-dp(20);
            for (int slot=0;slot<3;slot++) {
                JSONObject found=null; int order=0;
                for (int i=0;i<moves.length();i++) {
                    JSONObject m=moves.getJSONObject(i);
                    if (m.optInt("piece",-1)==slot) { found=m; order=i+1; break; }
                }
                if (found==null) continue;
                RectF s=CalibrationPrefs.getSlot(prefs,slot);
                float sx1=s.left*w, sx2=s.right*w, sy2=s.bottom*h;
                float cardW=Math.max(dp(86), (sx2-sx1)*0.95f);
                float cardH=dp(43);
                float cx=(sx1+sx2)/2f;
                float top=Math.min(sy2+dp(3), maxCardBottom-cardH);
                RectF cr=new RectF(cx-cardW/2f,top,cx+cardW/2f,top+cardH);
                c.drawRoundRect(cr,dp(9),dp(9),card);

                c.drawText(order+"  "+names[slot], cx+cardW*0.13f, top+dp(14), cardTitle);
                String xy="R"+(found.getInt("row")+1)+"  C"+(found.getInt("col")+1);
                c.drawText(xy,cx+cardW*0.13f,top+dp(34),coord);
                drawPiecePreview(c, found, cr.left+cardW*0.16f, cr.centerY(), cardH*0.72f);
            }

            float noteY = Math.min(h-dp(6), Math.max(bb+dp(12), h*0.89f));
            c.drawText("Overlay + coordinates use your saved calibration", w/2f, noteY, note);
        } catch (Exception ignored) {}
    }

    private void drawPiecePreview(Canvas c, JSONObject m, float centerX, float centerY, float maxSize) {
        try {
            JSONArray cells=m.optJSONArray("typedCells");
            if(cells==null) cells=m.optJSONArray("cells");
            if(cells==null || cells.length()==0) return;
            int maxR=0,maxC=0;
            for(int i=0;i<cells.length();i++){
                JSONArray z=cells.getJSONArray(i);
                maxR=Math.max(maxR,z.getInt(0)); maxC=Math.max(maxC,z.getInt(1));
            }
            float unit=Math.min(dp(7), Math.min(maxSize/(maxR+1f), maxSize/(maxC+1f)));
            float totalW=(maxC+1)*unit,totalH=(maxR+1)*unit;
            float x0=centerX-totalW/2f,y0=centerY-totalH/2f;
            for(int i=0;i<cells.length();i++){
                JSONArray z=cells.getJSONArray(i);
                int r=z.getInt(0), col=z.getInt(1), type=z.length()>2?z.optInt(2,1):1;
                preview.setColor(typeColor(type));
                RectF q=new RectF(x0+col*unit+dp(.5f),y0+r*unit+dp(.5f),x0+(col+1)*unit-dp(.5f),y0+(r+1)*unit-dp(.5f));
                c.drawRoundRect(q,dp(1.5f),dp(1.5f),preview);
                c.drawRoundRect(q,dp(1.5f),dp(1.5f),previewStroke);
            }
        } catch(Exception ignored){}
    }

    private int typeColor(int t){
        if(t==2) return Color.rgb(64,153,218);
        if(t==3) return Color.rgb(125,185,125);
        if(t==4) return Color.rgb(231,184,119);
        return Color.rgb(239,174,109);
    }

    private float dp(float x){ return x*getResources().getDisplayMetrics().density; }
}
