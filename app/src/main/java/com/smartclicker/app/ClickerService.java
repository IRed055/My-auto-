package com.smartclicker.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class ClickerService extends AccessibilityService {
    public static ClickerService instance;
    private WindowManager wm;
    private LinearLayout controlBar;
    private View editDialogView;
    private final List<TouchPoint> points = new ArrayList<>();
    private boolean isRunning = false;
    private boolean isStealth = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private TextView btnPlay;

    private static class TouchPoint {
        int stepNumber;
        int delayMs;
        int durationMs;
        boolean useRandom;
        TextView view;
        WindowManager.LayoutParams params;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        showControlBar();
    }

    public void showControlBar() {
        if (controlBar != null) return;
        SharedPreferences prefs = getSharedPreferences("ClickerPrefs", MODE_PRIVATE);
        float alpha = prefs.getInt("def_alpha", 85) / 100f;

        controlBar = new LinearLayout(this);
        controlBar.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#DD1A202C"));
        bg.setCornerRadius(30f);
        controlBar.setBackground(bg);
        controlBar.setPadding(12, 16, 12, 16);
        controlBar.setAlpha(alpha);

        btnPlay = createBarButton("▶", "#48BB78");
        btnPlay.setOnClickListener(v -> togglePlay());
        controlBar.addView(btnPlay);

        TextView btnAdd = createBarButton("+", "#4299E1");
        btnAdd.setOnClickListener(v -> {
            if (!isRunning) addTouchPoint();
        });
        controlBar.addView(btnAdd);

        TextView btnRemove = createBarButton("-", "#ECC94B");
        btnRemove.setOnClickListener(v -> {
            if (!isRunning && !points.isEmpty()) {
                TouchPoint last = points.remove(points.size() - 1);
                wm.removeView(last.view);
            }
        });
        controlBar.addView(btnRemove);

        TextView btnEye = createBarButton("👁", "#9F7AEA");
        btnEye.setOnClickListener(v -> toggleStealth());
        controlBar.addView(btnEye);

        TextView btnClose = createBarButton("✖", "#F56565");
        btnClose.setOnClickListener(v -> closeAll());
        controlBar.addView(btnClose);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 20;
        params.y = 250;

        controlBar.setOnTouchListener(new View.OnTouchListener() {
            int initX, initY;
            float touchX, touchY;
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    initX = params.x; initY = params.y;
                    touchX = e.getRawX(); touchY = e.getRawY();
                    return true;
                } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    params.x = initX + (int) (e.getRawX() - touchX);
                    params.y = initY + (int) (e.getRawY() - touchY);
                    wm.updateViewLayout(controlBar, params);
                    return true;
                }
                return false;
            }
        });

        wm.addView(controlBar, params);
    }

    private TextView createBarButton(String symbol, String colorHex) {
        TextView tv = new TextView(this);
        tv.setText(symbol);
        tv.setTextSize(18);
        tv.setTextColor(Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(Color.parseColor(colorHex));
        tv.setBackground(shape);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(95, 95);
        lp.setMargins(0, 8, 0, 8);
        tv.setLayoutParams(lp);
        return tv;
    }

    private void addTouchPoint() {
        SharedPreferences prefs = getSharedPreferences("ClickerPrefs", MODE_PRIVATE);
        int size = prefs.getInt("def_size", 110);
        float alpha = prefs.getInt("def_alpha", 85) / 100f;

        final TouchPoint tp = new TouchPoint();
        tp.stepNumber = points.size() + 1;
        tp.delayMs = prefs.getInt("def_delay", 300);
        tp.durationMs = prefs.getInt("def_duration", 60);
        tp.useRandom = true;

        TextView tv = new TextView(this);
        tv.setText(String.valueOf(tp.stepNumber));
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(16);
        tv.setGravity(Gravity.CENTER);
        tv.setAlpha(alpha);

        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.parseColor("#883182CE"));
        circle.setStroke(4, Color.WHITE);
        tv.setBackground(circle);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 300 + (points.size() * 40);
        params.y = 400 + (points.size() * 40);

        tp.view = tv;
        tp.params = params;

        tv.setOnTouchListener(new View.OnTouchListener() {
            int initX, initY;
            float touchX, touchY;
            long downTime;
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                if (isRunning) return false;
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    initX = params.x; initY = params.y;
                    touchX = e.getRawX(); touchY = e.getRawY();
                    downTime = System.currentTimeMillis();
                    return true;
                } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    params.x = initX + (int) (e.getRawX() - touchX);
                    params.y = initY + (int) (e.getRawY() - touchY);
                    wm.updateViewLayout(tv, params);
                    return true;
                } else if (e.getAction() == MotionEvent.ACTION_UP) {
                    if (Math.abs(e.getRawX() - touchX) < 15 && Math.abs(e.getRawY() - touchY) < 15) {
                        showEditPointDialog(tp);
                    }
                    return true;
                }
                return false;
            }
        });

        points.add(tp);
        wm.addView(tv, params);
    }

    private void showEditPointDialog(final TouchPoint tp) {
        if (editDialogView != null) wm.removeView(editDialogView);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(35, 35, 35, 35);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#F7FAFC"));
        bg.setCornerRadius(25f);
        bg.setStroke(4, Color.parseColor("#3182CE"));
        box.setBackground(bg);

        TextView title = new TextView(this);
        title.setText("تعديل اللمسة (نفس الرقم = نقر متزامن)");
        title.setTextColor(Color.BLACK);
        title.setTextSize(15);
        box.addView(title);

        final EditText etStep = createDialogInput(box, "رقم الخطوة:", String.valueOf(tp.stepNumber));
        final EditText etDelay = createDialogInput(box, "الانتظار قبلها (مللي ثانية):", String.valueOf(tp.delayMs));
        final EditText etDur = createDialogInput(box, "مدة اللمس/الضغط المطول (مللي ثانية):", String.valueOf(tp.durationMs));

        final CheckBox cbRand = new CheckBox(this);
        cbRand.setText("تفعيل العشوائية الذكية لهذه اللمسة");
        cbRand.setTextColor(Color.BLACK);
        cbRand.setChecked(tp.useRandom);
        box.addView(cbRand);

        Button btnSave = new Button(this);
        btnSave.setText("حفظ التعديل");
        btnSave.setBackgroundColor(Color.parseColor("#38A169"));
        btnSave.setTextColor(Color.WHITE);
        btnSave.setOnClickListener(v -> {
            try {
                tp.stepNumber = Integer.parseInt(etStep.getText().toString());
                tp.delayMs = Integer.parseInt(etDelay.getText().toString());
                tp.durationMs = Math.max(20, Integer.parseInt(etDur.getText().toString()));
                tp.useRandom = cbRand.isChecked();
                tp.view.setText(String.valueOf(tp.stepNumber));
            } catch (Exception ignored) {}
            wm.removeView(editDialogView);
            editDialogView = null;
        });
        box.addView(btnSave);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT);
        lp.dimAmount = 0.4f;
        lp.gravity = Gravity.CENTER;

        editDialogView = box;
        wm.addView(editDialogView, lp);
    }

    private EditText createDialogInput(LinearLayout parent, String label, String val) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.DKGRAY);
        parent.addView(tv);
        EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setText(val);
        et.setTextColor(Color.BLACK);
        parent.addView(et);
        return et;
    }

    private void toggleStealth() {
        isStealth = !isStealth;
        for (TouchPoint tp : points) {
            tp.view.setVisibility(isStealth ? View.INVISIBLE : View.VISIBLE);
        }
        controlBar.setAlpha(isStealth ? 0.25f : 0.85f);
    }

    private void togglePlay() {
        if (points.isEmpty()) {
            Toast.makeText(this, "أضف دائرة لمس أولاً من علامة (+)", Toast.LENGTH_SHORT).show();
            return;
        }
        isRunning = !isRunning;
        btnPlay.setText(isRunning ? "⏸" : "▶");
        setPointsTouchable(!isRunning);
        if (isRunning) {
            runStepGroup(0);
        } else {
            handler.removeCallbacksAndMessages(null);
        }
    }

    private void setPointsTouchable(boolean touchable) {
        for (TouchPoint tp : points) {
            if (touchable) {
                tp.params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            } else {
                tp.params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            }
            wm.updateViewLayout(tp.view, tp.params);
        }
    }

    private void runStepGroup(final int groupIndex) {
        if (!isRunning || points.isEmpty()) return;

        List<Integer> uniqueSteps = new ArrayList<>();
        for (TouchPoint tp : points) {
            if (!uniqueSteps.contains(tp.stepNumber)) uniqueSteps.add(tp.stepNumber);
        }
        Collections.sort(uniqueSteps);

        final int currentStepNum = uniqueSteps.get(groupIndex % uniqueSteps.size());
        final List<TouchPoint> group = new ArrayList<>();
        for (TouchPoint tp : points) {
            if (tp.stepNumber == currentStepNum) group.add(tp);
        }

        SharedPreferences prefs = getSharedPreferences("ClickerPrefs", MODE_PRIVATE);
        final int randPx = prefs.getInt("def_rand_px", 5);
        final int randMs = prefs.getInt("def_rand_ms", 25);

        int baseDelay = group.get(0).delayMs;
        if (group.get(0).useRandom && randMs > 0) {
            baseDelay += random.nextInt(randMs * 2 + 1) - randMs;
        }
        baseDelay = Math.max(20, baseDelay);

        handler.postDelayed(() -> {
            if (!isRunning) return;
            GestureDescription.Builder builder = new GestureDescription.Builder();
            int count = 0;
            for (TouchPoint tp : group) {
                if (count >= 10) break;
                int[] loc = new int[2];
                tp.view.getLocationOnScreen(loc);
                float cx = loc[0] + (tp.view.getWidth() / 2f);
                float cy = loc[1] + (tp.view.getHeight() / 2f);

                if (tp.useRandom && randPx > 0) {
                    cx += random.nextInt(randPx * 2 + 1) - randPx;
                    cy += random.nextInt(randPx * 2 + 1) - randPx;
                }
                cx = Math.max(1f, cx);
                cy = Math.max(1f, cy);

                Path path = new Path();
                path.moveTo(cx, cy);
                builder.addStroke(new GestureDescription.StrokeDescription(path, 0, tp.durationMs));
                count++;
            }
            dispatchGesture(builder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    runStepGroup(groupIndex + 1);
                }
                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    runStepGroup(groupIndex + 1);
                }
            }, null);
        }, baseDelay);
    }

    private void closeAll() {
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        for (TouchPoint tp : points) wm.removeView(tp.view);
        points.clear();
        if (editDialogView != null) { wm.removeView(editDialogView); editDialogView = null; }
        if (controlBar != null) { wm.removeView(controlBar); controlBar = null; }
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {}
}
