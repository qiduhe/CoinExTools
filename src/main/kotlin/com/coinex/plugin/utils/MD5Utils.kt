package com.coinex.plugin.utils

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * MD5 工具类
 * 提供字符串 MD5 加密功能
 */
object MD5Utils {

    /**
     * 将字节数组转换为十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = "0123456789abcdef"[v ushr 4]
            hexChars[i * 2 + 1] = "0123456789abcdef"[v and 0x0F]
        }
        return String(hexChars)
    }

    /**
     * 对字符串进行 MD5 加密
     * @param input 需要加密的字符串
     * @return MD5 加密后的字符串（32位小写）
     */
    fun md5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            bytesToHex(digest)
        } catch (e: NoSuchAlgorithmException) {
            Log.e { "MD5 加密失败: ${e.message}" }
            ""
        }
    }

}