package com.coinex.plugin


import com.coinex.plugin.utils.*
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project


object ConfigManager {

    private const val PACKAGE_ID = "coinex"
    private const val PROJECT_PR_URL = "${PACKAGE_ID}.pr_base_url"
    private const val KEY_LAST_TARGET_BRANCH = "${PACKAGE_ID}.last_tar_branch"
    private const val KEY_FEATURE_BASE_BRANCH = "${PACKAGE_ID}.feature_base_branch"
    private const val KEY_PERSONAL_BRANCH_SUFFIX = "${PACKAGE_ID}.personal_branch_suffix"
    private const val KEY_GH_PATH = "${PACKAGE_ID}.gh_path"
    private const val KEY_PR_TITLE_HISTORY = "${PACKAGE_ID}.pr_title_history"
    private const val MAX_COUNT_PR_TITLE_HISTORY = 5
    private const val DELIMITER_PR_TITLE_HISTORY = "\u001E"
    private const val KEY_SLACK_REVIEW_CHANNEL_ID = "${PACKAGE_ID}.slack_review_channel_id"

    const val DEFAULT_SLACK_TEAM_ID = "T027EKHKMBK"
    const val DEFAULT_SLACK_REVIEW_CHANNEL_ID = "C05FTRXTE3H"

    private fun getCommonUniqueKey(project: Project, key: String): String {
        val suffix = project.basePath ?: project.name
        return key + "_" + suffix
    }

    private fun getLastTargetBranchKey(project: Project): String {
        val suffix = project.basePath ?: GitUtils.getRemoteOriginUrl(project) ?: project.name
        return KEY_LAST_TARGET_BRANCH + "_" + MD5Utils.md5(suffix)
    }

    private fun getProjectUrlKey(project: Project): String {
        val suffix = GitUtils.getRemoteOriginUrl(project) ?: project.name
        return PROJECT_PR_URL + "_" + MD5Utils.md5(suffix)
    }

    fun createProjectUrl(project: Project) {
        try {
            // 读取本地存储的 GitHub 地址
            val key = getProjectUrlKey(project)
            val savedProjectUrl = PropertiesComponent.getInstance().getValue(key)
            Log.d { "createProjectUrl, savedProjectUrl: $savedProjectUrl" }
            if (savedProjectUrl == null) {
                val repoUrl = GitUtils.getRemoteOriginUrl(project)
                val projectUrl = ProjectCodeHelper.repoUrlToProjectUrl(repoUrl)
                PropertiesComponent.getInstance().setValue(key, projectUrl)
                Log.d { "createProjectUrl, projectUrl: $projectUrl" }
            }
        } catch (e: Exception) {
            Log.e { "create base url err: $e" }
        }
    }

    fun getProjectUrl(project: Project): String? {
        // 读取本地存储的 GitHub 地址
        val key = getProjectUrlKey(project)
        val url = PropertiesComponent.getInstance().getValue(key)
        val projectUrl = url?.trimEnd('/')?.trim()
        Log.d { "project=${project.name}  projectUrl=$projectUrl" }
        return projectUrl
    }

    fun setProjectUrl(project: Project, url: String?) {
        val key = getProjectUrlKey(project)
        PropertiesComponent.getInstance().setValue(key, url ?: "")
    }

    fun getLastTargetBranch(project: Project): String? {
        return PropertiesComponent.getInstance().getValue(getLastTargetBranchKey(project))
    }

    fun setLastTargetBranch(project: Project, branch: String?) {
        PropertiesComponent.getInstance().setValue(getLastTargetBranchKey(project), branch)
    }

    /**
     * 返回自动创建feature分支时，基于哪个分支迁出
     */
    fun getFeatureBaseBranch(project: Project): String {
        val key = getCommonUniqueKey(project, KEY_FEATURE_BASE_BRANCH)
        return PropertiesComponent.getInstance().getValue(key, "origin/main")
    }

    fun setFeatureBaseBranch(project: Project, branch: String?) {
        if (branch.isNullOrEmpty()) {
            return
        }
        val key = getCommonUniqueKey(project, KEY_FEATURE_BASE_BRANCH)
        PropertiesComponent.getInstance().setValue(key, branch)
    }

    fun getPersonalBranchSuffix(): String? {
        return PropertiesComponent.getInstance().getValue(KEY_PERSONAL_BRANCH_SUFFIX)
    }

    fun setPersonalBranchSuffix(suffix: String?) {
        PropertiesComponent.getInstance().setValue(KEY_PERSONAL_BRANCH_SUFFIX, suffix ?: "")
    }

    fun getGHPath(): String {
        val path = PropertiesComponent.getInstance().getValue(KEY_GH_PATH)
        if (path.isNullOrEmpty()) {
            return GHUtils.DEFAULT_GH_PATH
        }
        return path
    }

    fun setGHPath(suffix: String?) {
        PropertiesComponent.getInstance().setValue(KEY_GH_PATH, suffix ?: "")
    }

    fun getPRTitleHistory(project: Project): List<String> {
        val key = getCommonUniqueKey(project, KEY_PR_TITLE_HISTORY)
        val raw = PropertiesComponent.getInstance().getValue(key) ?: return emptyList()
        return raw.split(DELIMITER_PR_TITLE_HISTORY).take(MAX_COUNT_PR_TITLE_HISTORY)
    }

    fun addPRTitleToHistory(project: Project, title: String?) {
        if (title.isNullOrBlank()) return
        val key = getCommonUniqueKey(project, KEY_PR_TITLE_HISTORY)
        val current = getPRTitleHistory(project).filter { it != title }
        val newList = listOf(title) + current
        val value = newList.take(MAX_COUNT_PR_TITLE_HISTORY).joinToString(DELIMITER_PR_TITLE_HISTORY)
        PropertiesComponent.getInstance().setValue(key, value)
    }

    fun getSlackTeamId(): String {
        return DEFAULT_SLACK_TEAM_ID
    }

    fun getSlackHttpUrl(): String {
        return "https://app.slack.com/client/" + getSlackTeamId()
    }

    fun getSlackReviewChannelId(project: Project): String {
        val key = getCommonUniqueKey(project, KEY_SLACK_REVIEW_CHANNEL_ID)
        return PropertiesComponent.getInstance().getValue(key)
            .ifNullOrEmpty {
                DEFAULT_SLACK_REVIEW_CHANNEL_ID
            }
    }

    fun setSlackReviewChannelId(project: Project, id: String?) {
        val key = getCommonUniqueKey(project, KEY_SLACK_REVIEW_CHANNEL_ID)
        PropertiesComponent.getInstance().setValue(key, id)
    }
}