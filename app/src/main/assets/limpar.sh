hist "limpeza: encerrando restos de sessao anterior"
pkill -f "$SPAT" 2>/dev/null; pkill -f "$DPAT" 2>/dev/null; sleep 1
pkill -f "$PPAT" 2>/dev/null
xkill
pkill -9 -f "$PPAT" 2>/dev/null
xrun || rm -f "$T/.X0-lock" "$T/.X11-unix/X0"
echo encerrado > "$L/estado"
if srun || prun; then echo "FALHA o Ubuntu continua rodando"; exit 1; fi
if xrun; then echo "AVISO_X ainda tem um servidor X que nao consegui fechar"; xlist; fi
echo OK
