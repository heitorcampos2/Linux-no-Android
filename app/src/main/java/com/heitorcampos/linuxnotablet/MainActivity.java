package com.heitorcampos.linuxnotablet;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

/**
 * Linux no Tablet: abre o Ubuntu (udroid) com o Termux:X11 do jeito certo.
 * Confere se o encerramento anterior foi limpo, limpa o que sobrou, sobe o Termux:X11,
 * inicia o XFCE e só então abre a tela. Também encerra tudo conferindo se não sobrou nada.
 */
public class MainActivity extends Activity {
    static final int BG = 0xFF121418, CARD = 0xFF1C1F26, LINE = 0xFF2A2E38, TEXT = 0xFFF1F3F7,
            TEXT2 = 0xFF9AA3B2, ORANGE = 0xFFE95420, GREEN = 0xFF3FCF7F, YELLOW = 0xFFF2B33D, RED = 0xFFFF5A5F;

    static final String SETUP_CMD =
            "mkdir -p ~/.termux && f=~/.termux/termux.properties && touch $f && "
            + "(grep -q '^ *allow-external-apps' $f && sed -i 's/^ *allow-external-apps.*/allow-external-apps = true/' $f "
            + "|| echo 'allow-external-apps = true' >> $f) && termux-reload-settings && echo PRONTO";

    final Handler ui = new Handler(Looper.getMainLooper());
    SharedPreferences prefs;
    LinearLayout steps, help;
    TextView status, log;
    Button startBtn, stopBtn, histBtn, instBtn;
    LinearLayout btns, root, update;
    EditText distroField;
    final Map<String, TextView[]> rows = new HashMap<>();
    boolean busy, waitingPerm, semTraducao;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("cfg", MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildUi();
        if (b == null) ui.postDelayed(this::checkUpdate, 1500);
        if (b == null && (!installed(Termux.PKG) || !installed(Termux.X11_PKG))) {
            status.setText("Primeiro, vamos instalar o Linux no Android.");
            ui.postDelayed(this::openInstall, 300);
        } else if (b == null && getIntent().getBooleanExtra("start", false))
            ui.postDelayed(this::start, 400);
    }

    /** Confere o GitHub e, se tiver versão nova, mostra o aviso com as novidades e o botão de atualizar. */
    void checkUpdate() {
        Updater.check(this, (n, err) -> {
            if (n == null || isFinishing()) return;
            update.removeAllViews();
            update.addView(text("⬆  Nova versão " + n.version + " disponível", 18, ORANGE, true));
            if (!n.notes.isEmpty()) {
                TextView nt = text(n.notes, 14, TEXT, false);
                nt.setPadding(0, dp(6), 0, 0);
                update.addView(nt);
            }
            TextView prog = text("", 14, TEXT2, false);
            FlowLayout row = new FlowLayout(this, dp(10));
            row.setPadding(0, dp(10), 0, 0);
            row.addView(button("Atualizar agora", ORANGE, Color.WHITE, v -> {
                if (!Updater.install(this, n, (msg, done, e) -> { prog.setTextColor(e ? RED : TEXT); prog.setText(msg); }))
                    prog.setText("Libere \"Instalar apps desconhecidos\" para o Linux no Tablet e toque de novo.");
            }));
            row.addView(button("Depois", CARD, TEXT, v -> update.setVisibility(View.GONE)));
            update.addView(row);
            prog.setPadding(0, dp(8), 0, 0);
            update.addView(prog);
            update.setVisibility(View.VISIBLE);
        });
    }

