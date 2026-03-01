package com.jpgk.hardwaresdk.upgrade

import android.util.Log
import com.jpgk.hardwaresdk.HardwareSDK
import com.jpgk.hardwaresdk.hardwarelogger.LocalLogsPathManager
import com.jpgk.hardwaresdk.utils.DLogger
import com.xuexiang.xupdate.XUpdate
import com.xuexiang.xupdate.service.OnFileDownloadListener
import java.io.DataOutputStream
import java.io.File

object UpgradeManager {

     fun downloadApk(apkDownloadUrl:String) {
        if (HardwareSDK.application != null){
            XUpdate.newBuild(HardwareSDK.application!!.applicationContext)
                .apkCacheDir(LocalLogsPathManager.getLogsNewPath()) // Set the root directory of the download cache
                .build()
                .download(apkDownloadUrl, object : OnFileDownloadListener {
                    override fun onStart() {

                    }


                    override fun onProgress(progress: Float, total: Long) {
                        DLogger.log("progress::$progress, totals:$total")
                    }

                    override fun onCompleted(file: File): Boolean {
                        //ToastUtils.toast("apk下载完毕，文件路径：" + file.path)
//                    _XUpdate.startInstallApk(this@DDPaySettingAct, file);
                        // 请求存储权限
                        /* if (ContextCompat.checkSelfPermission(this@DDPaySettingAct, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                             ActivityCompat.requestPermissions(this@DDPaySettingAct, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_STORAGE_PERMISSION)
                         } else {*/
                        // 权限已获取，进行安装
                        //installApk(file)
                        DLogger.log("download complete::${file.absolutePath}")
                        //installApkSilently(file.absolutePath)
                        installAndLaunchApk(file.absolutePath,"com.jpgk.vendingmachine",".splash.SplashActivity")
                        /*}*/
                        return false
                    }

                    override fun onError(throwable: Throwable) {
                        DLogger.log("OnError::",throwable.toString())
                        //downloadApk(apkDownloadUrl)
                    }
                })
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