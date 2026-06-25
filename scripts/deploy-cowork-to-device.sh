#!/usr/bin/env bash
# Deploy Cowork automation layer to connected device, export rootfs to /sdcard.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SERIAL="${ADB_SERIAL:-192.168.1.108:37155}"
LOG="${LOG:-/tmp/cowork-deploy.log}"

adb() { command adb -s "$SERIAL" "$@"; }

echo "==> Deploying Cowork layer to $SERIAL (log: $LOG)" | tee "$LOG"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

mkdir -p "$TMP/cowork-share"
cp -a "$ROOT/app/src/main/assets/cowork/." "$TMP/cowork-share/"
cp "$ROOT/scripts/cowork-guest-layer.sh" "$TMP/cowork-share/"
chmod +x "$TMP/cowork-share/"*.sh "$TMP/cowork-share/computer-use/"*.py 2>/dev/null || true

tar -C "$TMP/cowork-share" -czf "$TMP/cowork-share.tgz" .
adb push "$TMP/cowork-share.tgz" /data/local/tmp/cowork-share.tgz | tee -a "$LOG"

adb shell "run-as com.proot sh -c '
  set -e
  mkdir -p files/usr/share/cowork
  cd files/usr/share/cowork
  tar -xzf /data/local/tmp/cowork-share.tgz
  chmod +x *.sh computer-use/*.py 2>/dev/null || true
  rm -f files/usr/.cowork_assets_v6 files/usr/.cowork_assets_v7
  for f in proot-xfce-cowork-setup cowork-guest-layer cowork-agent cowork-dispatch cowork-desktop-test proot-xfce-export; do
    ln -sf ../share/cowork/\${f}.sh files/usr/bin/\$f 2>/dev/null || ln -sf ../share/cowork/\$f files/usr/bin/\$f 2>/dev/null || true
  done
  echo ASSETS_OK
'" | tee -a "$LOG"

echo "==> Running cowork setup inside ubuntu (10–20 min)..." | tee -a "$LOG"
adb shell "run-as com.proot sh -c '
  export TERMUX_APP__PACKAGE_NAME=com.proot
  export TERMUX__PREFIX=\$PWD/files/usr
  export TERMUX__HOME=\$PWD/files/home
  export PREFIX=\$TERMUX__PREFIX PATH=\$PREFIX/bin:\$PATH LD_LIBRARY_PATH=\$PREFIX/lib
  cd /data/user/0/com.proot
  \$PREFIX/bin/bash \$PREFIX/share/cowork/proot-xfce-cowork-setup.sh ubuntu
'" 2>&1 | tee -a "$LOG"

echo "==> Exporting rootfs..." | tee -a "$LOG"
adb shell "run-as com.proot sh -c '
  export TERMUX_APP__PACKAGE_NAME=com.proot
  export TERMUX__PREFIX=\$PWD/files/usr
  export TERMUX__HOME=\$PWD/files/home
  export PREFIX=\$TERMUX__PREFIX PATH=\$PREFIX/bin:\$PATH LD_LIBRARY_PATH=\$PREFIX/lib HOME=\$TERMUX__HOME
  cd /data/user/0/com.proot
  OUTPUT=\$HOME/proot-cowork-ubuntu.tar.gz CLEAN_BEFORE_EXPORT=1 \$PREFIX/share/cowork/proot-xfce-export.sh ubuntu
  ls -lh \$HOME/proot-cowork-ubuntu.tar.gz
'" 2>&1 | tee -a "$LOG"

HOST_TAR="/tmp/proot-cowork-ubuntu.tar.gz"
echo "==> Pulling tarball to host then pushing to /sdcard..." | tee -a "$LOG"
adb exec-out "run-as com.proot cat files/home/proot-cowork-ubuntu.tar.gz" > "$HOST_TAR"
ls -lh "$HOST_TAR" | tee -a "$LOG"
adb push "$HOST_TAR" /sdcard/proot-cowork-ubuntu.tar.gz | tee -a "$LOG"
adb shell "ls -lh /sdcard/proot-cowork-ubuntu.tar.gz" | tee -a "$LOG"
echo "==> DONE: /sdcard/proot-cowork-ubuntu.tar.gz" | tee -a "$LOG"
