#!/usr/bin/env bash
# Patch termux terminal-view for live IME echo when char-based input is enforced.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TERMINAL_VIEW="$ROOT/third_party/termux-app/terminal-view/src/main/java/com/termux/view/TerminalView.java"

if [[ ! -f "$TERMINAL_VIEW" ]]; then
  echo "TerminalView.java not found at $TERMINAL_VIEW" >&2
  exit 1
fi

python3 - "$TERMINAL_VIEW" <<'PY'
import sys
from pathlib import Path

path = Path(sys.argv[1])
text = path.read_text()
marker = "COWORK_LIVE_COMPOSE_COMMIT"

if marker in text:
    print("==> terminal-view live-compose patch already applied")
    sys.exit(0)

needle = """            @Override
            public boolean deleteSurroundingText(int leftLength, int rightLength) {
                if (TERMINAL_VIEW_KEY_LOGGING_ENABLED) {
                    mClient.logInfo(LOG_TAG, "IME: deleteSurroundingText(" + leftLength + ", " + rightLength + ")");
                }"""

insert = """            @Override
            public boolean setComposingText(CharSequence text, int newCursorPosition) {
                if (mClient.shouldEnforceCharBasedInput() && text != null && text.length() > 0) {
                    if (TERMINAL_VIEW_KEY_LOGGING_ENABLED) {
                        mClient.logInfo(LOG_TAG, "IME: setComposingText live-commit(\\"" + text + "\\", " + newCursorPosition + ")");
                    }
                    return commitText(text, newCursorPosition);
                }
                return super.setComposingText(text, newCursorPosition);
            } // """ + marker + """

            @Override
            public boolean deleteSurroundingText(int leftLength, int rightLength) {
                if (TERMINAL_VIEW_KEY_LOGGING_ENABLED) {
                    mClient.logInfo(LOG_TAG, "IME: deleteSurroundingText(" + leftLength + ", " + rightLength + ")");
                }"""

if needle not in text:
    raise SystemExit("patch target missing in TerminalView.java")

path.write_text(text.replace(needle, insert, 1))
print("==> Applied terminal-view live-compose patch")
PY
