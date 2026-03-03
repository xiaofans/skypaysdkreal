package com.fataar.demo

import android.app.Application
import com.jpgk.hardwaresdk.HardwareSDK

class MainApp: Application() {

    override fun onCreate() {
        super.onCreate()
        HardwareSDK.init(this)
        HardwareSDK.initUpgradeConfig("com.jpgk.hardwaresdkdemo",".MainAct")
    }
}