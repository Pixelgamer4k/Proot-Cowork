#!/usr/bin/env bash
# Install XFCE + Mesa GL stack inside a proot-distro guest for embedded X11 (:0).
set -euo pipefail

DISTRO="${1:-ubuntu}"

if ! command -v proot-distro >/dev/null; then
  echo "proot-distro not found — reinstall app or run: pkg install proot-distro" >&2
  exit 1
fi

echo "==> Installing XFCE desktop + graphics stack in proot-distro: $DISTRO"
echo "    Target: DISPLAY=:0 (1280x720 @ 60Hz embedded X11)"

proot-distro login "$DISTRO" --shared-tmp -- bash -lc '
set -euo pipefail
export DEBIAN_FRONTEND=noninteractive

if command -v apt-get >/dev/null; then
  apt-get update
  apt-get install -y \
    xfce4 xfce4-terminal xfce4-goodies xfce4-session \
    dbus dbus-x11 \
    mesa-utils libgl1-mesa-dri libglx-mesa0 libegl-mesa0 libgbm1 libgl1 \
    greybird-gtk-theme elementary-xfce-icon-theme librsvg2-common gtk2-engines-pixbuf \
    fonts-dejavu fontconfig \
    x11-xserver-utils x11-utils
elif command -v apk >/dev/null; then
  apk update
  apk add xfce4 xfce4-terminal dbus dbus-x11 mesa-dri-gallium mesa-gl mesa-egl \
    mesa-utils font-dejavu fontconfig adwaita-icon-theme ttf-dejavu xorg-server-utils
else
  echo "Unsupported distro — use ubuntu, debian, or alpine" >&2
  exit 1
fi

# GTK/icon theme + wallpaper (empty xsettings = broken panel icons + black desktop).
for home in /root /home/*; do
  [ -d "$home" ] || continue
  xfconf_dir="$home/.config/xfce4/xfconf/xfce-perchannel-xml"
  mkdir -p "$xfconf_dir"

  cat > "$xfconf_dir/xsettings.xml" << "XEOF"
<?xml version="1.0" encoding="UTF-8"?>
<channel name="xsettings" version="1.0">
  <property name="Net" type="empty">
    <property name="ThemeName" type="string" value="Greybird"/>
    <property name="IconThemeName" type="string" value="elementary-xfce"/>
  </property>
  <property name="Gtk" type="empty">
    <property name="FontName" type="string" value="Sans 10"/>
    <property name="MonospaceFontName" type="string" value="Monospace 10"/>
    <property name="ThemeName" type="string" value="Greybird"/>
    <property name="IconSizes" type="string" value=""/>
  </property>
</channel>
XEOF

  cat > "$xfconf_dir/xfdesktop.xml" << "XEOF"
<?xml version="1.0" encoding="UTF-8"?>
<channel name="xfdesktop" version="1.0">
  <property name="backdrop" type="empty">
    <property name="screen0" type="empty">
      <property name="monitor0" type="empty">
        <property name="workspace0" type="empty">
          <property name="color-style" type="int" value="0"/>
          <property name="image-style" type="int" value="5"/>
          <property name="last-image" type="string" value="/usr/share/backgrounds/xfce/xfce-blue.jpg"/>
        </property>
      </property>
    </property>
  </property>
</channel>
XEOF

  cat > "$xfconf_dir/xfwm4.xml" << "XEOF"
<?xml version="1.0" encoding="UTF-8"?>
<channel name="xfwm4" version="1.0">
  <property name="general" type="empty">
    <property name="use_compositing" type="bool" value="false"/>
    <property name="vblank_mode" type="string" value="off"/>
    <property name="theme" type="string" value="Default"/>
  </property>
</channel>
XEOF
done

if command -v gtk-update-icon-cache >/dev/null; then
  gtk-update-icon-cache -f /usr/share/icons/elementary-xfce 2>/dev/null || true
  gtk-update-icon-cache -f /usr/share/icons/hicolor 2>/dev/null || true
fi

echo "==> XFCE + Mesa installed (Greybird theme, wallpaper, compositor off)"
'

echo "==> Done. Start with: proot-xfce-start $DISTRO"
