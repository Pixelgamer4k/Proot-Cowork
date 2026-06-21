#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/app/src/main/assets/runtime/aarch64"

if [[ -f "$DEST/bin/proot" ]]; then
  echo "==> proot runtime already present"
  exit 0
fi

mkdir -p "$DEST/bin" "$DEST/lib"
tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

curl -fsSL "https://packages.termux.dev/apt/termux-main/pool/main/p/proot/proot_5.1.107.80_aarch64.deb" -o "$tmpdir/proot.deb"
curl -fsSL "https://packages.termux.dev/apt/termux-main/pool/main/libt/libtalloc/libtalloc_2.4.3_aarch64.deb" -o "$tmpdir/libtalloc.deb"
curl -fsSL "https://packages.termux.dev/apt/termux-main/pool/main/liba/libandroid-shmem/libandroid-shmem_0.7_aarch64.deb" -o "$tmpdir/libshmem.deb"

dpkg-deb -x "$tmpdir/proot.deb" "$tmpdir/pe"
dpkg-deb -x "$tmpdir/libtalloc.deb" "$tmpdir/le"
dpkg-deb -x "$tmpdir/libshmem.deb" "$tmpdir/se"

cp "$tmpdir/pe/data/data/com.termux/files/usr/bin/proot" "$DEST/bin/"
cp -P "$tmpdir/le/data/data/com.termux/files/usr/lib/libtalloc.so"* "$DEST/lib/"
cp "$tmpdir/se/data/data/com.termux/files/usr/lib/libandroid-shmem.so" "$DEST/lib/"
chmod +x "$DEST/bin/proot"

echo "==> Bundled proot runtime to assets"
