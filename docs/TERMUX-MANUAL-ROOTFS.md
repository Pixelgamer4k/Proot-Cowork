# Build rootfs manually in Termux (no CI)

Use this when GitHub Actions rootfs builds are slow or unavailable. You run **plain Termux commands** on the phone; only the **inside-Ubuntu cowork install** uses bundled scripts (`cowork-guest-layer.sh`).

**Time:** ~1–2 hours on a fast ARM64 phone · **Storage:** ~6 GB free during build + export · **Output:** `proot-cowork-ubuntu.tar.gz` (~1.5–2 GB)

---

## Choose where to build

| Host | Best for |
|------|----------|
| **F-Droid Termux** | Building rootfs (more RAM, familiar `pkg`) |
| **Proot Cowork embedded Termux** | Rebuilding after small guest changes (cowork assets already in `$PREFIX/share/cowork`) |

Both use the same steps below. Set `PREFIX` if needed:

```bash
# F-Droid Termux (default)
PREFIX="${PREFIX:-$HOME/../usr}"

# Proot Cowork embedded Termux (in app terminal)
PREFIX="${PREFIX:-/data/user/0/com.proot/files/usr}"
export TERMUX_APP__PACKAGE_NAME=com.proot
export TERMUX__PREFIX="$PREFIX"
export PATH="$PREFIX/bin:$PATH"
```

---

## Part 1 — Termux packages (manual)

```bash
pkg update -y
pkg install -y git proot-distro rsync tar curl wget unzip
```

Clone the repo (needed for manifest templates and cowork scripts):

```bash
cd ~
git clone https://github.com/Pixelgamer4k/Proot-Cowork.git
cd Proot-Cowork
```

---

## Part 2 — Install Ubuntu container (manual)

```bash
proot-distro install ubuntu
proot-distro list    # should show "ubuntu"
```

Container rootfs path (used later):

```text
$PREFIX/var/lib/proot-distro/containers/ubuntu/rootfs/
```

---

## Part 3 — Install XFCE desktop inside Ubuntu (manual apt)

Enter the guest:

```bash
proot-distro login ubuntu --shared-tmp
```

Inside Ubuntu, run these commands (no wrapper scripts):

```bash
export DEBIAN_FRONTEND=noninteractive

apt-get update
apt-get install -y ca-certificates software-properties-common
add-apt-repository -y universe
add-apt-repository -y multiverse
apt-get update

apt-get install -y \
  task-xfce-desktop \
  xfce4-goodies \
  dbus dbus-x11 sudo \
  thunar thunar-archive-plugin thunar-volman \
  libreoffice-gtk3 \
  build-essential git vim curl wget nano less tree htop file \
  openssh-client rsync unzip zip jq \
  python3 python3-pip python3-venv python3-dev \
  net-tools iputils-ping \
  mesa-utils libgl1-mesa-dri libglx-mesa0 libegl-mesa0 libgbm1 libgl1 \
  greybird-gtk-theme elementary-xfce-icon-theme librsvg2-common gtk2-engines-pixbuf \
  fonts-dejavu fonts-liberation fonts-noto fonts-noto-color-emoji fontconfig \
  pulseaudio pavucontrol \
  xfce4-screenshooter xfce4-taskmanager xfce4-notes xfce4-power-manager \
  ristretto parole xarchiver file-roller evince mousepad \
  x11-xserver-utils x11-utils xdotool

apt-get clean
rm -rf /var/lib/apt/lists/*
```

Quick check (still inside guest):

```bash
test -x /usr/bin/startxfce4 || test -x /usr/bin/xfce4-session
echo "XFCE OK"
exit
```

---

## Part 4 — Install Cowork automation (guest script)

Back on the **Termux host** (not inside Ubuntu). This step uses the repo’s **guest-only** script for computer-use tools, Firefox, and agent files.

### Option A — helper (copies assets + runs guest script)

```bash
cd ~/Proot-Cowork
bash app/src/main/assets/cowork/proot-xfce-cowork-setup.sh ubuntu
```

Takes ~10–20 minutes (Firefox download, OCR, OpenCV, agent install).

### Option B — fully manual staging

```bash
DISTRO=ubuntu
ROOTFS="$PREFIX/var/lib/proot-distro/containers/$DISTRO/rootfs"
REPO=~/Proot-Cowork

rm -rf "$ROOTFS/cowork-bundle"
mkdir -p "$ROOTFS/cowork-bundle/cowork-assets/computer-use" \
         "$ROOTFS/cowork-bundle/cowork-assets/bin"

cp "$REPO/scripts/cowork-guest-layer.sh" "$ROOTFS/cowork-bundle/"
cp -a "$REPO/app/src/main/assets/cowork/computer-use/." \
      "$ROOTFS/cowork-bundle/cowork-assets/computer-use/"
for f in cowork-agent cowork-dispatch cowork-desktop-test; do
  cp "$REPO/app/src/main/assets/cowork/${f}.sh" \
     "$ROOTFS/cowork-bundle/cowork-assets/bin/$f"
  chmod 755 "$ROOTFS/cowork-bundle/cowork-assets/bin/$f"
done
chmod 755 "$ROOTFS/cowork-bundle/cowork-guest-layer.sh"

proot-distro login "$DISTRO" --shared-tmp -- bash -lc \
  'export DEBIAN_FRONTEND=noninteractive; bash /cowork-bundle/cowork-guest-layer.sh'
```

### Verify cowork layer

