package com.coinex.plugin.utils

import com.intellij.openapi.project.Project
import java.io.File

object ScriptInstaller {

    fun isScriptInstalled(project: Project, scriptDir: String, scriptName: String): Boolean {
        return FileUtils.isFileExistsInProjectRoot(project, scriptDir, scriptName)
    }

    /**
     * 将脚本复制到项目根目录
     */
    fun installScript(installPath: String?, scriptName: String): Boolean {
        try {
            val scriptDir = File(installPath)
            val scriptFile = File(scriptDir, scriptName)

            if (!scriptDir.exists()) {
                scriptDir.mkdirs()
            }

            // 如果文件已存在，先备份
            if (scriptFile.exists()) {
                val backupFile = File(scriptDir, "$scriptName.backup")
                scriptFile.copyTo(backupFile, overwrite = true)
                Log.d { "已备份现有脚本到 ${backupFile.absolutePath}" }
            }

            // 从资源中读取脚本内容
            val scriptContent = getScriptContentFromResources(scriptName)
            if (scriptContent.isNullOrEmpty()) {
                Log.d { "无法从资源中读取脚本内容" }
                return false
            }

            // 写入到项目根目录
            scriptFile.writeText(scriptContent)

            // 设置执行权限
            scriptFile.setExecutable(true)

            Log.d { "脚本已成功安装到 ${scriptFile.absolutePath}" }
            return true

        } catch (e: Exception) {
            Log.d { "安装脚本失败: ${e.message}" }
            return false
        }
    }

    /**
     * 从资源中读取脚本内容
     */
    private fun getScriptContentFromResources(scriptName: String): String? {
        return try {
            val inputStream = javaClass.getResourceAsStream("/${scriptName}")
            inputStream?.use { it.readBytes() }?.toString(Charsets.UTF_8)
        } catch (e: Exception) {
            Log.d { "读取资源文件失败: ${e.message}" }
            null
        }
    }


    /**
     * 卸载脚本（删除文件）
     */
    fun uninstallScript(project: Project, scriptName: String): Boolean {
        try {
            val scriptFile = File(project.basePath, scriptName)
            if (scriptFile.exists()) {
                scriptFile.delete()
                Log.d { "脚本已卸载: ${scriptFile.absolutePath}" }
                return true
            }
            return false
        } catch (e: Exception) {
            Log.d { "卸载脚本失败: ${e.message}" }
            return false
        }
    }
} 