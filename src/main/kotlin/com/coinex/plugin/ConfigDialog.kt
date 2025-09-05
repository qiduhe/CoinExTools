package com.coinex.plugin

import com.coinex.plugin.utils.BrowserUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ConfigDialog(private val project: Project) : DialogWrapper(project) {
    private val projectUrlField = JBTextField().apply {
        preferredSize = Dimension(420, preferredSize.height)
        minimumSize = Dimension(420, minimumSize.height)
    }
    private val jumpButton = JButton("跳转").apply {
        preferredSize = Dimension(50, preferredSize.height)
        minimumSize = Dimension(50, minimumSize.height)
    }
    private val personalBranchSuffixField = JBTextField().apply {
        preferredSize = Dimension(420, preferredSize.height)
        minimumSize = Dimension(420, minimumSize.height)
    }
    private val tipLabel = JBLabel("修改后自动保存").apply {
        horizontalAlignment = SwingConstants.CENTER
        foreground = Color.GRAY
        font = font.deriveFont(11f)
    }

    var projectUrlChangedListener: ((String) -> Unit)? = null
    var dialogClosedListener: ((isConfigChanged: Boolean) -> Unit)? = null

    var isProjectUrlEdited = false
    var isBranchSuffixEdited = false

    init {
        init()
        title = "配置中心"
        isModal = true
        isResizable = false
    }

    override fun createCenterPanel(): JComponent {
        projectUrlField.emptyText.text = "请输入正确的项目url"
        projectUrlField.text = ConfigManager.getProjectUrl(project) ?: ""
        projectUrlField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = save()
            override fun removeUpdate(e: DocumentEvent?) = save()
            override fun changedUpdate(e: DocumentEvent?) = save()
            private fun save() {
                isProjectUrlEdited = true
                val newProjectUrl = projectUrlField.text.trim()
                ConfigManager.setProjectUrl(project, newProjectUrl)
                projectUrlChangedListener?.invoke(newProjectUrl)
            }
        })

        personalBranchSuffixField.emptyText.text = "请输入个人分支后缀，如：hqd"
        personalBranchSuffixField.text = ConfigManager.getPersonalBranchSuffix() ?: ""
        personalBranchSuffixField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = save()
            override fun removeUpdate(e: DocumentEvent?) = save()
            override fun changedUpdate(e: DocumentEvent?) = save()
            private fun save() {
                isBranchSuffixEdited = true
                val newSuffix = personalBranchSuffixField.text.trim()
                ConfigManager.setPersonalBranchSuffix(newSuffix)
            }
        })

        // 设置测试按钮点击事件
        jumpButton.addActionListener {
            val url = projectUrlField.text.trim()
            if (BrowserUtils.isValidUrl(url)) {
                BrowserUtils.openInBrowser(url)
            } else {
                setErrorText("请输入正确的url", projectUrlField)
            }
        }

        // 创建包含输入框和按钮的面板
        val inputPanel = JPanel(BorderLayout())
        inputPanel.add(projectUrlField, BorderLayout.CENTER)
        inputPanel.add(jumpButton, BorderLayout.EAST)

        val formPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("项目地址: "), inputPanel, 1, false)
            .addComponent(JPanel().apply { preferredSize = Dimension(0, 8) })
            .addLabeledComponent(JBLabel("个人分支后缀: "), personalBranchSuffixField, 1, false)
            .panel
        formPanel.preferredSize = Dimension(formPanel.preferredSize.width, formPanel.preferredSize.height)

        val mainPanel = JPanel(BorderLayout())
        mainPanel.add(formPanel, BorderLayout.CENTER)
        mainPanel.add(Box.createVerticalStrut(12), BorderLayout.SOUTH)

        // southPanel布局调整，左侧为分支提示，右侧为取消按钮（由DialogWrapper管理），中间为tipLabel
        val southPanel = JPanel(BorderLayout())
        southPanel.add(Box.createVerticalStrut(8), BorderLayout.NORTH)
        southPanel.add(tipLabel, BorderLayout.CENTER)
        mainPanel.add(southPanel, BorderLayout.SOUTH)
        mainPanel.preferredSize = Dimension(mainPanel.preferredSize.width, mainPanel.preferredSize.height + 50)
        return mainPanel
    }

    override fun createActions(): Array<Action> = emptyArray()

    override fun dispose() {
        super.dispose()
        val isConfigChanged = isProjectUrlEdited || isBranchSuffixEdited
        dialogClosedListener?.invoke(isConfigChanged)
    }
}

