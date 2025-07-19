package com.coinex.plugin

import com.coinex.plugin.utils.GitUtils
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.JOptionPane

class CreatePRAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        if (!GitUtils.isGitRepository(project)) {
            JOptionPane.showMessageDialog(
                null,
                "当前项目不是 Git 仓库，无法创建 PR！",
                "提示",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }
        CreatePRDialog(project).show()
    }
}