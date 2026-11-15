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
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

/**
 * Passo a passo para instalar o Linux no Android: Termux e Termux:X11 (direto do GitHub),
 * liberar o Termux para este app, pacotes, udroid, Ubuntu e português. Cada etapa se confere sozinha.
 */
public class InstallActivity extends Activity {
    static final int BG = MainActivity.BG, CARD = MainActivity.CARD, LINE = MainActivity.LINE, TEXT = MainActivity.TEXT,
            TEXT2 = MainActivity.TEXT2, ORANGE = MainActivity.ORANGE, GREEN = MainActivity.GREEN, YELLOW = MainActivity.YELLOW, RED = MainActivity.RED;

    static final String TERMUX_API = "https://api.github.com/repos/termux/termux-app/releases/latest";
    static final String X11_API = "https://api.github.com/repos/termux/termux-x11/releases/tags/nightly";

    SharedPreferences prefs;
    LinearLayout list;
    TextView summary;
    final Map<Integer, TextView[]> marks = new HashMap<>();
    Map<String, String> info = new HashMap<>();
    boolean infoOk;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("cfg", MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    boolean narrow() { return getResources().getConfiguration().screenWidthDp < 600; }
    int ind() { return narrow() ? 0 : dp(46); }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration c) {
        super.onConfigurationChanged(c);
        marks.clear(); build(); refresh();
    }

    String distro() { return prefs.getString("distro", "jammy:xfce4"); }

