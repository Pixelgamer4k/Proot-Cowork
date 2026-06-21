#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

DISTRO="${DISTRO:-ubuntu}"
echo "==> Installing agent tools in $DISTRO"

proot-distro login "$DISTRO" --shared-tmp <<'GUEST'
set -e
export DEBIAN_FRONTEND=noninteractive
apt install -y python3 python3-pip python3-venv nodejs npm \
    build-essential jq unzip zip htop screen tmux \
    firefox-esr || apt install -y chromium-browser || true

# Workspace for agent artifacts
mkdir -p /home/cowork/workspace /home/cowork/artifacts
chown -R cowork:cowork /home/cowork

echo "==> Agent tools installed"
GUEST

echo "==> Done. Run 06-export-rootfs.sh next."
