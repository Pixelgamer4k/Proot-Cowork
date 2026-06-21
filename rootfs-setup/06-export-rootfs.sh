#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

DISTRO="${DISTRO:-ubuntu}"
OUTPUT="$HOME/proot-cowork-rootfs.tar.gz"
ROOTFS_DIR="$PREFIX/var/lib/proot-distro/installed-rootfs/$DISTRO"

echo "==> Exporting rootfs from $ROOTFS_DIR"

if [[ ! -d "$ROOTFS_DIR" ]]; then
    echo "ERROR: Rootfs not found at $ROOTFS_DIR"
    exit 1
fi

if [[ ! -f "$ROOTFS_DIR/start-desktop.sh" ]]; then
    echo "ERROR: start-desktop.sh missing. Run 04-xfce-install.sh first."
    exit 1
fi

# Clean apt cache to reduce size
proot-distro login "$DISTRO" --shared-tmp -e bash -c "apt clean && rm -rf /tmp/* /var/tmp/*" || true

cd "$ROOTFS_DIR"
tar -czf "$OUTPUT" .

SIZE=$(du -h "$OUTPUT" | cut -f1)
echo "==> Exported: $OUTPUT ($SIZE)"
echo "==> Import this file in Proot Cowork app."
