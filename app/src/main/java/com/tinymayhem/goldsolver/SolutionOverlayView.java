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
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private JSONArray moves;
    private String banner = "";

    // Normalized from the user's 709x1536 screenshots.
    private static final double BOARD_X0 = 128.0 / 709.0;
    private static final double BOARD_Y0 = 529.0 / 1536.0;
    private static final double BOARD_DX = 65.0 / 709.0;
    private static final double BOARD_DY = 65.0 / 1536.0;

    public SolutionOverlayView(Context c) {
        super(c);
        setBackgroundColor(Color.TRANSPARENT);
        fill.setColor(Color.argb(85, 255, 235, 59));
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(3));
        stroke.setColor(Color.WHITE);
        text.setColor(Color.WHITE);
        text.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(dp(24));
        text.setShadowLayer(dp(4), 0, dp(2), Color.BLACK);
    }

    public void setSolution(JSONArray m, String b) { moves = m; banner = b == null ? "" : b; invalidate(); }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (moves == null) return;
        int w = getWidth(), h = getHeight();
        try {
            for (int i = 0; i < moves.length(); i++) {
                JSONObject move = moves.getJSONObject(i);
                JSONArray cells = move.getJSONArray("cells");
                for (int j = 0; j < cells.length(); j++) {
                    JSONArray rc = cells.getJSONArray(j);
                    int r = move.getInt("row") + rc.getInt(0);
                    int col = move.getInt("col") + rc.getInt(1);
                    float cx = (float)((BOARD_X0 + col * BOARD_DX) * w);
                    float cy = (float)((BOARD_Y0 + r * BOARD_DY) * h);
                    float cw = (float)(BOARD_DX * w * .84);
                    float ch = (float)(BOARD_DY * h * .84);
                    RectF rect = new RectF(cx-cw/2, cy-ch/2, cx+cw/2, cy+ch/2);
                    c.drawRoundRect(rect, dp(5), dp(5), fill);
                    c.drawRoundRect(rect, dp(5), dp(5), stroke);
                }
                JSONArray first = cells.getJSONArray(0);
                int rr = move.getInt("row") + first.getInt(0);
                int cc = move.getInt("col") + first.getInt(1);
                float tx = (float)((BOARD_X0 + cc * BOARD_DX) * w);
                float ty = (float)((BOARD_Y0 + rr * BOARD_DY) * h + dp(8));
                c.drawText(String.valueOf(i+1), tx, ty, text);
            }
        } catch (Exception ignored) {}

        if (!banner.isEmpty()) {
            Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
            bg.setColor(Color.argb(210, 30, 22, 18));
            float pad = dp(12);
            float top = h * .16f;
            RectF r = new RectF(w*.08f, top, w*.92f, top + dp(50));
            c.drawRoundRect(r, dp(12), dp(12), bg);
            Paint bt = new Paint(text);
            bt.setTextSize(dp(16));
            c.drawText(banner, w/2f, top + dp(31), bt);
        }
    }

    private float dp(float x) { return x * getResources().getDisplayMetrics().density; }
}
