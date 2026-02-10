package com.coinex.plugin.utils

import com.intellij.openapi.project.Project
import java.io.File

object WarpUtils {

    private const val WARP_PATH = "/usr/local/bin/warp-cli"

    enum class WarpStatus {
        CONNECTED,
        DISCONNECTED,
        CONNECTING,
        UNKNOWN
    }

    private fun getWarpCliPath(): String? {
        if (!FileUtils.isExist(WARP_PATH)) {
            return null
        }
        return WARP_PATH
    }

    private fun getWarpStatus(): WarpStatus {
        return try {
            val process = ProcessBuilder(getWarpCliPath(), "status")
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            when {
                output.contains(": Connected") -> WarpStatus.CONNECTED
                output.contains(": Connecting") -> WarpStatus.CONNECTING
                output.contains(": Disconnected") -> WarpStatus.DISCONNECTED
                else -> WarpStatus.UNKNOWN
            }
        } catch (e: Exception) {
            Log.e { "warp status failed: ${e.message}" }
            WarpStatus.UNKNOWN
        }
    }

    private fun requestWarp(project: Project, connect: Boolean): Boolean {
        return try {
            val process = ProcessBuilder(getWarpCliPath(), if (connect) "connect" else "disconnect")
                .directory(File(project.basePath ?: ""))
                .redirectErrorStream(true)
                .start()
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    fun runInWarp(project: Project, callback: () -> Unit) {
        if (getWarpCliPath().isNullOrEmpty()) {
            callback()
            return
        }
        val initialStatus = getWarpStatus()
        val needRestore = initialStatus != WarpStatus.CONNECTED

        if (needRestore) {
            requestWarp(project, true)

            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < 15_000) {
                if (getWarpStatus() == WarpStatus.CONNECTED) {
                    break
                }
                Thread.sleep(500)
            }
        }

        try {
            callback()
        } finally {
            if (needRestore) {
                requestWarp(project, false)
            }
        }
    }
}