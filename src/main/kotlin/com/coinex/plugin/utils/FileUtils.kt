package com.coinex.plugin.utils

import com.intellij.openapi.project.Project
import java.io.File

object FileUtils {

    @JvmStatic
    fun isFileExistsInProjectRoot(project: Project, dir: String, fileName: String): Boolean {
        val basePath = project.basePath ?: return false

        val file = if (dir.isNotEmpty()) {
            File(basePath + File.separator + dir, fileName)
        } else {
            File(basePath, fileName)
        }
        return file.exists()
    }

    @JvmStatic
    fun isExist(path: String?): Boolean {
        return !path.isNullOrEmpty() || File(path).exists()
    }
}