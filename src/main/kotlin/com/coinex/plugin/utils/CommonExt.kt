package com.coinex.plugin.utils

fun <T> MutableList<T>.addIfNotExist(t: T) {
    if (!contains(t)) {
        add(t)
    }
}
