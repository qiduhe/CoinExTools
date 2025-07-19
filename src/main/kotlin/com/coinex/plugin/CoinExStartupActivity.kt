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

            fun getAllActionIds(): List<String> {
                val actionManager = ActionManager.getInstance()
                val sorted = actionManager.getActionIdList("Vcs").toList().sorted()
                val sorted2 = actionManager.getActionIdList("Git").toList().sorted()
                return mutableListOf<String>().also {
                    it.addAll(sorted)
                    it.addAll(sorted2)
                }
            }

            // 使用示例
            fun printAllActions() {
                val actionIds = getAllActionIds()
                println("ytempest 所有 Action ID:")
                actionIds.forEach { actionId ->
                    println("  $actionId")
                }
                println("ytempest 总共 ${actionIds.size} 个 Action")
            }

            printAllActions()
        }
    }
}