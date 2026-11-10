package com.beeiptv.media.activity;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

final class Ui {
    static final int BG = Color.parseColor("#0B0E14");
    static final int CARD = Color.parseColor("#151A24");
    static final int ACCENT = Color.parseColor("#00D4A6");
    static final int TEXT = Color.parseColor("#E8ECF3");
    static final int MUTED = Color.parseColor("#8A94A6");
    static final int WARN = Color.parseColor("#FF6B6B");

    private Ui() {}

    static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics());
    }

    static GradientDrawable round(int color, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    static Date parse(String raw) {
        raw = raw.trim();
        try {
            if (raw.matches("\\d{9,10}")) return new Date(Long.parseLong(raw) * 1000L);
        } catch (Exception ignored) {
        }
        String[] fmts = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "MMMM d, yyyy, hh:mm a",
                "MMMM d, yyyy", "EEEE, MMMM d, yyyy, hh:mm a", "dd-MM-yyyy", "dd.MM.yyyy"};
        for (String f : fmts) {
            try {
                return new SimpleDateFormat(f, Locale.ENGLISH).parse(raw);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    static String expiryText(String raw) {
        if (raw == null || raw.isEmpty()) return "تاريخ الانتهاء: غير معروف";
        Date d = parse(raw);
        if (d == null) return "ينتهي الاشتراك: " + raw;
        String shown = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(d);
        long days = TimeUnit.MILLISECONDS.toDays(d.getTime() - System.currentTimeMillis());
        if (days < 0) return "انتهى الاشتراك: " + shown;
        return "ينتهي الاشتراك: " + shown + "  (" + days + " يوم متبقي)";
    }
}
