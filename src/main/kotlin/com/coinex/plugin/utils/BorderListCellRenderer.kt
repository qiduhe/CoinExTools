package com.coinex.plugin.utils

import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.border.Border

class BorderListCellRenderer(private val border: Border) : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
        c.border = this.border
        return c
    }
}