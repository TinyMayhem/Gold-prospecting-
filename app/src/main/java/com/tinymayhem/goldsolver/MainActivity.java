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
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQ_CAPTURE = 9001;
    private static final int REQ_NOTIFY = 9002;
    private EditText blueInput, greenInput, yellowInput;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("gold_solver", MODE_PRIVATE);
        setContentView(buildUi());
        loadCounts();
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(26), dp(22), dp(22));
        root.setBackgroundColor(Color.rgb(248, 238, 218));

        TextView title = new TextView(this);
        title.setText("Gold Prospecting Solver");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(55, 35, 25));
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Floating read-only solver for Last War. It never taps or drags pieces for you.");
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
        blueInput = countBox("Blue", 5);
        greenInput = countBox("Green", 10);
        yellowInput = countBox("Yellow", 5);
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
        help.setText("How to use\n\n1. Reset totals when a fresh Gold Prospecting round begins.\n2. Tap Start SOLVE bubble and approve screen capture.\n3. Open Last War.\n4. When all three pieces are visible, tap the floating SOLVE button.\n5. Numbered placements appear over the board. Place 1, then 2, then 3.\n6. Tap SOLVE again for the next batch. The app carries the expected gem totals forward automatically.\n\nLong-press the SOLVE bubble to return here.");
        help.setTextSize(15);
        help.setTextColor(Color.rgb(65, 48, 40));
        help.setPadding(0, dp(20), 0, 0);
        root.addView(help);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        return scroll;
    }

    private EditText countBox(String name, int max) {
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
