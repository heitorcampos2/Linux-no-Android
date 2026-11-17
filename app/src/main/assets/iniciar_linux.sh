if srun; then echo "OK ja estava rodando"; exit 0; fi
# som: servidor PulseAudio do Termux, se estiver instalado
if command -v pulseaudio >/dev/null 2>&1; then
  pulseaudio --check 2>/dev/null || pulseaudio --start --exit-idle-time=-1 \
    --load="module-native-protocol-tcp auth-ip-acl=127.0.0.1 auth-anonymous=1" >/dev/null 2>&1
fi
# a sessao roda dentro deste script; quando o Ubuntu fecha (Sair no XFCE),
# ele registra o encerramento e desliga o Termux:X11 sozinho
cat > "$L/sessao.sh" <<'EOS'
L="$HOME/.linux-no-tablet"; T="${PREFIX:-/data/data/com.termux/files/usr}/tmp"
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
# o udroid quebra o comando nos espacos: roda um script da /tmp (compartilhada) sem espacos
udr login "$1" /bin/bash /tmp/lnt-xfce.sh
c=$?
echo "$(date '+%d/%m %H:%M:%S') sessao terminou (codigo $c)" >> "$L/historico.log"
echo encerrado > "$L/estado"
pkill -f '[c]om\.termux\.x11\.[A-Z]|[t]ermux-x11 :' 2>/dev/null
p=$(cat "$L/x11.pid" 2>/dev/null); [ -n "$p" ] && kill "$p" 2>/dev/null; rm -f "$L/x11.pid"
rm -f "$T/.X0-lock" "$T/.X11-unix/X0"
command -v termux-wake-unlock >/dev/null 2>&1 && termux-wake-unlock
EOS
# fuso horario do Android (o Ubuntu nao enxerga e ficaria em UTC, com a hora adiantada)
tz=$(/system/bin/getprop persist.sys.timezone 2>/dev/null)
printf '%s %s\n' "${tz:--}" "$(date +%z)" > "$T/lnt-tz"
# scripts que rodam DENTRO do Ubuntu (a /tmp do Ubuntu e a $PREFIX/tmp do Termux)
cat > "$T/lnt-xfce.sh" <<'EOX'
export DISPLAY=:0 PULSE_SERVER=127.0.0.1
# hora certa: usa o mesmo fuso do Android
if read -r tz off < /tmp/lnt-tz 2>/dev/null; then
  if [ "$tz" != "-" ] && [ -f "/usr/share/zoneinfo/$tz" ]; then
    export TZ="$tz"
    [ "$(readlink /etc/localtime 2>/dev/null)" = "/usr/share/zoneinfo/$tz" ] || ln -sf "/usr/share/zoneinfo/$tz" /etc/localtime
    [ "$(cat /etc/timezone 2>/dev/null)" = "$tz" ] || echo "$tz" > /etc/timezone
  elif [ -n "$off" ]; then
    # sem o pacote tzdata: monta o fuso pela diferenca de horas (ex.: -0300 vira <-03>3)
    h=$((10#${off:1:2})); m=${off:3:2}; [ "${off:0:1}" = "-" ] && s="" || s="-"
    export TZ="<${off:0:3}>$s$h:$m"
  fi
  echo "fuso da sessao: $TZ"
fi
# portugues do Brasil sempre que existir no Ubuntu: gera o idioma se faltar e deixa como padrao
temptbr() { locale -a 2>/dev/null | grep -qiE '^pt_BR\.utf-?8$'; }
if ! temptbr && [ -f /usr/share/i18n/locales/pt_BR ] && command -v locale-gen >/dev/null 2>&1; then
  grep -q '^pt_BR.UTF-8' /etc/locale.gen 2>/dev/null || echo 'pt_BR.UTF-8 UTF-8' >> /etc/locale.gen
  locale-gen pt_BR.UTF-8 >/dev/null 2>&1
fi
if temptbr; then
  export LANG=pt_BR.UTF-8 LANGUAGE=pt_BR:pt:en LC_ALL=pt_BR.UTF-8
  grep -q '^LANG=pt_BR.UTF-8' /etc/default/locale 2>/dev/null || printf 'LANG=pt_BR.UTF-8\nLANGUAGE=pt_BR:pt\n' > /etc/default/locale
elif [ -f /etc/default/locale ]; then
  . /etc/default/locale; export LANG LANGUAGE
fi
[ -e /usr/share/locale/pt_BR/LC_MESSAGES/thunar.mo ] || echo "aviso: faltam as traducoes dos programas (etapa 9 da Instalacao)"
echo "idioma da sessao: ${LANG:-padrao}"
exec dbus-launch --exit-with-session xfce4-session
EOX
echo rodando > "$L/estado"
hist "iniciando $DISTRO"
nohup "$P/bin/bash" "$L/sessao.sh" "$DISTRO" >"$L/sessao.log" 2>&1 </dev/null &
pid=$!
i=0
while [ $i -lt 60 ]; do
  srun && { hist "XFCE no ar"; echo OK; exit 0; }
  kill -0 $pid 2>/dev/null || break
  sleep 0.5; i=$((i+1))
done
echo "FALHA o XFCE nao abriu"
tail -n 20 "$L/sessao.log"
xdiag
exit 1
