#! /bin/bash

if [[ -z "${INITIAL_USERNAME}" ]]; then
  INITIAL_USERNAME="user"
fi

# Real setuid su fails inside proot on Android 10+; proot -0 already provides root.
export HOME="/home/${INITIAL_USERNAME}"
export USER="${INITIAL_USERNAME}"
export LOGNAME="${INITIAL_USERNAME}"
cd "${HOME}" || exit 1
exec /support/startVNCServerStep2.sh
