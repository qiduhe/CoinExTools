package com.coinex.plugin.utils

import com.coinex.plugin.ConfigManager
import com.intellij.openapi.project.Project

object MacAppJumpUtils {

    fun jumpSlack(project: Project): Boolean {
        try {
            val teamId = ConfigManager.getSlackTeamId()
            val channelId = ConfigManager.getSlackReviewChannelId(project)
            val process = ProcessBuilder("open", "slack://channel?team=$teamId&id=$channelId")
                .redirectErrorStream(true)
                .start()
            process.waitFor()
            return process.exitValue() == 0
        } catch (e: Exception) {
            return false
        }
    }
}