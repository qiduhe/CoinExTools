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
}