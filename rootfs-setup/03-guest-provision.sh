#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

DISTRO="${DISTRO:-ubuntu}"
echo "==> Provisioning guest inside $DISTRO"

proot-distro login "$DISTRO" --shared-tmp <<'GUEST'
set -e
export DEBIAN_FRONTEND=noninteractive

# Create cowork user with passwordless sudo
if ! id cowork &>/dev/null; then
    useradd -m -s /bin/bash cowork
    echo "cowork ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/cowork
    chmod 440 /etc/sudoers.d/cowork
fi

apt update
apt install -y sudo curl wget git vim nano ca-certificates dbus-x11

echo "==> Guest provision complete"
GUEST

echo "==> Done. Run 04-xfce-install.sh next."
