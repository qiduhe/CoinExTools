package com.coinex.plugin

import com.coinex.plugin.utils.*
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*


private const val MAIN_FONT_SIZE = 14.5F
private const val ROW_WIDTH = 350
private const val ROW_MIN_HEIGHT = 37
private const val LINE_SPACING = 10

private fun JBLabel.createTitle(title: String): JBLabel {
    return apply {
        font = font.deriveFont(MAIN_FONT_SIZE)
        text = title
    }
}

class CreateFeatureBranchDialog(
    private val project: Project,
    branchList: MutableList<String>,
    private val sourceBranch: String
) : DialogWrapper(project) {
    private val remoteBranchCB = ComboBox<String>()
    private val featureField = JBTextField()
    private val remotesBranchList = branchList.filter {
        !it.startsWith("fix/") && !it.startsWith("feat/")
    }
    private var isCreateSuccess = false
    var onDialogClosedListener: ((isCreateSuccess: Boolean) -> Unit)? = null

    init {
        title = "创建Feature分支"
        init()
        isResizable = false
        setCancelButtonText("取消")
        setOKButtonText("创建并Push")
    }

    override fun createCenterPanel(): JComponent {
        remoteBranchCB.apply {
            font = font.deriveFont(MAIN_FONT_SIZE)
            model = CollectionComboBoxModel(remotesBranchList)
            setRenderer(BranchDisplayRenderer(model))
            val mainBranch = ProjectCodeHelper.getMainBranch()
            selectedItem = if (remotesBranchList.contains(mainBranch)) {
                mainBranch
            } else {
                ConfigManager.getFeatureBaseBranch(project)
            }
            preferredSize = Dimension(ROW_WIDTH, ROW_MIN_HEIGHT)
            minimumSize = preferredSize
        }
        featureField.apply {
            font = font.deriveFont(MAIN_FONT_SIZE)
            val defaultName = ProjectCodeHelper.getFeatureBranchByFeat(sourceBranch)
            text = defaultName
            preferredSize = Dimension(ROW_WIDTH, ROW_MIN_HEIGHT)
            minimumSize = preferredSize
        }

        val row1 = JPanel(BorderLayout(10, 0)).apply {
            layout = BorderLayout(5, 0)
            add(JBLabel().createTitle("迁出分支：     "), BorderLayout.WEST)
            add(remoteBranchCB, BorderLayout.CENTER)
            preferredSize = Dimension(ROW_WIDTH, ROW_MIN_HEIGHT)
        }
        val row2 = JPanel(BorderLayout(10, 0)).apply {
            layout = BorderLayout(5, 0)
            add(JBLabel().createTitle("Feature名称："), BorderLayout.WEST)
            add(featureField, BorderLayout.CENTER)
            preferredSize = Dimension(ROW_WIDTH, ROW_MIN_HEIGHT)
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(row1)
            add(Box.createVerticalStrut(LINE_SPACING))
            add(row2)
            add(Box.createVerticalStrut(LINE_SPACING))
            border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
            preferredSize = Dimension(ROW_WIDTH, ROW_MIN_HEIGHT * 2 + 20)
        }
    }

    fun getSelectedBaseBranch(): String {
        return remoteBranchCB.selectedItem?.toString()?.trim() ?: ""
    }

    fun getFeatureBranchName(): String {
        return featureField.text?.trim() ?: ""
    }

    override fun doOKAction() {
        val baseBranch = getSelectedBaseBranch()
        val featureBranch = getFeatureBranchName()

        if (GitUtils.isLocalBranchExists(project, featureBranch)) {
            val yes =
                Utils.showConfirmDialog(project, message = "本地存在${featureBranch}分支，是否强制删除并重新创建")
            if (!yes) {
                return
            }
            val result = GitUtils.deleteLocalBranch(project, featureBranch, true)
            if (!result.isSuccess) {
                Messages.showErrorDialog(project, "${featureBranch}分支删除失败: ${result.errMsg}", "")
                return
            }
        }

        runProgressTask("创建 Feature 分支") { indicator ->
            indicator.text = "正在从 $baseBranch 创建 $featureBranch ..."
            val result = GitUtils.createLocalBranchIfNotExists(project, featureBranch, "origin/$baseBranch")
            if (!result.isSuccess) {
                SwingUtilities.invokeLater {
                    val err = result.output.ifEmpty { "创建失败" }
                    BalloonUtils.showBalloonCenter(project, rootPane, "❌ $featureBranch ${err}", 3500)
                }
                return@runProgressTask
            }

            indicator.text = "正在 push $featureBranch ..."
            WarpUtils.runInWarp(project) {
                val pushResult = GitUtils.pushBranchWithCancel(project, featureBranch, indicator)
                SwingUtilities.invokeLater {
                    if (pushResult.isSuccess) {
                        isCreateSuccess = true
                        close(0)
                    }
                    val tip =
                        if (pushResult.isSuccess) "✅ $featureBranch push 成功" else "❌ $featureBranch push 失败: ${pushResult.errMsg}"
                    BalloonUtils.showBalloonCenter(project, rootPane, tip, 3000)
                }
            }

        }
    }

    override fun doValidate(): ValidationInfo? {
        val name = featureField.text?.trim()
        if (name.isNullOrEmpty()) {
            return ValidationInfo("", featureField)
        }
        return null
    }

    private fun runProgressTask(title: String, run: (ProgressIndicator) -> Unit) {
        ProgressManager.getInstance().run(object : Task.Modal(project, title, true) {
            override fun run(indicator: ProgressIndicator) {
                run.invoke(indicator)
            }
        })
    }


    override fun dispose() {
        super.dispose()
        onDialogClosedListener?.invoke(isCreateSuccess)
    }
}