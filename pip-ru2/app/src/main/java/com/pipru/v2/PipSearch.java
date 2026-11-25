package com.pipru.v2;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

/** Лёгкий поиск в интернете: краткая выжимка из Википедии (без ключей и сторонних библиотек). */
final class PipSearch {
    static final class Result {
        /** ok, none, offline, error */
        String status = "error";
        String title = "";
        String text = "";
    }

    private PipSearch() {
    }

    static boolean online(Context c) {
        try {
            ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            Network n = cm.getActiveNetwork();
            if (n == null) return false;
            NetworkCapabilities nc = cm.getNetworkCapabilities(n);
            return nc != null && nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (RuntimeException e) {
            return false;
        }
    }

    static Result run(Context c, String query) {
        Result r = new Result();
        if (!online(c)) {
            r.status = "offline";
            return r;
        }
        HttpURLConnection con = null;
        try {
            String url = "https://ru.wikipedia.org/w/api.php?action=query&format=json&redirects=1"
                    + "&generator=search&gsrnamespace=0&gsrlimit=3&gsrsearch=" + URLEncoder.encode(query, "UTF-8")
                    + "&prop=extracts&exintro=1&explaintext=1&exsentences=4&exlimit=3";
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setConnectTimeout(6000);
            con.setReadTimeout(8000);
            con.setRequestProperty("User-Agent", "PipRu2/1.0 (Android voice pet)");
            con.setRequestProperty("Accept", "application/json");
            if (con.getResponseCode() != 200) {
                r.status = "error";
                return r;
            }
            JSONObject root = new JSONObject(read(con.getInputStream()));
            JSONObject q = root.optJSONObject("query");
            JSONObject pages = q == null ? null : q.optJSONObject("pages");
            if (pages == null || pages.length() == 0) {
                r.status = "none";
                return r;
            }
            JSONObject best = null;
            int bestIdx = Integer.MAX_VALUE;
            Iterator<String> it = pages.keys();
            while (it.hasNext()) {
                JSONObject p = pages.optJSONObject(it.next());
                if (p == null) continue;
                String ex = p.optString("extract", "").trim();
                if (ex.isEmpty()) continue;
                int idx = p.optInt("index", 99);
                if (idx < bestIdx) {
                    bestIdx = idx;
                    best = p;
                }
            }
            if (best == null) {
                r.status = "none";
                return r;
            }
            r.title = best.optString("title", "");
            r.text = shorten(best.optString("extract", ""));
            r.status = r.text.isEmpty() ? "none" : "ok";
            return r;
        } catch (IOException e) {
            r.status = online(c) ? "error" : "offline";
            if ("error".equals(r.status) && (e instanceof java.net.UnknownHostException
                    || e instanceof java.net.SocketTimeoutException || e instanceof java.net.ConnectException)) {
                r.status = "offline";
            }
            return r;
        } catch (Exception e) {
            r.status = "error";
            return r;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static String read(InputStream in) throws IOException {
        try (InputStream is = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > 512 * 1024) break;
            }
            return out.toString("UTF-8");
        }
    }

    /** Убираем скобки (произношения, даты), оставляем первые предложения до ~380 знаков. */
    static String shorten(String s) {
        s = s.replaceAll("\\([^()]*\\)", "").replaceAll("\\[[^\\]]*\\]", "");
        s = s.replaceAll("\\s+", " ").replace(" ,", ",").replace(" .", ".").trim();
        if (s.length() <= 380) return s;
        int cut = -1;
        for (int i = 0; i < 380; i++) {
            char ch = s.charAt(i);
            if ((ch == '.' || ch == '!' || ch == '?') && i + 1 < s.length() && s.charAt(i + 1) == ' ') cut = i + 1;
        }
        if (cut < 60) {
            int sp = s.lastIndexOf(' ', 380);
            return s.substring(0, sp > 0 ? sp : 380) + "…";
        }
        return s.substring(0, cut);
    }
}
