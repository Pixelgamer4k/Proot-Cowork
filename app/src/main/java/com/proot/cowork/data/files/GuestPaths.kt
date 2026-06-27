package com.proot.cowork.data.files

/** Well-known paths inside the Ubuntu proot-distro guest (login shell is root). */
object GuestPaths {
    const val ROOT = "/"
    const val HOME = "/root"
    const val DESKTOP = "/root/Desktop"
    const val ARTIFACTS_DIR = "/root/Desktop/Artifacts"

    fun isAllowed(path: String): Boolean {
        val normalized = normalize(path)
        if (normalized.isEmpty()) return false
        if (!normalized.startsWith("/")) return false
        // Block kernel virtual fs from shell listing (hangs / huge dirs).
        if (normalized == "/proc" || normalized.startsWith("/proc/")) return false
        if (normalized == "/sys" || normalized.startsWith("/sys/")) return false
        if (normalized == "/dev" || normalized.startsWith("/dev/")) return false
        return true
    }

    fun normalize(path: String): String {
        val trimmed = path.trim()
        if (trimmed.isEmpty() || trimmed == "/") return ROOT
        return trimmed.trimEnd('/')
    }

    fun ensureArtifactsCmd(): String =
        "mkdir -p '${ARTIFACTS_DIR}' '${DESKTOP}'"
}
