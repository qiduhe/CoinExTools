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
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import javax.swing.plaf.basic.ComboPopup


// label点击类型
private const val CLICK_DISABLE = -1

private const val CLICK_INSTALL_CODE_ANALYSIS_SCRIPT = 0x01
private const val CLICK_CODE_ANALYSIS = 0x02
private const val CLICK_OPEN_VERSiON_CTRL = 0x03

private const val CLICK_PUSH_SRC_BRANCH = 0x11

private const val CLICK_PUSH_TAR_BRANCH = 0x21

private const val CLICK_REBASE_SRC_ON_TARGET = 0x31
private const val CLICK_REBASE_HANDLE_CONFLICT = 0x32
private const val CLICK_REBASE_COMMIT_RESOLVE = 0x33


private const val MAIN_FONT_SIZE = 14.5F
private const val TIP_FONT_SIZE = 13.5F
private const val ROW_WIDTH = 500
private const val ROW_MIN_HEIGHT = 35
private const val LINE_SPACING = 5
private const val MAX_ROW_COUNT = 20
private const val COLOR_TIP = 0x8E8E8E

class CreatePRDialog(private val project: Project) : DialogWrapper(project) {
    private val projectUrlField = JBTextField()
    private val selectSourceBranchCB = ComboBox<String>()
    private val selectTargetBranchCB = ComboBox<String>()

    private val labelClickTypeMap = mutableMapOf<JBLabel, Int>()
    private val srcBranchPushLabel = JBLabel().createTipLabel()
    private val srcBranchRebaseLabel = JBLabel().createTipLabel()
    private val srcBranchCodeAnalysisLabel = JBLabel().createTipLabel()
    private val srcBranchNameCheckLabel = JBLabel().createTipLabel()
    private val tarBranchPushLabel = JBLabel().createTipLabel()
    private val targetBranchNameCheckLabel = JBLabel().createTipLabel()

    private val branches = Branches(project)
    private val currentBranch = GitUtils.getCurrentBranch(project)
    private val lastTargetBranch = ConfigManager.getLastTargetBranch(project)

    private var featToFeatureMap = ObservableMap<String, String>(
        onAdd = { feat, feature ->
            branches.targetList.addIfNotExist(feature)
            branches.targetList.sort()
            refreshTargetBranchPushLabel()
        },
        onRemove = { feat, feature ->
            branches.targetList.remove(feature)
            branches.targetList.sort()
            refreshTargetBranchPushLabel()
        },
        onUpdate = { feat, featureOld, featureNew ->
            branches.targetList.remove(featureOld)
            branches.targetList.addIfNotExist(featureNew)
            branches.targetList.sort()
            refreshTargetBranchPushLabel()
            if (selectTargetBranch == featureOld) {
                selectTargetBranchCB.selectedItem = featureNew
            }
        }
    )

    private val projectUrl: String?
        get() = projectUrlField.text?.trim { it <= ' ' }

    private val selectSourceBranch: String
        get() = selectSourceBranchCB.selectedItem?.toString()?.trim { it <= ' ' } ?: ""

    private val selectTargetBranch: String
        get() = selectTargetBranchCB.selectedItem?.toString()?.trim { it <= ' ' } ?: ""


