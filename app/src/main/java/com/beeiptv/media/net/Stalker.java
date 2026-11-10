package com.beeiptv.media.net;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public final class Stalker {
    public static final String UA =
            "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) "
                    + "MAG250 stbapp ver: 4 rev: 1812 Mobile Safari/533.3";

    private final String loadUrl;
    private final String mac;
    private String token = "";

    public Stalker(String portal, String mac) {
        this.loadUrl = normalize(portal);
        this.mac = mac.trim().toUpperCase();
    }

    public String getMac() {
        return mac;
    }

    static String normalize(String p) {
        p = p.trim();
        if (!p.toLowerCase().startsWith("http")) p = "http://" + p;
        while (p.endsWith("/")) p = p.substring(0, p.length() - 1);
        if (p.endsWith("/server/load.php")) return p;
        if (p.endsWith("/c")) p = p.substring(0, p.length() - 2);
        return p + "/server/load.php";
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private JSONObject call(String query) throws Exception {
        URL url = new URL(loadUrl + "?" + query + "&JsHttpRequest=1-xml");
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setConnectTimeout(12000);
        c.setReadTimeout(25000);
        c.setRequestProperty("User-Agent", UA);
        c.setRequestProperty("X-User-Agent", "Model: MAG250; Link: WiFi");
        c.setRequestProperty("Accept", "*/*");
        c.setRequestProperty("Cookie", "mac=" + enc(mac) + "; stb_lang=en; timezone=GMT");
        if (!token.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + token);
        int code = c.getResponseCode();
        if (code >= 400) throw new IOException("HTTP " + code);
        InputStream in = c.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        c.disconnect();
        String body = out.toString("UTF-8").trim();
        if (body.isEmpty() || body.charAt(0) != '{') throw new IOException("Bad response");
        return new JSONObject(body);
    }

    public void login() throws Exception {
        JSONObject r = call("type=stb&action=handshake&token=");
        JSONObject js = r.optJSONObject("js");
        if (js == null || js.optString("token").isEmpty()) throw new IOException("Handshake failed");
        token = js.optString("token");

        call("type=stb&action=get_profile&hd=1&ver=ImageDescription:%200.2.18-r14-pub-250;"
                + "%20PORTAL%20version:%205.5.0;%20API%20Version:%20JS%20API%20version:%20328;"
                + "&num_banks=2&sn=&stb_type=MAG250&client_type=STB&image_version=218"
                + "&video_out=hdmi&device_id=&device_id2=&signature=&auth_second_step=1"
                + "&hw_version=1.7-BD-00&not_valid_token=0&metrics=%7B%7D&hw_version_2=&api_signature=262"
                + "&prehash=&timestamp=" + (System.currentTimeMillis() / 1000));
    }

    public String expiry() {
        String[] queries = {
                "type=account_info&action=get_main_info",
                "type=stb&action=get_profile&hd=1&auth_second_step=1&api_signature=262"
        };
        for (String q : queries) {
            try {
                JSONObject js = call(q).optJSONObject("js");
                if (js == null) continue;
                String[] keys = {"end_date", "expire_billing_date", "tariff_expired_date", "expiration", "exp_date", "phone"};
                for (String k : keys) {
                    String v = js.optString(k, "").trim();
                    if (usable(v, k)) {
                        return v;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    private static boolean usable(String v, String key) {
        if (v.isEmpty() || v.equals("null") || v.equals("0") || v.startsWith("0000")) return false;
        if (key.equals("phone")) {
            return v.length() >= 8 && (v.matches(".*[A-Za-z].*")
                    || v.matches("\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}.*")
                    || v.matches("\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{4}.*"));
        }
        return true;
    }

    public List<String[]> genres() {
        List<String[]> out = new ArrayList<>();
        try {
            JSONArray a = call("type=itv&action=get_genres").optJSONArray("js");
            if (a != null) {
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.optJSONObject(i);
                    if (o == null) continue;
                    String id = o.optString("id");
                    String title = o.optString("title").trim();
                    if (title.isEmpty() || id.equals("*") || id.equals("0")) continue;
                    out.add(new String[]{id, title});
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public List<Channel> channels() throws Exception {
        List<Channel> list = new ArrayList<>();
        JSONObject r = call("type=itv&action=get_all_channels");
        JSONObject js = r.optJSONObject("js");
        JSONArray data = js == null ? null : js.optJSONArray("data");
        if (data != null) {
            addAll(list, data);
        }
        if (list.isEmpty()) {
            for (int p = 1; p <= 200; p++) {
                JSONObject pr = call("type=itv&action=get_ordered_list&genre=*&force_ch_link_check=&fav=0"
                        + "&sortby=number&hd=0&p=" + p);
                JSONObject pj = pr.optJSONObject("js");
                JSONArray pd = pj == null ? null : pj.optJSONArray("data");
                if (pd == null || pd.length() == 0) break;
                addAll(list, pd);
                int total = pj.optInt("total_items", 0);
                if (total > 0 && list.size() >= total) break;
            }
        }
        return list;
    }

    private static void addAll(List<Channel> out, JSONArray data) {
        for (int i = 0; i < data.length(); i++) {
            JSONObject o = data.optJSONObject(i);
            if (o == null) continue;
            String name = o.optString("name", "").trim();
            if (name.isEmpty()) continue;
            out.add(new Channel(o.optString("id"), name, o.optString("number"), o.optString("cmd"),
                    o.optString("tv_genre_id")));
        }
    }

    public String link(Channel ch) throws Exception {
        JSONObject r = call("type=itv&action=create_link&cmd=" + enc(ch.cmd)
                + "&series=&forced_storage=0&disable_ad=0&download=0&force_ch_link_check=0");
        JSONObject js = r.optJSONObject("js");
        String cmd = js == null ? "" : js.optString("cmd", "");
        if (cmd.isEmpty()) cmd = ch.cmd;
        int i = cmd.indexOf("http");
        if (i > 0) cmd = cmd.substring(i);
        cmd = cmd.trim();
        if (!cmd.startsWith("http")) throw new IOException("No stream link");
        return cmd;
    }
}
