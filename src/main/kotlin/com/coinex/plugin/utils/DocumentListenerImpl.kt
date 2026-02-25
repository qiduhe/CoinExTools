package com.coinex.plugin.utils

import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

open class DocumentListenerImpl : DocumentListener {
    override fun insertUpdate(e: DocumentEvent?) = onTextChanged(e)
    override fun removeUpdate(e: DocumentEvent?) = onTextChanged(e)
    override fun changedUpdate(e: DocumentEvent?) = onTextChanged(e)
    open fun onTextChanged(e: DocumentEvent?) {}
}