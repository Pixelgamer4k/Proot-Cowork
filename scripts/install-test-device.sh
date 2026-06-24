#!/usr/bin/env bash
# Install latest CI APK on test device, grant storage, launch, screenshot, smoke-test.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PKG="${PKG:-com.proot}"
ADB_DEVICE="${ADB_DEVICE:-192.168.1.126:41325}"
APK_DIR="${APK_DIR:-/tmp/cowork-apk}"
SCREENSHOT="${SCREENSHOT:-/tmp/cowork_screen.png}"

adb connect "$ADB_DEVICE" >/dev/null 2>&1 || true
ADB=(adb -s "$ADB_DEVICE")

echo "==> Uninstalling old packages"
"${ADB[@]}" uninstall com.proot.cowork.debug 2>/dev/null || true
"${ADB[@]}" uninstall com.proot.cowork 2>/dev/null || true
"${ADB[@]}" uninstall "$PKG" 2>/dev/null || true

APK="$(find "$APK_DIR" -name '*.apk' | head -1)"
if [[ -z "$APK" ]]; then
  echo "No APK in $APK_DIR — run: gh run download <run-id> -n proot-cowork-debug-apk -D $APK_DIR"
  exit 1
fi

echo "==> Installing $APK"
"${ADB[@]}" install -r "$APK"

echo "==> Granting storage (Android 11+)"
"${ADB[@]}" shell appops set "$PKG" MANAGE_EXTERNAL_STORAGE allow 2>/dev/null || true
"${ADB[@]}" shell pm grant "$PKG" android.permission.READ_EXTERNAL_STORAGE 2>/dev/null || true
"${ADB[@]}" shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS 2>/dev/null || true

echo "==> Launching app"
"${ADB[@]}" shell am start -n "$PKG/com.proot.cowork.MainActivity" >/dev/null
sleep 8

echo "==> Screenshot -> $SCREENSHOT"
"${ADB[@]}" shell screencap -p /sdcard/cowork_test.png
"${ADB[@]}" pull /sdcard/cowork_test.png "$SCREENSHOT" >/dev/null
echo "Saved $SCREENSHOT"

echo "==> Bootstrap markers"
"${ADB[@]}" shell "run-as $PKG ls -la files/usr/.termux_* 2>/dev/null | head -10" || true

echo "==> Done. Open terminal in app and run: pkg update"
