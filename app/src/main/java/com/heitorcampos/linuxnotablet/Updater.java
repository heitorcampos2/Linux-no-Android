package com.heitorcampos.linuxnotablet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

/** Atualização automática pelo GitHub: lê o version.json do repositório e instala o APK novo. */
public final class Updater {
    public static final String REPO = "https://github.com/heitorcampos2/Linux-no-Android";
    static final String VERSION_URL = "https://raw.githubusercontent.com/heitorcampos2/Linux-no-Android/main/version.json";

    public static class Info {
        public String version = "", apk = "", date = "";
        public long code;
        public String notes = "";
    }
    public interface Result { void done(Info newer, String error); }

    public static long myCode(Context c) {
        try {
            android.content.pm.PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
        } catch (Exception e) { return 0; }
    }
    public static String myName(Context c) {
        try { return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName; } catch (Exception e) { return "?"; }
    }

    /** Confere no GitHub; devolve a versão nova (ou null se já está na mais recente). */
    public static void check(Context c, Result r) {
        final Context ctx = c.getApplicationContext();
        Handler ui = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                JSONObject j = new JSONObject(GithubInstaller.fetchText(VERSION_URL + "?t=" + System.currentTimeMillis()));
                Info i = new Info();
                i.version = j.optString("version");
                i.code = j.optLong("code");
                i.apk = j.optString("apk", REPO + "/raw/main/download/LinuxNoTablet.apk");
                i.date = j.optString("data");
                JSONArray n = j.optJSONArray("notas");
                StringBuilder sb = new StringBuilder();
                if (n != null) for (int k = 0; k < n.length(); k++) sb.append("• ").append(n.getString(k)).append('\n');
                i.notes = sb.toString().trim();
                Info out = i.code > myCode(ctx) ? i : null;
                ui.post(() -> r.done(out, null));
            } catch (Exception e) {
                ui.post(() -> r.done(null, e.getMessage() == null ? "sem conexão" : e.getMessage()));
            }
        }).start();
    }

    /** Baixa e instala a versão nova. Devolve false se antes precisa liberar "instalar apps desconhecidos". */
    public static boolean install(Activity a, Info i, GithubInstaller.Progress p) {
        if (Build.VERSION.SDK_INT >= 26 && !a.getPackageManager().canRequestPackageInstalls()) {
            a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.getPackageName())));
            return false;
        }
        GithubInstaller.startDirect(a, i.apk, "Linux no Tablet " + i.version, p);
        return true;
    }
}
