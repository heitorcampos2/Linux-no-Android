package com.heitorcampos.linuxnotablet;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** Roda rotinas de shell dentro do Termux pela intenção RUN_COMMAND e devolve o resultado. */
public final class Termux {
    public static final String PKG = "com.termux";
    public static final String X11_PKG = "com.termux.x11";
    public static final String PERM = "com.termux.permission.RUN_COMMAND";
    static final String BASH = "/data/data/com.termux/files/usr/bin/bash";
    static final String HOME = "/data/data/com.termux/files/home";

    public static class Result {
        public int exit = -1;
        public String out = "", err = "", errmsg = "";
        public boolean timeout;
        public boolean ok() { return !timeout && exit == 0; }
        public String all() { return (out + (err.isEmpty() ? "" : "\n" + err) + (errmsg.isEmpty() ? "" : "\n" + errmsg)).trim(); }
    }
    public interface Callback { void done(Result r); }

    private static final Handler UI = new Handler(Looper.getMainLooper());
    private static final Map<Integer, Callback> PENDING = new HashMap<>();
    private static int seq = 100;

    static String asset(Context c, String name) {
        try (InputStream in = c.getAssets().open(name)) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            byte[] buf = new byte[4096]; int n;
            while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
            return b.toString("UTF-8");
        } catch (Exception e) { return ""; }
    }

    /** script = nome do arquivo em assets; distro vira $1. */
    public static void run(Context c, String script, String distro, long timeoutMs, Callback cb) {
        final int id = ++seq;
        String body = asset(c, "comum.sh") + "\n" + asset(c, script);
        Intent res = new Intent(c, ResultReceiver.class).putExtra("id", id);
        int fl = PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 31) fl |= PendingIntent.FLAG_MUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(c, id, res, fl);

        Intent i = new Intent("com.termux.RUN_COMMAND");
        i.setClassName(PKG, "com.termux.app.RunCommandService");
        i.putExtra("com.termux.RUN_COMMAND_PATH", BASH);
        // a rotina vai em base64: assim o texto dela não aparece na linha de comando
        // e o pgrep/pkill das próprias rotinas não se encontram por engano
        String b64 = android.util.Base64.encodeToString(body.getBytes(java.nio.charset.StandardCharsets.UTF_8), android.util.Base64.NO_WRAP);
        i.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-c", "eval \"$(printf %s \"$2\" | base64 -d)\"", "linux-no-tablet", distro, b64});
        i.putExtra("com.termux.RUN_COMMAND_WORKDIR", HOME);
        i.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);
        i.putExtra("com.termux.RUN_COMMAND_COMMAND_LABEL", "Linux no Tablet: " + script);
        i.putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", pi);
        synchronized (PENDING) { PENDING.put(id, cb); }
        try {
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
        } catch (Exception e) {
            Result r = new Result(); r.errmsg = "Não consegui chamar o Termux: " + e.getMessage();
            finish(id, r); return;
        }
        UI.postDelayed(() -> { Result r = new Result(); r.timeout = true; finish(id, r); }, timeoutMs);
    }

    /** Roda um comando numa sessão visível do Termux (o usuário acompanha o progresso). */
    public static boolean runVisible(Context c, String cmd, String label) {
        String full = "P=/data/data/com.termux/files/usr; [ -z \"$LD_PRELOAD\" ] && for f in $P/lib/libtermux-exec-ld-preload.so $P/lib/libtermux-exec.so; do [ -f $f ] && { export LD_PRELOAD=$f; break; }; done\n" + cmd + "\necho\necho '==> Pronto! Pode voltar ao Linux no Tablet.'\nread -r -p 'Aperte Enter para fechar esta aba ' _";
        Intent i = new Intent("com.termux.RUN_COMMAND");
        i.setClassName(PKG, "com.termux.app.RunCommandService");
        i.putExtra("com.termux.RUN_COMMAND_PATH", BASH);
        i.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", new String[]{"-c", full});
        i.putExtra("com.termux.RUN_COMMAND_WORKDIR", HOME);
        i.putExtra("com.termux.RUN_COMMAND_BACKGROUND", false);
        i.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0");
        i.putExtra("com.termux.RUN_COMMAND_COMMAND_LABEL", label);
        try {
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i); else c.startService(i);
            return true;
        } catch (Exception e) { return false; }
    }

    static void deliver(int id, Bundle b) {
        Result r = new Result();
        if (b != null) {
            r.out = s(b.getString("stdout"));
            r.err = s(b.getString("stderr"));
            r.exit = b.getInt("exitCode", -1);
            r.errmsg = s(b.getString("errmsg"));
            if (b.getInt("err", -1) > 0 && r.errmsg.isEmpty()) r.errmsg = "erro " + b.getInt("err");
        } else r.errmsg = "resposta vazia do Termux";
        UI.post(() -> finish(id, r));
    }

    private static void finish(int id, Result r) {
        Callback cb;
        synchronized (PENDING) { cb = PENDING.remove(id); }
        if (cb != null) cb.done(r);
    }
    private static String s(String v) { return v == null ? "" : v.trim(); }
}
