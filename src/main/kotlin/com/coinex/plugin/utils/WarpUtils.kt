package com.coinex.plugin.utils

object WarpUtils {

    fun isWarpConnected(): Boolean {
        return try {
            val processBuilder = ProcessBuilder("warp-cli", "status")
                .redirectErrorStream(true)

            val process = processBuilder.start()

            val result = process.inputStream.bufferedReader().use { reader ->
                reader.readText()
            }.trim()

            process.waitFor()
            result.contains("Connected")
        } catch (e: Exception) {
            Log.e { "Git command failed: ${e.message}" }
            false
        }
    }

    fun switchWarpStatus(connect: Boolean): Boolean {
        return try {
            val processBuilder = ProcessBuilder("warp-cli", if (connect) "connect" else "disconnect")
                .redirectErrorStream(true)

            val process = processBuilder.start()
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            Log.e { "Git command failed: ${e.message}" }
            false
        }
    }

    fun runInWarp(callback: () -> Unit) {
        if (!isWarpConnected()) {
            val closeWarpLater = switchWarpStatus(true)
            var count = 5
            do {
                Thread.sleep(500)
                count--
            } while (count > 0 && !isWarpConnected())

            callback.invoke()
            if (closeWarpLater) {
                switchWarpStatus(false)
            }
            return
        }
        callback.invoke()
    }
}