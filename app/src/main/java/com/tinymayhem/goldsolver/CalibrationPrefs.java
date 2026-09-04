package com.tinymayhem.goldsolver;

import android.content.SharedPreferences;
import android.graphics.RectF;

import org.json.JSONArray;
import org.json.JSONObject;

public final class CalibrationPrefs {
    private CalibrationPrefs() {}

    // Defaults are based on the user's 709 x 1536 Last War captures.
    private static final float DEF_BOARD_L = 95.5f / 709f;
    private static final float DEF_BOARD_T = 496.5f / 1536f;
    private static final float DEF_BOARD_R = 615.5f / 709f;
    private static final float DEF_BOARD_B = 1016.5f / 1536f;

    private static final float[][] DEF_SLOTS = new float[][]{
            {100f/709f, 1060f/1536f, 245f/709f, 1250f/1536f},
            {285f/709f, 1060f/1536f, 440f/709f, 1250f/1536f},
            {475f/709f, 1060f/1536f, 620f/709f, 1250f/1536f}
    };

    public static RectF defaultBoard() {
        return new RectF(DEF_BOARD_L, DEF_BOARD_T, DEF_BOARD_R, DEF_BOARD_B);
    }

    public static RectF defaultSlot(int i) {
        float[] s = DEF_SLOTS[Math.max(0, Math.min(2, i))];
        return new RectF(s[0], s[1], s[2], s[3]);
    }

    public static RectF getBoard(SharedPreferences p) {
        return new RectF(
                p.getFloat("cal_board_l", DEF_BOARD_L),
                p.getFloat("cal_board_t", DEF_BOARD_T),
                p.getFloat("cal_board_r", DEF_BOARD_R),
                p.getFloat("cal_board_b", DEF_BOARD_B));
    }

    public static RectF getSlot(SharedPreferences p, int i) {
        RectF d = defaultSlot(i);
        return new RectF(
                p.getFloat("cal_slot"+i+"_l", d.left),
                p.getFloat("cal_slot"+i+"_t", d.top),
                p.getFloat("cal_slot"+i+"_r", d.right),
                p.getFloat("cal_slot"+i+"_b", d.bottom));
    }

    public static void save(SharedPreferences p, RectF board, RectF[] slots) {
        SharedPreferences.Editor e = p.edit()
                .putFloat("cal_board_l", board.left)
                .putFloat("cal_board_t", board.top)
                .putFloat("cal_board_r", board.right)
                .putFloat("cal_board_b", board.bottom)
                .putBoolean("calibrated", true);
        for (int i=0;i<3;i++) {
            RectF s = slots[i];
            e.putFloat("cal_slot"+i+"_l", s.left)
                    .putFloat("cal_slot"+i+"_t", s.top)
                    .putFloat("cal_slot"+i+"_r", s.right)
                    .putFloat("cal_slot"+i+"_b", s.bottom);
        }
        e.apply();
    }

    public static void reset(SharedPreferences p) {
        SharedPreferences.Editor e = p.edit();
        e.remove("cal_board_l").remove("cal_board_t").remove("cal_board_r").remove("cal_board_b")
                .remove("calibrated");
        for (int i=0;i<3;i++) {
            e.remove("cal_slot"+i+"_l").remove("cal_slot"+i+"_t")
                    .remove("cal_slot"+i+"_r").remove("cal_slot"+i+"_b");
        }
        e.apply();
    }

    public static boolean isCalibrated(SharedPreferences p) {
        return p.getBoolean("calibrated", false);
    }

    public static JSONObject toJson(SharedPreferences p) {
        JSONObject root = new JSONObject();
        try {
            RectF b = getBoard(p);
            JSONObject jb = new JSONObject();
            jb.put("left", b.left); jb.put("top", b.top); jb.put("right", b.right); jb.put("bottom", b.bottom);
            root.put("board", jb);
            JSONArray slots = new JSONArray();
            for (int i=0;i<3;i++) {
                RectF s = getSlot(p, i);
                JSONObject js = new JSONObject();
                js.put("left", s.left); js.put("top", s.top); js.put("right", s.right); js.put("bottom", s.bottom);
                slots.put(js);
            }
            root.put("slots", slots);
        } catch (Exception ignored) {}
        return root;
    }
}
