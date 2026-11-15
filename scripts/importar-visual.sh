#!/bin/bash
# Aplica o visual exportado por exportar-visual.sh.
# Rode com o Ubuntu FECHADO (no app: Encerrar), entrando pelo Termux com: udroid login jammy:xfce4
#   curl -fsSL https://raw.githubusercontent.com/heitorcampos2/Linux-no-Android/main/scripts/importar-visual.sh | bash
set -u
F=/sdcard/Download/visual-ubuntu.tar.gz
[ -f "$F" ] || F=/sdcard/Download/visual-ubuntu-tablet.tar.gz
if [ ! -f "$F" ]; then echo "Nao achei /sdcard/Download/visual-ubuntu.tar.gz - copie o arquivo para a pasta Download."; exit 1; fi
if pgrep -x xfce4-session >/dev/null; then
  echo "O Ubuntu (XFCE) esta aberto: feche pelo app (Encerrar) e rode de novo pelo Termux com 'udroid login jammy:xfce4'."
  exit 1
fi
echo "Instalando os plugins do painel usados no outro aparelho..."
apt update -qq
tar -xzOf "$F" root/.config/lnt-plugins.txt 2>/dev/null | xargs -r apt install -y
echo "Aplicando o visual..."
cd / && tar -xzf "$F"
# se a Area de Trabalho tiver outro nome aqui, copia os atalhos para a daqui
ORIG=$(cat /root/.config/lnt-desktop-dir.txt 2>/dev/null)
AQUI=$(xdg-user-dir DESKTOP 2>/dev/null)
if [ -n "$ORIG" ] && [ -n "$AQUI" ] && [ "/$ORIG" != "$AQUI" ] && [ -d "/$ORIG" ]; then
  mkdir -p "$AQUI" && cp -n "/$ORIG"/*.desktop "$AQUI"/ 2>/dev/null
fi
chmod +x "$AQUI"/*.desktop 2>/dev/null
fc-cache -f >/dev/null 2>&1
for i in /usr/share/icons/* /root/.icons/* /root/.local/share/icons/*; do
  [ -f "$i/index.theme" ] && gtk-update-icon-cache -f -q "$i" 2>/dev/null
done
echo "Visual aplicado. Digite exit e abra o Ubuntu pelo app."
