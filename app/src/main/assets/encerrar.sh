hist "encerrar pedido pelo app"
if srun; then
  printf 'export DISPLAY=:0\nxfce4-session-logout --logout --fast\n' > "$T/lnt-sair.sh"
  udr login "$DISTRO" /bin/bash /tmp/lnt-sair.sh >/dev/null 2>&1 &
  i=0; while [ $i -lt 30 ] && srun; do sleep 0.5; i=$((i+1)); done
fi
forcado=0
if srun || prun; then
  forcado=1
  pkill -f "$SPAT" 2>/dev/null; pkill -f "$DPAT" 2>/dev/null; sleep 1
  pkill -f "$PPAT" 2>/dev/null; sleep 1
  pkill -9 -f "$PPAT" 2>/dev/null
fi
sleep 1
xkill
xrun || rm -f "$T/.X0-lock" "$T/.X11-unix/X0"
echo encerrado > "$L/estado"
command -v termux-wake-unlock >/dev/null 2>&1 && termux-wake-unlock
if srun || prun; then echo "FALHA o Ubuntu continua rodando"; exit 1; fi
if xrun; then echo "FALHA o servidor X continua rodando"; xlist; exit 1; fi
[ $forcado = 1 ] && { hist "encerrado (precisou forcar)"; echo "OK forcado"; exit 0; }
hist "encerrado certinho"; echo OK
