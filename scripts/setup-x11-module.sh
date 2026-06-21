#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
X11_DIR="$ROOT/third_party/termux-x11"

if [[ ! -d "$X11_DIR/.git" ]]; then
  echo "==> Cloning termux-x11 with submodules (shallow)..."
  mkdir -p "$ROOT/third_party"
  git clone --recurse-submodules --shallow-submodules --depth 1 \
    https://github.com/termux/termux-x11.git "$X11_DIR"
else
  echo "==> Updating termux-x11 submodules..."
  git -C "$X11_DIR" submodule update --init --recursive --depth 1
fi

echo "==> Patching termux-x11 app module as Android library..."
cp "$ROOT/scripts/termux-x11-library.build.gradle" "$X11_DIR/app/build.gradle"

echo "==> termux-x11 ready"
