package com.coinex.plugin.utils


inline fun String?.ifNullOrEmpty(block: () -> String): String {
    return if (this.isNullOrEmpty()) {
        block()
    } else {
        this
    }
}