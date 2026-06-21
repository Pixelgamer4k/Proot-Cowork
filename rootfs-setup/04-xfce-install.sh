#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

DISTRO="${DISTRO:-ubuntu}"
echo "==> Installing XFCE4 in $DISTRO"

proot-distro login "$DISTRO" --shared-tmp <<'GUEST'
set -e
export DEBIAN_FRONTEND=noninteractive
apt install -y xfce4 xfce4-terminal thunar mousepad

# Create start-desktop.sh at rootfs root
cat > /start-desktop.sh <<'SCRIPT'
#!/bin/bash
export DISPLAY=:0
export XDG_RUNTIME_DIR=/tmp
cd /home/cowork
exec dbus-launch --exit-with-session startxfce4
SCRIPT
chmod +x /start-desktop.sh
chown cowork:cowork /start-desktop.sh

echo "==> XFCE installed, start-desktop.sh created"
GUEST

echo "==> Done. Run 05-agent-tools.sh next."
