package com.jpgk.hardwaresdk.utils

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager

class AppVersionHelper(context: Context) {

    private val appContext = context.applicationContext
    private var cachedPackageInfo: PackageInfo? = null

    /**
     * 获取版本信息（带缓存）
     */
    fun getVersionName(): String {
        return try {
            getPackageInfo().versionName ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 获取版本号（带缓存）
     */
    fun getVersionCode(): Long {
        return try {
            val packageInfo = getPackageInfo()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    /**
     * 获取应用名称
     */
    fun getAppName(): String {
        return try {
            val packageInfo = getPackageInfo()
            val appInfo = appContext.packageManager.getApplicationInfo(
                appContext.packageName,
                PackageManager.GET_META_DATA
            )
            appContext.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 获取完整的版本信息字符串
     */
    fun getFullVersionInfo(): String {
        return "${getAppName()} v${getVersionName()} (${getVersionCode()})"
    }

    /**
     * 获取 PackageInfo（带缓存）
     */
    private fun getPackageInfo(): PackageInfo {
        if (cachedPackageInfo == null) {
            cachedPackageInfo = appContext.packageManager.getPackageInfo(
                appContext.packageName,
                PackageManager.GET_CONFIGURATIONS
            )
        }
        return cachedPackageInfo!!
    }

    /**
     * 清除缓存（当应用更新后可能需要）
     */
    fun clearCache() {
        cachedPackageInfo = null
    }
}