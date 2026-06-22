package com.proot.cowork.data.proot

import java.io.File

/**
 * Fake /proc entries that Android blocks or restricts, matching proot-distro's sysdata layout.
 */
object ProotSysdata {

    private const val FAKE_KERNEL_RELEASE = "6.17.0-PRoot-Cowork"
    private const val FAKE_KERNEL_VERSION =
        "#1 SMP PREEMPT_DYNAMIC Fri, 10 Oct 2025 00:00:00 +0000"

    fun ensure(sysdataDir: File) {
        sysdataDir.mkdirs()
        File(sysdataDir, "sys_empty").mkdirs()

        writeIfMissing(File(sysdataDir, "loadavg"), "0.12 0.07 0.02 2/165 765\n")
        writeIfMissing(
            File(sysdataDir, "version"),
            "Linux version $FAKE_KERNEL_RELEASE (proot@cowork) " +
                "(gcc (GCC) 13.3.0, GNU ld (GNU Binutils) 2.42) $FAKE_KERNEL_VERSION\n",
        )
        writeIfMissing(File(sysdataDir, "uptime"), "124.08 932.80\n")
        writeIfMissing(File(sysdataDir, "sysctl_entry_cap_last_cap"), "40\n")
        writeIfMissing(File(sysdataDir, "sysctl_inotify_max_user_watches"), "4096\n")
        writeIfMissing(File(sysdataDir, "sysctl_kernel_overflowuid"), "65534\n")
        writeIfMissing(File(sysdataDir, "sysctl_kernel_overflowgid"), "65534\n")
    }

    fun fakeProcBindArgs(sysdataDir: File): List<String> {
        val bindings = mutableListOf<String>()
        val checks = listOf(
            "/proc/loadavg" to "loadavg",
            "/proc/stat" to "stat",
            "/proc/uptime" to "uptime",
            "/proc/version" to "version",
            "/proc/vmstat" to "vmstat",
            "/proc/sys/kernel/cap_last_cap" to "sysctl_entry_cap_last_cap",
            "/proc/sys/fs/inotify/max_user_watches" to "sysctl_inotify_max_user_watches",
            "/proc/sys/kernel/overflowuid" to "sysctl_kernel_overflowuid",
            "/proc/sys/kernel/overflowgid" to "sysctl_kernel_overflowgid",
        )
        for ((realPath, fakeName) in checks) {
            if (!isReadable(realPath)) {
                bindings += "${File(sysdataDir, fakeName).absolutePath}:$realPath"
            }
        }
        return bindings
    }

    fun kernelReleaseArg(hostname: String, machine: String): String {
        return "\\Linux\\$hostname\\$FAKE_KERNEL_RELEASE\\$FAKE_KERNEL_VERSION\\$machine\\localdomain\\-1\\"
    }

    private fun isReadable(path: String): Boolean =
        runCatching { File(path).inputStream().use { it.read() } }.isSuccess

    private fun writeIfMissing(file: File, content: String) {
        if (!file.isFile) {
            file.writeText(content)
        }
    }
}
