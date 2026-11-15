for c in termux-x11 udroid pgrep; do
  if command -v "$c" >/dev/null 2>&1; then echo "TEM $c"; else echo "FALTA $c"; fi
done
echo "ESTADO $(cat "$L/estado" 2>/dev/null || echo nenhum)"
xproc && echo "X 1" || echo "X 0"
xabs && echo "XABS 1" || echo "XABS 0"
srun && echo "SESSAO 1" || echo "SESSAO 0"
prun && echo "PROOT 1" || echo "PROOT 0"
[ -e "$T/.X0-lock" ] && echo "TRAVA 1" || echo "TRAVA 0"
xsock && echo "SOQUETE 1" || echo "SOQUETE 0"
R="$P/var/lib/udroid/installed-filesystems"; suite="${DISTRO%%:*}"; var="${DISTRO#*:}"
d=$(ls -d "$R"/*"$suite"*"$var"* 2>/dev/null | head -n 1)
if [ -n "$d" ]; then
  [ -e "$d/usr/share/locale/pt_BR/LC_MESSAGES/thunar.mo" ] && echo "TRADUZIDO 1" || echo "TRADUZIDO 0"
fi
