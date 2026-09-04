package com.tinymayhem.goldsolver;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQ_CAPTURE = 9001;
    private static final int REQ_NOTIFY = 9002;
    private static final int REQ_CALIBRATION_IMAGE = 9003;
    private EditText blueInput, greenInput, yellowInput;
    private SharedPreferences prefs;
    private TextView calibrationStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("gold_solver", MODE_PRIVATE);
        setContentView(buildUi());
        loadCounts();
        updateCalibrationStatus();
    }

    @Override protected void onResume() {
        super.onResume();
        if (calibrationStatus != null) updateCalibrationStatus();
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(26), dp(22), dp(22));
        root.setBackgroundColor(Color.rgb(248, 238, 218));

        TextView title = new TextView(this);
        title.setText("Gold Prospecting Solver V5");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(55, 35, 25));
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Calibrated floating overlay for Last War. It only reads the screen and never taps or drags pieces.");
        sub.setTextSize(15);
        sub.setTextColor(Color.DKGRAY);
        sub.setPadding(0, dp(8), 0, dp(18));
        root.addView(sub);

        TextView countsTitle = new TextView(this);
        countsTitle.setText("Current gem totals");
        countsTitle.setTypeface(Typeface.DEFAULT_BOLD);
        countsTitle.setTextSize(18);
        root.addView(countsTitle);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        blueInput = countBox(5);
        greenInput = countBox(10);
        yellowInput = countBox(5);
        row.addView(wrapField("Blue /5", blueInput), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(wrapField("Green /10", greenInput), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(wrapField("Yellow /5", yellowInput), new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(row);

        Button save = button("Save totals");
        save.setOnClickListener(v -> { saveCounts(); Toast.makeText(this, "Gem totals saved", Toast.LENGTH_SHORT).show(); });
        root.addView(save);

        Button reset = button("Reset new round to 0 / 0 / 0");
        reset.setOnClickListener(v -> {
            blueInput.setText("0"); greenInput.setText("0"); yellowInput.setText("0"); saveCounts();
            Toast.makeText(this, "New round reset", Toast.LENGTH_SHORT).show();
        });
        root.addView(reset);

        TextView calTitle = new TextView(this);
        calTitle.setText("\nScreen calibration");
        calTitle.setTypeface(Typeface.DEFAULT_BOLD);
        calTitle.setTextSize(18);
        root.addView(calTitle);

        calibrationStatus = new TextView(this);
        calibrationStatus.setTextSize(14);
        calibrationStatus.setTextColor(Color.rgb(65,48,40));
        calibrationStatus.setPadding(0, dp(4), 0, dp(4));
        root.addView(calibrationStatus);

        Button calibrate = button("Calibrate overlay from screenshot");
        calibrate.setOnClickListener(v -> chooseCalibrationScreenshot());
        root.addView(calibrate);

        Button resetCal = button("Reset calibration to default");
        resetCal.setOnClickListener(v -> {
            CalibrationPrefs.reset(prefs);
            updateCalibrationStatus();
            Toast.makeText(this, "Calibration reset", Toast.LENGTH_SHORT).show();
        });
        root.addView(resetCal);

        TextView startTitle = new TextView(this);
        startTitle.setText("\nStart solver");
        startTitle.setTypeface(Typeface.DEFAULT_BOLD);
        startTitle.setTextSize(18);
        root.addView(startTitle);

        Button overlay = button("1. Allow floating overlay");
        overlay.setOnClickListener(v -> requestOverlay());
        root.addView(overlay);

        Button start = button("2. Start SOLVE bubble");
        start.setOnClickListener(v -> startCaptureFlow());
        root.addView(start);

        Button stop = button("Stop solver bubble");
        stop.setOnClickListener(v -> stopService(new Intent(this, CaptureOverlayService.class)));
        root.addView(stop);

        TextView help = new TextView(this);
        help.setText("How to use\n\n" +
                "CALIBRATE ONCE:\n" +
                "1. Take a screenshot while Gold Prospecting is fully visible.\n" +
                "2. Tap Calibrate overlay from screenshot and choose it.\n" +
                "3. Drag the 8x8 grid onto the board and line up the three piece boxes. Save.\n\n" +
                "PLAY:\n" +
                "1. Reset totals when a fresh round begins.\n" +
                "2. Start the SOLVE bubble and approve screen capture.\n" +
                "3. In Last War, tap SOLVE after the pieces stop moving.\n" +
                "4. The exact piece shapes are highlighted on the board. Coordinate cards below the pieces remain as a backup.\n" +
                "5. Long-press SOLVE to return here.\n\n" +
                "If the highlighted shape does not match the real piece, do not place it. Recalibrate the piece boxes or capture after the animation stops.");
        help.setTextSize(15);
        help.setTextColor(Color.rgb(65, 48, 40));
        help.setPadding(0, dp(20), 0, 0);
        root.addView(help);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        return scroll;
    }

    private EditText countBox(int max) {
        EditText e = new EditText(this);
        e.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        e.setText("0");
        e.setGravity(Gravity.CENTER);
        e.setSingleLine(true);
        e.setTag(max);
        return e;
    }

    private View wrapField(String label, EditText input) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(4), dp(4), dp(4), dp(8));
        TextView t = new TextView(this);
        t.setText(label);
        t.setGravity(Gravity.CENTER);
        t.setTextSize(13);
        box.addView(t);
        box.addView(input);
        return box;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(6), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    private void updateCalibrationStatus() {
        if (calibrationStatus == null) return;
        calibrationStatus.setText(CalibrationPrefs.isCalibrated(prefs)
                ? "Custom calibration saved ✓"
                : "Using default calibration. Calibrate once on this phone for accurate overlays.");
    }

    private void chooseCalibrationScreenshot() {
        // GET_CONTENT is more reliable across Samsung Gallery / My Files providers.
        // We immediately copy the selected image into our own cache, so the
        // calibration screen never depends on a temporary content-URI grant.
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(Intent.createChooser(i, "Choose Gold Prospecting screenshot"), REQ_CALIBRATION_IMAGE);
    }

    private String copyCalibrationImageToCache(Uri uri) {
        File out = new File(getCacheDir(), "gold_calibration_source.img");
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(out, false)) {
            if (in == null) return null;
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) fos.write(buf, 0, n);
            fos.flush();
            return out.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    private void requestOverlay() {
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission is already allowed", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        startActivity(i);
    }

    private void startCaptureFlow() {
        saveCounts();
        if (!Settings.canDrawOverlays(this)) {
            requestOverlay();
            Toast.makeText(this, "Allow overlay, then tap Start again", Toast.LENGTH_LONG).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFY);
        }
        MediaProjectionManager m = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(m.createScreenCaptureIntent(), REQ_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CALIBRATION_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            String cachedPath = copyCalibrationImageToCache(data.getData());
            if (cachedPath == null) {
                Toast.makeText(this, "Could not copy that screenshot. Try choosing it from Gallery or My Files.", Toast.LENGTH_LONG).show();
                return;
            }
            Intent c = new Intent(this, CalibrationActivity.class);
            c.putExtra("imagePath", cachedPath);
            try {
                startActivity(c);
            } catch (Exception e) {
                Toast.makeText(this, "Could not open calibration screen: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (requestCode == REQ_CAPTURE && resultCode == RESULT_OK && data != null) {
            Intent s = new Intent(this, CaptureOverlayService.class);
            s.putExtra("resultCode", resultCode);
            s.putExtra("resultData", data);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(s); else startService(s);
            Toast.makeText(this, "Solver bubble started. Open Last War.", Toast.LENGTH_LONG).show();
            moveTaskToBack(true);
        }
    }

    private int parse(EditText e) {
        try {
            int v = Integer.parseInt(e.getText().toString().trim());
            int max = (Integer)e.getTag();
            return Math.max(0, Math.min(max, v));
        } catch (Exception ex) { return 0; }
    }

    private void saveCounts() {
        prefs.edit().putInt("blue", parse(blueInput)).putInt("green", parse(greenInput)).putInt("yellow", parse(yellowInput)).apply();
    }

    private void loadCounts() {
        blueInput.setText(String.valueOf(prefs.getInt("blue", 0)));
        greenInput.setText(String.valueOf(prefs.getInt("green", 0)));
        yellowInput.setText(String.valueOf(prefs.getInt("yellow", 0)));
    }

    private int dp(int x) { return Math.round(x * getResources().getDisplayMetrics().density); }
}
