package com.coinex.plugin.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.JBColor
import javax.swing.JComponent

object BalloonUtils {
    fun showBalloonCenter(project: Project, text: String) {
        val projectWindow = WindowManager.getInstance().getFrame(project)
        if (projectWindow != null) {
            showBalloonCenter(project, projectWindow.rootPane, text)
        }
    }

    fun showBalloonCenter(project: Project, component: JComponent, text: String, duration: Long = 3000) {
        val balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(
                text,
                null,
                JBColor(0xF7F7F7, 0x2B2B2B),
                null
            )
            .setFadeoutTime(duration)
            .setShowCallout(false)
            .createBalloon()

        balloon.showInCenterOf(component)
    }

    private fun showScriptInstallBalloon(text: String) {
        val balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(
                text,
                null,
                JBColor(0xF7F7F7, 0x2B2B2B),
                null
            )
            .setFadeoutTime(3000)
            .setShowCallout(false)
            .createBalloon()

        // 显示在项目窗口中心
//        val projectWindow = project.getService(WindowManager::class.java)?.getFrame(project)
//        if (projectWindow != null) {
//            balloon.showInCenterOf(projectWindow.rootPane)
//        }
    }
}