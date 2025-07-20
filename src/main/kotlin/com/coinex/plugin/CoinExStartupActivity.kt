package com.coinex.plugin


import com.coinex.plugin.utils.GitUtils
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class CoinExStartupActivity : ProjectActivity {
    /**
     * 每打开一个项目会回调一次
     */
    override suspend fun execute(project: Project) {
        if (GitUtils.isGitRepository(project)) {
            // 初始化该项目默认的PR链接
            ConfigManager.createProjectUrl(project)
        }
    }
}