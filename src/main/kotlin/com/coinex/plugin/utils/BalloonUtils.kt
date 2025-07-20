package com.coinex.plugin.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import java.awt.Point
import javax.swing.JComponent

object BalloonUtils {
    fun showBalloonCenter(project: Project, text: String) {
        val projectWindow = WindowManager.getInstance().getFrame(project)
        if (projectWindow != null) {
            showBalloonCenter(project, projectWindow.rootPane, text)
        }
    }

    fun showBalloonCenter(project: Project, component: JComponent, text: String, duration: Long = 3000) {
        createBallon(text, duration).showInCenterOf(component)
    }

    fun showBalloonTop(project: Project, text: String, duration: Long = 3000) {
        val projectWindow = WindowManager.getInstance().getFrame(project)
        if (projectWindow == null) {
            return
        }
        val point = RelativePoint(projectWindow,
            Point (projectWindow.getWidth() / 2, 0));

        val balloon = createBallon(text, duration)
        balloon.show(point, Balloon.Position.atLeft)
    }

    private fun createBallon(text: String, duration: Long): Balloon {
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
        return balloon
    }
}