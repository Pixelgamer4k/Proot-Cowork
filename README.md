# Proot Cowork

Android-native AI cowork agent with embedded proot Linux desktop. Inspired by Kimi Work, Claude Cowork, and Hermes Agent.

## Features (Phased)

| Phase | Status | Features |
|-------|--------|----------|
| 1 | ✅ Done | Compose UI shell, 16:9 desktop placeholder, settings, agent chat UI |
| 2 | ✅ Current | Proot + X11 desktop, rootfs import, power controls |
| 3 | Planned | Koog agent, OpenAI-compatible API, plan/direct modes |
| 4 | Planned | Agent swarm, skills (SKILL.md), self-improvement |
| 5 | Planned | Schedule mode, file browser, external terminal |
| 6 | Planned | Polish, testing, release |

## Build

Debug APKs are built via GitHub Actions on every push to `main`. Phase 2 embeds **termux-x11** (native X11 server) — first CI build may take ~30–60 minutes.

```bash
# Local build (requires Android SDK + NDK)
bash scripts/setup-x11-module.sh
./gradlew assembleDebug
```

```bash
# Trigger manually
gh workflow run build-debug-apk.yml

# Download latest artifact
gh run list --workflow=build-debug-apk.yml --limit 1
gh run download <run-id> -n proot-cowork-debug-apk
```

Local build (requires Android SDK):

```bash
./gradlew assembleDebug
```

## Configuration

1. Install the debug APK on your **ARM64** Android device
2. Open app → tap **Add your rootfs** → select `proot-cowork-rootfs.tar.gz`
3. Wait for import + auto-start (X11 + proot desktop in the top panel)
4. Open **Settings** (gear) → set API URL, key, and model for Phase 3 agents

## Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and [docs/RESEARCH.md](docs/RESEARCH.md).

## Rootfs Setup

Build a custom proot-distro rootfs on Termux using scripts in `rootfs-setup/`, then export and import into the app. No extra commands needed after import — the desktop auto-starts.

## License

Apache 2.0
