package com.coinex.plugin.utils

import com.coinex.plugin.ConfigManager
import java.io.File

object GHUtils {

    const val DEFAULT_GH_PATH = "/opt/homebrew/bin/gh"

    fun runGHCommand(
        projectBasePath: String,
        needOutput: Boolean = true,
        vararg args: String
    ): GHCommandResult {
        return try {
            val ghPath = ConfigManager.getGHPath()
            if (!FileUtils.isExist(ghPath)) {
                return GHCommandResult.fail(output = "请安装 GitHub CLI (gh)，并在配置中心设置路径")
            }

            val processBuilder = ProcessBuilder(ghPath, *args)
                .directory(File(projectBasePath))
                .redirectErrorStream(true)

            val process = processBuilder.start()

            val result = if (needOutput) {
                process.inputStream.bufferedReader().use { reader ->
                    reader.readText()
                }.trim()
            } else {
                ""
            }

            process.waitFor()
            if (process.exitValue() != 0) {
                Log.e {
                    "gh command failed with exit code: ${process.exitValue()}, " +
                            "cmd:gh ${args.joinToString(separator = " ")}"
                }
            }
            return GHCommandResult.success(process.exitValue(), result)
        } catch (e: Exception) {
            Log.e { "gh command failed: ${e.message}" }
            GHCommandResult.fail(output = e.message ?: "")
        }
    }

    fun createPRRequest(
        projectBasePath: String,
        title: String,
        sourceBranch: String,
        targetBranch: String,
    ): GHCommandResult {
        return runGHCommand(
            projectBasePath,
            true,
            args = arrayOf(
                "pr", "create",
                "--title", title,
                "--body", "",
                "--base", targetBranch,
                "--head", sourceBranch
            )
        )
    }

    /**
     * 从gh创建pr输出中提取pr链接
     */
    fun extractPrUrl(text: String): String? {
        val pattern = Regex("""https://github\.com/[^\s]+/pull/\d+""")
        return pattern.find(text)?.value
    }
}