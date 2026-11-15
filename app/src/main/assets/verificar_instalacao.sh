for c in termux-x11 udroid pgrep git pulseaudio; do
  if command -v "$c" >/dev/null 2>&1; then echo "TEM $c"; else echo "FALTA $c"; fi
done
R="$P/var/lib/udroid/installed-filesystems"
suite="${DISTRO%%:*}"; var="${DISTRO#*:}"
d=$(ls -d "$R"/*"$suite"*"$var"* 2>/dev/null | head -n 1)
if [ -n "$d" ] && [ -d "$d/usr" ]; then echo "DISTRO 1"; else echo "DISTRO 0"; fi
if [ -n "$d" ] && { grep -aq 'pt_BR' "$d/usr/lib/locale/locale-archive" 2>/dev/null || ls -d "$d"/usr/lib/locale/pt_BR* >/dev/null 2>&1; }; then
  echo "PTBR 1"; else echo "PTBR 0"; fi
if [ -n "$d" ] && ls -d "$d"/usr/share/locale-langpack/pt* >/dev/null 2>&1; then echo "TRAD 1"; else echo "TRAD 0"; fi
if [ -n "$d" ] && [ -e "$d/usr/bin/libreoffice" ]; then echo "OFFICE 1"; else echo "OFFICE 0"; fi
if [ -n "$d" ] && [ -e "$d/usr/share/locale/pt_BR/LC_MESSAGES/thunar.mo" ]; then echo "APPTRAD 1"; else echo "APPTRAD 0"; fi
if [ -n "$d" ] && grep -q 'Fluent-round-Dark' "$d/root/.config/xfce4/xfconf/xfce-perchannel-xml/xsettings.xml" 2>/dev/null; then echo "TEMA 1"; else echo "TEMA 0"; fi
S="$d/var/lib/dpkg/status"
if [ -n "$d" ] && grep -q '^Package: .*craft-launcher$' "$S" 2>/dev/null; then echo "ACLAUNCHER 1"; else echo "ACLAUNCHER 0"; fi
n=$(grep -cE '^Package: (cad|deck|design|effect|fig|film|grid|light|pdf|photo|sound|vector|word)craft$' "$S" 2>/dev/null); echo "ACAPPS ${n:-0}"
if [ -n "$d" ] && find "$d/root" -maxdepth 1 -type l -lname '/sdcard/Download' 2>/dev/null | grep -q .; then echo "DOWNLOADS 1"; else echo "DOWNLOADS 0"; fi
ph=$(/system/bin/getprop persist.sys.fflag.override.settings_enable_monitor_phantom_procs 2>/dev/null); echo "PHANTOM ${ph:-?}"