    void openInstall() { saveDistro(); startActivity(new Intent(this, InstallActivity.class)); }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (i.getBooleanExtra("start", false)) ui.postDelayed(this::start, 300);
    }

    // ------------------------------------------------------------------ tela
    int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    GradientDrawable round(int color, float r, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(r));
        if (stroke != 0) g.setStroke(dp(1), stroke);
        return g;
    }

    TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    Button button(String s, int bg, int fg, View.OnClickListener l) {
        Button btn = new Button(this);
        btn.setText(s); btn.setAllCaps(false); btn.setTextSize(17); btn.setTextColor(fg);
        btn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        btn.setBackground(round(bg, 14, bg == CARD ? LINE : 0));
        btn.setPadding(dp(22), dp(14), dp(22), dp(14));
        btn.setOnClickListener(l);
        btn.setStateListAnimator(null);
        return btn;
    }

    LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }

    void buildUi() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(24);
        root.setPadding(pad, dp(28), pad, pad);
        sv.addView(root);

        // cabeçalho
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = text("▣", 26, Color.WHITE, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(round(ORANGE, 14, 0));
        head.addView(logo, lp(dp(52), dp(52)));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(16), 0, 0, 0);
        titles.addView(text("Linux no Tablet", 26, TEXT, true));
        titles.addView(text("Ubuntu (udroid) + Termux:X11", 15, TEXT2, false));
        head.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        Button about = button("ⓘ Sobre", CARD, TEXT, v -> startActivity(new Intent(this, AboutActivity.class)));
        about.setTextSize(14);
        about.setPadding(dp(14), dp(8), dp(14), dp(8));
        head.addView(about);
        root.addView(head);

        // aviso de versão nova (atualização pelo GitHub)
        update = new LinearLayout(this);
        update.setOrientation(LinearLayout.VERTICAL);
        update.setBackground(round(0xFF2A1A12, 16, ORANGE));
        update.setPadding(dp(18), dp(14), dp(18), dp(14));
        update.setVisibility(View.GONE);
        LinearLayout.LayoutParams upp = lp(-1, -2); upp.topMargin = dp(18);
        root.addView(update, upp);

        status = text("Pronto para iniciar.", 18, TEXT, true);
        status.setPadding(0, dp(22), 0, dp(12));
        root.addView(status);

        // botões principais
        btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.VERTICAL);
        startBtn = button("▶  Iniciar Ubuntu", ORANGE, Color.WHITE, v -> start());
        stopBtn = button("■  Encerrar", CARD, TEXT, v -> stop());
        histBtn = button("Histórico", CARD, TEXT, v -> history());
        instBtn = button("Instalação", CARD, TEXT, v -> openInstall());
        root.addView(btns);
        layoutButtons();

        // etapas
        steps = new LinearLayout(this);
        steps.setOrientation(LinearLayout.VERTICAL);
        steps.setBackground(round(CARD, 16, LINE));
        steps.setPadding(dp(18), dp(8), dp(18), dp(8));
        LinearLayout.LayoutParams sp = lp(-1, -2); sp.topMargin = dp(18);
        root.addView(steps, sp);
        addRow("pre", "Termux, Termux:X11 e permissão");
        addRow("conf", "Conferir o último encerramento");
        addRow("limpa", "Limpar restos (travas e processos)");
        addRow("x11", "Iniciar o Termux:X11 (tela :0)");
        addRow("linux", "Iniciar o Ubuntu (XFCE)");
        addRow("abrir", "Abrir a tela do Termux:X11");

        // ajuda (aparece quando falta configurar algo)
        help = new LinearLayout(this);
        help.setOrientation(LinearLayout.VERTICAL);
        help.setBackground(round(0xFF2A2116, 16, 0xFF5A4320));
        help.setPadding(dp(18), dp(14), dp(18), dp(14));
        help.setVisibility(View.GONE);
        LinearLayout.LayoutParams hp = lp(-1, -2); hp.topMargin = dp(16);
        root.addView(help, hp);

        // opções
        LinearLayout opts = new LinearLayout(this);
        opts.setOrientation(LinearLayout.VERTICAL);
        opts.setBackground(round(CARD, 16, LINE));
        opts.setPadding(dp(18), dp(12), dp(18), dp(12));
        LinearLayout.LayoutParams op = lp(-1, -2); op.topMargin = dp(16);
        root.addView(opts, op);
        // 2.7: o Ubuntu só inicia quando o usuário toca em Iniciar Ubuntu
        LinearLayout drow = new LinearLayout(this);
        drow.setGravity(Gravity.CENTER_VERTICAL);
        drow.addView(text("Linux do udroid:", 16, TEXT2, false));
        distroField = new EditText(this);
        distroField.setText(prefs.getString("distro", "jammy:xfce4"));
        distroField.setTextColor(TEXT); distroField.setTextSize(16);
        distroField.setSingleLine(true);
        distroField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        distroField.setOnFocusChangeListener((v, f) -> { if (!f) saveDistro(); });
        LinearLayout.LayoutParams dfp = new LinearLayout.LayoutParams(0, -2, 1); dfp.leftMargin = dp(12);
        drow.addView(distroField, dfp);
        opts.addView(drow);

        // registro
        log = text("", 13, TEXT2, false);
        log.setTypeface(Typeface.MONOSPACE);
        log.setTextIsSelectable(true);
        log.setBackground(round(0xFF0C0E11, 12, LINE));
        log.setPadding(dp(14), dp(12), dp(14), dp(12));
        log.setVisibility(View.GONE);
        LinearLayout.LayoutParams lgp = lp(-1, -2); lgp.topMargin = dp(16);
        root.addView(log, lgp);

        setContentView(sv);
    }

    boolean narrow() { return getResources().getConfiguration().screenWidthDp < 600; }

    /** Tablet/paisagem: os 4 botões numa linha. Celular na vertical: Iniciar em cima, os outros 3 embaixo. */
    void layoutButtons() {
        for (View v : new View[]{startBtn, stopBtn, histBtn, instBtn})
            if (v.getParent() != null) ((ViewGroup) v.getParent()).removeView(v);
        btns.removeAllViews();
        boolean n = narrow();
        for (Button b : new Button[]{stopBtn, histBtn, instBtn}) b.setTextSize(n ? 15 : 17);
        if (n) {
            btns.addView(startBtn, lp(-1, -2));
            LinearLayout r = new LinearLayout(this);
            LinearLayout.LayoutParams rp = lp(-1, -2); rp.topMargin = dp(10);
            btns.addView(r, rp);
            for (int i = 0; i < 3; i++) {
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
                if (i > 0) p.leftMargin = dp(8);
                Button b = new Button[]{stopBtn, histBtn, instBtn}[i];
                b.setPadding(dp(6), dp(12), dp(6), dp(12));
                r.addView(b, p);
            }
        } else {
            LinearLayout r = new LinearLayout(this);
            btns.addView(r, lp(-1, -2));
            float[] w = {1.4f, 1f, 0.8f, 0.8f};
            Button[] bs = {startBtn, stopBtn, histBtn, instBtn};
            for (int i = 0; i < 4; i++) {
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, w[i]);
                if (i > 0) p.leftMargin = dp(12);
                bs[i].setPadding(dp(22), dp(14), dp(22), dp(14));
                r.addView(bs[i], p);
            }
        }
        if (root != null) { int pad = n ? dp(14) : dp(24); root.setPadding(pad, dp(n ? 18 : 28), pad, pad); }
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration c) {
        super.onConfigurationChanged(c);
        layoutButtons();
    }

    void addRow(String key, String title) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, dp(10), 0, dp(10));
        TextView icon = text("○", 20, TEXT2, true);
        icon.setGravity(Gravity.CENTER);
        r.addView(icon, lp(dp(34), -2));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(8), 0, 0, 0);
        TextView t = text(title, 17, TEXT, false);
        TextView d = text("", 14, TEXT2, false);
        d.setVisibility(View.GONE);
        col.addView(t); col.addView(d);
        r.addView(col, new LinearLayout.LayoutParams(0, -2, 1));
        steps.addView(r);
        rows.put(key, new TextView[]{icon, d});
    }

    /** estado: 0 aguardando, 1 rodando, 2 ok, 3 aviso, 4 erro, 5 pulado */
    void mark(String key, int st, String detail) {
        TextView[] v = rows.get(key);
        if (v == null) return;
        String[] ic = {"○", "◌", "✔", "!", "✖", "–"};
        int[] col = {TEXT2, ORANGE, GREEN, YELLOW, RED, TEXT2};
        v[0].setText(ic[st]); v[0].setTextColor(col[st]);
        if (detail == null || detail.isEmpty()) v[1].setVisibility(View.GONE);
        else { v[1].setText(detail); v[1].setVisibility(View.VISIBLE); v[1].setTextColor(st == 4 ? RED : st == 3 ? YELLOW : TEXT2); }
    }

    void resetRows() { for (String k : rows.keySet()) mark(k, 0, null); }

    void setBusy(boolean b, String msg) {
        busy = b;
        startBtn.setEnabled(!b); stopBtn.setEnabled(!b);
        startBtn.setAlpha(b ? .5f : 1f); stopBtn.setAlpha(b ? .5f : 1f);
        if (msg != null) status.setText(msg);
    }

    void showLog(String s) {
        if (s == null || s.trim().isEmpty()) { log.setVisibility(View.GONE); return; }
        log.setText(s.trim()); log.setVisibility(View.VISIBLE);
    }

    String distro() {
        String d = distroField.getText().toString().trim();
        return d.isEmpty() ? "jammy:xfce4" : d;
    }
    void saveDistro() { prefs.edit().putString("distro", distro()).apply(); }

    // ------------------------------------------------------------------ ajuda
    void showHelp(String title, String msg, String cmd, boolean openTermux, String url) {
        help.removeAllViews();
        help.addView(text(title, 17, YELLOW, true));
        TextView m = text(msg, 15, TEXT, false);
        m.setPadding(0, dp(6), 0, dp(8));
        help.addView(m);
        if (cmd != null) {
            TextView c = text(cmd, 13, TEXT, false);
            c.setTypeface(Typeface.MONOSPACE);
            c.setTextIsSelectable(true);
            c.setBackground(round(0xFF0C0E11, 10, 0));
            c.setPadding(dp(12), dp(10), dp(12), dp(10));
            help.addView(c);
        }
        FlowLayout row = new FlowLayout(this, dp(10));
        row.setPadding(0, dp(10), 0, 0);
        if (cmd != null) row.addView(button("Copiar comando", CARD, TEXT, v -> copy(cmd)));
        if (openTermux) row.addView(button("Abrir o Termux", CARD, TEXT, v -> openPkg(Termux.PKG)));
        if (url != null) row.addView(button("Baixar", CARD, TEXT, v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)))));
        row.addView(button("Passo a passo de instalação", CARD, TEXT, v -> openInstall()));
        help.addView(row);
        help.setVisibility(View.VISIBLE);
    }

    void copy(String s) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("comando", s));
        Toast.makeText(this, "Copiado. Cole no Termux e aperte Enter.", Toast.LENGTH_LONG).show();
    }

    boolean installed(String pkg) {
        try { getPackageManager().getPackageInfo(pkg, 0); return true; } catch (Exception e) { return false; }
    }

    boolean openPkg(String pkg) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) return false;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(i); return true; } catch (Exception e) { return false; }
    }

    // ------------------------------------------------------------------ iniciar
    boolean preCheck() {
        help.setVisibility(View.GONE);
        waitingPerm = false;
        if (!installed(Termux.PKG)) {
            mark("pre", 4, "O Termux não está instalado.");
            showHelp("Falta o Termux", "Instale o Termux pelo GitHub ou F-Droid (a versão da Play Store não funciona com o Termux:X11).",
                    null, false, "https://github.com/termux/termux-app/releases");
            return false;
        }
        if (!installed(Termux.X11_PKG)) {
            mark("pre", 4, "O app Termux:X11 não está instalado.");
            showHelp("Falta o Termux:X11", "Instale o app Termux:X11 (arquivo app-arm64-v8a-debug.apk da última versão).",
                    null, false, "https://github.com/termux/termux-x11/releases");
            return false;
        }
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Termux.PERM) != PackageManager.PERMISSION_GRANTED) {
            mark("pre", 3, "Toque em Permitir para o app poder pedir comandos ao Termux.");
            waitingPerm = true;
            requestPermissions(new String[]{Termux.PERM}, 7);
            return false;
        }
        mark("pre", 2, null);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] p, int[] g) {
        if (code != 7) return;
        if (g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED) start();
        else {
            mark("pre", 4, "Permissão negada.");
            setBusy(false, "Sem a permissão não dá para controlar o Termux.");
            showHelp("Permissão do Termux", "Abra Configurações › Apps › Linux no Tablet › Permissões › Outras permissões e libere \"Executar comandos no ambiente do Termux\".",
                    null, false, null);
            Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
            try { startActivity(i); } catch (Exception ignored) { }
        }
    }

    void start() {
        if (busy) return;
        saveDistro();
        resetRows(); showLog(null);
        setBusy(true, "Conferindo…");
        mark("pre", 1, null);
        if (!preCheck()) { setBusy(false, waitingPerm ? "Aguardando a permissão…" : "Falta configurar uma coisa (veja abaixo)."); return; }
        mark("conf", 1, null);
        Termux.run(this, "conferir.sh", distro(), 15000, r -> {
            if (r.timeout || (r.exit != 0 && r.out.isEmpty())) {
                mark("conf", 4, r.timeout ? "O Termux não respondeu." : r.all());
                setBusy(false, "O Termux ainda não aceita comandos de outro app.");
                showHelp("Configuração de uma vez só",
                        "Abra o Termux, cole o comando abaixo e aperte Enter. Ele libera o Termux para receber comandos deste app (allow-external-apps). Depois volte aqui e toque em Iniciar.",
                        SETUP_CMD, true, null);
                return;
            }
            Map<String, String> m = parse(r.out);
            semTraducao = "0".equals(m.get("TRADUZIDO"));
            StringBuilder falta = new StringBuilder();
            if ("FALTA".equals(m.get("termux-x11"))) falta.append("pkg install x11-repo -y && pkg install termux-x11-nightly -y\n");
            if ("FALTA".equals(m.get("pgrep"))) falta.append("pkg install procps -y\n");
            if ("FALTA".equals(m.get("udroid"))) falta.append("# instale o udroid: https://github.com/RandomCoderOrg/ubuntu-on-android\n");
            if (falta.length() > 0) {
                mark("conf", 4, "Falta pacote no Termux.");
                setBusy(false, "Falta instalar pacote no Termux (veja abaixo).");
                showHelp("Falta pacote no Termux", "Rode no Termux:", falta.toString().trim(), true, null);
                return;
            }
            boolean x = "1".equals(m.get("X")) || "1".equals(m.get("XABS")), s = "1".equals(m.get("SESSAO")), sock = "1".equals(m.get("SOQUETE"));
            boolean proot = "1".equals(m.get("PROOT")), lock = "1".equals(m.get("TRAVA"));
            String est = m.get("ESTADO");
            if (x && s && sock) {
                mark("conf", 2, "O Ubuntu já está aberto. Só vou abrir a tela.");
                mark("limpa", 5, null); mark("x11", 5, null); mark("linux", 5, null);
                openX11();
                return;
            }
            boolean dirty = "rodando".equals(est) || x || s || proot || lock || sock;
            if (dirty) {
                StringBuilder why = new StringBuilder("O último encerramento não foi limpo:");
                if ("rodando".equals(est)) why.append(" sessão não fechou;");
                if (x) why.append(" Termux:X11 ainda rodando;");
                if (s || proot) why.append(" Ubuntu pela metade;");
                if (lock || sock) why.append(" trava da tela :0 sobrando;");
                mark("conf", 3, why.toString().replaceAll(";$", "."));
                clean();
            } else {
                mark("conf", 2, "nenhum".equals(est) ? "Primeira vez por aqui." : "Encerramento anterior OK.");
                mark("limpa", 5, "Nada para limpar.");
                startX();
            }
        });
    }

    void clean() {
        mark("limpa", 1, null);
        status.setText("Limpando restos da sessão anterior…");
        Termux.run(this, "limpar.sh", distro(), 25000, r -> {
            if (!r.ok()) { fail("limpa", r, "Não consegui limpar tudo."); return; }
            if (r.out.contains("AVISO_X")) mark("limpa", 3, "Sobrou um servidor X que não consegui fechar; vou tentar usar ou reiniciar.");
            else mark("limpa", 2, "Travas e processos antigos removidos.");
            startX();
        });
    }

    void startX() {
        mark("x11", 1, null);
        status.setText("Iniciando o Termux:X11…");
        Termux.run(this, "iniciar_x.sh", distro(), 30000, r -> {
            if (!r.ok()) {
                fail("x11", r, "O Termux:X11 não subiu.");
                if (r.out.contains("OUTRO_X") || r.out.contains("already running"))
                    showHelp("Outro servidor X está segurando a tela :0",
                            "Feche o app Termux:X11 (arraste ele para fora nos apps recentes) e, no Termux, rode o comando abaixo. Depois toque em Iniciar de novo.",
                            "pkill -f termux.x11; pkill -f termux-x11; pkill Xwayland; rm -f $PREFIX/tmp/.X0-lock $PREFIX/tmp/.X11-unix/X0", true, null);
                return;
            }
            mark("x11", 2, r.out.contains("ja estava") ? "Já estava rodando." : "Tela :0 pronta.");
            startLinux();
        });
    }

    void startLinux() {
        mark("linux", 1, "Pode levar uns segundos…");
        status.setText("Iniciando o Ubuntu…");
        Termux.run(this, "iniciar_linux.sh", distro(), 45000, r -> {
            if (!r.ok()) { fail("linux", r, "O Ubuntu não abriu."); return; }
            if (semTraducao) mark("linux", 3, "XFCE no ar, mas parte dos menus fica em inglês: faça a etapa 9 em Instalação.");
            else mark("linux", 2, r.out.contains("ja estava") ? "Já estava rodando." : "XFCE no ar.");
            openX11();
        });
    }

    void openX11() {
        mark("abrir", 1, null);
        if (openPkg(Termux.X11_PKG)) {
            mark("abrir", 2, null);
            setBusy(false, "Ubuntu aberto. Para sair, use Sair no menu do XFCE ou o botão Encerrar aqui.");
        } else {
            mark("abrir", 4, "Não consegui abrir o app Termux:X11.");
            setBusy(false, "Abra o app Termux:X11 manualmente.");
        }
    }

    void fail(String key, Termux.Result r, String msg) {
        mark(key, 4, r.timeout ? "Demorou demais (sem resposta do Termux)." : msg);
        setBusy(false, msg + " Veja o registro abaixo.");
        showLog(r.all());
    }

    // ------------------------------------------------------------------ encerrar
    void stop() {
        if (busy) return;
        if (!installed(Termux.PKG) || (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Termux.PERM) != PackageManager.PERMISSION_GRANTED)) {
            start(); return;
        }
        resetRows(); showLog(null); help.setVisibility(View.GONE);
        setBusy(true, "Encerrando o Ubuntu e o Termux:X11…");
        Termux.run(this, "encerrar.sh", distro(), 45000, r -> {
            if (r.ok()) {
                setBusy(false, r.out.contains("forcado")
                        ? "Encerrado. O XFCE não fechou sozinho, então forcei — mas não sobrou nada rodando."
                        : "Encerrado certinho: nada sobrou rodando.");
            } else {
                setBusy(false, r.timeout ? "O Termux não respondeu ao encerrar." : "Algo ficou rodando. Veja o registro.");
                showLog(r.all());
            }
        });
    }

    void history() {
        if (busy) return;
        if (!installed(Termux.PKG)) { preCheck(); return; }
        Termux.run(this, "historico.sh", distro(), 15000, r -> showLog(r.timeout ? "O Termux não respondeu." : r.all()));
    }

    static Map<String, String> parse(String out) {
        Map<String, String> m = new HashMap<>();
        for (String line : out.split("\n")) {
            String[] p = line.trim().split(" ", 2);
            if (p.length < 2) continue;
            if (p[0].equals("TEM") || p[0].equals("FALTA")) m.put(p[1].trim(), p[0]);
            else m.put(p[0], p[1].trim());
        }
        return m;
    }
}
