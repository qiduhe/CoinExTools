package com.coinex.plugin.utils

class GitCommandResult private constructor(val exitValue: Int, val output: String) {
    val isSuccess: Boolean
        get() = exitValue == 0

    val isPushRejected: Boolean
        get() = output.contains("[rejected]")

    val hasConflict: Boolean
        get() = output.contains("CONFLICT")

    val errMsg: String
        get() = when {
            isPushRejected -> {
                "push is rejected"
            }

            hasConflict -> {
                "存在冲突"
            }

            else -> {
                output
            }
        }
    
    override fun toString(): String {
        return output
    }

    companion object {
        fun fail(exitValue: Int = -1, output: String = ""): GitCommandResult {
            return create(exitValue, output)
        }

        fun success(exitValue: Int = -1, output: String = ""): GitCommandResult {
            return create(exitValue, output)
        }
        fun create(exitValue: Int, output: String): GitCommandResult {
            return GitCommandResult(exitValue, output)
        }
    }
}