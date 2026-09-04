package com.tinymayhem.goldsolver;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;

public class CalibrationActivity extends Activity {
    private Bitmap bitmap;
    private CalibrationView calibrationView;
    private SharedPreferences prefs;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("gold_solver", MODE_PRIVATE);
        enterImmersive();

        Uri uri = getIntent().getData();
        if (uri == null) {
            Toast.makeText(this, "No screenshot selected", Toast.LENGTH_LONG).show();
            finish(); return;
        }
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            bitmap = null;
        }
        if (bitmap == null) {
            Toast.makeText(this, "Could not open screenshot", Toast.LENGTH_LONG).show();
            finish(); return;
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        calibrationView = new CalibrationView(this, bitmap, prefs);
        root.addView(calibrationView, new FrameLayout.LayoutParams(-1, -1));

        TextView help = new TextView(this);
        help.setText("CALIBRATION\nDrag the 4 board corners onto the outside edges of the 8x8 grid.\nThen line up LEFT, MIDDLE and RIGHT boxes around the three piece areas.");
        help.setTextColor(Color.WHITE);
        help.setTextSize(14);
        help.setTypeface(Typeface.DEFAULT_BOLD);
        help.setGravity(Gravity.CENTER);
        help.setPadding(dp(12), dp(8), dp(12), dp(8));
        help.setBackgroundColor(Color.argb(205, 20, 16, 14));
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        hp.setMargins(dp(8), dp(8), dp(8), 0);
        root.addView(help, hp);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        buttons.setPadding(dp(6), dp(4), dp(6), dp(6));
        buttons.setBackgroundColor(Color.argb(220, 20, 16, 14));

        Button reset = new Button(this); reset.setText("Reset");
        Button cancel = new Button(this); cancel.setText("Cancel");
        Button save = new Button(this); save.setText("Save calibration");
        buttons.addView(reset, new LinearLayout.LayoutParams(0, dp(52), 1));
        buttons.addView(cancel, new LinearLayout.LayoutParams(0, dp(52), 1));
        buttons.addView(save, new LinearLayout.LayoutParams(0, dp(52), 1.5f));

        reset.setOnClickListener(v -> calibrationView.resetDefaults());
        cancel.setOnClickListener(v -> finish());
        save.setOnClickListener(v -> {
            calibrationView.saveCalibration(prefs);
            Toast.makeText(this, "Calibration saved", Toast.LENGTH_SHORT).show();
            finish();
        });

        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        root.addView(buttons, bp);
        setContentView(root);
    }

    private void enterImmersive() {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override protected void onDestroy() {
        if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        super.onDestroy();
    }

    private int dp(int x) { return Math.round(x * getResources().getDisplayMetrics().density); }
}
