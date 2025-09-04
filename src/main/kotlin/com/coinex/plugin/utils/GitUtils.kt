package com.coinex.plugin.utils

import com.intellij.openapi.project.Project
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import com.intellij.openapi.progress.ProgressIndicator

object GitUtils {

    fun runGitCommand(
        projectBasePath: String,
        vararg args: String
    ): GitCommandResult {
        return runGitCommand(projectBasePath, true, null, *args)
    }

    fun runGitCommand(
        projectBasePath: String,
        needOutput: Boolean = true,
        progressExt: ((ProcessBuilder) -> Unit)? = null,
        vararg args: String
    ): GitCommandResult {
        return try {
            val processBuilder = ProcessBuilder("git", *args)
                .directory(File(projectBasePath))
                .redirectErrorStream(true)

            progressExt?.invoke(processBuilder)

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
                    "Git command failed with exit code: ${process.exitValue()}, " +
                            "cmd:git ${args.joinToString(separator = " ")}"
                }
            }
            return GitCommandResult.create(process.exitValue(), result)
        } catch (e: Exception) {
            Log.e { "Git command failed: ${e.message}" }
            GitCommandResult.fail(output = e.message ?: "")
        }
    }


    fun getRemoteOriginUrl(project: Project): String? {
        val basePath = project.basePath
        if (!basePath.isNullOrEmpty()) {
            return runGitCommand(basePath, "remote", "get-url", "origin").output
        }
        return null

    }

    /**
     * 获取所有远程分支名π
     */
    fun getRemoteBranchList(project: Project): MutableList<String> {
        val result = mutableListOf<String>()
        val basePath = project.basePath ?: return result
        try {
            val process = ProcessBuilder("git", "branch", "-r")
                .directory(File(basePath))
                .redirectErrorStream(true)
                .start()
            BufferedReader(process.inputStream.reader()).useLines { lines ->
                lines.forEach { line ->
                    val branch = line.trim()
                    // 过滤掉 HEAD -> origin/main 这样的行
                    if (branch.isNotEmpty() && !branch.contains("->")) {
                        val branchName = branch.replace("origin/", "")
                        result.add(branchName)
                    }
                }
            }
            process.waitFor()
        } catch (e: Exception) {
            println("获取远程分支失败: ${e.message}")
        }
        return result
    }

    /**
     * 获取 origin 的 push url
     */
    fun getRemotePushUrl(project: Project): String? {
        val basePath = project.basePath ?: return null
        val output = runGitCommand(basePath, "remote", "get-url", "--push", "origin").output
        return output.ifEmpty { null }
    }

    /**
     * 获取当前项目的分支名
     * @param project 项目
     * @return 当前分支名，如果获取失败返回 null
     */
    fun getCurrentBranch(project: Project): String? {
        val basePath = project.basePath
        if (!basePath.isNullOrEmpty()) {
            val currentBranch = runGitCommand(basePath, "branch", "--show-current").output
            if (currentBranch.isNotEmpty()) {
                return currentBranch
            }
        }

        // 如果当前分支正在rebase，通过在rebase文件里读取分支
        if (isRebaseInProgress(project)) {
            val rebaseHeadBranch = getRebaseHeadBranchName(project)
            if (!rebaseHeadBranch.isNullOrEmpty()) {
                return rebaseHeadBranch
            }
        }

        try {
            // 直接从 .git/HEAD 文件读取
            val gitBranch = getCurrentBranchFromGitHead(project)
            if (gitBranch != null) {
                return gitBranch
            }
        } catch (e: Exception) {
            Log.e { "获取 Git 分支失败: " + e.message }
        }

        return null
    }

    /**
     * 从 .git/HEAD 文件读取当前分支
     */
    private fun getCurrentBranchFromGitHead(project: Project): String? {
        try {
            val projectPath = project.getBasePath()
            if (projectPath == null) {
                return null
            }

            val gitHeadPath = Paths.get(projectPath, ".git", "HEAD")
            if (!Files.exists(gitHeadPath)) {
                return null
            }

            val headContent = Files.readString(gitHeadPath).trim { it <= ' ' }


            // HEAD 文件格式通常是 "ref: refs/heads/branch-name"
            if (headContent.startsWith("ref: refs/heads/")) {
                return headContent.substring("ref: refs/heads/".length)
            }


            // 如果是 detached HEAD，返回前7位 commit hash
            if (headContent.length >= 7) {
                return headContent.substring(0, 7)
            }
        } catch (e: IOException) {
            Log.d { "读取 .git/HEAD 文件失败: " + e.message }
        }

        return null
    }

    /**
     * 检查项目是否是 Git 仓库
     */
    fun isGitRepository(project: Project): Boolean {
        val projectPath = project.basePath ?: return false
        val gitDir = Paths.get(projectPath, ".git")
        return Files.exists(gitDir) && Files.isDirectory(gitDir)
    }

    /**
     * https://ghp_PGgKgzudxQ4F7Ay5UfrphbsUrfuw22224E@github.com/username/reponame.git/
     * @return 去除 token 后的 URL，如 https://github.com/username/reponame.git/
     */
    fun removeGitHubToken(repoUrl: String?): String {
        if (repoUrl.isNullOrEmpty()) {
            return ""
        }

        try {
            // 正则表达式匹配 token@github.com 模式
            val regex = Regex("https://[^@]+@github\\.com")
            return regex.replace(repoUrl, "https://github.com")
        } catch (e: Exception) {
            Log.e { "去除 GitHub token 失败: ${e.message}" }
            return repoUrl
        }
    }

    /**
     * 获取本地分支相对于远程分支未push的commit数量
     * @param project 项目
     * @param branch 分支名
     * @return 未push的commit数量，出错返回0
     */
    fun getUnPushedCommitCount(project: Project, hasRemote: Boolean, branch: String?): Int {
        val basePath = project.basePath ?: return 0
        if (branch.isNullOrBlank()) return 0
        // 先判断远程分支是否存在
        if (!hasRemote) {
            // 远程分支不存在，返回本地分支所有提交数
            val output = runGitCommand(basePath, "rev-list", branch, "--count").output
            return output.toIntOrNull() ?: 0
        }
        // 正常统计未push的commit数量
        val output = runGitCommand(basePath, "rev-list", "origin/$branch..$branch", "--count").output
        return output.toIntOrNull() ?: 0
    }

    /**
     * 推送本地分支到远程
     * @param project 项目
     * @param branch 分支名
     * @return true表示成功，false表示失败
     */
    fun pushBranch(project: Project, branch: String?): GitCommandResult {
        val basePath = project.basePath ?: return GitCommandResult.fail()
        if (branch.isNullOrBlank()) return GitCommandResult.fail()
        return runGitCommand(basePath, false, null, "push", "-u", "origin", branch)
    }

    fun pushBranchForceWithLease(project: Project, branch: String?): GitCommandResult {
        val basePath = project.basePath ?: return GitCommandResult.fail()
        if (branch.isNullOrBlank()) return GitCommandResult.fail()
        val result = runGitCommand(basePath, "push", "--force-with-lease", "origin", branch)
        return result
    }


    fun pushBranchWithCancel(project: Project, branchName: String, indicator: ProgressIndicator?): GitCommandResult {
        val basePath = project.basePath ?: return GitCommandResult.fail()
        try {
            val process = ProcessBuilder("git", "push", "-u", "origin", branchName)
                .directory(java.io.File(basePath))
                .redirectErrorStream(true)
                .start()

            // 检查取消状态，主动中断
            while (process.isAlive) {
                if (indicator?.isCanceled == true) {
                    process.destroy()
                    return GitCommandResult.fail()
                }
                Thread.sleep(100)
            }
            val output = process.inputStream.bufferedReader().use { reader ->
                reader.readText()
            }.trim()
            val result = GitCommandResult.success(process.exitValue(), output)
            Log.e { "Git push result: $output" }
            return result
        } catch (e: Exception) {
            Log.e { "Git push failed: ${e.message}" }
            return GitCommandResult.fail()
        }
    }

    /**
     * 获取所有本地分支名
     * @param project 项目
     * @return 本地分支名列表
     */
    fun getLocalBranchList(project: Project): MutableList<String> {
        val basePath = project.basePath ?: return mutableListOf()
        val result = runGitCommand(basePath, "branch")
        // 解析分支名，去掉*和前后空格
        return result.output.lines().mapNotNull {
            it.replace("*", "").trim().takeIf { name -> name.isNotEmpty() }
        }.toMutableList()
    }

    fun getNewestCommitId(project: Project, branch: String?): String? {
        val basePath = project.basePath ?: return null
        if (branch.isNullOrBlank()) return null
        val output = runGitCommand(basePath, "rev-parse", branch).output
        return output.ifEmpty { null }
    }

    fun createLocalBranchIfNotExists(project: Project, branch: String, fromBranch: String): GitCommandResult {
        val basePath = project.basePath
        val localBranches = getLocalBranchList(project)
        if (basePath == null || localBranches.contains(branch)) {
            return GitCommandResult.fail()
        }
        return runGitCommand(basePath, "branch", branch, fromBranch)
    }

    fun isLocalBranchExists(project: Project, branchName: String): Boolean {
        val localBranches = getLocalBranchList(project)
        return localBranches.contains(branchName)
    }

    fun needRebase(project: Project, source: String, target: String): Boolean {
        if (source.isEmpty() || target.isEmpty()) {
            return false
        }
        val basePath = project.basePath ?: return false
        val result = runGitCommand(basePath, false, null, "merge-base", "--is-ancestor", "origin/$target", source)
        return result.exitValue != 0
    }

    fun rebaseCurrentOnToTargetBranch(project: Project, target: String): GitCommandResult {
        val basePath = project.basePath ?: return GitCommandResult.fail()
        return runGitCommand(basePath, "rebase", target)
    }

    fun rebaseContinue(project: Project): GitCommandResult {
        val basePath = project.basePath ?: return GitCommandResult.fail()
        return runGitCommand(
            basePath,
            true,
            { processBuilder ->
                processBuilder.environment()["GIT_EDITOR"] = "true" // 让 git 以“空编辑器”方式跳过编辑步骤，自动提交当前 message
            },
            args = arrayOf("rebase", "--continue"),
        )
    }

    fun isRebaseInProgress(project: Project): Boolean {
        val basePath = project.basePath ?: return false
        // .git/rebase-merge 或 .git/rebase-apply 目录存在时，说明正在 rebase
        val gitDir = File(basePath, ".git")
        val rebaseMerge = File(gitDir, "rebase-merge")
        val rebaseApply = File(gitDir, "rebase-apply")
        return rebaseMerge.exists() || rebaseApply.exists()
    }


    fun getRebaseHeadBranchName(project: Project): String? {
        val basePath = project.basePath ?: return null
        val gitDir = File(basePath, ".git")
        val rebaseMerge = File(gitDir, "rebase-merge")
        val headNameFile = File(rebaseMerge, "head-name")
        if (rebaseMerge.exists() && headNameFile.exists()) {
            try {
                val content = headNameFile.readText().trim()
                // 通常内容为 "refs/heads/branch-name"，取最后一段
                return content.replace("refs/heads/", "")
            } catch (e: Exception) {
                Log.e { "读取 rebase-merge/head-name 失败: ${e.message}" }
            }
        }
        return null
    }

    fun hasUnresolvedConflicts(project: Project): Boolean {
        val basePath = project.basePath ?: return false
        val result = runGitCommand(basePath, "status", "--porcelain")
        // 检查输出中是否有冲突标记
        return result.output.lines().any { it.startsWith("UU ") || it.startsWith("AA ") || it.startsWith("DU ") }
    }

    fun hasUnPushCommitOnceRebase(project: Project): Boolean {
        val basePath = project.basePath ?: return false
        val gitDir = File(basePath, ".git")
        val rebaseHead = File(gitDir, "REBASE_HEAD")
        return rebaseHead.exists() && rebaseHead.readText().trim().isNotEmpty()
    }


}
