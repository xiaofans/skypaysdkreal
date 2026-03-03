package com.jpgk.hardwaresdk.upgrade

import com.jpgk.hardwaresdk.HardwareSDK
import com.jpgk.hardwaresdk.hardwarelogger.LocalLogsPathManager
import com.jpgk.hardwaresdk.iot.IotLogger
import java.io.DataOutputStream
import java.io.File

object UpgradeManager {

     fun downloadApk(apkDownloadUrl:String) {
        if (HardwareSDK.application != null){
            ApkDownloader.downloadApk(
                url = apkDownloadUrl,
                downloadDir = File(LocalLogsPathManager.getLogsNewPath()),
                onProgress = {
                    IotLogger.w("UpgradeManager","down apk onProgress:${it}")
                },
                onSuccess = { path ->
                    IotLogger.w("UpgradeManager","down apk onSuccess:${path}")
                    installAndLaunchApk(apkPath = path, packageName = HardwareSDK.packageName?:"", mainActivity = HardwareSDK.splashAct?:"")
                },
                onError = {
                    it.printStackTrace()
                    IotLogger.w("UpgradeManager","down apk onError:${it.toString()}")
                }
            )
        }else{
            IotLogger.w("UpgradeManager","NOT INIT")
        }
    }

    fun installAndLaunchApk(apkPath: String, packageName: String, mainActivity: String): Boolean {
        try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)

            // 拷贝 APK 到 /data/local/tmp 以避免存储权限问题
            val tmpApkPath = "/data/local/tmp/temp_app.apk"
            outputStream.writeBytes("cp \"$apkPath\" \"$tmpApkPath\"\n")
            outputStream.writeBytes("chmod 777 \"$tmpApkPath\"\n")

            // **提前设置应用启动任务（nohup sleep 10 && am start）**
            val startCommand = "nohup sh -c 'sleep 60 && am start -n \"$packageName/$mainActivity\"' &\n"
            outputStream.writeBytes(startCommand)
            outputStream.flush()

            // **静默安装 APK**
            outputStream.writeBytes("pm install -r \"$tmpApkPath\"\n")
            outputStream.flush()

            // 监听安装结果
            val exitCode = process.waitFor()
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            outputStream.close()

            return exitCode == 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}