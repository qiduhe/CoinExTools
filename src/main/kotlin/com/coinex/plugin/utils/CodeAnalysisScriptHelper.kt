package com.coinex.plugin.utils

import com.intellij.openapi.project.Project
import java.io.File

object CodeAnalysisScriptHelper {


    private const val CODE_ANALYSIS_SCRIPT_NAME = "codeAnalysis.sh"
    private const val CODE_ANALYSIS_SCRIPT_DIR = "script"
    private const val CODE_ANALYSIS_RESULT_PATH = "build/codeAnalysisReports"

    fun isProjectHasCheckScript(project: Project): Boolean {
        return ScriptInstaller.isScriptInstalled(project, CODE_ANALYSIS_SCRIPT_DIR, CODE_ANALYSIS_SCRIPT_NAME)
    }

    fun installScript(project: Project): Boolean {
        val success = ScriptInstaller.installScript(
            project.basePath + File.separator + CODE_ANALYSIS_SCRIPT_DIR,
            CODE_ANALYSIS_SCRIPT_NAME
        )
        if (success) {
            GitUtils.addToGitignoreIfNeeded(project, getCodeAnalysisScriptPath())
        }
        return success
    }

    fun isBranchHadRunCodeAnalysis(project: Project, branch: String?): Boolean {
        branch ?: return false
        val reportsDir = File(project.basePath, CODE_ANALYSIS_RESULT_PATH)
        val reportFileName = getReportFileByBranch(branch)
        val reportFile = File(reportsDir, reportFileName)
        if (!reportFile.exists()) {
            return false
        }
        // 读取报告文件内容
        val reportCommitId = try {
            reportFile.readText().trim()
        } catch (e: Exception) {
            return false
        }
        // 获取分支最新commit id
        val newestCommitId = GitUtils.getNewestCommitId(project, branch)
        Log.d { "reportCommitId=$reportCommitId   newestCommitId=$newestCommitId   " }
        if (!reportCommitId.isNullOrEmpty() && reportCommitId == newestCommitId) {
            return true
        }
        return false
    }

    fun getReportFileByBranch(branch: String): String {
        return branch.replace("/", "-")
    }

    fun getCodeAnalysisScriptPath(): String {
        return CODE_ANALYSIS_SCRIPT_DIR + File.separator + CODE_ANALYSIS_SCRIPT_NAME
    }
}