package com.beeiptv.media.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.beeiptv.media.net.Channel;
import com.beeiptv.media.net.Session;
import com.beeiptv.media.net.Stalker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ChannelsActivity extends Activity {
    private final List<Channel> shown = new ArrayList<>();
    private final List<TextView> chips = new ArrayList<>();
    private final List<String> chipIds = new ArrayList<>();
    private BaseAdapter adapter;
    private TextView count;
    private TextView exp;
    private String selectedGenre = null;
    private String query = "";
    private boolean opening;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Session.stalker == null) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        }
        buildUi();
        styleChips();
        filter();
        load();
    }

    private void load() {
        if (Session.loaded) return;
        final Stalker s = Session.stalker;
        new Thread(() -> {
            final String e = s.expiry();
            runOnUiThread(() -> {
                if (Session.stalker != s) return;
                Session.expiry = e;
                if (!isFinishing()) exp.setText(Ui.expiryText(e));
            });
        }).start();
        new Thread(() -> {
            try {
                final List<Channel> ch = s.channels();
                runOnUiThread(() -> {
                    if (Session.stalker != s) return;
                    Session.channels = ch;
                    Session.loaded = true;
                    if (!isFinishing()) filter();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (Session.stalker == s && !isFinishing()) {
                        count.setText("تعذر تحميل القنوات: " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);
        int pad = Ui.dp(this, 16);
        root.setPadding(pad, pad, pad, 0);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("Lumen TV");
        title.setTextColor(Ui.ACCENT);
        title.setTextSize(24);
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView change = new TextView(this);
        change.setText("تغيير الحساب");
        change.setTextColor(Ui.MUTED);
        change.setTextSize(13);
        change.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 8));
        change.setOnClickListener(v -> {
            Session.stalker = null;
            Intent i = new Intent(this, SignInActivity.class);
            i.putExtra("manual", true);
            startActivity(i);
            finish();
        });
        head.addView(change);
        root.addView(head);

        exp = new TextView(this);
        exp.setText(Session.loaded ? Ui.expiryText(Session.expiry) : "ينتهي الاشتراك: جاري التحميل…");
        exp.setTextColor(Ui.TEXT);
        exp.setTextSize(15);
        exp.setBackground(Ui.round(Ui.CARD, 12, this));
        int p = Ui.dp(this, 14);
        exp.setPadding(p, p, p, p);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ep.topMargin = Ui.dp(this, 12);
        root.addView(exp, ep);

        EditText search = new EditText(this);
        search.setHint("ابحث عن قناة…");
        search.setHintTextColor(Ui.MUTED);
        search.setTextColor(Ui.TEXT);
        search.setSingleLine(true);
        search.setBackground(Ui.round(Ui.CARD, 12, this));
        search.setPadding(p, p, p, p);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = Ui.dp(this, 10);
        root.addView(search, sp);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                query = s.toString();
                filter();
            }
        });

        if (!Session.genres.isEmpty()) {
            HorizontalScrollView hs = new HorizontalScrollView(this);
            hs.setHorizontalScrollBarEnabled(false);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.addView(chip("الكل", null));
            for (String[] g : Session.genres) row.addView(chip(g[1], g[0]));
            hs.addView(row);
            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            hp.topMargin = Ui.dp(this, 10);
            root.addView(hs, hp);
        }

        count = new TextView(this);
        count.setTextColor(Ui.MUTED);
        count.setTextSize(12);
        count.setPadding(Ui.dp(this, 4), Ui.dp(this, 10), 0, Ui.dp(this, 6));
        root.addView(count);

        ListView list = new ListView(this);
        list.setDivider(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        list.setDividerHeight(Ui.dp(this, 6));
        list.setSelector(android.R.color.transparent);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return shown.size(); }
            @Override public Object getItem(int i) { return shown.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View v, ViewGroup parent) {
                TextView t;
                if (v instanceof TextView) {
                    t = (TextView) v;
                } else {
                    t = new TextView(ChannelsActivity.this);
                    t.setTextColor(Ui.TEXT);
                    t.setTextSize(16);
                    t.setSingleLine(true);
                    t.setBackground(Ui.round(Ui.CARD, 10, ChannelsActivity.this));
                    int q = Ui.dp(ChannelsActivity.this, 14);
                    t.setPadding(q, q, q, q);
                    t.setLayoutParams(new android.widget.AbsListView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                }
                Channel c = shown.get(i);
                t.setText((c.number.isEmpty() ? "" : c.number + "   ") + c.name);
                return t;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((a, v, pos, id) -> {
            hideKeyboard(search);
            play(shown.get(pos));
        });
        root.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private TextView chip(String text, final String genreId) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(13);
        t.setSingleLine(true);
        int h = Ui.dp(this, 14);
        int v = Ui.dp(this, 8);
        t.setPadding(h, v, h, v);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = Ui.dp(this, 8);
        t.setLayoutParams(lp);
        t.setOnClickListener(x -> {
            selectedGenre = genreId;
            styleChips();
            filter();
        });
        chips.add(t);
        chipIds.add(genreId);
        return t;
    }

    private void styleChips() {
        for (int i = 0; i < chips.size(); i++) {
            boolean sel = Objects.equals(chipIds.get(i), selectedGenre);
            TextView t = chips.get(i);
            t.setTextColor(sel ? Ui.BG : Ui.TEXT);
            t.setBackground(Ui.round(sel ? Ui.ACCENT : Ui.CARD, 18, this));
        }
    }

    private void hideKeyboard(View v) {
        InputMethodManager m = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (m != null) m.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private void filter() {
        String q = query.trim().toLowerCase(Locale.ROOT);
        shown.clear();
        for (Channel c : Session.channels) {
            if (selectedGenre != null && !selectedGenre.equals(c.genre)) continue;
            if (q.isEmpty() || c.name.toLowerCase(Locale.ROOT).contains(q) || c.number.equals(q)) {
                shown.add(c);
            }
        }
        if (Session.loaded) {
            count.setText(shown.size() + " قناة");
        } else {
            count.setText("جاري تحميل القنوات…");
        }
        adapter.notifyDataSetChanged();
    }

    private void play(final Channel ch) {
        if (opening) return;
        opening = true;
        final Stalker s = Session.stalker;
        new Thread(() -> {
            try {
                String url = s.link(ch);
                runOnUiThread(() -> {
                    opening = false;
                    Intent i = new Intent(this, PlayerActivity.class);
                    i.putExtra("url", url);
                    i.putExtra("name", ch.name);
                    i.putExtra("mac", s.getMac());
                    startActivity(i);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    opening = false;
                    Toast.makeText(this, "تعذر فتح القناة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
