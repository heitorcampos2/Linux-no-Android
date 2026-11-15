package com.heitorcampos.linuxnotablet;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Sobre: versão, autor, atualização pelo GitHub e créditos dos projetos usados. */
public class AboutActivity extends Activity {
    static final int BG = MainActivity.BG, CARD = MainActivity.CARD, LINE = MainActivity.LINE, TEXT = MainActivity.TEXT,
            TEXT2 = MainActivity.TEXT2, ORANGE = MainActivity.ORANGE, GREEN = MainActivity.GREEN, RED = MainActivity.RED;

    static final String[][] CREDITS = {
            {"Termux", "Terminal Linux para Android", "https://github.com/termux/termux-app", "GPLv3"},
            {"Termux:X11", "Servidor da tela (X11) para Android", "https://github.com/termux/termux-x11", "GPLv3"},
            {"udroid (ubuntu-on-android)", "Gerenciador do Ubuntu dentro do Termux", "https://github.com/RandomCoderOrg/fs-manager-udroid", "GPLv3"},
            {"Ubuntu e XFCE", "Sistema e área de trabalho", "https://xfce.org", "GPL / LGPL"},
            {"Fluent GTK theme", "Tema das janelas (vinceliuice)", "https://github.com/vinceliuice/Fluent-gtk-theme", "GPLv3"},
            {"Win11 icon theme", "Ícones (yeyushengfan258)", "https://github.com/yeyushengfan258/Win11-icon-theme", "GPLv3"},
            {"ArtCraft", "Suíte de apps criativos (storytold)", "https://github.com/storytold", "MIT / Apache-2.0"},
            {"LibreOffice", "Pacote de escritório", "https://www.libreoffice.org", "MPL-2.0"},
    };

    TextView upStatus;
    LinearLayout upBox;

