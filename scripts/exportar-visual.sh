#!/bin/bash
# Exporta o visual do XFCE (painel, menu, tema, icones, cursor, fontes, papel de parede,
# janelas e atalhos da Area de Trabalho) para /sdcard/Download/visual-ubuntu.tar.gz
# Uso (dentro do Ubuntu, como root):
#   curl -fsSL https://raw.githubusercontent.com/heitorcampos2/Linux-no-Android/main/scripts/exportar-visual.sh | bash
set -u
# Rodou no Termux (fora do Ubuntu)? Entra no Ubuntu sozinho e roda la dentro.
if [ "${PREFIX:-}" = "/data/data/com.termux/files/usr" ]; then
  D="${DISTRO:-jammy:xfce4}"
  echo "Voce esta no Termux: entrando no Ubuntu ($D) para rodar..."
  curl -fsSL "https://raw.githubusercontent.com/heitorcampos2/Linux-no-Android/main/scripts/exportar-visual.sh" -o "$PREFIX/tmp/lna-exportar-visual.sh" || { echo "Sem internet?"; exit 1; }
  exec udroid login "$D" /bin/bash /tmp/lna-exportar-visual.sh
fi
O=/sdcard/Download/visual-ubuntu.tar.gz
L=/tmp/lnt-lista
: > "$L"
cd /
for p in root/.config/xfce4 root/.config/gtk-3.0 root/.config/gtk-4.0 root/.gtkrc-2.0 \
         root/.themes root/.icons root/.local/share/icons root/.local/share/themes \
         root/.local/share/fonts root/.fonts root/.config/autostart root/.local/share/applications; do
  [ -e "/$p" ] && echo "$p" >> "$L"
done
echo "Procurando temas e icones instalados por voce..."
for d in /usr/share/themes/* /usr/share/icons/*; do
  dpkg -S "$d" >/dev/null 2>&1 || echo "${d#/}" >> "$L"
done
X=/root/.config/xfce4/xfconf/xfce-perchannel-xml/xfce4-desktop.xml
if [ -f "$X" ]; then
  grep -o 'value="/[^"]*"' "$X" | sed -e 's/^value="//' -e 's/"$//' | grep -Ei '\.(jpg|jpeg|png|svg|webp)$' | sort -u |
  while read -r f; do [ -f "$f" ] && echo "${f#/}" >> "$L"; done
fi
DESK=$(xdg-user-dir DESKTOP 2>/dev/null)
if [ -n "$DESK" ] && [ -d "$DESK" ]; then
  find "$DESK" -maxdepth 1 -name '*.desktop' | sed 's#^/##' >> "$L"
  echo "${DESK#/}" > /root/.config/lnt-desktop-dir.txt
fi
dpkg-query -W -f='${binary:Package}\n' 'xfce4-*plugin*' 2>/dev/null > /root/.config/lnt-plugins.txt
echo root/.config/lnt-plugins.txt >> "$L"
[ -f /root/.config/lnt-desktop-dir.txt ] && echo root/.config/lnt-desktop-dir.txt >> "$L"
echo "Compactando (pode levar 1 a 3 minutos)..."
if tar -czf "$O" -T "$L"; then
  ls -lh "$O"
  echo "Pronto: mande $(basename "$O") para a pasta Download do outro aparelho."
else
  echo "Algo deu errado ao compactar."
fi
