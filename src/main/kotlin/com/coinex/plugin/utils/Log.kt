package com.coinex.plugin.utils

inline fun Log.d(msg: () -> String?) {
    if (IS_LOG) {
        println("CoinExPlugin: " + msg.invoke())
    }
}

inline fun Log.e(msg: () -> String?) {
    if (IS_LOG) {
        System.err.println("CoinExPlugin: " + msg.invoke())
    }
}


object Log {
    const val IS_LOG = false
}
