package com.coinex.plugin.utils

import java.awt.Color
import java.awt.Component
import javax.swing.ComboBoxModel
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

/**
 * 自定义ListCellRenderer，用于修改下拉框显示内容并高亮当前分支
 */
class BranchDisplayRenderer(
    private val model: ComboBoxModel<String?>
) : DefaultListCellRenderer() {

    override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)

        // 为当前分支设置高亮颜色
        foreground = if (value is String && value == model.selectedItem) {
            Color(if (isSelected) 0xFF66CC88.toInt() else 0xCD66CC88.toInt())
        } else {
            null
        }

        return component
    }
}
