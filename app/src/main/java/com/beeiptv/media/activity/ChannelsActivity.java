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
    private final List<String[]> shownGroups = new ArrayList<>(); // {id, name, count}
    private final List<TextView> modeChips = new ArrayList<>();
    private EditText search;
    // 0 = الكل (قنوات + أقسام) ، 1 = قنوات فقط ، 2 = أقسام فقط
    private int mode = 0;
