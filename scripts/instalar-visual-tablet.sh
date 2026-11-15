#!/bin/bash
# Deixa o XFCE igual ao do tablet:
#   tema Fluent-round-Dark, icones Win11-dark, fonte Noto Sans, botoes das janelas a direita,
#   barra embaixo (48 px, cantos arredondados) com menu Iniciar (Whisker), janelas, bandeja e relogio.
# Rode com o Ubuntu FECHADO, pelo Termux:  udroid login jammy:xfce4
#   curl -fsSL https://raw.githubusercontent.com/heitorcampos2/Linux-no-Android/main/scripts/instalar-visual-tablet.sh | bash
set -u
# Rodou no Termux (fora do Ubuntu)? Entra no Ubuntu sozinho e roda la dentro.
if [ "${PREFIX:-}" = "/data/data/com.termux/files/usr" ]; then
  D="${DISTRO:-jammy:xfce4}"
  echo "Voce esta no Termux: entrando no Ubuntu ($D) para rodar..."
  curl -fsSL "https://raw.githubusercontent.com/heitorcampos2/Linux-no-Android/main/scripts/instalar-visual-tablet.sh" -o "$PREFIX/tmp/lna-instalar-visual-tablet.sh" || { echo "Sem internet?"; exit 1; }
  exec udroid login "$D" /bin/bash /tmp/lna-instalar-visual-tablet.sh
fi
if pgrep -x xfce4-session >/dev/null; then
  echo "O Ubuntu (XFCE) esta aberto. Feche pelo app (Encerrar) e rode de novo pelo Termux com: udroid login jammy:xfce4"
  exit 1
fi
echo "== 1/4 Pacotes (menu Iniciar, fontes e ferramentas do tema) =="
apt update
apt install -y git curl sassc gtk2-engines-murrine gnome-themes-extra libglib2.0-dev-bin \
  fonts-noto-core fonts-ubuntu xfce4-whiskermenu-plugin || echo "Aviso: algum pacote nao instalou, seguindo."

echo "== 2/4 Baixando o visual e os temas do GitHub =="
cd /tmp && rm -rf lna Fluent-gtk-theme Win11-icon-theme
git clone --depth 1 https://github.com/heitorcampos2/Linux-no-Android.git lna || { echo "Sem internet ou GitHub fora do ar."; exit 1; }
git clone --depth 1 https://github.com/vinceliuice/Fluent-gtk-theme.git && bash Fluent-gtk-theme/install.sh --tweaks round
git clone --depth 1 https://github.com/yeyushengfan258/Win11-icon-theme.git && bash Win11-icon-theme/install.sh

echo "== 3/4 Aplicando as configuracoes =="
V=/tmp/lna/visual
C=/root/.config/xfce4
X=$C/xfconf/xfce-perchannel-xml
if [ -d "$C" ]; then B=/root/.config/xfce4-antes-$(date +%Y%m%d-%H%M%S); cp -a "$C" "$B" && echo "Visual anterior guardado em $B"; fi
mkdir -p "$X" "$C/panel" "$C/terminal" /root/.config/gtk-3.0
cp -f "$V"/xfconf/*.xml "$X"/
rm -rf "$C"/panel/* && cp -a "$V"/panel/. "$C"/panel/
cp -f "$V"/terminal/terminalrc "$C"/terminal/
cp -f "$V"/gtk-3.0/gtk.css /root/.config/gtk-3.0/
if [ -f "$V"/papel-de-parede.png ]; then
  mkdir -p /usr/share/backgrounds && cp -f "$V"/papel-de-parede.png /usr/share/backgrounds/linux-no-android.png
  cp -f "$V"/desktop/xfce4-desktop.xml "$X"/
  echo "Papel de parede aplicado."
fi
rm -rf /root/.cache/sessions

echo "== 4/4 Limpando =="
cd / && rm -rf /tmp/lna /tmp/Fluent-gtk-theme /tmp/Win11-icon-theme
echo "Pronto! Digite exit e abra o Ubuntu pelo app."