    int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    GradientDrawable round(int c, float r, int stroke) {
        GradientDrawable g = new GradientDrawable(); g.setColor(c); g.setCornerRadius(dp(r));
        if (stroke != 0) g.setStroke(dp(1), stroke); return g;
    }
    TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    Button button(String s, int bg, int fg, View.OnClickListener l) {
        Button b = new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(15); b.setTextColor(fg);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setBackground(round(bg, 12, bg == CARD ? LINE : 0));
        b.setPadding(dp(16), dp(10), dp(16), dp(10)); b.setStateListAnimator(null); b.setOnClickListener(l); return b;
    }
    LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(round(CARD, 16, LINE));
        c.setPadding(dp(18), dp(16), dp(18), dp(16));
        return c;
    }
    void web(String u) { try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); } catch (Exception ignored) { } }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        boolean narrow = getResources().getConfiguration().screenWidthDp < 600;

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = narrow ? dp(14) : dp(24);
        root.setPadding(pad, dp(20), pad, dp(32));
        sv.addView(root);

        root.addView(button("‹ Voltar", CARD, TEXT, v -> finish()), new LinearLayout.LayoutParams(-2, -2));

        // app
        LinearLayout app = card();
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2); cp.topMargin = dp(16);
        root.addView(app, cp);
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = text("▣", 30, Color.WHITE, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(round(ORANGE, 16, 0));
        head.addView(logo, new LinearLayout.LayoutParams(dp(64), dp(64)));
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setPadding(dp(16), 0, 0, 0);
        t.addView(text("Linux no Tablet", 24, TEXT, true));
        t.addView(text("Versão " + Updater.myName(this), 15, TEXT2, false));
        t.addView(text("Feito por Heitor Campos", 15, TEXT2, false));
        head.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        app.addView(head);
        TextView desc = text("Abre o Ubuntu (udroid + XFCE) no Android com um toque: confere o último encerramento, limpa restos, sobe o Termux:X11 e o XFCE, e guia a instalação do zero.", 15, TEXT2, false);
        desc.setPadding(0, dp(12), 0, 0);
        app.addView(desc);
        FlowLayout links = new FlowLayout(this, dp(10));
        links.setPadding(0, dp(12), 0, 0);
        links.addView(button("Página no GitHub", CARD, TEXT, v -> web(Updater.REPO)), new ViewGroup.LayoutParams(-2, -2));
        links.addView(button("Novidades", CARD, TEXT, v -> web(Updater.REPO + "#novidades-da-" + Updater.myName(this).replace(".", ""))), new ViewGroup.LayoutParams(-2, -2));
        app.addView(links);

        // atualização
        LinearLayout up = card();
        LinearLayout.LayoutParams up2 = new LinearLayout.LayoutParams(-1, -2); up2.topMargin = dp(14);
        root.addView(up, up2);
        up.addView(text("Atualização automática", 18, TEXT, true));
        TextView ud = text("O app confere o GitHub sempre que abre e avisa quando tem versão nova. A atualização baixa e instala por cima, sem perder nada.", 14, TEXT2, false);
        ud.setPadding(0, dp(6), 0, 0);
        up.addView(ud);
        upStatus = text("", 15, TEXT, false);
        upStatus.setPadding(0, dp(10), 0, 0);
        up.addView(upStatus);
        upBox = new LinearLayout(this);
        upBox.setOrientation(LinearLayout.VERTICAL);
        up.addView(upBox);
        FlowLayout ub = new FlowLayout(this, dp(10));
        ub.setPadding(0, dp(10), 0, 0);
        ub.addView(button("Verificar agora", ORANGE, Color.WHITE, v -> check()), new ViewGroup.LayoutParams(-2, -2));
        up.addView(ub);

        // créditos
        TextView ct = text("Créditos", 20, TEXT, true);
        ct.setPadding(0, dp(24), 0, dp(4));
        root.addView(ct);
        TextView cd = text("O Linux no Tablet só automatiza estes projetos de código aberto. Todo o mérito do Linux rodando no Android é deles. Toque para abrir.", 14, TEXT2, false);
        root.addView(cd);
        for (String[] c : CREDITS) {
            LinearLayout row = card();
            row.setPadding(dp(16), dp(12), dp(16), dp(12));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2); rp.topMargin = dp(10);
            root.addView(row, rp);
            LinearLayout l1 = new LinearLayout(this);
            l1.setGravity(Gravity.CENTER_VERTICAL);
            l1.addView(text(c[0], 16, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
            l1.addView(text(c[3], 12, TEXT2, false));
            row.addView(l1);
            row.addView(text(c[1], 14, TEXT2, false));
            TextView url = text(c[2].replace("https://", ""), 13, ORANGE, false);
            row.addView(url);
            row.setOnClickListener(v -> web(c[2]));
        }
        TextView foot = text("\nTermux, Termux:X11 e udroid não são afiliados a este app.", 12, TEXT2, false);
        foot.setGravity(Gravity.CENTER);
        root.addView(foot, new LinearLayout.LayoutParams(-1, -2));

        setContentView(sv);
        check();
    }

    void check() {
        upStatus.setTextColor(TEXT);
        upStatus.setText("Conferindo no GitHub…");
        upBox.removeAllViews();
        Updater.check(this, (n, err) -> {
            if (err != null) { upStatus.setTextColor(RED); upStatus.setText("Não consegui conferir agora (" + err + ")."); return; }
            if (n == null) { upStatus.setTextColor(GREEN); upStatus.setText("✔ Você está na versão mais recente (" + Updater.myName(this) + ")."); return; }
            upStatus.setTextColor(ORANGE);
            upStatus.setText("Nova versão " + n.version + " disponível" + (n.date.isEmpty() ? "" : " (" + n.date + ")"));
            if (!n.notes.isEmpty()) { TextView nt = text(n.notes, 14, TEXT2, false); nt.setPadding(0, dp(6), 0, 0); upBox.addView(nt); }
            TextView prog = text("", 14, TEXT, false);
            Button go = button("Atualizar agora", ORANGE, Color.WHITE, v -> {
                if (!Updater.install(this, n, (msg, done, e) -> { prog.setTextColor(e ? RED : TEXT); prog.setText(msg); }))
                    prog.setText("Libere \"Instalar apps desconhecidos\" para o Linux no Tablet e toque de novo.");
            });
            LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-2, -2); gp.topMargin = dp(10);
            upBox.addView(go, gp);
            prog.setPadding(0, dp(8), 0, 0);
            upBox.addView(prog);
        });
    }
}
