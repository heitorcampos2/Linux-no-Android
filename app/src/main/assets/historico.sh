echo "== historico =="; tail -n 25 "$L/historico.log" 2>/dev/null
echo; echo "== sessao.log =="; tail -n 25 "$L/sessao.log" 2>/dev/null
echo; echo "== x11.log =="; tail -n 15 "$L/x11.log" 2>/dev/null
