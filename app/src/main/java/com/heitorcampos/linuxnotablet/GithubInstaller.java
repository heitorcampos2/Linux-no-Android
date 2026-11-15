package com.heitorcampos.linuxnotablet;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Baixa o APK mais recente de um release do GitHub e instala pelo instalador do Android. */
public final class GithubInstaller {
    public interface Progress { void update(String msg, boolean done, boolean error); }

    static Progress current;
    static final Handler UI = new Handler(Looper.getMainLooper());

    static void post(String m, boolean done, boolean err) {
        UI.post(() -> { if (current != null) current.update(m, done, err); });
    }

    static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000); c.setReadTimeout(30000);
        c.setRequestProperty("User-Agent", "LinuxNoTablet");
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setInstanceFollowRedirects(true);
        return c;
    }

    /** Escolhe o APK do processador do aparelho (ou o universal). */
    static JSONObject pick(JSONArray assets) throws Exception {
        String[] abis = Build.SUPPORTED_ABIS;
        for (String abi : abis)
            for (int i = 0; i < assets.length(); i++) {
                JSONObject a = assets.getJSONObject(i);
                String n = a.getString("name");
                if (n.endsWith(".apk") && !n.contains("sharedUid") && n.contains(abi)) return a;
            }
        for (int i = 0; i < assets.length(); i++) {
            JSONObject a = assets.getJSONObject(i);
            String n = a.getString("name");
            if (n.endsWith(".apk") && !n.contains("sharedUid") && n.contains("universal")) return a;
        }
        for (int i = 0; i < assets.length(); i++) {
            JSONObject a = assets.getJSONObject(i);
            if (a.getString("name").endsWith(".apk")) return a;
        }
        return null;
    }

    public static void start(Context ctx, String apiUrl, String label, Progress p) {
        current = p;
        final Context c = ctx.getApplicationContext();
        new Thread(() -> {
            try {
                post("Procurando a versão mais nova no GitHub…", false, false);
                HttpURLConnection h = open(apiUrl);
                if (h.getResponseCode() != 200) throw new Exception("GitHub respondeu " + h.getResponseCode());
                ByteArrayOutputStream b = new ByteArrayOutputStream();
                try (InputStream in = h.getInputStream()) { byte[] buf = new byte[8192]; int n; while ((n = in.read(buf)) > 0) b.write(buf, 0, n); }
                JSONObject rel = new JSONObject(b.toString("UTF-8"));
                JSONObject a = pick(rel.getJSONArray("assets"));
                if (a == null) throw new Exception("não achei o APK nesse release");
                installFrom(c, a.getString("browser_download_url"), a.getString("name"), a.optLong("size", -1), label);
            } catch (Exception e) {
                post("Não deu certo: " + e.getMessage(), true, true);
            }
        }).start();
    }

    /** Baixa um APK de um link direto e instala (usado na atualização do próprio app). */
    public static void startDirect(Context ctx, String apkUrl, String label, Progress p) {
        current = p;
        final Context c = ctx.getApplicationContext();
        new Thread(() -> {
            try { installFrom(c, apkUrl, label + ".apk", -1, label); }
            catch (Exception e) { post("Não deu certo: " + e.getMessage(), true, true); }
        }).start();
    }

    /** Lê um texto (ex.: version.json) do GitHub. */
    public static String fetchText(String url) throws Exception {
        HttpURLConnection h = open(url);
        h.setRequestProperty("Accept", "*/*");
        h.setUseCaches(false);
        if (h.getResponseCode() != 200) throw new Exception("GitHub respondeu " + h.getResponseCode());
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (InputStream in = h.getInputStream()) { byte[] buf = new byte[8192]; int n; while ((n = in.read(buf)) > 0) b.write(buf, 0, n); }
        return b.toString("UTF-8");
    }

    static void installFrom(Context c, String url, String name, long size, String label) throws Exception {
        {
            {
                HttpURLConnection d = open(url);
                d.setRequestProperty("Accept", "application/octet-stream");
                if (d.getResponseCode() != 200) throw new Exception("download respondeu " + d.getResponseCode());
                if (size <= 0) size = d.getContentLengthLong();

                PackageInstaller pi = c.getPackageManager().getPackageInstaller();
                PackageInstaller.SessionParams sp = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                int sid = pi.createSession(sp);
                try (PackageInstaller.Session s = pi.openSession(sid)) {
                    try (InputStream in = d.getInputStream(); OutputStream out = s.openWrite("apk", 0, size > 0 ? size : -1)) {
                        byte[] buf = new byte[65536]; long got = 0; int n, last = -1;
                        while ((n = in.read(buf)) > 0) {
                            out.write(buf, 0, n); got += n;
                            int pct = size > 0 ? (int) (got * 100 / size) : -1;
                            if (pct != last) { last = pct; post("Baixando " + name + (pct >= 0 ? "  " + pct + "%" : "  " + got / 1048576 + " MB"), false, false); }
                        }
                        s.fsync(out);
                    }
                    post("Instalando " + label + "… confirme na tela do Android.", false, false);
                    Intent cb = new Intent(c, InstallReceiver.class).putExtra("label", label);
                    int fl = PendingIntent.FLAG_UPDATE_CURRENT;
                    if (Build.VERSION.SDK_INT >= 31) fl |= PendingIntent.FLAG_MUTABLE;
                    PendingIntent pend = PendingIntent.getBroadcast(c, sid, cb, fl);
                    s.commit(pend.getIntentSender());
                }
            }
        }
    }

    /** Resposta do instalador do Android. */
    public static class InstallReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            int st = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
            String label = intent.getStringExtra("label");
            if (st == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                Intent confirm = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirm != null) {
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try { ctx.startActivity(confirm); } catch (Exception ignored) { }
                }
                return;
            }
            if (st == PackageInstaller.STATUS_SUCCESS) { post(label + " instalado.", true, false); return; }
            String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            if (st == PackageInstaller.STATUS_FAILURE_CONFLICT || st == PackageInstaller.STATUS_FAILURE_INCOMPATIBLE
                    || (msg != null && msg.contains("signatures")))
                post("Já existe um " + label + " com outra assinatura (provavelmente da Play Store). Desinstale ele antes e tente de novo — os dados dele serão apagados.", true, true);
            else if (st == PackageInstaller.STATUS_FAILURE_ABORTED)
                post("Instalação cancelada.", true, true);
            else post("Falhou ao instalar" + (msg != null ? ": " + msg : "."), true, true);
        }
    }
}
