package com.tinymayhem.goldsolver;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.*;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class CaptureOverlayService extends Service {
    private static final String CHANNEL = "gold_solver_capture";
    private final Handler main = new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private TextView bubble;
    private SolutionOverlayView solutionView;
    private WebView web;
    private MediaProjection projection;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;
    private int width, height, density;
    private SharedPreferences prefs;
    private boolean solverReady = false;
    private boolean busy = false;

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("gold_solver", MODE_PRIVATE);
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        createChannel();
        startForeground(42, notification("Solver V5 ready"));
        createBubble();
        createHiddenWebView();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("resultData")) {
            int resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
            Intent data;
            if (Build.VERSION.SDK_INT >= 33) data = intent.getParcelableExtra("resultData", Intent.class);
            else data = intent.getParcelableExtra("resultData");
            if (data != null) setupProjection(resultCode, data);
        }
        return START_NOT_STICKY;
    }

    private void setupProjection(int resultCode, Intent data) {
        if (projection != null) return;
        MediaProjectionManager m = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        projection = m.getMediaProjection(resultCode, data);
        projection.registerCallback(new MediaProjection.Callback() {
            @Override public void onStop() { main.post(() -> stopSelf()); }
        }, main);

        if (Build.VERSION.SDK_INT >= 30) {
            WindowMetrics metrics = wm.getMaximumWindowMetrics();
            Rect bounds = metrics.getBounds();
            width = bounds.width(); height = bounds.height();
        } else {
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getRealMetrics(dm);
            width = dm.widthPixels; height = dm.heightPixels;
        }
        density = getResources().getDisplayMetrics().densityDpi;
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3);
        virtualDisplay = projection.createVirtualDisplay("GoldSolverCapture", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, main);
    }

    private void createBubble() {
        bubble = new TextView(this);
        bubble.setText("💎\nSOLVE");
        bubble.setGravity(Gravity.CENTER);
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(12);
        bubble.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.argb(235, 39, 127, 159));
        bg.setCornerRadius(dp(30));
        bg.setStroke(dp(2), Color.WHITE);
        bubble.setBackground(bg);
        bubble.setElevation(dp(8));

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                dp(70), dp(70), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.END;
        lp.x = dp(12); lp.y = dp(210);
        wm.addView(bubble, lp);

        final long[] down = {0};
        bubble.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy; int ox, oy; boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        down[0] = System.currentTimeMillis(); sx=e.getRawX(); sy=e.getRawY(); ox=lp.x; oy=lp.y; moved=false; return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx=e.getRawX()-sx, dy=e.getRawY()-sy;
                        if (Math.abs(dx)>8 || Math.abs(dy)>8) moved=true;
                        lp.x = Math.max(0, ox - Math.round(dx));
                        lp.y = Math.max(0, oy + Math.round(dy));
                        try { wm.updateViewLayout(bubble, lp); } catch(Exception ignored){}
                        return true;
                    case MotionEvent.ACTION_UP:
                        long held = System.currentTimeMillis()-down[0];
                        if (!moved && held > 650) openMain();
                        else if (!moved) solveNow();
                        return true;
                }
                return false;
            }
        });
    }

    private void createHiddenWebView() {
        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAllowFileAccess(true);
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) { solverReady = true; }
        });
        web.loadUrl("file:///android_asset/solver.html");

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                1, 1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.alpha = 0.01f;
        wm.addView(web, lp);
    }

    private void solveNow() {
        if (busy) return;
        if (projection == null || imageReader == null) { toast("Screen capture is not ready. Restart the solver bubble."); return; }
        if (!solverReady) { toast("Solver is still loading. Try again in a second."); return; }
        busy = true;
        hideSolution();
        bubble.setVisibility(View.INVISIBLE);
        main.postDelayed(this::captureLatest, 180);
    }

    private void captureLatest() {
        Image image = null;
        try {
            image = imageReader.acquireLatestImage();
            if (image == null) {
                main.postDelayed(this::captureLatest, 90);
                return;
            }
            Bitmap bmp = imageToBitmap(image);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
            String b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
            int b = prefs.getInt("blue",0), g=prefs.getInt("green",0), y=prefs.getInt("yellow",0);
            String cal = CalibrationPrefs.toJson(prefs).toString();
            String js = "solveScreenshot('data:image/jpeg;base64," + b64 + "',"+b+","+g+","+y+","+cal+")";
            web.evaluateJavascript(js, null);
            bmp.recycle();
        } catch (Throwable t) {
            busy = false; bubble.setVisibility(View.VISIBLE); toast("Capture failed: " + t.getClass().getSimpleName());
        } finally {
            if (image != null) image.close();
        }
    }

    private Bitmap imageToBitmap(Image image) {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap padded = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        padded.copyPixelsFromBuffer(buffer);
        Bitmap cropped = Bitmap.createBitmap(padded, 0, 0, width, height);
        if (cropped != padded) padded.recycle();
        return cropped;
    }

    private void showSolution(JSONObject obj) {
        try {
            JSONArray moves = obj.getJSONArray("moves");
            JSONObject counts = obj.getJSONObject("counts");
            prefs.edit().putInt("blue", counts.getInt("blue"))
                    .putInt("green", counts.getInt("green"))
                    .putInt("yellow", counts.getInt("yellow")).apply();
            String banner = obj.optString("summary", "Solution ready");

            if (solutionView == null) {
                solutionView = new SolutionOverlayView(this, prefs);
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT);
                lp.gravity = Gravity.TOP | Gravity.START;
                wm.addView(solutionView, lp);
            }
            solutionView.setSolution(moves, banner);
            solutionView.setVisibility(View.VISIBLE);
            bubble.setVisibility(View.VISIBLE);
            busy = false;
            updateNotification(banner);
        } catch (Exception e) {
            busy=false; bubble.setVisibility(View.VISIBLE); toast("Could not read solution");
        }
    }

    private void hideSolution() { if (solutionView != null) solutionView.setVisibility(View.GONE); }

    private void openMain() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }

    private void toast(String s) { main.post(() -> Toast.makeText(this, s, Toast.LENGTH_LONG).show()); }

    private class Bridge {
        @JavascriptInterface public void onSolved(String json) {
            main.post(() -> {
                try {
                    JSONObject o = new JSONObject(json);
                    if (!o.optBoolean("ok", false)) {
                        busy=false; bubble.setVisibility(View.VISIBLE);
                        toast(o.optString("error", "Detection failed"));
                        return;
                    }
                    showSolution(o);
                } catch (Exception e) {
                    busy=false; bubble.setVisibility(View.VISIBLE); toast("Solver response error");
                }
            });
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "Gold Solver", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("Keeps the screen-capture solver available while Last War is open.");
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    private Notification notification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_menu_search).setContentTitle("Gold Prospecting Solver V5")
                .setContentText(text).setContentIntent(pi).setOngoing(true).build();
    }

    private void updateNotification(String s) {
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(42, notification(s));
    }

    @Override public void onDestroy() {
        hideSolution();
        try { if (bubble != null) wm.removeView(bubble); } catch(Exception ignored){}
        try { if (solutionView != null) wm.removeView(solutionView); } catch(Exception ignored){}
        try { if (web != null) { wm.removeView(web); web.destroy(); } } catch(Exception ignored){}
        try { if (virtualDisplay != null) virtualDisplay.release(); } catch(Exception ignored){}
        try { if (imageReader != null) imageReader.close(); } catch(Exception ignored){}
        try { if (projection != null) projection.stop(); } catch(Exception ignored){}
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
    private int dp(int x) { return Math.round(x * getResources().getDisplayMetrics().density); }
}
