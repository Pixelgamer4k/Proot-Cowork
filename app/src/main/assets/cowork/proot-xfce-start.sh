#!/usr/bin/env bash
# Launch XFCE inside proot-distro on embedded Termux:X11 (DISPLAY=:0, 1280x720).
set -euo pipefail

DISTRO="${1:-ubuntu}"
export DISPLAY="${DISPLAY:-:0}"
export GDK_BACKEND=x11
export QT_QPA_PLATFORM=xcb
export XDG_CURRENT_DESKTOP=XFCE
export DESKTOP_SESSION=xfce
export LIBGL_ALWAYS_SOFTWARE="${LIBGL_ALWAYS_SOFTWARE:-1}"
export GALLIUM_DRIVER="${GALLIUM_DRIVER:-llvmpipe}"

P="${PREFIX:-/data/data/com.termux/files/usr}"
SOCKET="$P/tmp/.X11-unix/X0"

if [[ ! -S "$SOCKET" ]]; then
  echo "X11 server not running — open Proot-Cowork app and wait for X11 to start" >&2
  exit 1
fi

echo "==> Starting XFCE in $DISTRO on $DISPLAY (1280x720 target)"
echo "    Tap Show X11 in the app"

exec proot-distro login "$DISTRO" --shared-tmp -- env \
  DISPLAY="$DISPLAY" \
  GDK_BACKEND=x11 \
  QT_QPA_PLATFORM=xcb \
  XDG_CURRENT_DESKTOP=XFCE \
  DESKTOP_SESSION=xfce \
  LIBGL_ALWAYS_SOFTWARE="$LIBGL_ALWAYS_SOFTWARE" \
  GALLIUM_DRIVER="$GALLIUM_DRIVER" \
  bash -lc '
    apply_xfce_appearance() {
      sleep 2
      if command -v xfconf-query >/dev/null; then
        xfconf-query -c xsettings -p /Gtk/ThemeName -n -t string -s Greybird 2>/dev/null \
          || xfconf-query -c xsettings -p /Gtk/ThemeName -s Greybird 2>/dev/null || true
        xfconf-query -c xsettings -p /Gtk/IconThemeName -n -t string -s elementary-xfce 2>/dev/null \
          || xfconf-query -c xsettings -p /Gtk/IconThemeName -s elementary-xfce 2>/dev/null || true
        xfconf-query -c xsettings -p /Gtk/FontName -n -t string -s "Sans 10" 2>/dev/null \
          || xfconf-query -c xsettings -p /Gtk/FontName -s "Sans 10" 2>/dev/null || true
        img="/usr/share/backgrounds/xfce/xfce-blue.jpg"
        if [ -f "$img" ]; then
          for mon in monitor0 monitor1 monitorLVDS-1 monitordefault; do
            p="/backdrop/screen0/$mon/workspace0/last-image"
            xfconf-query -c xfdesktop -p "$p" -n -t string -s "$img" 2>/dev/null \
              || xfconf-query -c xfdesktop -p "$p" -s "$img" 2>/dev/null || true
          done
        fi
        xfdesktop --reload 2>/dev/null || true
      fi
      xsetroot -solid "#3b5ba9" 2>/dev/null || true
    }
    apply_xfce_appearance &
    if command -v dbus-launch >/dev/null; then
      exec dbus-launch --exit-with-session xfce4-session
    fi
    exec startxfce4
  '
