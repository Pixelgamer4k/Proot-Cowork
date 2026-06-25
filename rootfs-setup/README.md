# Proot Cowork Rootfs Setup

Build Ubuntu + XFCE + Cowork automation, export once, import into **Proot Cowork**.

## Two ways to build

| Method | When to use |
|--------|-------------|
| **[Manual Termux guide](../docs/TERMUX-MANUAL-ROOTFS.md)** | On-device build; step-by-step commands (recommended while CI runs) |
| **GitHub Actions** | Faster on GitHub runners: `gh workflow run build-rootfs.yml` |
| **Script shortcuts** (below) | After `00-install-termux-scripts.sh` — one-liners for repeat builds |

---

## Manual build (no outer scripts)

Full walkthrough: **[docs/TERMUX-MANUAL-ROOTFS.md](../docs/TERMUX-MANUAL-ROOTFS.md)**

Summary:

1. `pkg install proot-distro rsync tar git`
2. `proot-distro install ubuntu`
3. `proot-distro login ubuntu` → manual `apt-get install` for XFCE (Part 3 in doc)
4. `bash app/src/main/assets/cowork/proot-xfce-cowork-setup.sh ubuntu` (guest script only)
5. `proot-distro backup ubuntu -o ~/proot-cowork-ubuntu.tar.gz` or manual `rsync` + `tar` (Part 5 in doc)
6. Import in Cowork → `proot-xfce-start ubuntu` → **Show X11**

Output: `~/proot-cowork-ubuntu.tar.gz`

---

## Script shortcuts (F-Droid Termux)

```bash
cd ~/Proot-Cowork/rootfs-setup
bash 00-install-termux-scripts.sh   # installs proot-xfce-* commands

bash 01-termux-bootstrap.sh
bash 02-install-distro.sh
proot-xfce-install ubuntu
bash ../app/src/main/assets/cowork/proot-xfce-cowork-setup.sh ubuntu
proot-xfce-export ubuntu
```

`00-install-termux-scripts.sh` installs `proot-xfce-install`, `proot-xfce-export`, and `proot-xfce-start` under `$PREFIX/bin`.

```bash
# Optional: shrink before export
CLEAN_BEFORE_EXPORT=1 proot-xfce-export ubuntu

# Custom output path
OUTPUT=/sdcard/Download/ubuntu.tar.gz proot-xfce-export ubuntu
```

---

## Import into Proot Cowork

1. Install APK from CI (`proot-cowork-debug-apk`).
2. Copy `proot-cowork-ubuntu.tar.gz` to `Android/data/com.proot/files/` or use file picker.
3. In app terminal: `proot-xfce-start ubuntu`
4. Tap **Show X11**.

Re-export after guest changes; re-import in Cowork.

---

## Scripts reference

| Script | Purpose |
|--------|---------|
| `00-install-termux-scripts.sh` | Install `proot-xfce-*` on F-Droid Termux |
| `01-termux-bootstrap.sh` | `pkg` + proot-distro |
| `02-install-distro.sh` | `proot-distro install ubuntu` |
| `03-guest-provision.sh` | user + sudo (optional) |
| `04-xfce-x11.sh` | XFCE + Mesa for embedded X11 |
| `07-export-proot-container.sh` | Export wrapper → `proot-xfce-export` |
| `../scripts/cowork-guest-layer.sh` | **Inside Ubuntu** — automation + agent |
| `../app/src/main/assets/cowork/proot-xfce-cowork-setup.sh` | Stage assets + run guest layer |
| `proot-xfce-export.sh` | Canonical export (also in APK assets) |

---

## Legacy UserLAnd VNC path

```bash
bash 04-xfce-install.sh      # xfce4 + tightvncserver
bash 06-export-rootfs.sh     # proot-cowork-rootfs.tar.gz
```

---

## Import formats

| Archive layout | Supported |
|----------------|-----------|
| `ubuntu/manifest.json` + `ubuntu/rootfs/` | Yes (proot-distro backup) |
| Flat rootfs (`usr/bin/bash` at top level) | Yes (wrapped as ubuntu) |

Templates: `templates/ubuntu/manifest.json`, `templates/ubuntu/sysdata/`

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Import fails validation | Run XFCE install + cowork setup before export |
| Black desktop / missing icons | `proot-xfce-start ubuntu`, re-export if needed |
| `proot-distro list` empty but login works | Normal after import |
| Export bind-mount errors | Use manual `rsync` excludes — see [TERMUX-MANUAL-ROOTFS.md](../docs/TERMUX-MANUAL-ROOTFS.md) Part 5c |

See [third_party/USERLAND-NOTICE.md](../third_party/USERLAND-NOTICE.md) for the legacy VNC backend.
