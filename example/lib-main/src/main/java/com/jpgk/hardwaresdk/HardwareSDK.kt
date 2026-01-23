package com.jpgk.hardwaresdk

import android.app.Application
import com.jpgk.hardwaresdk.hardwarelogger.SystemLogger
import com.jpgk.hardwaresdk.hardwarelogger.VendingMachineLogger
import com.jpgk.hardwaresdk.utils.AppVersionHelper

object HardwareSDK {

     var initialized = false

     var logger: VendingMachineLogger? = null
     var application:Application? = null
     var appVersion:String? = ""
    fun init(
        application: Application
    ) {
        if (initialized) return
        this.application = application
        initLoggers()
        // 1. 初始化日志
        // 2. 初始化 Socket
        // 3. 初始化设备
        initialized = true
        appVersion = AppVersionHelper(application).getVersionName()
    }


    fun initLoggers(){
        if (application == null){
            return
        }
        initLogger()
        SystemLogger.start(context = application!!, keepDays = 30)
    }


    private fun initLogger() {
        if (application == null){
            return
        }
        logger = VendingMachineLogger(application!!)
        logger!!.log("SYSTEM", "Logger", "日志系统初始化完成")
    }

}
