package com.beeiptv.media.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.beeiptv.media.net.Session;
import com.beeiptv.media.net.Stalker;

import java.util.ArrayList;
import java.util.List;

public class SignInActivity extends Activity {
    private EditText etPortal;
    private EditText etMac;
    private Button btn;
    private TextView status;
    private SharedPreferences prefs;
    private boolean busy;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("lumen", MODE_PRIVATE);
        Stalker.cacheDir = getCacheDir();
        buildUi();
        handle(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handle(intent);
    }

    private void handle(Intent in) {
        String d = in.getStringExtra("domain");
        String a = in.getStringExtra("address");
        if (notEmpty(d) && notEmpty(a)) {
            etPortal.setText(d);
            etMac.setText(a);
            login();
            return;
        }
        String sp = prefs.getString("portal", "");
        String sm = prefs.getString("mac", "");
        etPortal.setText(sp);
        etMac.setText(sm);
        if (!in.getBooleanExtra("manual", false) && notEmpty(sp) && notEmpty(sm) && Session.stalker == null) {
            login();
        }
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setBackgroundColor(Ui.BG);
        int pad = Ui.dp(this, 24);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Lumen TV");
        title.setTextColor(Ui.ACCENT);
        title.setTextSize(34);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("سجّل الدخول ببوابة Stalker");
        sub.setTextColor(Ui.MUTED);
        sub.setTextSize(14);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 28));
        root.addView(sub);

        etPortal = field("رابط البوابة  http://host/c/", InputType.TYPE_TEXT_VARIATION_URI);
        etMac = field("MAC  00:1A:79:XX:XX:XX", InputType.TYPE_CLASS_TEXT);
        root.addView(etPortal);
        root.addView(etMac);

        btn = new Button(this);
        btn.setText("دخول");
        btn.setTextColor(Ui.BG);
        btn.setTextSize(16);
        btn.setBackground(Ui.round(Ui.ACCENT, 14, this));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52));
        lp.topMargin = Ui.dp(this, 8);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(v -> login());
        root.addView(btn);

        status = new TextView(this);
        status.setTextColor(Ui.MUTED);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, Ui.dp(this, 16), 0, 0);
        root.addView(status);

        setContentView(root);
    }

    private EditText field(String hint, int type) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Ui.MUTED);
        e.setTextColor(Ui.TEXT);
        e.setInputType(type);
        e.setSingleLine(true);
        e.setBackground(Ui.round(Ui.CARD, 12, this));
        int p = Ui.dp(this, 16);
        e.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(this, 12);
        e.setLayoutParams(lp);
        return e;
    }

    private void setBusy(boolean v, String msg) {
        busy = v;
        btn.setEnabled(!v);
        btn.setAlpha(v ? 0.5f : 1f);
        status.setTextColor(Ui.MUTED);
        status.setText(msg);
    }

    private void login() {
        if (busy) return;
        final String portal = etPortal.getText().toString().trim();
        final String mac = etMac.getText().toString().trim();
        if (portal.isEmpty() || mac.isEmpty()) {
            status.setTextColor(Ui.WARN);
            status.setText("أدخل رابط البوابة و الـ MAC");
            return;
        }
        setBusy(true, "جاري تسجيل الدخول…");
        new Thread(() -> {
            try {
                Stalker s = new Stalker(portal, mac);
                s.login();
                List<String[]> genres = s.loginGenres();
                runOnUiThread(() -> {
                    Session.stalker = s;
                    Session.expiry = "";
                    Session.genres = genres;
                    Session.channels = new ArrayList<>();
                    Session.loaded = false;
                    prefs.edit().putString("portal", portal).putString("mac", mac).apply();
                    Intent i = new Intent(this, ChannelsActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(i);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setBusy(false, "");
                    status.setTextColor(Ui.WARN);
                    status.setText("فشل الدخول: " + e.getMessage());
                });
            }
        }).start();
    }
}
