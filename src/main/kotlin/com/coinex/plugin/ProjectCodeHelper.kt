package com.coinex.plugin

import com.coinex.plugin.utils.GitUtils

object ProjectCodeHelper {
    private const val COINEX_EXCHANGE_REPO_IDENTITY = "coinex_exchange_android.git"
    private const val COINEX_EXCHANGE_PROJECT_BASE_URL = "https://github.com/coinexcom/coinex_exchange_android"

    private const val COINEX_WALLET_REPO_IDENTITY = "coinex_wallet_android.git"
    private const val COINEX_WALLET_PROJECT_URL = "https://github.com/coinexcom/coinex_wallet_android"

    fun repoUrlToProjectUrl(rawRepoUrl: String?): String {
        val projectUrl = GitUtils.removeGitHubToken(rawRepoUrl)
        return when  {
            projectUrl.contains(COINEX_EXCHANGE_REPO_IDENTITY) -> {
                COINEX_EXCHANGE_PROJECT_BASE_URL
            }

            projectUrl.contains(COINEX_WALLET_REPO_IDENTITY)  -> {
                COINEX_WALLET_PROJECT_URL
            }

            else -> {
                projectUrl.replace(".git", "")
            }
        }
    }

    fun getFeatureBranchByFeat(branch: String?): String {
        var feature = branch ?: ""
        if (feature.startsWith("feat/")) {
            feature = feature.replace("feat/", "feature-")
            val suffixArray = arrayOf("hqd", ConfigManager.getPersonalBranchSuffix())
            suffixArray.forEach { suffix ->
                if (!suffix.isNullOrEmpty()) {
                    if (feature.endsWith("_$suffix")) {
                        feature = feature.replace("_$suffix", "")
                    }
                    if (feature.endsWith("-$suffix")) {
                        feature = feature.replace("-$suffix", "")
                    }
                }
            }

        }
        feature = feature
            .replace("/", "-")
            .replace("_", "-")
        return feature
    }

    fun getDevBranchByFix(branch: String?): String {
        var feature = branch ?: ""
        if (feature.startsWith("fix/")) {
            feature = feature.replace("fix/", "dev-")
            val suffixArray = arrayOf("hqd", ConfigManager.getPersonalBranchSuffix())
            suffixArray.forEach { suffix ->
                if (!suffix.isNullOrEmpty()) {
                    if (feature.endsWith("_$suffix")) {
                        feature = feature.replace("_$suffix", "")
                    }
                    if (feature.endsWith("-$suffix")) {
                        feature = feature.replace("-$suffix", "")
                    }
                }
            }

        }
        feature = feature
            .replace("/", "-")
            .replace("_", "-")
        return feature
    }

    fun getMainBranch(): String {
        return "main"
    }

    fun isFeatBranch(branch: String): Boolean {
        return branch.startsWith("feat/")
    }

    fun isFixBranch(branch: String): Boolean {
        return branch.startsWith("fix/")
    }

    fun isDevBranch(branch: String): Boolean {
        return branch.startsWith("dev-")
    }


    fun isFeatureBranch(branch: String): Boolean {
        return branch.startsWith("feature-")
    }

    fun isValidBranchName(name: String): Boolean {
        return name.startsWith("feat/")
                || name.startsWith("feature-")
                || name.startsWith("fix")
                || name.startsWith("dev-")
                || name == "main"
                || name == "develop"
                || name == "master"
    }

}
