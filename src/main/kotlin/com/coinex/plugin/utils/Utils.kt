package com.coinex.plugin.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBLabel
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.StringReader
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.parser.ParserDelegator

object Utils {
    fun copyTextToClipboard(command: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(command)
        clipboard.setContents(selection, selection)
    }

    fun showTerminalWindow(project: Project): ToolWindow? {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal")
        toolWindow?.activate(null)
        return toolWindow
    }

    fun stripHtmlTags(string: String): String {
        val sb = StringBuilder()
        val parser = ParserDelegator()
        val callback = object : HTMLEditorKit.ParserCallback() {
            override fun handleText(text: CharArray, pos: Int) {
                sb.append(text)
            }
        }
        parser.parse(StringReader(string), callback, true)
        return sb.toString()
    }

    fun getTextRangeInLabel(label: JBLabel, mark: String): Rectangle? {
        val text = stripHtmlTags(label.text)
        val markStart = text.indexOf(mark)
        if (markStart != -1) {
            val prefix = text.substring(0, markStart)
            val fm = label.getFontMetrics(label.font)
            val lineHeight = fm.height

            val prefixWidth = fm.stringWidth(prefix)
            val baseBranchWidth = fm.stringWidth(mark)
            val x = prefixWidth
            val y = 0
            return Rectangle(x, y, baseBranchWidth, lineHeight)
        }
        return null
    }

    fun isInRectangle(rect: Rectangle?, x: Int, y: Int): Boolean {
        rect ?: return false
        val isInX = rect.x <= x && x <= rect.x + rect.width
        val isInY = rect.y <= y && y <= rect.y + rect.height
        return isInX && isInY
    }

    fun showVersionControl(project: Project) {
        val window = ToolWindowManager.getInstance(project).getToolWindow("Version Control")
        window?.show(null)
    }


    fun showCommit(project: Project) {
        val window = ToolWindowManager.getInstance(project).getToolWindow("Commit")
        window?.show(null)
    }

    fun showConfirmDialog(
        project: Project,
        title: String = "",
        message: String = "",
        yesText: String = "确定",
        noText: String = "取消"
    ): Boolean {
        return MessageDialogBuilder.yesNo(title, message)
            .yesText(yesText)
            .noText(noText)
            .ask(project)
    }
}