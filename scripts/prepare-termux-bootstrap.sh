#!/usr/bin/env bash
# Termux bootstrap: libbash.so in jniLibs + bootstrap.bin asset (gzip tar of prefix).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JNILIBS="$ROOT/app/src/main/jniLibs/arm64-v8a"
ASSET="$ROOT/app/src/main/assets/bootstrap.bin"
ARCH="aarch64"
URL="${TERMUX_BOOTSTRAP_URL:-https://github.com/termux/termux-packages/releases/latest/download/bootstrap-${ARCH}.zip}"

if [[ -f "$ASSET" && -f "$JNILIBS/libbash.so" ]]; then
  echo "==> Termux bootstrap already prepared"
  ls -lh "$ASSET" "$JNILIBS/libbash.so"
  exit 0
fi

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

echo "==> Downloading Termux bootstrap"
curl -fsSL "$URL" -o "$tmpdir/bootstrap.zip"
unzip -q "$tmpdir/bootstrap.zip" -d "$tmpdir/prefix"

if [[ -f "$tmpdir/prefix/SYMLINKS.txt" ]]; then
  while IFS='←' read -r target linkpath; do
    rm -f "$tmpdir/prefix/$linkpath"
    mkdir -p "$(dirname "$tmpdir/prefix/$linkpath")"
    ln -sf "$target" "$tmpdir/prefix/$linkpath"
  done < "$tmpdir/prefix/SYMLINKS.txt"
  rm -f "$tmpdir/prefix/SYMLINKS.txt"
fi

mkdir -p "$JNILIBS"
bash_src="$tmpdir/prefix/bin/bash"
[[ -L "$bash_src" ]] && bash_src="$(readlink -f "$bash_src")"
cp "$bash_src" "$JNILIBS/libbash.so"
chmod +x "$JNILIBS/libbash.so"
rm -f "$tmpdir/prefix/bin/bash"

mkdir -p "$(dirname "$ASSET")"
tar -czf "$ASSET" -C "$tmpdir/prefix" .
echo "==> Wrote $ASSET and $JNILIBS/libbash.so"
ls -lh "$ASSET" "$JNILIBS/libbash.so"
