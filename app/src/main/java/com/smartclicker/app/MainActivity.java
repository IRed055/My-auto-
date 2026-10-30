package com.smartclicker.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private EditText etDelay, etDuration, etRandomPx, etRandomMs, etSize, etAlpha;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("ClickerPrefs", MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(Color.parseColor("#F5F7FA"));

        TextView title = new TextView(this);
        title.setText("النقر الذكي - الإعدادات الأساسية");
        title.setTextSize(20);
        title.setTextColor(Color.parseColor("#1A202C"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 30);
        layout.addView(title);

        Button btnAcc = createButton("1. تفعيل خدمة إمكانية الوصول (إلزامي)", "#3182CE");
        btnAcc.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            Toast.makeText(this, "ابحث عن (النقر الذكي) وقم بتفعيله", Toast.LENGTH_LONG).show();
        });
        layout.addView(btnAcc);

        etDelay = addInputField(layout, "وقت الانتظار الافتراضي قبل اللمسة (مللي ثانية):", String.valueOf(prefs.getInt("def_delay", 300)));
        etDuration = addInputField(layout, "مدة اللمسة الافتراضية (مللي ثانية):", String.valueOf(prefs.getInt("def_duration", 60)));
        etRandomPx = addInputField(layout, "نطاق العشوائية في المكان لمنع الحظر (بكسل):", String.valueOf(prefs.getInt("def_rand_px", 5)));
        etRandomMs = addInputField(layout, "نطاق العشوائية في الوقت لمنع الحظر (مللي ثانية):", String.valueOf(prefs.getInt("def_rand_ms", 25)));
        etSize = addInputField(layout, "حجم دائرة اللمس (من 60 إلى 200):", String.valueOf(prefs.getInt("def_size", 110)));
        etAlpha = addInputField(layout, "شفافية الدوائر واللوحة (من 20 إلى 100%):", String.valueOf(prefs.getInt("def_alpha", 85)));

        Button btnSave = createButton("2. حفظ الإعدادات الافتراضية", "#38A169");
        btnSave.setOnClickListener(v -> savePreferences());
        layout.addView(btnSave);

        Button btnStart = createButton("3. إظهار شريط التحكم العائم", "#805AD5");
        btnStart.setOnClickListener(v -> {
            savePreferences();
            if (ClickerService.instance != null) {
                ClickerService.instance.showControlBar();
                Toast.makeText(this, "تم إظهار الشريط العائم", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "يرجى تفعيل خدمة إمكانية الوصول أولاً من الزر الأول!", Toast.LENGTH_LONG).show();
            }
        });
        layout.addView(btnStart);

        scroll.addView(layout);
        setContentView(scroll);
    }

    private void savePreferences() {
        try {
            prefs.edit()
                .putInt("def_delay", Integer.parseInt(etDelay.getText().toString()))
                .putInt("def_duration", Integer.parseInt(etDuration.getText().toString()))
                .putInt("def_rand_px", Integer.parseInt(etRandomPx.getText().toString()))
                .putInt("def_rand_ms", Integer.parseInt(etRandomMs.getText().toString()))
                .putInt("def_size", Math.max(60, Math.min(250, Integer.parseInt(etSize.getText().toString()))))
                .putInt("def_alpha", Math.max(20, Math.min(100, Integer.parseInt(etAlpha.getText().toString()))))
                .apply();
            Toast.makeText(this, "تم حفظ الإعدادات بنجاح", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "تأكد من إدخال أرقام صحيحة", Toast.LENGTH_SHORT).show();
        }
    }

    private EditText addInputField(LinearLayout parent, String labelText, String defaultVal) {
        TextView tv = new TextView(this);
        tv.setText(labelText);
        tv.setTextColor(Color.parseColor("#2D3748"));
        tv.setPadding(0, 20, 0, 5);
        parent.addView(tv);

        EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setText(defaultVal);
        et.setBackgroundColor(Color.WHITE);
        et.setPadding(20, 15, 20, 15);
        parent.addView(et);
        return et;
    }

    private Button createButton(String text, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setBackgroundColor(Color.parseColor(colorHex));
        btn.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 25, 0, 10);
        btn.setLayoutParams(lp);
        return btn;
    }
}