    init {
        init()
        title = "创建Pull Requests"
        setCancelButtonText("取消")
        setOKButtonText("创建")
        isResizable = false

        window.addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowActivated(e: java.awt.event.WindowEvent?) {
                // Dialog 被激活（获得焦点或切回窗口）
                refreshSourceBranchSelected(false)
                refreshTargetBranchSelected(false)
            }

            override fun windowDeactivated(e: java.awt.event.WindowEvent?) {
                // Dialog 被取消激活
            }
        })
    }


    override fun createCenterPanel(): JComponent {
        projectUrlField.apply {
            font = font.deriveFont(MAIN_FONT_SIZE)
            emptyText.text = "请输入项目地址"
            setText(ConfigManager.getProjectUrl(project))
            preferredSize = Dimension(ROW_WIDTH, ROW_MIN_HEIGHT)
            projectUrlField.isOpaque = true
            isEditable = false
            cursor = Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    val dialog = ConfigDialog(project)
                    dialog.projectUrlChangedListener = { projectUrl ->
                        Log.d { "项目地址输入变化: $projectUrl" }
                        projectUrlField.text = projectUrl
                    }
                    dialog.show()
                }
            })
        }

        selectSourceBranchCB.apply {
            initBranchComboBox()
            addCopySupport(project)
            model = CollectionComboBoxModel(branches.sourceList)
            setRenderer(BranchDisplayRenderer(model))
            addActionListener { refreshSourceBranchSelected(false) }
        }
        initSourceBranchSelect()


        selectTargetBranchCB.apply {
            initBranchComboBox()
            addCopySupport(project)
            model = CollectionComboBoxModel(branches.targetList)
            setRenderer(BranchDisplayRenderer(model))
            addActionListener { refreshTargetBranchSelected(false) }
        }
        initTargetBranchSelect()

        val tipColor = Color(COLOR_TIP)
        srcBranchRebaseLabel.foreground = tipColor
        srcBranchRebaseLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                e ?: return
                onSourceBranchRebaseClick()
            }
        })
        srcBranchPushLabel.foreground = tipColor
        srcBranchPushLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                onSourceBranchPushClick()
            }
        })
        srcBranchNameCheckLabel.foreground = tipColor
        srcBranchCodeAnalysisLabel.foreground = tipColor
        srcBranchCodeAnalysisLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                onCodeAnalysisClick()
            }
        })

        targetBranchNameCheckLabel.foreground = tipColor
        tarBranchPushLabel.foreground = tipColor
        tarBranchPushLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                e ?: return
                onTargetBranchPushClick(e)
            }
        })


        fun wrapperComponent(component: Component): JPanel = JPanel().apply {
            preferredSize = Dimension(component.preferredSize.width, component.preferredSize.height)
            layout = BorderLayout()
            add(component, BorderLayout.CENTER)
        }

        val formBuilder = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel().createTitle("项目地址："), wrapperComponent(projectUrlField), 1, false)
            .addComponent(JPanel().createRowSpacing())
            .addComponent(JPanel().createRowSpacing())
            .addLabeledComponent(JBLabel().createTitle("源分支："), wrapperComponent(selectSourceBranchCB), 1, false)
            .addComponentToRightColumn(srcBranchRebaseLabel, 1)
            .addComponentToRightColumn(srcBranchPushLabel, 1)
            .addComponentToRightColumn(srcBranchCodeAnalysisLabel, 1)
            .addComponentToRightColumn(srcBranchNameCheckLabel, 1)
            .addComponent(JPanel().createRowSpacing())
            .addLabeledComponent(JBLabel().createTitle("目标分支："), wrapperComponent(selectTargetBranchCB), 1, false)
            .addComponentToRightColumn(targetBranchNameCheckLabel, 1)
            .addComponentToRightColumn(tarBranchPushLabel, 1)
            .addComponentFillVertically(JPanel(), 0)
        val dialogPanel = formBuilder.panel

        return dialogPanel
    }

    private fun isBranchHasLocalBranch(branch: String?): Boolean {
        return !branch.isNullOrEmpty() && branches.localList.contains(branch)
    }

    private fun isBranchHasRemoteBranch(branch: String?): Boolean {
        return !branch.isNullOrEmpty() && branches.remoteList.contains(branch)
    }

    private fun initSourceBranchSelect() {
        if (!currentBranch.isNullOrEmpty()) {
            selectSourceBranchCB.selectedItem = currentBranch
        } else {
            selectSourceBranchCB.selectedItem = branches.sourceList.firstOrNull()
        }

        val sourceBranch = selectTargetBranch
        if (ProjectCodeHelper.isFeatBranch(sourceBranch)) {
            featToFeatureMap.put(sourceBranch, ProjectCodeHelper.getFeatureBranchByFeat(sourceBranch))
        }

        refreshSourceBranchSelected(true)
    }

    private fun refreshSourceBranchSelected(fromInit: Boolean) {
        val sourceBranch = selectSourceBranch
        refreshSourceBranchPushCheck()
        refreshSourceBranchCodeAnalysis()

        refreshSourceBranchNameCheck()
        refreshTargetBranchNameCheck()

        refreshSourceBranchRebaseCheck()

        // 如果该feat分支还没有feature分支，则添加
        if (ProjectCodeHelper.isFeatBranch(sourceBranch) && !featToFeatureMap.containsKey(sourceBranch)) {
            featToFeatureMap.put(sourceBranch, ProjectCodeHelper.getFeatureBranchByFeat(sourceBranch))
        }
    }

    private fun refreshSourceBranchRebaseCheck() {
        val source = selectSourceBranch
        val target = selectTargetBranch
        if (source.isEmpty() || target.isEmpty()) {
            srcBranchRebaseLabel.text = ""
            srcBranchRebaseLabel.setVisibleIfTextValid()
            return
        }

        if (GitUtils.isRebaseInProgress(project)) {
            if (GitUtils.hasUnresolvedConflicts(project)) {
                srcBranchRebaseLabel.clickType = CLICK_REBASE_HANDLE_CONFLICT
                srcBranchRebaseLabel.text = "⚠\uFE0F rebase冲突未处理完，点此处理冲突"
            } else {
                srcBranchRebaseLabel.clickType = CLICK_REBASE_COMMIT_RESOLVE
                srcBranchRebaseLabel.text = "⚠\uFE0F rebase冲突处理完，点击确定commit并push"
            }


        } else if (isBranchHasRemoteBranch(source) && GitUtils.needRebase(project, source, target)) {
            srcBranchRebaseLabel.clickType = CLICK_REBASE_SRC_ON_TARGET
            srcBranchRebaseLabel.text = "⚠\uFE0F 源分支落后于目标分支(${target})，建议先 rebase。是否现在自动 rebase？"

        } else {
            srcBranchRebaseLabel.text = ""
        }
        srcBranchRebaseLabel.setVisibleIfTextValid()
    }

    private fun refreshSourceBranchNameCheck() {
        val sourceBranch = selectSourceBranch
        // 源分支变更时，如果目标分支是feature，那么检测源分支命名是否正确
        var targetBranchNameTip = ""
        if (ProjectCodeHelper.isValidBranchName(selectTargetBranch)
            && !ProjectCodeHelper.isValidBranchName(sourceBranch)
        ) {
            targetBranchNameTip = "⚠\uFE0F ${sourceBranch} 命名可能有误，请自行确认"
        }
        srcBranchNameCheckLabel.text = targetBranchNameTip
        srcBranchNameCheckLabel.setVisibleIfTextValid()
    }

    private fun refreshSourceBranchCodeAnalysis() {
        val sourceBranch = selectSourceBranch

        srcBranchCodeAnalysisLabel.clickType = CLICK_DISABLE
        if (CodeAnalysisScriptHelper.isProjectHasCheckScript(project)) {
            if (CodeAnalysisScriptHelper.isBranchHadRunCodeAnalysis(project, sourceBranch)) {
                srcBranchCodeAnalysisLabel.text = "✅该分支已完成代码检测，再次检测"
                srcBranchCodeAnalysisLabel.clickType = CLICK_CODE_ANALYSIS
            } else if (currentBranch == selectSourceBranch) {
                srcBranchCodeAnalysisLabel.text =
                    "⚠\uFE0F 该分支未进行代码检测，点此去执行"
                srcBranchCodeAnalysisLabel.clickType = CLICK_CODE_ANALYSIS
            } else {
                srcBranchCodeAnalysisLabel.text = "⚠\uFE0F 该分支未进行代码检测，点击去切换分支"
                srcBranchCodeAnalysisLabel.clickType = CLICK_OPEN_VERSiON_CTRL
            }

        } else {
            srcBranchCodeAnalysisLabel.text = "⚠\uFE0F 未安装代码检测脚本，点此安装"
            srcBranchCodeAnalysisLabel.clickType = CLICK_INSTALL_CODE_ANALYSIS_SCRIPT
        }
        srcBranchCodeAnalysisLabel.setVisibleIfTextValid()
    }

    private fun refreshSourceBranchPushCheck() {
        val source = selectSourceBranch
        val target = selectTargetBranch
        if (GitUtils.isRebaseInProgress(project)
            || (isBranchHasRemoteBranch(source) && GitUtils.needRebase(project, source, target))
        ) {
            srcBranchPushLabel.text = ""
            srcBranchPushLabel.setVisibleIfTextValid()
            return
        }

        val hasRemote = isBranchHasRemoteBranch(source)
        val unPushedCount = GitUtils.getUnPushedCommitCount(project, hasRemote, source)
        val statusTip = StringBuilder()
        if (!hasRemote) {
            statusTip.append("❌ 该分支还没push，点此去push")
        } else if (unPushedCount > 0) {
            statusTip.append("❌ 该分支有未push的commit，点此去push")
        }
        srcBranchPushLabel.clickType = CLICK_PUSH_SRC_BRANCH
        srcBranchPushLabel.text = statusTip.toString()
        srcBranchPushLabel.setVisibleIfTextValid()
    }

    private fun initTargetBranchSelect() {
        if (ProjectCodeHelper.isFeatBranch(selectSourceBranch)) {
            selectTargetBranchCB.selectedItem = ProjectCodeHelper.getFeatureBranchByFeat(currentBranch)
        } else if (!lastTargetBranch.isNullOrEmpty() && branches.targetList.contains(lastTargetBranch)) {
            selectTargetBranchCB.selectedItem = lastTargetBranch
        } else {
            selectTargetBranchCB.selectedItem = branches.targetList.firstOrNull()
        }

        refreshTargetBranchSelected(true)
    }

    private fun refreshTargetBranchSelected(fromInit: Boolean) {
        val targetBranch = selectTargetBranch
        if (targetBranch.isNotEmpty()) {
            Log.d { "目标分支选中变化: $targetBranch" }
            ConfigManager.setLastTargetBranch(project, targetBranch)
        }

        refreshSourceBranchNameCheck()
        refreshSourceBranchPushCheck()
        refreshTargetBranchNameCheck()

        refreshTargetBranchPushLabel()
    }

    private fun refreshTargetBranchNameCheck() {
        val targetBranch = selectTargetBranch

        // 目标分支变更时，如果源分支是feat，那么检测目标分支命名是否正确
        var targetBranchNameTip = ""
        if (ProjectCodeHelper.isValidBranchName(selectSourceBranch)
            && !ProjectCodeHelper.isValidBranchName(targetBranch)
        ) {
            targetBranchNameTip = "⚠\uFE0F ${targetBranch} 命名可能有误，请自行确认"
        }
        targetBranchNameCheckLabel.text = targetBranchNameTip
        targetBranchNameCheckLabel.setVisibleIfTextValid()

        refreshSourceBranchRebaseCheck()
    }

    private fun refreshTargetBranchPushLabel() {
        val targetBranch = selectTargetBranch
        tarBranchPushLabel.clickType = CLICK_DISABLE
        tarBranchPushLabel.text = ""
        if (ProjectCodeHelper.isFeatureBranch(targetBranch)) {
            val featureBranch = targetBranch
            if (!isBranchHasRemoteBranch(featureBranch)) {

                if (!isBranchHasLocalBranch(featureBranch)) {
                    tarBranchPushLabel.clickType = CLICK_PUSH_TAR_BRANCH
                    // 没有远程分支，也没有本地分支
                    val highlightColor = "#66CC88"
                    val labelColor = ColorUtil.toHtmlColor(tarBranchPushLabel.foreground)

                    fun addHighlightColor(text: String): String = "<span style='color:$highlightColor;'>$text</span>"
                    fun addNormalColor(text: String): String = "<span style='color:$labelColor;'>$text</span>"

                    val baseBranch = ConfigManager.getFeatureBaseBranch(project)
                    tarBranchPushLabel.text = StringBuilder()
                        .append("<html>")
                        .append(addNormalColor("❌"))
                        .append(addHighlightColor(" $baseBranch "))
                        .append(addNormalColor("→"))
                        .append(addHighlightColor(" $featureBranch "))
                        .append(addNormalColor("，点此创建并push"))
                        .append("</html>")
                        .toString()
                } else {
                    // 没有远程分支，但有本地分支
                    tarBranchPushLabel.clickType = CLICK_PUSH_TAR_BRANCH
                    tarBranchPushLabel.text = "❌ 该分支还没push，点此去push"
                }
            }
        }
        tarBranchPushLabel.setVisibleIfTextValid()
    }


    private fun onCodeAnalysisClick() {
        if (!srcBranchCodeAnalysisLabel.isClickable()) {
            return
        }
        when (srcBranchCodeAnalysisLabel.clickType) {
            CLICK_INSTALL_CODE_ANALYSIS_SCRIPT -> {
                if (!CodeAnalysisScriptHelper.isProjectHasCheckScript(project)) {
                    installScriptToProject {
                        SwingUtilities.invokeLater {
                            refreshSourceBranchSelected(false)
                            startCodeAnalysis()
                        }
                    }
                }
            }

            CLICK_CODE_ANALYSIS -> {
                startCodeAnalysis()
            }

            CLICK_OPEN_VERSiON_CTRL -> {
                close(0)
                Utils.showVersionControl(project)
            }
        }


    }

    private fun startCodeAnalysis() {
        val command = CodeAnalysisScriptHelper.getCodeAnalysisScriptPath();
        Utils.copyTextToClipboard(command)

        val toolWindow = Utils.showTerminalWindow(project) ?: return
        BalloonUtils.showBalloonCenter(
            project,
            toolWindow.component,
            "命令已复制到剪贴板，粘贴到 Terminal 回车即可运行",
            3500
        )
        close(0)
    }

    private fun onSourceBranchRebaseClick() {
        val source = selectSourceBranch
        val target = selectTargetBranch
        if (source.isEmpty() || target.isEmpty()) {
            return
        }
        val clickType = srcBranchRebaseLabel.clickType
        if (clickType == CLICK_REBASE_SRC_ON_TARGET) {
            rebaseSourceOntoTargetBranch(source, target)
            return
        }
        if (clickType == CLICK_REBASE_HANDLE_CONFLICT) {
            jumpConflictResolve()
            return
        }

        if (clickType == CLICK_REBASE_COMMIT_RESOLVE) {
            val yes =
                Utils.showConfirmDialog(project, message = "确定commit冲突处理，并使用 --force-with-lease 推送分支")
            if (!yes) {
                return
            }
            runProgressTask("push 分支...") { indicator ->
                indicator.text = "正在 commit rebase冲突..."
                val result = GitUtils.rebaseContinue(project)
                if (!result.isSuccess) {
                    if (result.hasConflict) {
                        SwingUtilities.invokeLater {
                            BalloonUtils.showBalloonTop(project, "rebase出现冲突，请处理冲突", 5000)
                        }
                        jumpConflictResolve(true)
                    } else {
                        SwingUtilities.invokeLater {
                            Messages.showErrorDialog(project, "rebase commit 失败: ${result.errMsg}", "")
                        }
                    }
                    return@runProgressTask
                }

                indicator.text = "正在 push rebase代码..."
                val pushResult = GitUtils.pushBranchForceWithLease(project, source)
                if (!pushResult.isSuccess) {
                    SwingUtilities.invokeLater {
                        Messages.showErrorDialog(project, "push rebase 失败: ${pushResult.errMsg}", "")
                    }
                    return@runProgressTask
                }
                SwingUtilities.invokeLater {
                    BalloonUtils.showBalloonCenter(project, rootPane, "push成功")
                    branches.refreshLocalBranchList()
                    refreshSourceBranchSelected(false)
                }
            }
            return
        }

    }

    private fun rebaseSourceOntoTargetBranch(source: String, target: String) {
        if (source != currentBranch) {
            Messages.showWarningDialog(project, "请先切换到源分支（$source）后再执行 rebase。", "rebase 检查")
            return
        }
        val needRebase = GitUtils.needRebase(project, source, target)
        if (!needRebase) {
            Messages.showInfoMessage(project, "源分支已经包含目标分支，无需 rebase。", "rebase 检查")
            return
        }

        val yes = Utils.showConfirmDialog(project, "", "$source 落后于 $target，现在进行 rebase？")
        if (!yes) {
            return
        }
        val originTarget = "origin/$target"
        doRebaseSourceOntoTargetBranch(project, source, originTarget)
    }

    private fun doRebaseSourceOntoTargetBranch(project: Project, source: String, originTarget: String) {
        runProgressTask("rebase 分支") { indicator ->
            indicator.text = "正在 rebase $originTarget ..."
            try {
                val result = GitUtils.rebaseCurrentOnToTargetBranch(project, originTarget)
                val output = result.output
                if (result.isSuccess) {
                    // rebase无冲突
                    SwingUtilities.invokeLater {
                        if (result.isUpToDate) {
                            val yes =
                                Utils.showConfirmDialog(project, message = "当前分支已经 up to date，是否push分支？")
                            if (yes) {
                                pushSourceBranch(source)
                            }
                        } else {
                            BalloonUtils.showBalloonCenter(project, rootPane, "${source} 分支 rebase 成功！")
                            branches.refreshLocalBranchList()
                            refreshSourceBranchSelected(false)
                        }
                    }
                } else if (result.hasConflict) {
                    // rebase出现冲突
                    SwingUtilities.invokeLater {
                        BalloonUtils.showBalloonTop(project, "rebase出现冲突，请处理冲突", 5000)
                        jumpConflictResolve(true)
                    }
                } else {
                    SwingUtilities.invokeLater {
                        Messages.showErrorDialog(project, "rebase 失败: $output", "rebase 错误")
                    }
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    Messages.showErrorDialog(project, "rebase 失败: ${e.message}", "rebase 错误")
                }
            }
        }
    }

    private fun jumpConflictResolve(refresh: Boolean = false) {

        fun doShowGitResolveConflictsDialog() {
            Utils.showCommit(project)
            Utils.showGitResolveConflictsDialog(project)
        }

        if (refresh) {
            runProgressTask("刷新git文件变更...", {
                VcsDirtyScopeManager.getInstance(project).markEverythingDirty()

                SwingUtilities.invokeAndWait {
                    Messages.showWarningDialog(project, "rebase存在冲突，请手动处理代码冲突", "")
                }

                ChangeListManager.getInstance(project).invokeAfterUpdate(false, {
                    SwingUtilities.invokeLater {
                        doShowGitResolveConflictsDialog()
                    }
                })
            })
        } else {
            SwingUtilities.invokeLater {
                doShowGitResolveConflictsDialog()
            }
        }
    }


    private fun onSourceBranchPushClick() {
        if (!srcBranchPushLabel.isClickable()) {
            return
        }
        if (srcBranchPushLabel.clickType == CLICK_PUSH_SRC_BRANCH) {
            val sourceBranch = selectSourceBranch
            val yes = Utils.showConfirmDialog(project, message = "确定push分支: $sourceBranch")
            if (!yes) {
                return
            }
            pushSourceBranch(sourceBranch)
        }
    }

    private fun pushSourceBranch(sourceBranch: String) {
        runProgressTask("Push分支") { indicator ->
            if (indicator.isCanceled) return@runProgressTask

            indicator.text = "正在push $sourceBranch ..."
            // push网络需要开启warp
            WarpUtils.runInWarp {
                var pushResult = GitUtils.pushBranchWithCancel(project, sourceBranch, indicator)
                if (!pushResult.isSuccess && pushResult.isPushRejected) {
                    var userChoice = ConflictChoice.NONE
                    SwingUtilities.invokeAndWait {
                        ConflictResolveDialog(project, sourceBranch)
                            .setConflictChoice {
                                userChoice = it
                            }
                            .show()
                    }

                    when (userChoice) {
                        ConflictChoice.REBASE -> {
                            indicator.text = "正在rebase远程分支..."
                            val rebaseResult = GitUtils.rebaseCurrentOnToTargetBranch(project, "origin/$sourceBranch")
                            val output = rebaseResult.output
                            SwingUtilities.invokeLater {
                                if (rebaseResult.isSuccess) {
                                    // rebase无冲突
                                    BalloonUtils.showBalloonCenter(
                                        project, rootPane, "${sourceBranch} 分支 rebase 成功！"
                                    )
                                    branches.refreshLocalBranchList()
                                    refreshSourceBranchSelected(false)
                                } else if (rebaseResult.hasConflict) {
                                    // rebase出现冲突
                                    BalloonUtils.showBalloonTop(project, "rebase出现冲突，请处理冲突", 5000)
                                    jumpConflictResolve(true)
                                } else {
                                    Messages.showErrorDialog(project, "rebase 失败: $output", "rebase 错误")
                                }
                            }
                        }

                        ConflictChoice.FORCE_PUSH -> {
                            indicator.text = "正在force push $sourceBranch ..."
                            pushResult = GitUtils.pushBranchForceWithLease(project, sourceBranch)
                        }

                        else -> {}
                    }
                }
                if (pushResult.isSuccess) {
                    SwingUtilities.invokeLater {
                        branches.refreshRemoteBranchList()
                        refreshSourceBranchSelected(false)
                    }
                }
                Log.d { "${sourceBranch} Push结果：$pushResult" }

                if (indicator.isCanceled) return@runInWarp

                SwingUtilities.invokeLater {
                    if (indicator.isCanceled) return@invokeLater
                    val pushTip =
                        if (pushResult.isSuccess) "${sourceBranch}分支push成功" else "${sourceBranch}分支push失败: ${pushResult.errMsg}"
                    BalloonUtils.showBalloonCenter(project, rootPane, pushTip, 3500)
                }
            }
        }
    }

    private fun onTargetBranchPushClick(e: MouseEvent) {
        if (!tarBranchPushLabel.isClickable()) {
            return
        }

        // 点击修改迁出分支
        val base = ConfigManager.getFeatureBaseBranch(project)
        val baseRect = Utils.getTextRangeInLabel(tarBranchPushLabel, " $base ")
        if (Utils.isInRectangle(baseRect, e.x, e.y)) {
            val newBase = Messages.showInputDialog(
                project,
                "",
                "修改迁出分支",
                null,
                base,
                null
            )?.trim()
            if (!newBase.isNullOrEmpty()) {
                ConfigManager.setFeatureBaseBranch(project, newBase)
                refreshTargetBranchPushLabel()
            }
            return
        }

        // 点击修改feature分支
        val sourceBranch = selectSourceBranch
        val feature = featToFeatureMap.get(sourceBranch)
        val featureRect = Utils.getTextRangeInLabel(tarBranchPushLabel, " $feature ")
        if (Utils.isInRectangle(featureRect, e.x, e.y)) {
            val newFeature = Messages.showInputDialog(
                project,
                "",
                "修改feature分支",
                null,
                feature,
                null
            )?.trim()
            if (!newFeature.isNullOrEmpty()) {
                featToFeatureMap.put(sourceBranch, newFeature)
            }
            return
        }

        if (tarBranchPushLabel.clickType == CLICK_PUSH_TAR_BRANCH) {
            // 点击推送分支
            val branchName = selectTargetBranch
            val isBranchExistLocal = GitUtils.isLocalBranchExists(this.project, branchName)
            val title = if (isBranchExistLocal) "确定push分支" else "确定创建并push分支"
            val yes = Utils.showConfirmDialog(project, title, branchName)
            if (!yes) {
                return
            }

            runProgressTask("Push分支") { indicator ->
                indicator.text = "正在推送分支 $branchName ..."
                if (indicator.isCanceled) return@runProgressTask

                if (!isBranchExistLocal) {
                    indicator.text = "正从 ${ConfigManager.getFeatureBaseBranch(project)} 分支创建 $branchName ..."
                    val result = GitUtils.createLocalBranchIfNotExists(
                        this.project, branchName, ConfigManager.getFeatureBaseBranch(project)
                    )
                    if (result.isSuccess) {
                        SwingUtilities.invokeLater {
                            branches.refreshLocalBranchList()
                            refreshTargetBranchSelected(false)
                        }
                    }
                    if (indicator.isCanceled) return@runProgressTask
                    if (!result.isSuccess) {
                        SwingUtilities.invokeLater {
                            val errMsg = result.output
                            val msg =
                                if (errMsg.isEmpty()) "${branchName} 分支创建失败"
                                else "${branchName} 分支创建失败: ${errMsg}"
                            BalloonUtils.showBalloonCenter(project, rootPane, msg, 2000)
                        }
                        return@runProgressTask
                    }

                    if (indicator.isCanceled) return@runProgressTask
                }

                indicator.text = "正在push $branchName ..."
                // push网络需要开启warp
                WarpUtils.runInWarp {
                    val pushResult = GitUtils.pushBranchWithCancel(this.project, branchName, indicator)
                    Log.d { "${branchName} Push结果：$pushResult" }
                    if (indicator.isCanceled) return@runInWarp

                    SwingUtilities.invokeLater {
                        if (pushResult.isSuccess) {
                            branches.refreshRemoteBranchList()
                            refreshTargetBranchSelected(false)
                        }
                        if (indicator.isCanceled) return@invokeLater
                        val pushTip =
                            if (pushResult.isSuccess) "${branchName}分支push成功" else "${branchName}分支push失败: ${pushResult.errMsg}"
                        BalloonUtils.showBalloonCenter(project, rootPane, pushTip, 3500)
                    }
                }
            }
        }
    }

    private fun runProgressTask(title: String, run: (ProgressIndicator) -> Unit) {
        ProgressManager.getInstance().run(object : Task.Modal(project, title, true) {
            override fun run(indicator: ProgressIndicator) {
                run.invoke(indicator)
            }
        })
    }

    private fun installScriptToProject(callback: () -> Unit) {
        ProgressManager.getInstance().run(object : Task.Modal(project, "安装脚本", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "正在安装代码检测脚本..."
                val success = CodeAnalysisScriptHelper.installScript(project)
                if (success) {
                    val scriptPath = CodeAnalysisScriptHelper.getCodeAnalysisScriptPath()
                    addScriptToGitignoreIfNeeded(project, scriptPath)
                    BalloonUtils.showBalloonCenter(project, rootPane, "✅ 脚本已安装到 $scriptPath")
                } else {
                    BalloonUtils.showBalloonCenter(project, rootPane, "❌ 脚本安装失败，请检查项目权限")
                }
                callback.invoke()
            }
        })
    }

    private fun addScriptToGitignoreIfNeeded(project: Project, scriptPath: String) {
        val projectRoot = File(project.basePath ?: return)
        val gitignoreFile = File(projectRoot, ".gitignore")
        if (!gitignoreFile.exists()) {
            return
        }
        try {
            val lines = gitignoreFile.readLines()
            if (lines.none { it.trim() == scriptPath }) {
                gitignoreFile.appendText("\n$scriptPath\n")
            }
        } catch (e: Exception) {
            Log.d { "追加 codeAnalysis.sh 到 .gitignore 失败: ${e.message}" }
        }
    }

    override fun doValidate(): ValidationInfo? {
        if (projectUrl.isNullOrEmpty()) {
            return ValidationInfo("请输入 GitHub 仓库地址", projectUrlField)
        }
        if (selectSourceBranch.isEmpty()) {
            return ValidationInfo("请输入源分支名", selectSourceBranchCB)
        }
        if (selectTargetBranch.isEmpty()) {
            return ValidationInfo("请输入目标分支名", selectTargetBranchCB)
        }
        return null
    }

    override fun doOKAction() {
        // push成功后再执行创建PR逻辑
        val prUrl = "${projectUrl}/compare/${selectTargetBranch}...${selectSourceBranch}"
        if (!BrowserUtils.isValidUrl(prUrl)) {
            Messages.showErrorDialog(project, "链接错误，请检测 [项目地址] 是否正确，并可以跳转", "")
            return
        }
        BrowserUtils.openInBrowser(prUrl)
        super.doOKAction()
    }

    private class Branches(private val project: Project) {
        val remoteList = GitUtils.getRemoteBranchList(project)
        val localList = GitUtils.getLocalBranchList(project)

        val sourceList by lazy { createSourceList() }
        val targetList by lazy { createTargetList() }

        private fun createSourceList(): MutableList<String> {
            val set = mutableSetOf<String>().apply {
                addAll(localList)
                addAll(remoteList)
            }
            val list = set.toMutableList()
            list.sort()
            return list
        }

        private fun createTargetList(): MutableList<String> {
            val list = remoteList.toMutableList()
            list.sort()
            return list
        }

        fun refreshRemoteBranchList() {
            remoteList.clear()
            remoteList.addAll(GitUtils.getRemoteBranchList(project))

            sourceList.clear()
            sourceList.addAll(createSourceList())

            targetList.clear()
            targetList.addAll(createTargetList())
        }

        fun refreshLocalBranchList() {
            localList.clear()
            localList.addAll(GitUtils.getLocalBranchList(project))

            sourceList.clear()
            sourceList.addAll(createSourceList())
        }
    }

    var JBLabel.clickType: Int
        get() {
            return labelClickTypeMap[this] ?: CLICK_DISABLE
        }
        set(clickable) {
            labelClickTypeMap.put(this, clickable)
            cursor =
                Cursor.getPredefinedCursor(if (isClickable()) Cursor.HAND_CURSOR else Cursor.TEXT_CURSOR)
        }

    fun JBLabel.isClickable(): Boolean {
        return clickType != CLICK_DISABLE
    }

    fun JBLabel.setVisibleIfTextValid() {
        this.isVisible = !this.text.isNullOrEmpty()
    }
}

