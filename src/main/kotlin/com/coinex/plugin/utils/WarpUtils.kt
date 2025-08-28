package com.coinex.plugin.utils

import java.io.File

object WarpUtils {

    fun isWarpConnected(path: String): Boolean {
        try {
            val processBuilder = ProcessBuilder("warp-cli", "status")
                .directory(File(path))
                .redirectErrorStream(true)

            val process = processBuilder.start()

            val result = process.inputStream.bufferedReader().use { reader ->
                reader.readText()
            }.trim()

            process.waitFor()
            return result.contains("Connected")
        } catch (e: Exception) {
            Log.e { "Git command failed: ${e.message}" }
        }
        return false
    }

    fun switchWarpStatus(path: String, connect: Boolean): Boolean {
        try {
            val processBuilder = ProcessBuilder("warp-cli", if (connect) "connect" else "disconnect")
                .directory(File(path))
                .redirectErrorStream(true)

            val process = processBuilder.start()
            process.waitFor()
            return process.exitValue() == 0
        } catch (e: Exception) {
            Log.e { "Git command failed: ${e.message}" }
        }

        return false
    }

    fun runInWarp(path: String, callback: () -> Unit) {
        if (!isWarpConnected(path)) {
            val closeWarpLater = switchWarpStatus(path, true)
            callback.invoke()
            if (closeWarpLater) {
                switchWarpStatus(path, false)
            }
            return
        }
        callback.invoke()
    }
}