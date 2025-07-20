package com.coinex.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*


enum class ConflictChoice { NONE, REBASE, FORCE_PUSH }

private const val ROW_MIN_HEIGHT = 30
private const val ROW_SPACE = 3

private const val FONT_TITLE = 14F
private const val FONT_NORMAL = 13F

class ConflictResolveDialog(private val project: Project) : DialogWrapper(project) {
    private val rebaseButton = JButton("rebase分支")
    private val forcePushButton = JButton("强推分支")

    private var choiceListener: ((ConflictChoice) -> Unit)? = null
    private var choice = ConflictChoice.NONE

    init {
        init()
        title = "推送冲突处理"
        isModal = true
        isResizable = false
    }

    fun setConflictChoice(listener: ((ConflictChoice) -> Unit)?): ConflictResolveDialog {
        choiceListener = listener
        return this
    }


    override fun createCenterPanel(): JComponent {
        rebaseButton.addActionListener {
            choice = ConflictChoice.REBASE
            choiceListener?.invoke(choice)
            close(OK_EXIT_CODE)
        }
        forcePushButton.addActionListener {
            choice = ConflictChoice.FORCE_PUSH
            choiceListener?.invoke(choice)
            close(OK_EXIT_CODE)
        }

        val tipLabel = JBLabel("⚠\uFE0F 推送被 reject，远程分支和本地存在冲突，请根据你的情况选择对应的处理方式").apply {
            border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
            horizontalAlignment = SwingConstants.LEFT
            font = font.deriveFont(Font.PLAIN, FONT_TITLE)
            preferredSize = Dimension(preferredSize.width, ROW_MIN_HEIGHT)
        }

        val pullPanel = createRowPanel(JBLabel("1. 未拉取最新代码，去rebase远程分支最新代码"), rebaseButton)
        val forcePushPanel = createRowPanel(JBLabel("2. 进行了 rebase，使用 --force-with-lease 强推"), forcePushButton)

        // 取消按钮
        val cancelButton = JButton("取消").apply {
            font = font.deriveFont(Font.PLAIN, FONT_NORMAL)
            preferredSize = Dimension(preferredSize.width, ROW_MIN_HEIGHT)
            addActionListener { close(CANCEL_EXIT_CODE) }
        }
        val cancelPanel = JPanel()
        cancelPanel.layout = BoxLayout(cancelPanel, BoxLayout.X_AXIS)
        cancelPanel.add(Box.createHorizontalGlue())
        cancelPanel.add(cancelButton)
        cancelPanel.add(Box.createHorizontalGlue())

        val formPanel = FormBuilder.createFormBuilder()
            .addComponent(tipLabel)
            .addComponent(createRowSpacing())
            .addComponent(pullPanel)
            .addComponent(createRowSpacing())
            .addComponent(forcePushPanel)
            .addComponent(createRowSpacing(ROW_SPACE * 4))
            .addComponent(cancelPanel)
            .panel

        formPanel.border = BorderFactory.createEmptyBorder(10, 10, 3, 10)

        return formPanel
    }

    override fun createActions(): Array<Action> = emptyArray()

    private fun createRowSpacing(space: Int = ROW_SPACE): JPanel {
        return JPanel().apply { preferredSize = Dimension(preferredSize.width, space) }
    }

    private fun createRowPanel(label: JLabel, button: JButton): JPanel {
        label.font = label.font.deriveFont(Font.PLAIN, FONT_NORMAL)
        button.preferredSize = Dimension(button.preferredSize.width, ROW_MIN_HEIGHT)
        button.font = button.font.deriveFont(Font.PLAIN, FONT_NORMAL)
        label.preferredSize = Dimension(label.preferredSize.width, ROW_MIN_HEIGHT)

        val panel = JPanel(GridBagLayout())
        val gbcLabel = GridBagConstraints().apply {
            gridx = 0
            weightx = 1.0
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.NONE
            ipadx = 0
            ipady = 0
        }
        val gbcButton = GridBagConstraints().apply {
            gridx = 1
            weightx = 0.0
            anchor = GridBagConstraints.EAST
            fill = GridBagConstraints.NONE
            ipadx = 0
            ipady = 0
        }
        panel.add(label, gbcLabel)
        panel.add(button, gbcButton)
        return panel
    }
}