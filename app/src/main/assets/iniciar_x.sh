command -v termux-wake-lock >/dev/null 2>&1 && termux-wake-lock
if xproc && xsock; then echo "OK ja estava rodando"; exit 0; fi
sobe() {
  xrun || rm -f "$T/.X0-lock" "$T/.X11-unix/X0"
  nohup termux-x11 :0 -ac >"$L/x11.log" 2>&1 </dev/null &
  echo $! > "$L/x11.pid"
  i=0
  while [ $i -lt 60 ]; do
    xpid || { grep -q 'already running' "$L/x11.log" 2>/dev/null && return 2; return 1; }
    # servidor no ar: processo vivo e tela :0 criada (ou o log do X ja passou do teclado)
    if xsock || grep -q 'xkbcomp' "$L/x11.log" 2>/dev/null; then sleep 1; xpid && return 0; fi
    sleep 0.5; i=$((i+1))
  done
  xpid && return 0
  return 1
}
xrun && xkill
sobe; r=$?
if [ $r = 2 ]; then
  hist "Termux:X11: ja havia outro servidor na tela :0, derrubando e tentando de novo"
  xkill; sleep 1
  sobe; r=$?
fi
if [ $r = 0 ]; then hist "Termux:X11 iniciado"; echo OK; exit 0; fi
echo "FALHA o Termux:X11 nao criou a tela :0"
grep -v -i 'keysym\|conflicting\|xkbcomp\|^"> *"$' "$L/x11.log" | tail -n 6
if xrun; then echo "OUTRO_X processo segurando a tela :0"; fi
xdiag
exit 1
