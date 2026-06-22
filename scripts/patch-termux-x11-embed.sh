#!/usr/bin/env bash
# Patches termux-x11 for embedding inside Proot Cowork (not standalone APK).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CMD_EP="$ROOT/third_party/termux-x11/app/src/main/java/com/termux/x11/CmdEntryPoint.java"

if [[ ! -f "$CMD_EP" ]]; then
  echo "CmdEntryPoint.java not found at $CMD_EP" >&2
  exit 1
fi

python3 - "$CMD_EP" <<'PY'
import sys
from pathlib import Path

path = Path(sys.argv[1])
text = path.read_text()

old = '''        String path = "lib/" + Build.SUPPORTED_ABIS[0] + "/libXlorie.so";
        ClassLoader loader = CmdEntryPoint.class.getClassLoader();
        URL res = loader != null ? loader.getResource(path) : null;
        String libPath = res != null ? res.getFile().replace("file:", "") : null;
        if (libPath != null) {
            try {
                System.load(libPath);
            } catch (Exception e) {
                Log.e("CmdEntryPoint", "Failed to dlopen " + libPath, e);
                System.err.println("Failed to load native library. Did you install the right apk? Try the universal one.");
                System.exit(134);
            }
        } else {
            // It is critical only when it is not running in Android application process
            if (MainActivity.getInstance() == null) {
                System.err.println("Failed to acquire native library. Did you install the right apk? Try the universal one.");
                System.exit(134);
            }
        }'''

new = '''        try {
            System.loadLibrary("Xlorie");
        } catch (UnsatisfiedLinkError e) {
            Log.e("CmdEntryPoint", "Failed to load libXlorie", e);
            if (MainActivity.getInstance() == null) {
                System.err.println("Failed to load native library.");
                System.exit(134);
            }
        }'''

if old in text:
    path.write_text(text.replace(old, new))
    print("Patched CmdEntryPoint native library loading")
elif 'System.loadLibrary("Xlorie")' in text:
    print("CmdEntryPoint already patched")
else:
    print("CmdEntryPoint patch target not found", file=sys.stderr)
    sys.exit(1)
PY
