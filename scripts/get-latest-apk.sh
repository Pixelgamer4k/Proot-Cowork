#!/usr/bin/env bash
# Download the latest debug APK from GitHub Actions (no local Gradle build).
set -euo pipefail

REPO="${REPO:-Pixelgamer4k/Proot-Cowork}"
WORKFLOW="${WORKFLOW:-build-debug-apk.yml}"
OUT_DIR="${1:-/tmp/proot-cowork-apk}"

run_id="$(gh run list --repo "$REPO" --workflow "$WORKFLOW" --status success --limit 1 --json databaseId -q '.[0].databaseId')"
if [[ -z "$run_id" || "$run_id" == "null" ]]; then
  echo "No successful CI run found for $WORKFLOW" >&2
  exit 1
fi

mkdir -p "$OUT_DIR"
rm -f "$OUT_DIR"/*.apk 2>/dev/null || true
gh run download "$run_id" --repo "$REPO" -n proot-cowork-debug-apk -D "$OUT_DIR"

apk="$(find "$OUT_DIR" -name '*.apk' -type f | head -1)"
if [[ -z "$apk" ]]; then
  echo "Download succeeded but no APK found in $OUT_DIR" >&2
  exit 1
fi

echo "Run: $run_id"
echo "APK: $apk"
echo "Install: adb install -r \"$apk\""
