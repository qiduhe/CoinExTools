package com.coinex.plugin.utils

class GHCommandResult private constructor(val exitValue: Int, val output: String) {
    val isSuccess: Boolean
        get() = exitValue == 0

    val isAlreadyExists: Boolean
        get() {
            return output.contains("already exists")
        }
    override fun toString(): String {
        return output
    }

    companion object {
        fun fail(exitValue: Int = -1, output: String = ""): GHCommandResult {
            return GHCommandResult(exitValue, output)
        }

        fun success(exitValue: Int = -1, output: String = ""): GHCommandResult {
            return GHCommandResult(exitValue, output)
        }
    }
}