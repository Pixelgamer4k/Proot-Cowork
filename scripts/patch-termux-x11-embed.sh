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
changed = False

replacements = [
    (
        '''        String path = "lib/" + Build.SUPPORTED_ABIS[0] + "/libXlorie.so";
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
        }''',
        '''        try {
            System.loadLibrary("Xlorie");
        } catch (UnsatisfiedLinkError e) {
            Log.e("CmdEntryPoint", "Failed to load libXlorie", e);
        }''',
    ),
    (
        '''        try {
            System.loadLibrary("Xlorie");
        } catch (UnsatisfiedLinkError e) {
            Log.e("CmdEntryPoint", "Failed to load libXlorie", e);
            if (MainActivity.getInstance() == null) {
                System.err.println("Failed to load native library.");
                System.exit(134);
            }
        }''',
        '''        try {
            System.loadLibrary("Xlorie");
        } catch (UnsatisfiedLinkError e) {
            Log.e("CmdEntryPoint", "Failed to load libXlorie", e);
        }''',
    ),
    (
        '''    CmdEntryPoint(String[] args) {
        if (!start(args))
            System.exit(1);

        spawnListeningThread();
        sendBroadcastDelayed();
    }''',
        '''    CmdEntryPoint(String[] args) {
        if (!start(args)) {
            Log.e("CmdEntryPoint", "native start() failed");
            if (getenv("TERMUX_X11_OVERRIDE_PACKAGE") == null
                    && System.getProperty("TERMUX_X11_OVERRIDE_PACKAGE") == null) {
                System.exit(1);
            }
            return;
        }

        spawnListeningThread();
        sendBroadcastDelayed();
    }''',
    ),
    (
        '''        String targetPackage = getenv("TERMUX_X11_OVERRIDE_PACKAGE");
        if (targetPackage == null)
            targetPackage = "com.termux.x11";''',
        '''        String targetPackage = getenv("TERMUX_X11_OVERRIDE_PACKAGE");
        if (targetPackage == null)
            targetPackage = System.getProperty("TERMUX_X11_OVERRIDE_PACKAGE");
        if (targetPackage == null)
            targetPackage = "com.termux.x11";''',
    ),
]

for old, new in replacements:
    if old in text:
        text = text.replace(old, new)
        changed = True

if not changed and 'System.getProperty("TERMUX_X11_OVERRIDE_PACKAGE")' in text:
    print("CmdEntryPoint already fully patched")
elif not changed:
    print("CmdEntryPoint patch targets not found", file=sys.stderr)
    sys.exit(1)
else:
    path.write_text(text)
    print("Patched CmdEntryPoint for embedded mode")
PY
