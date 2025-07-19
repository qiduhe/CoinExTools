package com.coinex.plugin.utils

import java.awt.Desktop
import java.net.URI


object BrowserUtils {

    fun isValidUrl(url: String): Boolean {
        return try {
            val uri = URI(url)
            uri.scheme != null && (uri.scheme == "http" || uri.scheme == "https")
        } catch (e: Exception) {
            false
        }
    }

    fun openInBrowser(url: String): Boolean {
        return try {
            val uri = URI(url)
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(uri)
                Log.d { "成功在浏览器中打开: $url" }
                true
            } else {
                Log.e { "当前系统不支持浏览器操作" }
                false
            }
        } catch (e: Exception) {
            Log.e { "无法打开浏览器: ${e.message}" }
            false
        }
    }
} 