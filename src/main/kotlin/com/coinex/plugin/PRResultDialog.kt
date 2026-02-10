package com.coinex.plugin

import com.coinex.plugin.utils.BalloonUtils
import com.coinex.plugin.utils.BrowserUtils
import com.coinex.plugin.utils.Utils
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument

private const val MAIN_FONT_SIZE = 14.5F
private const val ATTR_URL = "clickable_url"

class PRResultDialog(
    private val project: Project,
    private val prUrl: String,
    private val desc: String
) : DialogWrapper(project) {

    private val prMessage get() = Triple(prUrl, "@频道", "$desc，麻烦PR")

    init {
        init()
        title = "Review"
        isModal = true
        isResizable = false
        setCancelButtonText("关闭")
        setOKButtonText("已复制")
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout()).apply {
            size = Dimension(300, 30)
        }
        mainPanel.add(createTextPane(), BorderLayout.CENTER)
        mainPanel.add(Box.createVerticalStrut(12), BorderLayout.SOUTH)
        return mainPanel
    }

    private fun createTextPane(): JTextPane {
        val textPane = JTextPane().apply {
            margin = Insets(8, 12, 8, 12)
            font = font.deriveFont(MAIN_FONT_SIZE)
            isEditable = false
            isOpaque = false
        }

        val defaultStyle = createTestStyle(UIUtil.getLabelForeground())
        val urlStyle = createTestStyle(
            color = JBColor(0x1E88E5, 0x6AB7FF),
            url = prMessage.first,
        )
        val channelStyle = createTestStyle(JBColor(0xFB8C00, 0xFFB74D))

        val (prUrl, channel, desc) = prMessage
        val doc = textPane.styledDocument
        doc.insertString(doc.length, prUrl, urlStyle)
        doc.insertString(doc.length, " ", defaultStyle)
        doc.insertString(doc.length, channel, channelStyle)
        doc.insertString(doc.length, "\n", defaultStyle)
        doc.insertString(doc.length, desc, defaultStyle)

        // 设置行间距
        doc.setParagraphAttributes(
            0,
            doc.length,
            SimpleAttributeSet().apply {
                StyleConstants.setLineSpacing(this, 0.2F)
            },
            false
        )

        setupUrlClick(textPane, doc)

        return textPane
    }


    private fun createTestStyle(color: Color, url: String? = null) = SimpleAttributeSet().apply {
        StyleConstants.setForeground(this, color)
        if (url != null) {
            addAttribute(ATTR_URL, url)
        }
    }

    private fun setupUrlClick(textPane: JTextPane, doc: StyledDocument) {
        val mouseAdapter = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val pos = textPane.viewToModel2D(e.point)
                if (pos < 0) return

                val element = doc.getCharacterElement(pos)
                val url = element.attributes.getAttribute(ATTR_URL)
                if (url is String) {
                    BrowserUtils.openInBrowser(url)
                }
            }

            override fun mouseMoved(e: MouseEvent) {
                val pos = textPane.viewToModel2D(e.point)
                if (pos < 0) return

                val element = doc.getCharacterElement(pos)
                val isLink = element.attributes.isDefined(ATTR_URL)
                textPane.cursor = Cursor.getPredefinedCursor(if (isLink) Cursor.HAND_CURSOR else Cursor.TEXT_CURSOR)
            }
        }
        // 点击处理
        textPane.addMouseListener(mouseAdapter)
        // 为了 mouseMoved 生效
        textPane.addMouseMotionListener(mouseAdapter)
    }

    override fun doOKAction() {
        copyPrMessage()
        BalloonUtils.showBalloonCenter(project, "已复制")
        super.doOKAction()
    }

    override fun show() {
        copyPrMessage()
        super.show()
    }

    private fun copyPrMessage() {
        val message = with(prMessage) { "$first $second $third" }
        Utils.copyTextToClipboard(message)
    }
}