package com.jpgk.hardwaresdk.iot

import com.jpgk.hardwaresdk.HardwareSDK
import com.jpgk.hardwaresdk.socket.SdkSocket

class IOTConnectionManager private constructor() {

    companion object {

        @Volatile
        private var instance: IOTConnectionManager? = null

        fun getInstance(): IOTConnectionManager {
            return instance ?: synchronized(this) {
                instance ?: IOTConnectionManager().also { instance = it }
            }
        }
    }

    private var machineCode: String? = null
    private var inited = false


    fun init(machineCode: String,iotInitListener: IotInitListener) {
        if (!HardwareSDK.initialized){
            throw Exception("请先初始化HardwareSdk!")
        }else{
            IotSdk.init(HardwareSDK.application!!.applicationContext,machineCode,iotInitListener)
        }

    }


    fun addListener(listener: IOTListener) {
        SdkSocket.addListener(listener)
    }

    fun removeListener(listener: IOTListener) {
       SdkSocket.removeListener(listener)
    }

    fun send(msg:String){
        SdkSocket.send(msg)
    }

    }