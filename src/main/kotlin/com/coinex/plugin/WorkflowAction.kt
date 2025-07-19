package com.coinex.plugin

import com.coinex.plugin.utils.BalloonUtils
import com.coinex.plugin.utils.BrowserUtils
import com.coinex.plugin.utils.GitUtils
import com.coinex.plugin.utils.Utils
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.JOptionPane

class WorkflowAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val projectUrl = ConfigManager.getProjectUrl(project)
        if (projectUrl.isNullOrEmpty()) {
            JOptionPane.showMessageDialog(
                null,
                "当前还没有配置项目地址，请到先配置~",
                "提示",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }
        val curBranch = GitUtils.getCurrentBranch(project)
        if (!curBranch.isNullOrEmpty()) {
            Utils.copyTextToClipboard(curBranch)
            BalloonUtils.showBalloonCenter(project,"$curBranch 已复制到粘贴板")
        }
        val workflowUrl = "${projectUrl}/actions/workflows/dev_deploy_manual.yml"
        BrowserUtils.openInBrowser(workflowUrl)
    }
} 