# Proot Cowork Rootfs Setup

Build a Ubuntu proot-distro rootfs that Proot Cowork can import. The app runs the desktop with **embedded VNC** (Xvfb + x11vnc + XFCE inside proot).

## Prerequisites

On your Android device (Termux from **F-Droid**):

1. [Termux](https://f-droid.org/en/packages/com.termux/)
2. Inside Termux: `pkg update && pkg install proot-distro`

Termux:X11 is **not** required for Proot Cowork v0.6+.

## Quick Setup (Ubuntu + XFCE + VNC)

Run these scripts in order from Termux (clone or copy `rootfs-setup/` into Termux):

```bash
bash 01-termux-bootstrap.sh
bash 02-install-distro.sh
bash 03-guest-provision.sh
bash 04-xfce-install.sh
bash 05-agent-tools.sh   # optional
bash 06-export-rootfs.sh
```

This creates `proot-cowork-rootfs.tar.gz` in your home directory.

Transfer it to phone storage, then in Proot Cowork tap **Add your rootfs** → Import.

## What the app does

1. Extract tarball to `files/rootfs/`
2. Deploy `/start-desktop.sh` (VNC session script) if needed
3. Run proot with bind mounts for `/dev`, `/proc`, `/sys`
4. Guest starts Xvfb `:99`, x11vnc on port `5900`, then `startxfce4`
5. Embedded RFB viewer in the 16:9 panel connects to `127.0.0.1:5900`

## Required guest packages

- `xfce4`, `dbus-x11` — desktop session
- `xvfb`, `x11vnc` — virtual display + VNC server

Script `04-xfce-install.sh` installs these and writes `/start-desktop.sh`.

**Do not use** `proot-distro backup` for export — it nests paths incorrectly. Use `06-export-rootfs.sh`.

## Export note (proot-distro v5+)

Rootfs lives at `containers/<name>/rootfs/` (not `installed-rootfs/`). The export script detects both layouts.

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Import: missing Xvfb/x11vnc | Re-run `04-xfce-install.sh` |
| Import: missing startxfce4 | `apt install -y xfce4 dbus-x11` in guest |
| Grey/black VNC panel | Check logs in app; ensure `VNC_READY` in guest log |
| XFCE panel crashes (glycin) | App sets `GDK_PIXBUF_DISABLE_GLYCIN=1` in start script |
| `/etc/sudoers.d/cowork` error | Re-run `03-guest-provision.sh` (installs sudo first) |
| `can't sanitize binding /proc/self/fd/0` | Harmless proot warning on Termux |
| Large tarball | `apt clean` in guest before export |
| `tar: ./var/lib/snapd/void` | Use updated `06-export-rootfs.sh` excludes |

See [docs/RESEARCH.md](../docs/RESEARCH.md) for architecture notes.
