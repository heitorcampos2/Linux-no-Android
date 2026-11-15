# comum a todas as rotinas (o app junta este arquivo antes de cada uma)
# $1 = nome do Linux no udroid (ex.: jammy:xfce4)
L="$HOME/.linux-no-tablet"; mkdir -p "$L"
# quando outro app pede o comando, o Termux nao carrega o termux-exec (que traduz o
# "#!/bin/bash" dos scripts, como o do udroid). Carrega aqui e chama o udroid pelo bash.
P="${PREFIX:-/data/data/com.termux/files/usr}"
if [ -z "$LD_PRELOAD" ]; then
  for f in "$P/lib/libtermux-exec-ld-preload.so" "$P/lib/libtermux-exec.so"; do
    [ -f "$f" ] && { export LD_PRELOAD="$f"; break; }
  done
fi
export PATH="$P/bin:$PATH"
udr() { "$P/bin/bash" "$(command -v udroid)" "$@"; }
T="${PREFIX:-/data/data/com.termux/files/usr}/tmp"
DISTRO="${1:-jammy:xfce4}"
# os colchetes evitam que o pgrep encontre a si mesmo
# termux-x11 antigo: CmdEntryPoint; novo: Loader. Pega qualquer um.
XPAT='[c]om\.termux\.x11\.[A-Z]|[t]ermux-x11 :|[X]wayland'
SPAT='(^|/)[x]fce4-session( |$)'
DPAT='(^|/)[d]bus-launch --exit-with-session'
PPAT='[p]root'
# servidor X vivo segura o soquete abstrato @...X11-unix/X0 (some sozinho quando o processo morre)
xabs() { grep -q '@.*X11-unix/X0' /proc/net/unix 2>/dev/null; }
# PID do termux-x11 que o app iniciou (vale mesmo se o nome do processo for diferente)
xpid() { p=$(cat "$L/x11.pid" 2>/dev/null); [ -n "$p" ] && kill -0 "$p" 2>/dev/null; }
xproc() { xpid || pgrep -f "$XPAT" >/dev/null 2>&1; }
# a tela :0 existe? arquivo do soquete (onde for) ou soquete abstrato
xsock() { [ -S "$T/.X11-unix/X0" ] || [ -S "${TMPDIR:-$T}/.X11-unix/X0" ] || xabs; }
xrun() { xproc || xabs; }
xkill() {
  p=$(cat "$L/x11.pid" 2>/dev/null)
  [ -n "$p" ] && kill "$p" 2>/dev/null
  pkill -f "$XPAT" 2>/dev/null; sleep 1
  [ -n "$p" ] && kill -9 "$p" 2>/dev/null
  pkill -9 -f "$XPAT" 2>/dev/null; sleep 0.5
  rm -f "$L/x11.pid"
}
xdiag() {
  echo "-- diagnostico --"
  echo "TMPDIR=${TMPDIR:-?} T=$T"
  ls -la "$T/.X11-unix" 2>&1 | head -5
  [ -r /proc/net/unix ] && echo "abstrato: $(grep -c 'X11-unix/X0' /proc/net/unix 2>/dev/null)" || echo "abstrato: sem acesso"
  p=$(cat "$L/x11.pid" 2>/dev/null); echo "pid=$p vivo=$(kill -0 "$p" 2>/dev/null && echo sim || echo nao)"
  [ -n "$p" ] && tr '\0' ' ' 2>/dev/null < "/proc/$p/cmdline" | head -c 200 && echo
  ps -eo pid,args 2>/dev/null | grep -iE 'x11|wayland' | grep -v grep | head -5
}
xlist() { ps -eo pid,args 2>/dev/null | grep -E "$XPAT" | head -5; }
srun() { pgrep -f "$SPAT" >/dev/null 2>&1 || pgrep -f "$DPAT" >/dev/null 2>&1; }
prun() { pgrep -f "$PPAT" >/dev/null 2>&1; }
hist() { echo "$(date '+%d/%m %H:%M:%S') $*" >> "$L/historico.log"; }