private fun JBLabel.createTitle(title: String): JBLabel {
    return apply {
        font = font.deriveFont(MAIN_FONT_SIZE)
        text = title
    }
}

private fun JPanel.createRowSpacing(): JPanel {
    return apply { preferredSize = Dimension(ROW_WIDTH, LINE_SPACING) }
}

private fun JBLabel.createTipLabel(): JBLabel {
    return apply {
        horizontalAlignment = JBLabel.LEFT
        font = font.deriveFont(TIP_FONT_SIZE)
        border = BorderFactory.createEmptyBorder(4, 7, 4, 0)
    }
}

private fun JComboBox<String>.initBranchComboBox() {
    font = font.deriveFont(MAIN_FONT_SIZE)
    isEditable = false
    selectedItem = ""
    preferredSize = Dimension(ROW_WIDTH, ROW_MIN_HEIGHT)
    minimumSize = Dimension(ROW_WIDTH, ROW_MIN_HEIGHT)
    maximumRowCount = MAX_ROW_COUNT
    isPopupVisible = false
}

/**
 * 为下拉框添加复制功能的扩展函数
 */
private fun JComboBox<String>.addCopySupport(project: Project) {
    val comboBox = this
    addKeyListener(object : KeyAdapter() {
        override fun keyReleased(e: KeyEvent?) {
            e ?: return
            val isC = e.keyCode == KeyEvent.VK_C
            val isCtrl = e.isControlDown && !e.isMetaDown
            val isCmd = e.isMetaDown && !e.isControlDown
            if (isC && (isCtrl || isCmd)) {
                if (comboBox.isPopupVisible) {
                    return
                }
                val popup = comboBox.getUI().getAccessibleChild(comboBox, 0)
                if (popup !is ComboPopup) {
                    return
                }

                val index = popup.list.anchorSelectionIndex
                val highlightItem = comboBox.model.getElementAt(index)
                if (!highlightItem.isNullOrEmpty()) {
                    Utils.copyTextToClipboard(highlightItem)
                    BalloonUtils.showBalloonCenter(project, comboBox, "已复制", 300)
                }
            }
        }
    })
}