    // ------------------------------------------------------------------ helpers de tela
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
    LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }

    boolean installed(String pkg) {
        try { getPackageManager().getPackageInfo(pkg, 0); return true; } catch (Exception e) { return false; }
    }
    boolean fromPlayStore(String pkg) {
        try {
            String inst = Build.VERSION.SDK_INT >= 30
                    ? getPackageManager().getInstallSourceInfo(pkg).getInstallingPackageName()
                    : getPackageManager().getInstallerPackageName(pkg);
            return "com.android.vending".equals(inst);
        } catch (Exception e) { return false; }
    }
    boolean permOk() {
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission(Termux.PERM) == PackageManager.PERMISSION_GRANTED;
    }
    void copy(String s) {
        ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("comando", s));
        Toast.makeText(this, "Copiado. Cole no Termux e aperte Enter.", Toast.LENGTH_SHORT).show();
    }
    void openTermux() {
        Intent i = getPackageManager().getLaunchIntentForPackage(Termux.PKG);
        if (i != null) startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        else Toast.makeText(this, "Instale o Termux primeiro (etapa 1).", Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------------ montagem
    void build() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = narrow() ? dp(14) : dp(24);
        root.setPadding(pad, dp(20), pad, dp(32));
        sv.addView(root);

        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.addView(text("Instalar o Linux no Android", narrow() ? 21 : 24, TEXT, true));
        t.addView(text("Ubuntu " + distro() + " com Termux, Termux:X11 e udroid", 14, TEXT2, false));
        if (narrow()) {
            LinearLayout top = new LinearLayout(this);
            top.addView(button("‹ Voltar", CARD, TEXT, v -> finish()));
            top.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
            top.addView(button("↻ Conferir", CARD, TEXT, v -> refresh()));
            root.addView(top);
            t.setPadding(0, dp(14), 0, 0);
            root.addView(t);
        } else {
            LinearLayout head = new LinearLayout(this);
            head.setGravity(Gravity.CENTER_VERTICAL);
            head.addView(button("‹ Voltar", CARD, TEXT, v -> finish()));
            t.setPadding(dp(16), 0, 0, 0);
            head.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
            head.addView(button("↻ Conferir", CARD, TEXT, v -> refresh()));
            root.addView(head);
        }

        summary = text("", 16, TEXT, true);
        summary.setPadding(0, dp(18), 0, dp(4));
        root.addView(summary);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);

        String d = distro();
        // 1
        step(1, "Instalar o Termux (do GitHub)",
                "O Termux é o terminal Linux do Android. Use a versão do GitHub: a da Play Store é antiga e não conversa com o Termux:X11. O app baixa a versão certa para o seu processador e abre o instalador.",
                null, new Object[][]{
                        {"Baixar e instalar", (Runnable) () -> download(1, TERMUX_API, "Termux")},
                        {"Página no GitHub", (Runnable) () -> web("https://github.com/termux/termux-app/releases")}});
        // 2
        step(2, "Instalar o Termux:X11 (do GitHub)",
                "É a \"tela\" onde o Ubuntu aparece. Vem do release nightly oficial.",
                null, new Object[][]{
                        {"Baixar e instalar", (Runnable) () -> download(2, X11_API, "Termux:X11")},
                        {"Página no GitHub", (Runnable) () -> web("https://github.com/termux/termux-x11/releases/tag/nightly")}});
        // 3
        step(3, "Abrir o Termux uma vez e liberar este app",
                "Abra o Termux e espere ele terminar a preparação inicial (aparece o símbolo ~ $). Depois cole o comando abaixo: ele permite que o Linux no Tablet mande comandos para o Termux. Por fim, toque em \"Dar permissão\".",
                MainActivity.SETUP_CMD, new Object[][]{
                        {"Abrir o Termux", (Runnable) this::openTermux},
                        {"Dar permissão", (Runnable) () -> {
                            if (Build.VERSION.SDK_INT >= 23 && !permOk()) requestPermissions(new String[]{Termux.PERM}, 9);
                            else refresh();
                        }}});
        // 4 — processos filhos (Android 12+ mata o Linux depois de um tempo)
        String adb = "adb shell \"/system/bin/device_config set_sync_disabled_for_tests persistent; /system/bin/device_config put activity_manager max_phantom_processes 2147483647\"\nadb shell settings put global settings_enable_monitor_phantom_procs false";
        step(4, "Liberar os processos filhos (modo desenvolvedor)",
                "A partir do Android 12, o sistema encerra programas que rodam \"por baixo\" do Termux (o Ubuntu inteiro), e o Linux fecha sozinho depois de um tempo (\"Process completed (signal 9)\").\n\n"
                        + "1) Toque em \"Sobre o tablet\" e toque 7 vezes em Número da versão (no Xiaomi: Versão do HyperOS/MIUI) até aparecer \"Você agora é um desenvolvedor\".\n"
                        + "2) Toque em \"Opções do desenvolvedor\" e ATIVE \"Desativar restrições de processos filhos\" (Android 14 ou mais novo).\n"
                        + "3) Em \"Bateria do Termux\", deixe a bateria como \"Sem restrições\".\n\n"
                        + "Android 12 e 13 não têm essa opção: rode os comandos abaixo de um computador com o depurador adb (cabo USB e Depuração USB ligada).",
                adb, new Object[][]{
                        {"Sobre o tablet", (Runnable) () -> openSettings(Settings.ACTION_DEVICE_INFO_SETTINGS)},
                        {"Opções do desenvolvedor", (Runnable) () -> openSettings(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)},
                        {"Bateria do Termux", (Runnable) () -> {
                            try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + Termux.PKG))); }
                            catch (Exception e) { openSettings(Settings.ACTION_SETTINGS); }
                        }}});
        // 4
        step(5, "Atualizar o Termux",
                "Atualiza a lista de pacotes. Se perguntar algo (Y/N ou qual arquivo manter), só aperte Enter.",
                "pkg update -y && pkg upgrade -y", runBtn(5, "pkg update -y && pkg upgrade -y", "Atualizar Termux"));
        // 5
        String pk = "pkg install -y x11-repo && pkg install -y termux-x11-nightly pulseaudio procps git";
        step(6, "Instalar os pacotes da tela, do som e utilitários",
                "termux-x11-nightly (servidor da tela), pulseaudio (som), procps (conferência dos processos) e git (para baixar o udroid).",
                pk, runBtn(6, pk, "Pacotes do X11"));
        // 6
        String ud = "cd ~ && rm -rf fs-manager-udroid && git clone https://github.com/RandomCoderOrg/fs-manager-udroid.git && cd fs-manager-udroid && bash install.sh; cd ~";
        step(7, "Instalar o udroid",
                "O udroid é quem baixa e roda o Ubuntu dentro do Termux (projeto RandomCoderOrg/fs-manager-udroid).",
                ud, runBtn(7, ud, "Instalar udroid"));
        // 7
        String ins = "udroid install " + d;
        step(8, "Instalar o Ubuntu (" + d + ")",
                "Baixa o Ubuntu com a área de trabalho XFCE. É um download grande e demora — deixe o tablet carregando e com o Wi-Fi ligado. Para outra versão, troque o nome na tela inicial do app.",
                ins, runBtn(8, ins, "Instalar Ubuntu"));
        // 8
        String pt = """
                # libera as traducoes (a imagem enxuta do Ubuntu descarta /usr/share/locale)
                for f in /etc/dpkg/dpkg.cfg.d/*; do [ -f "$f" ] && sed -i '\\#usr/share/locale#d' "$f"; done
                apt update
                apt install -y locales language-pack-pt language-pack-pt-base language-pack-gnome-pt language-pack-gnome-pt-base
                locale-gen pt_BR.UTF-8
                update-locale LANG=pt_BR.UTF-8 LANGUAGE=pt_BR:pt
                # reinstala os programas para voltarem com as traducoes
                apt install --reinstall -y $(dpkg-query -W -f='${binary:Package} ${db:Status-Status}\\n' | awk '$2=="installed"{print $1}' | grep -E 'xfce|thunar|xfwm|xfdesktop|^exo|garcon|mousepad|ristretto|tumbler|libreoffice|gtk-3|gtk2')
                echo "Portugues instalado. Feche e abra o Ubuntu de novo."
                """.strip();
        String ptRun = inUbuntu(pt, "lnt-ptbr.sh");
        step(9, "Deixar o Ubuntu em português (completo)",
                "A imagem do Ubuntu do udroid vem sem as traduções dos programas, por isso menus ficam em inglês mesmo com pt-BR ligado. Este passo libera as traduções, instala o pt-BR e reinstala os programas do XFCE já traduzidos. Demora alguns minutos; depois feche e abra o Ubuntu. Para fazer à mão: \"udroid login " + d + "\", rode o comando e depois \"exit\".",
                pt, runBtn(9, ptRun, "Português no Ubuntu"));
        // extras
        TextView ex = text("Extras", 20, TEXT, true);
        ex.setPadding(0, dp(26), 0, 0);
        list.addView(ex);
        String ac = """
                # ArtCraft (storytold no GitHub): baixa tudo primeiro, instala todos os apps juntos e o launcher por ultimo
                apt update && apt install -y curl ca-certificates
                case "$(dpkg --print-architecture)" in arm64) A=aarch64;; amd64) A=x86_64;; *) A=$(uname -m);; esac
                mkdir -p /tmp/artcraft && cd /tmp/artcraft && rm -f ./*.deb
                baixa() {
                  url=$(curl -fsSL "https://api.github.com/repos/storytold/$1/releases/latest" | grep -o "https://[^\\"]*-linux-$A\\.deb\\"" | tr -d '"' | head -n 1)
                  if [ -n "$url" ] && curl -fL --retry 3 -o "$1.deb" "$url"; then echo "ok: $1"; else echo "FALHOU: $1"; fi
                }
                for app in cadcraft deckcraft designcraft effectcraft figcraft filmcraft gridcraft lightcraft pdfcraft photocraft soundcraft vectorcraft wordcraft; do baixa $app; done
                echo "== instalando todos os apps juntos =="
                apt install -y ./*craft.deb
                echo "== instalando o ArtCraft Launcher (por ultimo) =="
                baixa craft-launcher && apt install -y ./craft-launcher.deb
                cd / && rm -rf /tmp/artcraft
                echo "ArtCraft instalado: abra pelo menu Aplicativos."
                """.strip();
        step(10, "ArtCraft: todos os apps e o launcher",
                "Baixa a versão mais nova (arm64) de cada app do ArtCraft no GitHub — CADCraft, DeckCraft, DesignCraft, EffectCraft, FigCraft, FilmCraft, GridCraft, LightCraft, PdfCraft, PhotoCraft, SoundCraft, VectorCraft e WordCraft —, instala todos juntos (assim o apt resolve as dependências de uma vez) e instala o ArtCraft Launcher por último. Depois, as atualizações vêm pelo próprio launcher.",
                ac, runBtn(10, inUbuntu(ac, "lnt-artcraft.sh"), "Instalar ArtCraft"));
        String lo = "apt update && apt install -y libreoffice libreoffice-l10n-pt-br libreoffice-help-pt-br hunspell-pt-br libreoffice-gtk3";
        step(11, "LibreOffice em português (opcional)",
                "Pacote de escritório completo: Writer (textos), Calc (planilhas) e Impress (apresentações), em pt-BR e com corretor ortográfico. Depois aparece no menu Aplicativos › Escritório. É um download grande.",
                lo, runBtn(11, inUbuntu(lo, "lnt-office.sh"), "Instalar LibreOffice"));
        String w11 = """
                apt update
                apt install -y git sassc gtk2-engines-murrine gnome-themes-extra libglib2.0-dev-bin fonts-noto-core
                cd /tmp && rm -rf Fluent-gtk-theme Win11-icon-theme
                git clone --depth 1 https://github.com/vinceliuice/Fluent-gtk-theme.git && bash Fluent-gtk-theme/install.sh
                git clone --depth 1 https://github.com/yeyushengfan258/Win11-icon-theme.git && bash Win11-icon-theme/install.sh
                # aplica na proxima vez que o XFCE abrir (e se apaga depois)
                mkdir -p /root/.config/autostart
                cat > /root/.config/autostart/lnt-tema.desktop <<'TEMA'
                [Desktop Entry]
                Type=Application
                Name=Aplicar tema Windows 11
                Exec=sh -c "sleep 4; xfconf-query -c xsettings -p /Net/ThemeName -s Fluent-Dark; xfconf-query -c xsettings -p /Net/IconThemeName -s Win11-dark; xfconf-query -c xfwm4 -p /general/theme -s Fluent-Dark; xfconf-query -c xsettings -p /Gtk/FontName -s 'Noto Sans 10'; rm -f /root/.config/autostart/lnt-tema.desktop"
                TEMA
                echo "Tema instalado. Abra o Ubuntu: ele e aplicado sozinho."
                """.strip();
        step(12, "Tema Windows 11 (opcional)",
                "Instala o tema Fluent (janelas e botões no estilo do Windows 11, escuro) e os ícones Win11, e aplica sozinho na próxima vez que o Ubuntu abrir. Para trocar depois: Configurações › Aparência (estilo e ícones) e Gerenciador de janelas (bordas).",
                w11, runBtn(12, inUbuntu(w11, "lnt-tema.sh"), "Tema Windows 11"));
        String dl = """
                # liga a pasta Downloads do Ubuntu na pasta Download do Android (e cria o atalho Tablet)
                if [ ! -d /sdcard/Download ]; then echo "Sem acesso ao armazenamento do Android. Feche e abra o Ubuntu e rode de novo."; exit 1; fi
                D=$(xdg-user-dir DOWNLOAD 2>/dev/null); { [ -z "$D" ] || [ "$D" = "$HOME" ]; } && D="$HOME/Downloads"
                if [ -L "$D" ]; then rm -f "$D"
                elif [ -d "$D" ] && [ -n "$(ls -A "$D")" ]; then mv "$D" "$D-antigo"; echo "Arquivos antigos guardados em $D-antigo"
                else rm -rf "$D"; fi
                ln -s /sdcard/Download "$D"
                DESK=$(xdg-user-dir DESKTOP 2>/dev/null); [ -n "$DESK" ] && [ -d "$DESK" ] && [ ! -e "$DESK/Tablet" ] && ln -s /sdcard "$DESK/Tablet"
                B="$HOME/.config/gtk-3.0/bookmarks"; mkdir -p "$(dirname "$B")"
                grep -q "file:///sdcard/Download " "$B" 2>/dev/null || echo "file:///sdcard/Download Downloads do tablet" >> "$B"
                grep -q "file:///sdcard " "$B" 2>/dev/null || echo "file:///sdcard Tablet" >> "$B"
                echo "Pronto: $D agora e a pasta Download do Android."
                """.strip();
        String dlRun = "ls /storage/emulated/0 >/dev/null 2>&1 || { echo 'Permita o acesso aos arquivos na janela do Android...'; termux-setup-storage; sleep 6; }\n" + inUbuntu(dl, "lnt-downloads.sh");
        step(13, "Pasta Downloads do tablet no Ubuntu",
                "Faz a pasta Downloads do Ubuntu ser a mesma pasta Download do Android: o que você baixa no Ubuntu aparece no tablet e vice-versa. Também cria o atalho Tablet na Área de Trabalho e na barra lateral do gerenciador de arquivos. Se o Termux ainda não tiver acesso aos arquivos, o Android vai pedir permissão — aceite. Se for a primeira vez, feche e abra o Ubuntu e rode de novo.",
                dl, runBtn(13, dlRun, "Downloads do tablet"));
        String upT = "pkg update -y && pkg upgrade -y";
        String upU = "apt update && apt full-upgrade -y && apt autoremove -y && apt clean";
        String upAll = upT + "\nudroid upgrade || true\n" + inUbuntu(upU, "lnt-update.sh");
        step(14, "Rotina de atualização (de vez em quando)",
                "Atualiza tudo: os pacotes do Termux, os scripts do udroid e o Ubuntu, e no fim apaga o que não serve mais para liberar espaço. Uma vez por mês está ótimo. Feche o Ubuntu antes (Encerrar).\n\nTermux:  " + upT + "\nudroid:  udroid upgrade\nUbuntu (dentro dele):  " + upU,
                upAll, runBtn(14, upAll, "Atualizar tudo"));
        String disk = "echo '== Espaco livre =='; df -h $HOME | tail -n 1\necho '== Tamanho do Ubuntu =='; du -sh $PREFIX/var/lib/udroid/installed-filesystems/* 2>/dev/null\n" + inUbuntu("apt clean && echo Cache do apt limpo", "lnt-limpar.sh");
        step(15, "Ver espaço e limpar cache",
                "Mostra quanto espaço sobra no tablet e quanto o Ubuntu ocupa, e limpa os pacotes baixados que já foram instalados.",
                disk, runBtn(15, disk, "Espaço e limpeza"));

        // final
        step(16, "Pronto! Abrir com o Linux no Tablet",
                "A partir de agora é só abrir este app: ele confere o último encerramento, limpa restos, sobe o Termux:X11, inicia o Ubuntu e abre a tela. Para sair, use Sair no menu do XFCE ou o botão Encerrar.",
                null, new Object[][]{{"▶  Iniciar Ubuntu", (Runnable) () -> {
                    startActivity(new Intent(this, MainActivity.class).putExtra("start", true)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    finish();
                }}});

        setContentView(sv);
    }

    Object[][] runBtn(int n, String cmd, String label) {
        return new Object[][]{{"Rodar no Termux", (Runnable) () -> {
            if (!installed(Termux.PKG) || !permOk()) {
                Toast.makeText(this, "Faça a etapa 3 primeiro (liberar o Termux para este app).", Toast.LENGTH_LONG).show();
                return;
            }
            if (!Termux.runVisible(this, cmd, label)) Toast.makeText(this, "Não consegui abrir o Termux.", Toast.LENGTH_SHORT).show();
        }}};
    }

    /** Comando para colar no Termux que roda "cmd" dentro do Ubuntu (o udroid quebra comandos nos espaços, então vai por script na /tmp). */
    String inUbuntu(String cmd, String file) {
        return "cat > $PREFIX/tmp/" + file + " <<'EOF'\n" + cmd + "\nEOF\nudroid login " + distro() + " /bin/bash /tmp/" + file;
    }

    void openSettings(String action) {
        try { startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (Exception e) {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); } catch (Exception ignored) { }
            Toast.makeText(this, "Abra pelas Configurações do Android.", Toast.LENGTH_LONG).show();
        }
    }

    boolean devOn() {
        try { return Settings.Global.getInt(getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1; }
        catch (Exception e) { return false; }
    }

    /** marca a etapa 4: phantom = valor de persist.sys.fflag.override.settings_enable_monitor_phantom_procs (null = sem como ler) */
    void markDev(String phantom) {
        if (Build.VERSION.SDK_INT < 31) { mark(4, 1, "não precisa (Android 11 ou menor)"); return; }
        if ("false".equals(phantom)) { mark(4, 1, "processos filhos liberados"); return; }
        if (!devOn()) { mark(4, 0, "ative o modo desenvolvedor"); return; }
        if (Build.VERSION.SDK_INT >= 34) mark(4, phantom == null ? 3 : 2, phantom == null ? "confira a opção" : "falta ativar a opção");
        else mark(4, 3, "Android 12/13: use os comandos adb");
    }

    void web(String url) { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }

    void step(int n, String title, String desc, String code, Object[][] buttons) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(round(CARD, 16, LINE));
        card.setPadding(narrow() ? dp(14) : dp(18), dp(14), narrow() ? dp(14) : dp(18), dp(16));
        LinearLayout.LayoutParams cp = lp(-1, -2); cp.topMargin = dp(14);
        list.addView(card, cp);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView num = text(String.valueOf(n), 16, Color.WHITE, true);
        num.setGravity(Gravity.CENTER);
        num.setBackground(round(0xFF3A3F4B, 20, 0));
        row.addView(num, lp(dp(34), dp(34)));
        TextView tt = text(title, narrow() ? 16 : 18, TEXT, true);
        tt.setPadding(dp(12), 0, dp(8), 0);
        row.addView(tt, new LinearLayout.LayoutParams(0, -2, 1));
        TextView st = text("", 14, TEXT2, true);
        row.addView(st);
        card.addView(row);

        TextView d = text(desc, 15, TEXT2, false);
        d.setPadding(ind(), dp(8), 0, 0);
        card.addView(d);

        if (code != null) {
            HorizontalScrollView hs = new HorizontalScrollView(this);
            TextView c = text(code, 13, 0xFFE6E9EF, false);
            c.setTypeface(Typeface.MONOSPACE);
            c.setTextIsSelectable(true);
            c.setPadding(dp(12), dp(10), dp(12), dp(10));
            hs.addView(c);
            hs.setBackground(round(0xFF0C0E11, 10, 0));
            LinearLayout.LayoutParams hp = lp(-1, -2); hp.topMargin = dp(10); hp.leftMargin = ind();
            card.addView(hs, hp);
        }

        TextView prog = text("", 14, TEXT2, false);
        prog.setPadding(ind(), dp(6), 0, 0);
        prog.setVisibility(View.GONE);
        card.addView(prog);

        FlowLayout br = new FlowLayout(this, dp(10));
        br.setPadding(ind(), dp(10), 0, 0);
        boolean first = true;
        if (code != null) { br.addView(button("Copiar", CARD, TEXT, v -> copy(code))); first = false; }
        for (Object[] bt : buttons) {
            Runnable r = (Runnable) bt[1];
            Button b = button((String) bt[0], first ? ORANGE : CARD, first ? Color.WHITE : TEXT, v -> r.run());
            br.addView(b, new ViewGroup.LayoutParams(-2, -2));
            first = false;
        }
        card.addView(br);
        marks.put(n, new TextView[]{num, st, prog});
    }

    /** estado: 0 a fazer, 1 ok, 2 atenção, 3 sem como conferir */
    void mark(int n, int state, String label) {
        TextView[] v = marks.get(n);
        if (v == null) return;
        int[] bg = {0xFF3A3F4B, GREEN, YELLOW, 0xFF3A3F4B};
        v[0].setBackground(round(bg[state], 20, 0));
        v[0].setText(state == 1 ? "✔" : String.valueOf(n));
        v[0].setTextColor(state == 2 ? Color.BLACK : Color.WHITE);
        v[1].setText(label == null ? "" : label);
        v[1].setTextColor(state == 1 ? GREEN : state == 2 ? YELLOW : TEXT2);
    }

    void progress(int n, String msg, boolean err) {
        TextView[] v = marks.get(n);
        if (v == null) return;
        v[2].setVisibility(msg == null ? View.GONE : View.VISIBLE);
        v[2].setText(msg == null ? "" : msg);
        v[2].setTextColor(err ? RED : TEXT);
    }

    // ------------------------------------------------------------------ downloads
    void download(int n, String api, String label) {
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            progress(n, "Libere \"Instalar apps desconhecidos\" para o Linux no Tablet e toque em Baixar de novo.", false);
            startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));
            return;
        }
        GithubInstaller.start(this, api, label, (msg, done, err) -> {
            progress(n, msg, err);
            if (done && !err) refresh();
        });
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] p, int[] g) { refresh(); }

    // ------------------------------------------------------------------ conferência
    void refresh() {
        boolean tx = installed(Termux.PKG), x11 = installed(Termux.X11_PKG);
        mark(1, tx ? (fromPlayStore(Termux.PKG) ? 2 : 1) : 0, tx ? (fromPlayStore(Termux.PKG) ? "da Play Store — troque pela do GitHub" : "instalado") : "a fazer");
        mark(2, x11 ? 1 : 0, x11 ? "instalado" : "a fazer");
        markDev(null);
        if (!tx || !permOk()) {
            mark(3, tx && !permOk() ? 2 : 0, tx ? "falta a permissão" : "a fazer");
            for (int i = 5; i <= 15; i++) mark(i, 3, "");
            mark(16, 0, "");
            summary.setText(!tx ? "Comece pela etapa 1." : "Faça a etapa 3 para o app conferir o resto sozinho.");
            return;
        }
        summary.setText("Conferindo…");
        Termux.run(this, "verificar_instalacao.sh", distro(), 15000, r -> {
            if (r.timeout || r.out.isEmpty()) {
                mark(3, 2, "o Termux não respondeu");
                for (int i = 5; i <= 15; i++) mark(i, 3, "");
                summary.setText("Abra o Termux, cole o comando da etapa 3 e toque em Conferir.");
                return;
            }
            Map<String, String> m = MainActivity.parse(r.out);
            mark(3, 1, "liberado");
            String ph = m.get("PHANTOM");
            markDev(ph == null || ph.isEmpty() || "?".equals(ph) ? null : ph);
            boolean pk = "TEM".equals(m.get("termux-x11")) && "TEM".equals(m.get("pgrep"));
            mark(5, pk ? 1 : 3, pk ? "feito" : "");
            mark(6, pk ? 1 : 0, pk ? "instalados" : "a fazer");
            boolean ud = "TEM".equals(m.get("udroid"));
            mark(7, ud ? 1 : 0, ud ? "instalado" : "a fazer");
            boolean ds = "1".equals(m.get("DISTRO"));
            mark(8, ds ? 1 : 0, ds ? "instalado" : "a fazer");
            boolean pt = "1".equals(m.get("PTBR"));
            boolean tr = "1".equals(m.get("TRAD")) && "1".equals(m.get("APPTRAD"));
            mark(9, pt && tr ? 1 : pt ? 2 : 0, pt && tr ? "em português" : pt ? "faltam traduções — rode de novo" : "a fazer");
            boolean acL = "1".equals(m.get("ACLAUNCHER"));
            String acN = m.get("ACAPPS");
            mark(10, acL ? 1 : 3, acL ? "launcher + " + (acN == null ? "?" : acN) + " apps" : "opcional");
            boolean of = "1".equals(m.get("OFFICE"));
            mark(11, of ? 1 : 3, of ? "instalado" : "opcional");
            boolean tm = "1".equals(m.get("TEMA"));
            mark(12, tm ? 1 : 3, tm ? "instalado" : "opcional");
            boolean dk = "1".equals(m.get("DOWNLOADS"));
            mark(13, dk ? 1 : 3, dk ? "ligada ao tablet" : "opcional");
            mark(14, 3, "quando quiser");
            mark(15, 3, "quando quiser");
            boolean all = x11 && pk && ud && ds;
            mark(16, all ? 1 : 0, all ? "tudo pronto" : "");
            summary.setText(all ? (pt ? "Tudo pronto! Toque em Iniciar Ubuntu na última etapa." : "Tudo pronto. Falta só o português (etapa 9, opcional).")
                    : "Siga as etapas que ainda não têm ✔.");
        });
    }
}