```bash
proot-distro login ubuntu --shared-tmp -- bash -lc '
  test -f /opt/cowork/computer-use/cowork_desktop.py && echo computer-use: OK
  command -v xdotool && xdotool --version
  command -v firefox && firefox --version || command -v falkon
  command -v cowork-agent && echo cowork-agent: OK
'
```

Optional smoke test:

```bash
proot-distro login ubuntu --shared-tmp -- cowork-desktop-test
```

---

## Part 5 — Export tarball (manual)

### 5a. Optional — shrink guest before export

```bash
proot-distro login ubuntu --shared-tmp -- bash -lc 'apt-get clean; rm -rf /var/lib/apt/lists/* /tmp/*'
```

### 5b. Try native backup first

```bash
proot-distro backup ubuntu -o ~/proot-cowork-ubuntu.tar.gz
```

If that works, verify:

```bash
tar -tzf ~/proot-cowork-ubuntu.tar.gz | head
tar -tzf ~/proot-cowork-ubuntu.tar.gz | grep -q ubuntu/rootfs/usr/bin/bash && echo OK
```

Skip to **Part 6** if the archive looks correct.

### 5c. Manual pack (if `proot-distro backup` fails)

```bash
DISTRO=ubuntu
REPO=~/Proot-Cowork
RUNTIME="$PREFIX/var/lib/proot-distro/containers/$DISTRO"
OUT="${OUT:-$HOME/proot-cowork-ubuntu.tar.gz}"
WORKDIR="$(mktemp -d)"
STAGE="$WORKDIR/$DISTRO"

mkdir -p "$STAGE/rootfs" "$STAGE/sysdata"
cp "$REPO/rootfs-setup/templates/ubuntu/manifest.json" "$STAGE/"
cp -a "$REPO/rootfs-setup/templates/ubuntu/sysdata/." "$STAGE/sysdata/"

rsync -aHAXx \
  --exclude=var/lib/snapd --exclude=snap \
  --exclude=proc --exclude=sys --exclude=dev --exclude=run --exclude=/tmp \
  --exclude=mnt --exclude=data --exclude=storage --exclude=sdcard \
  --exclude=apex --exclude=odm --exclude=product --exclude=system \
  --exclude=system_ext --exclude=vendor --exclude=linkerconfig \
  "$RUNTIME/rootfs/" "$STAGE/rootfs/"

rm -f "$OUT"
tar -C "$WORKDIR" -czf "$OUT" \
  --exclude="${DISTRO}/rootfs/var/lib/snapd" \
  --exclude="${DISTRO}/rootfs/snap" \
  --exclude="${DISTRO}/rootfs/proc" \
  --exclude="${DISTRO}/rootfs/sys" \
  --exclude="${DISTRO}/rootfs/dev" \
  --exclude="${DISTRO}/rootfs/run" \
  --exclude="${DISTRO}/rootfs/tmp" \
  "$DISTRO"

rm -rf "$WORKDIR"
du -h "$OUT"
```

### 5d. Copy to shared storage (optional)

```bash
# Termux storage permission: termux-setup-storage
cp ~/proot-cowork-ubuntu.tar.gz /sdcard/Download/
```

Or from a PC:

```bash
adb push ~/proot-cowork-ubuntu.tar.gz /sdcard/Download/
```

---

## Part 6 — Import into Proot Cowork

1. Install the debug APK from CI (`proot-cowork-debug-apk` artifact).
2. Copy the tarball to either:
   - `Android/data/com.proot/files/proot-cowork-ubuntu.tar.gz`, or
   - anywhere and use **Import Ubuntu desktop → Choose file…**
3. Wait for import to finish.
4. In the app terminal:

   ```bash
   proot-xfce-start ubuntu
   ```

5. Tap **Show X11** in the app.

---

## What uses scripts vs manual commands

| Step | Method |
|------|--------|
| Termux `pkg`, `proot-distro install` | **Manual commands** (this doc) |
| XFCE + base packages in Ubuntu | **Manual `apt-get`** (Part 3) |
| Cowork automation, Firefox, agent | **`cowork-guest-layer.sh`** inside guest (Part 4) |
| Export | **Manual** `proot-distro backup` or `rsync` + `tar` (Part 5) |
| CI Docker build | `.github/workflows/build-rootfs.yml` (optional alternative) |

Convenience commands (optional, not required for this guide):

- `proot-xfce-install` — wraps Part 3 in one script
- `proot-xfce-export` — wraps Part 5 in one script
- `proot-xfce-cowork-setup.sh` — wraps Part 4 Option A

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `proot-distro: command not found` | `pkg install proot-distro` |
| Export `cp` errors on `mnt`, `storage`, `vendor` | Use the `rsync --exclude=…` list in Part 5c |
| Import fails validation | Re-run Part 3 + Part 4; ensure `ubuntu/manifest.json` exists in tarball |
| Black desktop after import | `proot-xfce-start ubuntu`, then **Show X11** |
| Cowork layer missing | Re-run Part 4; check `/opt/cowork/computer-use/cowork_desktop.py` in guest |
| Permission denied writing `/sdcard` | `termux-setup-storage`, or use `adb push` from PC |

---

## CI alternative

To build the same rootfs on GitHub instead of the phone:

```bash
gh workflow run build-rootfs.yml
gh run list --workflow=build-rootfs.yml --limit 1
gh run download <run-id> -n proot-cowork-ubuntu-rootfs
```

See [rootfs-setup/README.md](../rootfs-setup/README.md) for script-based shortcuts.
