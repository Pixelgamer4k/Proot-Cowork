# Proot Cowork

Android-native AI cowork agent with embedded proot Linux desktop. Inspired by Kimi Work, Claude Cowork, and Hermes Agent.

## Features (Phased)

| Phase | Status | Features |
|-------|--------|----------|
| 1 | ✅ Current | Compose UI shell, 16:9 desktop placeholder, settings, agent chat UI |
| 2 | Planned | Proot + X11 desktop, rootfs import, power controls |
| 3 | Planned | Koog agent, OpenAI-compatible API, plan/direct modes |
| 4 | Planned | Agent swarm, skills (SKILL.md), self-improvement |
| 5 | Planned | Schedule mode, file browser, external terminal |
| 6 | Planned | Polish, testing, release |

## Build

Debug APKs are built via GitHub Actions on every push to `main`:

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

1. Open app → Settings (gear icon)
2. Set API Base URL (e.g. `https://openrouter.ai/api/v1`)
3. Set API Key and Model (e.g. `openrouter/owl-alpha`)
4. Import rootfs (see [rootfs-setup/README.md](rootfs-setup/README.md))

## Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and [docs/RESEARCH.md](docs/RESEARCH.md).

## Rootfs Setup

Build a custom proot-distro rootfs on Termux using scripts in `rootfs-setup/`, then export and import into the app. No extra commands needed after import — the desktop auto-starts.

## License

Apache 2.0
